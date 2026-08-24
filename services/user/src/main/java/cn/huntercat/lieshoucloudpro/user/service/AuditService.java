package cn.huntercat.lieshoucloudpro.user.service;

import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.huntercat.lieshoucloudpro.user.domain.AuditLog;
import cn.huntercat.lieshoucloudpro.user.domain.AuditLog.Action;
import cn.huntercat.lieshoucloudpro.user.domain.AuditLog.Outcome;
import cn.huntercat.lieshoucloudpro.user.feign.AuditClient;
import cn.huntercat.lieshoucloudpro.user.feign.CreateAuditLogRequest;

/**
 * 操作审计服务（DATA_SECURITY.md §7 · append-only · ADR-0030 Stage 2：上提 core.audit）.
 *
 * <p>写路径从本地 audit_logs 表改为 Feign → 统一审计服务（lieshoucloud-audit / audit_events 表）。任何失败 （audit 服务不可用 /
 * 网络异常）一律降级日志，<b>绝不阻塞用户/租户/角色写操作主流程</b>；本地 V6 audit_logs 表保留为 历史归档（不再写入，也不经 UI 查询）。
 */
@Service
public class AuditService {

  private static final Logger log = LoggerFactory.getLogger(AuditService.class);

  private final AuditClient auditClient;

  public AuditService(AuditClient auditClient) {
    this.auditClient = auditClient;
  }

  /** 从请求提取来源 IP（X-Forwarded-For 优先，回退 remoteAddr）与 UA */
  public static String clientIp(HttpServletRequest req) {
    String xff = req.getHeader("X-Forwarded-For");
    if (xff != null && !xff.isBlank()) {
      return xff.split(",")[0].trim();
    }
    String remote = req.getRemoteAddr();
    return remote == null ? null : remote;
  }

  public static String userAgent(HttpServletRequest req) {
    String ua = req.getHeader("User-Agent");
    return (ua == null || ua.isBlank()) ? null : (ua.length() > 255 ? ua.substring(0, 255) : ua);
  }

  /** 记录一次操作（Feign → core.audit · 失败降级日志，不影响业务） */
  public AuditLog record(
      Long tenantId,
      Long userId,
      Action action,
      String resourceType,
      Long resourceId,
      String detail,
      String sourceIp,
      String userAgent,
      Outcome outcome,
      String requestId) {
    CreateAuditLogRequest req =
        new CreateAuditLogRequest(
            userId,
            action.name(),
            resourceType,
            resourceId,
            truncate(detail, 500),
            sourceIp,
            userAgent,
            outcome.name(),
            requestId,
            "user");
    try {
      auditClient.create(req, tenantId == null ? null : String.valueOf(tenantId));
      return toDto(tenantId, req, outcome);
    } catch (Exception e) {
      log.warn("审计投递失败（不影响业务主流程）: action={} resource={}/{}", action, resourceType, resourceId, e);
      return null;
    }
  }

  /** 便捷：记录成功操作（作用域租户 = 请求租户；平台操作用操作者租户兜底） */
  public void recordSuccess(
      Long tenantId,
      Long userId,
      Action action,
      String resourceType,
      Long resourceId,
      String detail,
      HttpServletRequest req) {
    record(
        tenantId,
        userId,
        action,
        resourceType,
        resourceId,
        detail,
        clientIp(req),
        userAgent(req),
        Outcome.SUCCESS,
        req.getHeader("X-Request-Id"));
  }

  private static AuditLog toDto(Long tenantId, CreateAuditLogRequest req, Outcome outcome) {
    AuditLog log = new AuditLog();
    log.setTenantId(tenantId);
    log.setUserId(req.userId());
    log.setAction(Action.valueOf(req.action()));
    log.setResourceType(req.resourceType());
    log.setResourceId(req.resourceId());
    log.setDetail(req.detail());
    log.setSourceIp(req.sourceIp());
    log.setUserAgent(req.userAgent());
    log.setOutcome(outcome);
    log.setRequestId(req.requestId());
    return log;
  }

  private static String truncate(String s, int max) {
    if (s == null) return null;
    return s.length() <= max ? s : s.substring(0, max);
  }
}

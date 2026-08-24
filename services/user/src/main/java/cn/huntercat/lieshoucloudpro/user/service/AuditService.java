package cn.huntercat.lieshoucloudpro.user.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;

import cn.huntercat.lieshoucloudpro.user.domain.AuditLog;
import cn.huntercat.lieshoucloudpro.user.domain.AuditLog.Action;
import cn.huntercat.lieshoucloudpro.user.domain.AuditLog.Outcome;
import cn.huntercat.lieshoucloudpro.user.domain.AuditLogRepository;

/**
 * 操作审计服务（DATA_SECURITY.md §7 · append-only）.
 *
 * <p>record() 使用 {@code REQUIRES_NEW} 独立事务：即使业务事务回滚，审计也落库 （审计关注「发生了什么尝试」，不随业务成败回滚）。
 */
@Service
public class AuditService {

  private final AuditLogRepository repo;

  public AuditService(AuditLogRepository repo) {
    this.repo = repo;
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

  /** 记录一次操作（独立事务，业务回滚不影响审计） */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
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
    AuditLog log = new AuditLog();
    log.setTenantId(tenantId);
    log.setUserId(userId);
    log.setAction(action);
    log.setResourceType(resourceType);
    log.setResourceId(resourceId);
    log.setDetail(truncate(detail, 500));
    log.setSourceIp(sourceIp);
    log.setUserAgent(userAgent);
    log.setOutcome(outcome);
    log.setRequestId(requestId);
    return repo.save(log);
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

  private static String truncate(String s, int max) {
    if (s == null) return null;
    return s.length() <= max ? s : s.substring(0, max);
  }
}

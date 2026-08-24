package cn.huntercat.lieshoucloudpro.user.feign;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import org.springframework.cloud.openfeign.FeignClient;

import java.util.Map;

/**
 * user-service → core.audit 投递审计（ADR-0030 Stage 2 · 统一审计库上提）.
 *
 * <p>调用失败由 AuditService 降级日志，<b>绝不阻塞用户/租户/角色写操作主流程</b>（与审批站内信同一降级哲学）。
 */
@FeignClient(name = "lieshoucloud-audit", path = "/api/audit-logs")
public interface AuditClient {

  @PostMapping
  Map<String, Object> create(
      @RequestBody CreateAuditLogRequest body,
      @RequestHeader(value = "X-Tenant-Id", required = false) String tenantId);
}

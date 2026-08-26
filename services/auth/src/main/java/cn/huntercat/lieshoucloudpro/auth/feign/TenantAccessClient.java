package cn.huntercat.lieshoucloudpro.auth.feign;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import org.springframework.cloud.openfeign.FeignClient;

import cn.huntercat.lieshoucloudpro.auth.feign.dto.TenantAccessItem;
import java.util.List;

/**
 * 集团版子公司切换器数据源（Phase 1 §3.2 统一账号）.
 *
 * <p>从 user-service 拉用户可访问租户列表（主属 + user_tenant_grants）。 透传 X-User-Id（本人）放行；user-service {@code GET
 * /api/tenant-access/user/{userId}}.
 */
@FeignClient(
    name = "lieshoucloud-user",
    contextId = "tenantAccessClient",
    path = "/api/tenant-access")
public interface TenantAccessClient {

  @GetMapping("/user/{userId}")
  List<TenantAccessItem> tenantAccess(
      @PathVariable Long userId, @RequestHeader("X-User-Id") Long callerId);
}

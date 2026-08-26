package cn.huntercat.lieshoucloudpro.user.service.dto;

import java.util.List;

/**
 * 租户访问上下文 DTO（自 TenantAccessController 内联 record 下沉，供 Controller / TenantAccessService / Local
 * 适配器共用）.
 */
public final class TenantAccessDtos {

  private TenantAccessDtos() {}

  /** 可访问租户项：主属租户（primary=true）+ 跨公司授权（user_tenant_grants） */
  public record TenantAccessItem(
      Long tenantId,
      String tenantCode,
      String tenantName,
      String edition,
      List<String> roles,
      List<String> permissions,
      boolean primary) {}
}

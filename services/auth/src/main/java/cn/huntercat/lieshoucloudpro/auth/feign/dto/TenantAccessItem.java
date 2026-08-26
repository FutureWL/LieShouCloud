package cn.huntercat.lieshoucloudpro.auth.feign.dto;

import java.util.List;

/**
 * 用户可访问租户项（集团版子公司切换器 · Phase 1 §3.2）.
 *
 * <p>从 user-service {@code /api/tenant-access/user/{userId}} 拉取： 主属租户（primary=true）+
 * 跨公司授权（user_tenant_grants）。
 */
public record TenantAccessItem(
    Long tenantId,
    String tenantCode,
    String tenantName,
    String edition,
    List<String> roles,
    boolean primary) {}

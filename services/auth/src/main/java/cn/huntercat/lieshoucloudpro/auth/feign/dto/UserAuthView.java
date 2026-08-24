package cn.huntercat.lieshoucloudpro.auth.feign.dto;

import java.util.List;

/**
 * auth-service 从 user-service 拉到的鉴权视图（包含 passwordHash）.
 *
 * <p>仅在内部 service-to-service 调用使用；不会暴露给前端.
 *
 * <p>Phase 5 SpringDoc: UserAuthView 是内部 DTO, 不参与 OpenAPI spec 生成.
 *
 * <p>Phase 6（ADR-0021）: 新增 {@code status}，登录时校验账户状态；null 兜底为 ACTIVE（兼容旧 user-service）.
 *
 * <p>Phase 8（ADR-0022）: 新增 {@code tenantId} / {@code tenantCode}，JWT 带租户维度.
 *
 * <p>Phase 10（ADR-0035）: 新增 {@code tenantName} / {@code tenantEdition}，登录时返回租户品牌/版别信息.
 */
public record UserAuthView(
    Long id,
    Long tenantId,
    String tenantCode,
    String tenantName,
    String tenantEdition,
    String username,
    String displayName,
    String passwordHash,
    List<String> roles,
    String status) {}

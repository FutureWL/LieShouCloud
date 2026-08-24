package cn.huntercat.lieshoucloudpro.user.feign;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 审计投递请求（user → core.audit · 与 audit-service 的 CreateAuditLogRequest 字段对齐）.
 *
 * <p>字段覆盖 DATA_SECURITY §7 六要素 + 来源服务；action/outcome 为字符串（兼容现有枚举），sourceService 固定为 {@code user}。
 */
public record CreateAuditLogRequest(
    Long userId,
    @NotBlank @Size(max = 16) String action,
    @NotBlank @Size(max = 64) String resourceType,
    Long resourceId,
    @Size(max = 500) String detail,
    @Size(max = 64) String sourceIp,
    @Size(max = 255) String userAgent,
    @NotBlank @Size(max = 16) String outcome,
    @Size(max = 64) String requestId,
    @NotBlank @Size(max = 64) @Schema(hidden = true) String sourceService) {}

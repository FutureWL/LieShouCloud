package cn.huntercat.lieshoucloudpro.admin.feign.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/**
 * Admin 服务看到的 user-service 用户视图（DTO）。
 *
 * <p>Phase 3 暂不复用 user-service 实体类（避免跨服务耦合），用 DTO 解耦。 Phase 5 起用 {@link Schema} 注解让 SpringDoc 把本
 * DTO 映射成 OpenAPI schema (UserDTO)。
 */
@Schema(
    description = "User view as exposed by admin-service (DTO, decoupled from user-service entity)")
public record UserDTO(
    @Schema(description = "User id", example = "1") Long id,
    @Schema(description = "Username", example = "futurewl") String username,
    @Schema(description = "Display name", example = "Future Wang") String displayName,
    @Schema(description = "Created at (UTC)") Instant createdAt) {}

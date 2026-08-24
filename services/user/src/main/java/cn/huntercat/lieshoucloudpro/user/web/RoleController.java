package cn.huntercat.lieshoucloudpro.user.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import cn.huntercat.lieshoucloudpro.user.domain.AuditLog;
import cn.huntercat.lieshoucloudpro.user.domain.Role;
import cn.huntercat.lieshoucloudpro.user.domain.RoleRepository;
import cn.huntercat.lieshoucloudpro.user.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;

/**
 * 角色管理端点（RBAC · ADR-0024）.
 *
 * <p>读：PLATFORM_ADMIN 或 TENANT_ADMIN（租户内管理员需要角色选项）；写：PLATFORM_ADMIN。
 */
@RestController
@RequestMapping("/api/roles")
@Tag(name = "Role", description = "Role definitions (RBAC)")
public class RoleController {

  private final RoleRepository repo;
  private final AuditService audit;

  public RoleController(RoleRepository repo, AuditService audit) {
    this.repo = repo;
    this.audit = audit;
  }

  @Operation(summary = "List roles", description = "PLATFORM_ADMIN or TENANT_ADMIN can read.")
  @ApiResponse(responseCode = "200", description = "List of roles")
  @ApiResponse(responseCode = "403", description = "Forbidden")
  @GetMapping
  public ResponseEntity<?> list(
      @org.springframework.web.bind.annotation.RequestHeader(
              value = "X-User-Roles",
              required = false)
          String rolesHeader) {
    if (!AuthRoles.hasAny(rolesHeader, AuthRoles.PLATFORM_ADMIN, AuthRoles.TENANT_ADMIN)) {
      return forbidden();
    }
    return ResponseEntity.ok(repo.findByOrderByScopeAscIdAsc());
  }

  @Operation(summary = "Create custom role", description = "PLATFORM_ADMIN only.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Created"),
    @ApiResponse(responseCode = "400", description = "Code taken"),
    @ApiResponse(responseCode = "403", description = "Forbidden")
  })
  @PostMapping
  public ResponseEntity<?> create(
      @Valid @RequestBody CreateRoleRequest body,
      @org.springframework.web.bind.annotation.RequestHeader(
              value = "X-User-Roles",
              required = false)
          String rolesHeader,
      @org.springframework.web.bind.annotation.RequestHeader(value = "X-User-Id", required = false)
          String userIdHeader,
      @org.springframework.web.bind.annotation.RequestHeader(
              value = "X-Tenant-Id",
              required = false)
          String tenantHeader,
      jakarta.servlet.http.HttpServletRequest req) {
    if (!AuthRoles.hasAny(rolesHeader, AuthRoles.PLATFORM_ADMIN)) {
      return forbidden();
    }
    if (repo.findByCode(body.code()).isPresent()) {
      return ResponseEntity.badRequest().body(Map.of("error", "ROLE_CODE_TAKEN"));
    }
    Role.Scope scopeRole =
        body.scope() == null ? Role.Scope.TENANT : Role.Scope.valueOf(body.scope());
    Role saved =
        repo.save(new Role(body.code(), body.name(), scopeRole, body.description(), false));
    audit.recordSuccess(
        parseLong(tenantHeader),
        parseLong(userIdHeader),
        AuditLog.Action.CREATE,
        "ROLE",
        saved.getId(),
        "创建角色 " + saved.getCode(),
        req);
    return ResponseEntity.ok(saved);
  }

  @Operation(
      summary = "Update role (name/description/scope)",
      description = "PLATFORM_ADMIN only; code immutable; system roles read-only.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Updated"),
    @ApiResponse(responseCode = "400", description = "System role is read-only"),
    @ApiResponse(responseCode = "403", description = "Forbidden"),
    @ApiResponse(responseCode = "404", description = "Role not found")
  })
  @PutMapping("/{id}")
  public ResponseEntity<?> update(
      @Parameter(description = "Role id", example = "1") @PathVariable Long id,
      @Valid @RequestBody UpdateRoleRequest body,
      @org.springframework.web.bind.annotation.RequestHeader(
              value = "X-User-Roles",
              required = false)
          String rolesHeader,
      @org.springframework.web.bind.annotation.RequestHeader(value = "X-User-Id", required = false)
          String userIdHeader,
      @org.springframework.web.bind.annotation.RequestHeader(
              value = "X-Tenant-Id",
              required = false)
          String tenantHeader,
      jakarta.servlet.http.HttpServletRequest req) {
    if (!AuthRoles.hasAny(rolesHeader, AuthRoles.PLATFORM_ADMIN)) {
      return forbidden();
    }
    return repo.findById(id)
        .map(
            r -> {
              if (r.isSystem()) {
                return ResponseEntity.badRequest().body(Map.of("error", "SYSTEM_ROLE_READONLY"));
              }
              if (body.name() != null && !body.name().isBlank()) r.setName(body.name());
              if (body.description() != null) r.setDescription(body.description());
              if (body.scope() != null && !body.scope().isBlank()) {
                r.setScope(Role.Scope.valueOf(body.scope()));
              }
              Role saved = repo.save(r);
              audit.recordSuccess(
                  parseLong(tenantHeader),
                  parseLong(userIdHeader),
                  AuditLog.Action.UPDATE,
                  "ROLE",
                  saved.getId(),
                  "更新角色 " + saved.getCode(),
                  req);
              return ResponseEntity.ok(saved);
            })
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  @Operation(
      summary = "Delete custom role",
      description = "PLATFORM_ADMIN only; system roles cannot be deleted.")
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "Deleted"),
    @ApiResponse(responseCode = "400", description = "System role cannot be deleted"),
    @ApiResponse(responseCode = "403", description = "Forbidden"),
    @ApiResponse(responseCode = "404", description = "Role not found")
  })
  @DeleteMapping("/{id}")
  public ResponseEntity<?> delete(
      @Parameter(description = "Role id", example = "1") @PathVariable Long id,
      @org.springframework.web.bind.annotation.RequestHeader(
              value = "X-User-Roles",
              required = false)
          String rolesHeader,
      @org.springframework.web.bind.annotation.RequestHeader(value = "X-User-Id", required = false)
          String userIdHeader,
      @org.springframework.web.bind.annotation.RequestHeader(
              value = "X-Tenant-Id",
              required = false)
          String tenantHeader,
      jakarta.servlet.http.HttpServletRequest req) {
    if (!AuthRoles.hasAny(rolesHeader, AuthRoles.PLATFORM_ADMIN)) {
      return forbidden();
    }
    java.util.Optional<Role> opt = repo.findById(id);
    if (opt.isEmpty()) {
      return ResponseEntity.notFound().build();
    }
    if (opt.get().isSystem()) {
      return ResponseEntity.badRequest().body(Map.of("error", "SYSTEM_ROLE_READONLY"));
    }
    Role r = opt.get();
    repo.delete(r);
    audit.recordSuccess(
        parseLong(tenantHeader),
        parseLong(userIdHeader),
        AuditLog.Action.DELETE,
        "ROLE",
        id,
        "删除角色 " + r.getCode(),
        req);
    return ResponseEntity.noContent().build();
  }

  private static Long parseLong(String s) {
    if (s == null || s.isBlank()) return null;
    try {
      return Long.parseLong(s.trim());
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private ResponseEntity<Object> forbidden() {
    return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
        .body(Map.of("error", "FORBIDDEN", "message", "insufficient role"));
  }

  public record CreateRoleRequest(
      @jakarta.validation.constraints.NotBlank String code,
      @jakarta.validation.constraints.NotBlank String name,
      String scope,
      String description) {}

  public record UpdateRoleRequest(String name, String scope, String description) {}
}

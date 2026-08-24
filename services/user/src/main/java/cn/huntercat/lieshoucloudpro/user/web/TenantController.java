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
import cn.huntercat.lieshoucloudpro.user.domain.Tenant;
import cn.huntercat.lieshoucloudpro.user.domain.TenantRepository;
import cn.huntercat.lieshoucloudpro.user.domain.UserRepository;
import cn.huntercat.lieshoucloudpro.user.service.AuditService;
import cn.huntercat.lieshoucloudpro.user.service.TenantRegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;

/**
 * 租户管理端点（多租户 · ADR-0022 · Phase 8 运营视角）.
 *
 * <p>完整路径含上下文：{@code /api/tenants/**}（由 gateway 转发，需 JWT 鉴权——平台运营操作）。
 *
 * <p>RBAC（ADR-0024）：全部端点要求 {@code PLATFORM_ADMIN}（X-User-Roles header，由 gateway 从 JWT 注入）。
 */
@RestController
@RequestMapping("/api/tenants")
@Tag(
    name = "Tenant",
    description = "Tenant provisioning / lifecycle (platform ops, PLATFORM_ADMIN)")
public class TenantController {

  private final TenantRepository repo;
  private final UserRepository userRepo;
  private final AuditService audit;
  private final TenantRegistrationService registration;

  public TenantController(
      TenantRepository repo,
      UserRepository userRepo,
      AuditService audit,
      TenantRegistrationService registration) {
    this.repo = repo;
    this.userRepo = userRepo;
    this.audit = audit;
    this.registration = registration;
  }

  @Operation(summary = "List all tenants", description = "Return every tenant (no pagination yet).")
  @ApiResponse(responseCode = "200", description = "List of tenants (may be empty)")
  @ApiResponse(responseCode = "403", description = "Requires PLATFORM_ADMIN")
  @GetMapping
  public ResponseEntity<?> list(
      @org.springframework.web.bind.annotation.RequestHeader(
              value = "X-User-Roles",
              required = false)
          String rolesHeader) {
    if (!AuthRoles.hasAny(rolesHeader, AuthRoles.PLATFORM_ADMIN)) {
      return forbidden();
    }
    return ResponseEntity.ok(repo.findAll());
  }

  @Operation(summary = "Get tenant by id")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Tenant found"),
    @ApiResponse(responseCode = "403", description = "Requires PLATFORM_ADMIN"),
    @ApiResponse(responseCode = "404", description = "Tenant not found")
  })
  @GetMapping("/{id}")
  public ResponseEntity<?> get(
      @Parameter(description = "Tenant id", example = "1") @PathVariable Long id,
      @org.springframework.web.bind.annotation.RequestHeader(
              value = "X-User-Roles",
              required = false)
          String rolesHeader) {
    if (!AuthRoles.hasAny(rolesHeader, AuthRoles.PLATFORM_ADMIN)) {
      return forbidden();
    }
    return repo.findById(id)
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  @Operation(
      summary = "Self-service tenant registration (public)",
      description = "创建租户 + 管理员（TENANT_ADMIN），注册即开通（ACTIVE 可直接登录）。公开端点，无鉴权。")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Registered tenant + admin"),
    @ApiResponse(responseCode = "400", description = "Invalid input / code taken / weak password")
  })
  @PostMapping("/register")
  public ResponseEntity<?> register(
      @Valid @RequestBody RegisterTenantRequest body, jakarta.servlet.http.HttpServletRequest req) {
    try {
      TenantRegistrationService.RegistrationResult result =
          registration.register(
              body.tenantName(),
              body.tenantCode(),
              body.username(),
              body.displayName(),
              body.password(),
              body.email());
      audit.recordSuccess(
          result.tenant().getId(),
          null,
          AuditLog.Action.CREATE,
          "TENANT",
          result.tenant().getId(),
          "自助开通租户 " + result.tenant().getName() + " (" + result.tenant().getCode() + ")",
          req);
      return ResponseEntity.ok(
          Map.of(
              "tenant", result.tenant(),
              "adminUsername", result.adminUsername(),
              "adminDisplayName", result.adminDisplayName()));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest()
          .body(Map.of("error", "REGISTER_INVALID", "message", e.getMessage()));
    }
  }

  @Operation(
      summary = "Provision tenant (开租户)",
      description =
          "Create a tenant with unique code. This is the core 'offline sale -> provision' step.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Provisioned tenant"),
    @ApiResponse(responseCode = "400", description = "Invalid input / code already taken"),
    @ApiResponse(responseCode = "403", description = "Requires PLATFORM_ADMIN")
  })
  @PostMapping
  public ResponseEntity<?> create(
      @Valid @RequestBody CreateTenantRequest body,
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
      return ResponseEntity.badRequest()
          .body(Map.of("error", "TENANT_CODE_TAKEN", "code", body.code()));
    }
    Tenant.Edition edition = parseEdition(body.edition());
    if (body.edition() != null && !body.edition().isBlank() && edition == null) {
      return ResponseEntity.badRequest().body(Map.of("error", "INVALID_EDITION"));
    }
    Tenant t = new Tenant(body.name(), body.code(), edition);
    Tenant saved = repo.save(t);
    audit.recordSuccess(
        parseLong(tenantHeader),
        parseLong(userIdHeader),
        AuditLog.Action.CREATE,
        "TENANT",
        saved.getId(),
        "开通租户 " + saved.getName() + " (" + saved.getCode() + ")",
        req);
    return ResponseEntity.ok(saved);
  }

  @Operation(
      summary = "Update tenant (name / status)",
      description = "Rename or enable/disable a tenant. DISABLED blocks login for its users.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Updated tenant"),
    @ApiResponse(responseCode = "403", description = "Requires PLATFORM_ADMIN"),
    @ApiResponse(responseCode = "404", description = "Tenant not found"),
    @ApiResponse(responseCode = "400", description = "Invalid status value")
  })
  @PutMapping("/{id}")
  public ResponseEntity<?> update(
      @Parameter(description = "Tenant id", example = "1") @PathVariable Long id,
      @Valid @RequestBody UpdateTenantRequest body,
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
            t -> {
              if (body.name() != null && !body.name().isBlank()) t.setName(body.name());
              if (body.status() != null && !body.status().isBlank()) {
                try {
                  t.setStatus(Tenant.Status.valueOf(body.status()));
                } catch (IllegalArgumentException e) {
                  return ResponseEntity.badRequest().body(Map.of("error", "INVALID_STATUS"));
                }
              }
              if (body.edition() != null && !body.edition().isBlank()) {
                try {
                  t.setEdition(Tenant.Edition.valueOf(body.edition()));
                } catch (IllegalArgumentException e) {
                  return ResponseEntity.badRequest().body(Map.of("error", "INVALID_EDITION"));
                }
              }
              Tenant saved = repo.save(t);
              audit.recordSuccess(
                  parseLong(tenantHeader),
                  parseLong(userIdHeader),
                  AuditLog.Action.UPDATE,
                  "TENANT",
                  saved.getId(),
                  "更新租户 " + saved.getName() + " (" + saved.getCode() + ")",
                  req);
              return ResponseEntity.ok(saved);
            })
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  /** 删除租户（仅当无用户时允许；有用户 → 409，建议改用停用 status=DISABLED）. */
  @Operation(
      summary = "Delete tenant (only when it has no users)",
      description =
          "Hard delete is only allowed for empty tenants. Use DISABLED status to decommission tenants with users.")
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "Deleted"),
    @ApiResponse(responseCode = "403", description = "Requires PLATFORM_ADMIN"),
    @ApiResponse(responseCode = "404", description = "Tenant not found"),
    @ApiResponse(
        responseCode = "409",
        description = "Tenant still has users; decommission with DISABLED instead")
  })
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(
      @Parameter(description = "Tenant id", example = "1") @PathVariable Long id,
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
      return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).build();
    }
    java.util.Optional<Tenant> opt = repo.findById(id);
    if (opt.isEmpty()) {
      return ResponseEntity.notFound().build();
    }
    long userCount = userRepo.countByTenantId(id);
    if (userCount > 0) {
      return ResponseEntity.status(org.springframework.http.HttpStatus.CONFLICT).build();
    }
    Tenant t = opt.get();
    repo.delete(t);
    audit.recordSuccess(
        parseLong(tenantHeader),
        parseLong(userIdHeader),
        AuditLog.Action.DELETE,
        "TENANT",
        id,
        "删除租户 " + t.getName() + " (" + t.getCode() + ")",
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
        .body(Map.of("error", "FORBIDDEN", "message", "requires PLATFORM_ADMIN"));
  }

  /** Phase 8: CreateTenantRequest DTO（内联；edition 可选默认 GENERIC · ADR-0035） */
  public record CreateTenantRequest(
      @jakarta.validation.constraints.NotBlank String name,
      @jakarta.validation.constraints.NotBlank String code,
      String edition) {}

  /** 租户自助开通请求体（公开端点 · SaaS 增长路径 · issue #24） */
  public record RegisterTenantRequest(
      @jakarta.validation.constraints.NotBlank String tenantName,
      @jakarta.validation.constraints.NotBlank String tenantCode,
      @jakarta.validation.constraints.NotBlank String username,
      @jakarta.validation.constraints.NotBlank String displayName,
      @jakarta.validation.constraints.NotBlank String password,
      String email) {}

  /** Phase 8: UpdateTenantRequest DTO（内联；name/status/edition 均可选） */
  public record UpdateTenantRequest(String name, String status, String edition) {}

  /** 解析版别（空 → GENERIC；非法 → null 由调用方判定） */
  private static Tenant.Edition parseEdition(String edition) {
    if (edition == null || edition.isBlank()) return Tenant.Edition.GENERIC;
    try {
      return Tenant.Edition.valueOf(edition);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }
}

package cn.huntercat.lieshoucloudpro.user.web;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
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
import cn.huntercat.lieshoucloudpro.user.domain.PermissionRepository;
import cn.huntercat.lieshoucloudpro.user.domain.Role;
import cn.huntercat.lieshoucloudpro.user.domain.RoleRepository;
import cn.huntercat.lieshoucloudpro.user.domain.Tenant;
import cn.huntercat.lieshoucloudpro.user.domain.TenantInvite;
import cn.huntercat.lieshoucloudpro.user.domain.TenantInviteRepository;
import cn.huntercat.lieshoucloudpro.user.domain.TenantRepository;
import cn.huntercat.lieshoucloudpro.user.domain.User;
import cn.huntercat.lieshoucloudpro.user.domain.UserRepository;
import cn.huntercat.lieshoucloudpro.user.service.AuditService;
import cn.huntercat.lieshoucloudpro.user.service.MenuService;
import cn.huntercat.lieshoucloudpro.user.web.dto.UserAuthView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * User 服务 REST 端点.
 *
 * <p>完整路径含上下文：{@code /api/users/**}（由 gateway 转发）.
 *
 * @see .ai/decisions/0016-springdoc-openapi.md
 * @see .ai/decisions/0017-spring-security-jwt.md
 */
@RestController
@RequestMapping("/api/users")
@Tag(name = "User", description = "User CRUD + lookup endpoints")
public class UserController {

  private final UserRepository repo;
  private final TenantRepository tenantRepo;
  private final TenantInviteRepository inviteRepo;
  private final RoleRepository roleRepo;
  private final PermissionRepository permissionRepo;
  private final MenuService menuService;
  private final AuditService audit;
  private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

  /** 默认租户编码（兼容未显式传租户的调用） · ADR-0022 */
  private static final String DEFAULT_TENANT_CODE = "huntercat";

  public UserController(
      UserRepository repo,
      TenantRepository tenantRepo,
      TenantInviteRepository inviteRepo,
      RoleRepository roleRepo,
      PermissionRepository permissionRepo,
      MenuService menuService,
      AuditService audit) {
    this.repo = repo;
    this.tenantRepo = tenantRepo;
    this.inviteRepo = inviteRepo;
    this.roleRepo = roleRepo;
    this.permissionRepo = permissionRepo;
    this.menuService = menuService;
    this.audit = audit;
  }

  @Operation(
      summary = "List users",
      description = "Tenant-scoped: if X-Tenant-Id header present, only that tenant's users.")
  @ApiResponse(responseCode = "200", description = "List of users (may be empty)")
  @GetMapping
  public ResponseEntity<?> list(
      @org.springframework.web.bind.annotation.RequestHeader(
              value = "X-Tenant-Id",
              required = false)
          String tenantHeader,
      @org.springframework.web.bind.annotation.RequestHeader(
              value = "X-User-Roles",
              required = false)
          String rolesHeader) {
    Long tid = parseTenantHeader(tenantHeader);
    // 租户内请求强制过滤（ADR-0022 安全关键）：只能看自己租户的用户
    if (tid != null) {
      return ResponseEntity.ok(repo.findByTenantId(tid));
    }
    // 无租户上下文 = 跨租户平台查看 → 需 PLATFORM_ADMIN（RBAC · ADR-0024）
    if (!AuthRoles.hasAny(rolesHeader, AuthRoles.PLATFORM_ADMIN)) {
      return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
          .body(Map.of("error", "FORBIDDEN"));
    }
    return ResponseEntity.ok(repo.findAll());
  }

  @Operation(summary = "Count users")
  @GetMapping("/count")
  public ResponseEntity<?> count(
      @org.springframework.web.bind.annotation.RequestHeader(
              value = "X-Tenant-Id",
              required = false)
          String tenantHeader,
      @org.springframework.web.bind.annotation.RequestHeader(
              value = "X-User-Roles",
              required = false)
          String rolesHeader) {
    Long tid = parseTenantHeader(tenantHeader);
    if (tid != null) {
      return ResponseEntity.ok(repo.countByTenantId(tid));
    }
    if (!AuthRoles.hasAny(rolesHeader, AuthRoles.PLATFORM_ADMIN)) {
      return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
          .body(Map.of("error", "FORBIDDEN"));
    }
    return ResponseEntity.ok(repo.count());
  }

  @Operation(
      summary = "Get user by id",
      description = "Tenant-scoped: cross-tenant access returns 404.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "User found"),
    @ApiResponse(responseCode = "404", description = "User not found (or cross-tenant)")
  })
  @GetMapping("/{id}")
  public ResponseEntity<User> get(
      @Parameter(description = "User id", example = "1") @PathVariable Long id,
      @org.springframework.web.bind.annotation.RequestHeader(
              value = "X-Tenant-Id",
              required = false)
          String tenantHeader) {
    Long tid = parseTenantHeader(tenantHeader);
    return repo.findById(id)
        .filter(u -> tenantMatches(u, tid))
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  @Operation(
      summary = "Create user",
      description =
          "Body must include username + displayName + plaintext password (will be hashed). email/phone optional.")
  @ApiResponse(responseCode = "200", description = "Created user with assigned id + passwordHash")
  @PostMapping
  public ResponseEntity<?> create(
      @Valid @RequestBody CreateUserRequest body,
      @org.springframework.web.bind.annotation.RequestHeader(
              value = "X-Tenant-Id",
              required = false)
          String tenantHeader,
      @org.springframework.web.bind.annotation.RequestHeader(value = "X-User-Id", required = false)
          String userIdHeader,
      jakarta.servlet.http.HttpServletRequest req) {
    Long forcedTenantId = parseTenantHeader(tenantHeader);
    Tenant tenant = null;
    String role = "USER";
    if (body.inviteCode() != null && !body.inviteCode().isBlank()) {
      // —— 邀请码优先（ADR-0023 Phase 2）：租户/角色来自邀请码 ——
      TenantInvite invite = inviteRepo.findByCode(body.inviteCode()).orElse(null);
      if (invite == null || !invite.isValid()) {
        return ResponseEntity.badRequest().body(Map.of("error", "INVALID_INVITE"));
      }
      // 租户内请求强制：邀请码租户必须与请求租户一致，否则拒绝
      if (forcedTenantId != null && !invite.getTenantId().equals(forcedTenantId)) {
        return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
            .body(Map.of("error", "INVITE_TENANT_MISMATCH"));
      }
      tenant = tenantRepo.findById(invite.getTenantId()).orElse(null);
      if (tenant == null || tenant.getStatus() != Tenant.Status.ACTIVE) {
        return ResponseEntity.badRequest().body(Map.of("error", "TENANT_NOT_ACTIVE"));
      }
      role = invite.getRole();
      invite.consume();
      inviteRepo.save(invite);
    } else if (forcedTenantId != null) {
      // —— 租户内请求强制：只能用请求的租户创建（忽略 tenantCode）——
      tenant = tenantRepo.findById(forcedTenantId).orElse(null);
      if (tenant == null || tenant.getStatus() != Tenant.Status.ACTIVE) {
        return ResponseEntity.badRequest().body(Map.of("error", "TENANT_NOT_ACTIVE"));
      }
    } else {
      // —— 常规注册：tenantCode 指定租户（默认 huntercat）——
      String code =
          (body.tenantCode() == null || body.tenantCode().isBlank())
              ? DEFAULT_TENANT_CODE
              : body.tenantCode();
      tenant = tenantRepo.findByCode(code).orElse(null);
      if (tenant == null) {
        return ResponseEntity.badRequest()
            .body(Map.of("error", "TENANT_NOT_FOUND", "message", code));
      }
    }
    if (repo.existsByTenantIdAndUsername(tenant.getId(), body.username())) {
      return ResponseEntity.badRequest()
          .body(Map.of("error", "USERNAME_TAKEN", "message", body.username()));
    }
    User u = new User();
    u.setTenantId(tenant.getId());
    u.setUsername(body.username());
    u.setDisplayName(body.displayName());
    u.setEmail(body.email());
    u.setPhone(body.phone());
    u.setPasswordHash(encoder.encode(body.password()));
    u.setRoles(List.of(roleByCode(role)));
    User saved = repo.save(u);
    audit.recordSuccess(
        tenant.getId(),
        parseUserId(userIdHeader),
        AuditLog.Action.CREATE,
        "USER",
        saved.getId(),
        "创建用户 " + saved.getUsername(),
        req);
    // 返回带租户信息（tenantCode/tenantName/tenantEdition）——auth-service 注册后直接签发
    // JWT 需要；不返回 passwordHash（User 实体的敏感字段不外泄）。
    return ResponseEntity.ok(
        Map.of(
            "id", saved.getId(),
            "tenantId", saved.getTenantId(),
            "tenantCode", tenant.getCode(),
            "tenantName", tenant.getName(),
            "tenantEdition", tenant.getEdition() == null ? "GENERIC" : tenant.getEdition().name(),
            "username", saved.getUsername(),
            "displayName", saved.getDisplayName()));
  }

  @Operation(
      summary = "Update user (partial)",
      description =
          "Update displayName/email/phone/status/roles; password only when provided. username immutable.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Updated user"),
    @ApiResponse(responseCode = "404", description = "User not found"),
    @ApiResponse(responseCode = "400", description = "Invalid status value")
  })
  @PutMapping("/{id}")
  public ResponseEntity<?> update(
      @Parameter(description = "User id", example = "1") @PathVariable Long id,
      @Valid @RequestBody UpdateUserRequest body,
      @org.springframework.web.bind.annotation.RequestHeader(
              value = "X-Tenant-Id",
              required = false)
          String tenantHeader,
      @org.springframework.web.bind.annotation.RequestHeader(value = "X-User-Id", required = false)
          String userIdHeader,
      jakarta.servlet.http.HttpServletRequest req) {
    Long tid = parseTenantHeader(tenantHeader);
    java.util.Optional<User> opt = repo.findById(id);
    if (opt.isEmpty() || !tenantMatches(opt.get(), tid)) {
      // 不存在或跨租户 → 404（不泄露存在性）
      return ResponseEntity.notFound().build();
    }
    User u = opt.get();
    if (body.displayName() != null && !body.displayName().isBlank()) {
      u.setDisplayName(body.displayName());
    }
    if (body.email() != null && !body.email().isBlank()) {
      u.setEmail(body.email());
    }
    if (body.phone() != null && !body.phone().isBlank()) {
      u.setPhone(body.phone());
    }
    if (body.status() != null && !body.status().isBlank()) {
      try {
        u.setStatus(User.Status.valueOf(body.status()));
      } catch (IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", "INVALID_STATUS"));
      }
    }
    if (body.roles() != null && body.roles().length > 0) {
      java.util.List<Role> newRoles =
          java.util.Arrays.stream(body.roles())
              .map(this::roleByCode)
              .filter(java.util.Objects::nonNull)
              .toList();
      if (!newRoles.isEmpty()) {
        u.setRoles(newRoles);
      }
    }
    if (body.password() != null && !body.password().isBlank()) {
      u.setPasswordHash(encoder.encode(body.password()));
    }
    User saved = repo.save(u);
    audit.recordSuccess(
        u.getTenantId(),
        parseUserId(userIdHeader),
        AuditLog.Action.UPDATE,
        "USER",
        saved.getId(),
        "更新用户 " + saved.getUsername(),
        req);
    return ResponseEntity.ok(saved);
  }

  @Operation(summary = "Delete user by id")
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "Deleted"),
    @ApiResponse(responseCode = "404", description = "User not found")
  })
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(
      @Parameter(description = "User id") @PathVariable Long id,
      @org.springframework.web.bind.annotation.RequestHeader(
              value = "X-Tenant-Id",
              required = false)
          String tenantHeader,
      @org.springframework.web.bind.annotation.RequestHeader(value = "X-User-Id", required = false)
          String userIdHeader,
      jakarta.servlet.http.HttpServletRequest req) {
    Long tid = parseTenantHeader(tenantHeader);
    java.util.Optional<User> opt = repo.findById(id);
    if (opt.isEmpty() || !tenantMatches(opt.get(), tid)) {
      return ResponseEntity.notFound().build();
    }
    User u = opt.get();
    repo.deleteById(id);
    audit.recordSuccess(
        u.getTenantId(),
        parseUserId(userIdHeader),
        AuditLog.Action.DELETE,
        "USER",
        id,
        "删除用户 " + u.getUsername(),
        req);
    return ResponseEntity.noContent().build();
  }

  // ============================================================
  // 租户上下文工具（ADR-0022 安全）
  // ============================================================

  /** 解析 X-Tenant-Id header（非法/空 → null = 平台上下文） */
  private Long parseTenantHeader(String value) {
    if (value == null || value.isBlank()) return null;
    try {
      return Long.parseLong(value);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  /** 资源是否属于当前租户（无租户上下文 = 平台管理，放行） */
  private boolean tenantMatches(User u, Long tenantHeader) {
    return tenantHeader == null || u.getTenantId().equals(tenantHeader);
  }

  /** 给 admin-service Feign 用：根据 username 查 User（不含密码 hash）. */
  @Operation(summary = "Get user by username (admin Feign internal)")
  @ApiResponse(responseCode = "200", description = "User found")
  @ApiResponse(responseCode = "404", description = "User not found")
  @GetMapping("/by-username/{username}")
  public ResponseEntity<User> byUsername(
      @Parameter(description = "Username", example = "futurewl") @PathVariable String username) {
    return repo.findByUsername(username)
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  /**
   * Phase 11（ADR-0024 P2 阶段 4）: 菜单数据驱动 —— 当前用户可见菜单树.
   *
   * <p>默认清单 ⊕ 租户覆盖（tenant_menu_configs）⊕ 权限过滤（X-User-Permissions）→ 排序树。 由 gateway 经 JWT 鉴权后透传
   * X-Tenant-Id / X-User-Permissions；租户不存在 → 404。
   */
  @Operation(summary = "Get current user menu tree (data-driven · ADR-0024 P2)")
  @ApiResponse(responseCode = "200", description = "Menu tree (permission-filtered)")
  @ApiResponse(responseCode = "404", description = "Tenant not found")
  @GetMapping("/me/menus")
  public ResponseEntity<?> myMenus(
      @org.springframework.web.bind.annotation.RequestHeader(
              value = "X-Tenant-Id",
              required = false)
          String tenantHeader,
      @org.springframework.web.bind.annotation.RequestHeader(
              value = "X-User-Permissions",
              required = false)
          String permissionsHeader) {
    Long tenantId = parseUserId(tenantHeader);
    if (tenantId == null) {
      return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
          .body(Map.of("error", "TENANT_CONTEXT_REQUIRED", "message", "缺少租户上下文（X-Tenant-Id）"));
    }
    Tenant tenant = tenantRepo.findById(tenantId).orElse(null);
    if (tenant == null) {
      return ResponseEntity.notFound().build();
    }
    List<String> permissions =
        permissionsHeader == null || permissionsHeader.isBlank()
            ? List.of()
            : List.of(permissionsHeader.split(","));
    return ResponseEntity.ok(menuService.buildMenus(tenantId, tenant.getEdition(), permissions));
  }

  /**
   * Phase 5 + Phase 8: 给 auth-service Feign 用：按租户 + username 查鉴权视图（含 passwordHash）.
   *
   * <p>仅 service-to-service 调用；通过 gateway 白名单 {@code /api/users/auth/**} 路径实现.
   */
  @Operation(
      summary = "Get user auth view by tenant (service-to-service, contains passwordHash)",
      description =
          "INTERNAL endpoint for auth-service only. Resolve tenant by code then user by (tenant_id, username).")
  @ApiResponse(responseCode = "200", description = "UserAuthView returned")
  @ApiResponse(responseCode = "404", description = "Tenant or user not found")
  @GetMapping("/auth/by-tenant/{tenantCode}/{username}")
  public ResponseEntity<?> authByTenantAndUsername(
      @Parameter(description = "Tenant code", example = "huntercat") @PathVariable
          String tenantCode,
      @Parameter(description = "Username") @PathVariable String username) {
    Tenant tenant = tenantRepo.findByCode(tenantCode).orElse(null);
    if (tenant == null) {
      return ResponseEntity.notFound().build();
    }
    // Phase 8: 租户被停用 → 阻断该租户所有登录
    if (tenant.getStatus() != Tenant.Status.ACTIVE) {
      return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
          .body(Map.of("error", "TENANT_DISABLED", "tenantCode", tenantCode));
    }
    return repo.findByTenantIdAndUsername(tenant.getId(), username)
        .map(
            u ->
                ResponseEntity.ok(
                    new UserAuthView(
                        u.getId(),
                        u.getTenantId(),
                        tenant.getCode(),
                        tenant.getName(),
                        tenant.getEdition() == null ? null : tenant.getEdition().name(),
                        u.getUsername(),
                        u.getDisplayName(),
                        u.getPasswordHash(),
                        u.getRoles() == null || u.getRoles().isEmpty()
                            ? List.of("USER")
                            : u.getRoles().stream().map(Role::getCode).toList(),
                        u.getStatus() == null ? "ACTIVE" : u.getStatus().name(),
                        permissionRepo.findCodesByUserId(u.getId()))))
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  /**
   * Phase 8: 给 auth-service 用：按手机号查鉴权视图（验证码登录 · ADR-0023）.
   *
   * <p>仅 service-to-service；gateway 白名单 {@code /api/users/auth/**} 已覆盖。
   */
  @Operation(summary = "Get user auth view by phone (service-to-service)")
  @ApiResponse(responseCode = "200", description = "UserAuthView returned")
  @ApiResponse(responseCode = "404", description = "User not found")
  @GetMapping("/auth/by-phone/{phone}")
  public ResponseEntity<?> authByPhone(
      @Parameter(description = "Phone", example = "13800000000") @PathVariable String phone) {
    return repo.findByPhone(phone)
        .map(u -> ResponseEntity.ok(toAuthView(u)))
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  /**
   * Phase 8: 给 auth-service 用：按邮箱查鉴权视图（验证码登录 · ADR-0023）.
   *
   * <p>仅 service-to-service；gateway 白名单 {@code /api/users/auth/**} 已覆盖。
   */
  @Operation(summary = "Get user auth view by email (service-to-service)")
  @ApiResponse(responseCode = "200", description = "UserAuthView returned")
  @ApiResponse(responseCode = "404", description = "User not found")
  @GetMapping("/auth/by-email/{email}")
  public ResponseEntity<?> authByEmail(
      @Parameter(description = "Email", example = "user@huntercat.cn") @PathVariable String email) {
    return repo.findByEmail(email)
        .map(u -> ResponseEntity.ok(toAuthView(u)))
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  /** 按角色 code 查 Role 实体（不存在返回 null） */
  /** X-User-Id header → Long（gateway 从 JWT uid 注入）；空/非法 → null */
  private static Long parseUserId(String header) {
    if (header == null || header.isBlank()) return null;
    try {
      return Long.parseLong(header.trim());
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private Role roleByCode(String code) {
    return roleRepo.findByCode(code).orElse(null);
  }

  /** 组装 UserAuthView（含租户编码 + 角色 codes + 权限 codes · ADR-0024 Phase 2） */
  private UserAuthView toAuthView(User u) {
    Tenant tenant = tenantRepo.findById(u.getTenantId()).orElse(null);
    java.util.List<String> roleCodes =
        u.getRoles() == null || u.getRoles().isEmpty()
            ? List.of("USER")
            : u.getRoles().stream().map(Role::getCode).toList();
    java.util.List<String> permissionCodes = permissionRepo.findCodesByUserId(u.getId());
    return new UserAuthView(
        u.getId(),
        u.getTenantId(),
        tenant == null ? null : tenant.getCode(),
        tenant == null ? null : tenant.getName(),
        tenant == null || tenant.getEdition() == null ? null : tenant.getEdition().name(),
        u.getUsername(),
        u.getDisplayName(),
        u.getPasswordHash(),
        roleCodes,
        u.getStatus() == null ? "ACTIVE" : u.getStatus().name(),
        permissionCodes);
  }

  /**
   * Phase 6: auth-service 登录成功后回写最近登录时间（不暴露密码/敏感字段）.
   *
   * <p>幂等：用户不存在时静默忽略（登录已失败，无需回写）。
   */
  @Operation(
      summary = "Mark last login (service-to-service, called by auth-service on successful login)")
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "Marked"),
    @ApiResponse(responseCode = "404", description = "User not found")
  })
  @PostMapping("/{id}/login-marker")
  public ResponseEntity<Void> markLastLogin(
      @Parameter(description = "User id", example = "1") @PathVariable Long id) {
    java.util.Optional<User> opt = repo.findById(id);
    if (opt.isEmpty()) {
      return ResponseEntity.notFound().build();
    }
    User u = opt.get();
    u.setLastLoginAt(Instant.now());
    repo.save(u);
    return ResponseEntity.noContent().build();
  }

  /** Phase 5: 占位 health 端点（被 admin-service 通过 Feign + circuit breaker 调用）. */
  @Operation(summary = "Health probe")
  @GetMapping("/_health")
  public Map<String, String> health() {
    return Map.of("status", "UP", "service", "user");
  }

  /**
   * Phase 5 + Phase 8: CreateUserRequest DTO（tenantCode 可选默认 huntercat；inviteCode 可选则租户/角色来自邀请码）
   */
  public record CreateUserRequest(
      @jakarta.validation.constraints.NotBlank String username,
      @jakarta.validation.constraints.NotBlank String displayName,
      @jakarta.validation.constraints.NotBlank String password,
      String email,
      String phone,
      String tenantCode,
      String inviteCode) {}

  /** Phase 7: UpdateUserRequest DTO（内联；字段均可选，传入才更新；password 传入才改） */
  public record UpdateUserRequest(
      String displayName,
      String email,
      String phone,
      String status,
      String[] roles,
      String password) {}
}

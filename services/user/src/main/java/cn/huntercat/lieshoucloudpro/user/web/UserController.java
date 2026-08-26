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

import cn.huntercat.lieshoucloudpro.user.domain.User;
import cn.huntercat.lieshoucloudpro.user.service.UserService;
import cn.huntercat.lieshoucloudpro.user.service.dto.UserDtos.CreateUserRequest;
import cn.huntercat.lieshoucloudpro.user.service.dto.UserDtos.UpdateUserRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;

/**
 * User 服务 REST 端点（HTTP 适配层；业务规则见 {@link UserService}，ARCHITECTURE.md §4.2 下沉）.
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

  private final UserService users;

  public UserController(UserService users) {
    this.users = users;
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
    return okOrError(
        () ->
            users.list(
                TenantContext.parseLong(tenantHeader), UserService.isPlatformAdmin(rolesHeader)));
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
    return okOrError(
        () ->
            users.count(
                TenantContext.parseLong(tenantHeader), UserService.isPlatformAdmin(rolesHeader)));
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
    return users
        .findById(id, TenantContext.parseLong(tenantHeader))
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
    return okOrError(
        () ->
            users.create(
                body,
                TenantContext.parseLong(tenantHeader),
                TenantContext.parseLong(userIdHeader),
                req));
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
    return okOrError(
        () ->
            users
                .update(
                    id,
                    body,
                    TenantContext.parseLong(tenantHeader),
                    TenantContext.parseLong(userIdHeader),
                    req)
                .map(u -> (Object) u)
                .orElse(null),
        true);
  }

  @Operation(summary = "Delete user by id")
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "Deleted"),
    @ApiResponse(responseCode = "404", description = "User not found")
  })
  @DeleteMapping("/{id}")
  public ResponseEntity<?> delete(
      @Parameter(description = "User id") @PathVariable Long id,
      @org.springframework.web.bind.annotation.RequestHeader(
              value = "X-Tenant-Id",
              required = false)
          String tenantHeader,
      @org.springframework.web.bind.annotation.RequestHeader(value = "X-User-Id", required = false)
          String userIdHeader,
      jakarta.servlet.http.HttpServletRequest req) {
    boolean deleted =
        users
            .delete(
                id,
                TenantContext.parseLong(tenantHeader),
                TenantContext.parseLong(userIdHeader),
                req)
            .isPresent();
    return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
  }

  /** 给 admin-service Feign 用：根据 username 查 User（不含密码 hash）. */
  @Operation(summary = "Get user by username (admin Feign internal)")
  @ApiResponse(responseCode = "200", description = "User found")
  @ApiResponse(responseCode = "404", description = "User not found")
  @GetMapping("/by-username/{username}")
  public ResponseEntity<User> byUsername(
      @Parameter(description = "Username", example = "futurewl") @PathVariable String username) {
    return users
        .findByUsername(username)
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
    return okOrError(
        () -> users.buildMenus(TenantContext.parseLong(tenantHeader), permissionsHeader));
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
    return okOrError(() -> users.authByTenantAndUsername(tenantCode, username));
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
    return okOrError(() -> users.authByPhone(phone));
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
    return okOrError(() -> users.authByEmail(email));
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
    return users.markLastLogin(id)
        ? ResponseEntity.noContent().build()
        : ResponseEntity.notFound().build();
  }

  /** Phase 5: 占位 health 端点（被 admin-service 通过 Feign + circuit breaker 调用）. */
  @Operation(summary = "Health probe")
  @GetMapping("/_health")
  public Map<String, String> health() {
    return Map.of("status", "UP", "service", "user");
  }

  // ============================================================
  // 工具
  // ============================================================

  /** 执行业务；业务异常（UserBizException）由 {@link UserBizExceptionAdvice} 统一转译 HTTP 状态 + 错误码。 */
  private static <T> ResponseEntity<T> okOrError(java.util.function.Supplier<T> action) {
    return okOrError(action, false);
  }

  /** {@code notFoundAsNull=true} 时业务返回 null（如 update 的 Optional.empty）→ 404。 */
  private static <T> ResponseEntity<T> okOrError(
      java.util.function.Supplier<T> action, boolean notFoundAsNull) {
    T result = action.get();
    if (notFoundAsNull && result == null) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(result);
  }
}

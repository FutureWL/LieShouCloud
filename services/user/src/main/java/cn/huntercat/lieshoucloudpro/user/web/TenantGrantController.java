package cn.huntercat.lieshoucloudpro.user.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import cn.huntercat.lieshoucloudpro.user.domain.AuditLog;
import cn.huntercat.lieshoucloudpro.user.domain.Role;
import cn.huntercat.lieshoucloudpro.user.domain.RoleRepository;
import cn.huntercat.lieshoucloudpro.user.domain.Tenant;
import cn.huntercat.lieshoucloudpro.user.domain.TenantRepository;
import cn.huntercat.lieshoucloudpro.user.domain.User;
import cn.huntercat.lieshoucloudpro.user.domain.UserRepository;
import cn.huntercat.lieshoucloudpro.user.domain.UserTenantGrant;
import cn.huntercat.lieshoucloudpro.user.domain.UserTenantGrantRepository;
import cn.huntercat.lieshoucloudpro.user.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 统一账号跨公司授权（集团版 · Phase 1 §3.2）.
 *
 * <p>总部管理员（PLATFORM_ADMIN）给总部用户授予子公司租户访问权： 用户 × 子公司租户 × 角色（TENANT scope）。主属租户天然可访问，无需授权。
 *
 * <p>安全：仅 PLATFORM_ADMIN 可管理授权（跨租户操作）；角色仅限 TENANT scope。
 */
@RestController
@RequestMapping("/api/tenant-grants")
@Tag(name = "TenantGrant", description = "统一账号跨子公司授权（集团版）")
public class TenantGrantController {

  private final UserTenantGrantRepository grantRepo;
  private final UserRepository userRepo;
  private final TenantRepository tenantRepo;
  private final RoleRepository roleRepo;
  private final AuditService audit;

  public TenantGrantController(
      UserTenantGrantRepository grantRepo,
      UserRepository userRepo,
      TenantRepository tenantRepo,
      RoleRepository roleRepo,
      AuditService audit) {
    this.grantRepo = grantRepo;
    this.userRepo = userRepo;
    this.tenantRepo = tenantRepo;
    this.roleRepo = roleRepo;
    this.audit = audit;
  }

  /** 授权请求 */
  public record GrantRequest(@NotNull Long userId, @NotNull Long tenantId, @NotNull Long roleId) {}

  /** 授权视图（列表展示用） */
  public record GrantView(
      Long id,
      Long userId,
      String username,
      Long tenantId,
      String tenantCode,
      String tenantName,
      Long roleId,
      String roleCode,
      String roleName,
      java.time.Instant grantedAt) {}

  @Operation(summary = "查询用户的跨公司授权列表（PLATFORM_ADMIN）")
  @GetMapping
  public ResponseEntity<?> list(
      @RequestParam(required = false) Long userId,
      @RequestHeader(value = "X-User-Roles", required = false) String rolesHeader) {
    if (!AuthRoles.hasAny(rolesHeader, AuthRoles.PLATFORM_ADMIN)) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .body(Map.of("error", "FORBIDDEN", "message", "requires PLATFORM_ADMIN"));
    }
    List<UserTenantGrant> grants =
        userId == null ? grantRepo.findAll() : grantRepo.findByUserId(userId);
    return ResponseEntity.ok(grants.stream().map(this::toView).collect(Collectors.toList()));
  }

  @Operation(summary = "授权：用户 × 子公司租户 × 角色（PLATFORM_ADMIN）")
  @PostMapping
  @Transactional
  public ResponseEntity<?> grant(
      @Valid @RequestBody GrantRequest req,
      @RequestHeader(value = "X-User-Roles", required = false) String rolesHeader,
      @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
      @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
      HttpServletRequest request) {
    if (!AuthRoles.hasAny(rolesHeader, AuthRoles.PLATFORM_ADMIN)) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .body(Map.of("error", "FORBIDDEN", "message", "requires PLATFORM_ADMIN"));
    }
    User user =
        userRepo
            .findById(req.userId())
            .orElseThrow(() -> new IllegalArgumentException("用户不存在: " + req.userId()));
    Tenant tenant =
        tenantRepo
            .findById(req.tenantId())
            .orElseThrow(() -> new IllegalArgumentException("子公司租户不存在: " + req.tenantId()));
    Role role =
        roleRepo
            .findById(req.roleId())
            .orElseThrow(() -> new IllegalArgumentException("角色不存在: " + req.roleId()));
    if (role.getScope() != Role.Scope.TENANT) {
      return ResponseEntity.badRequest()
          .body(
              Map.of(
                  "error",
                  "INVALID_ROLE_SCOPE",
                  "message",
                  "仅租户内角色（TENANT scope）可授权给子公司，请勿使用平台角色"));
    }
    if (user.getTenantId().equals(tenant.getId())) {
      return ResponseEntity.badRequest()
          .body(
              Map.of(
                  "error", "PRIMARY_TENANT_NOT_GRANTABLE", "message", "主属租户无需授权（天然可访问），请选择子公司租户"));
    }
    if (grantRepo.existsByUserIdAndTenantIdAndRoleId(user.getId(), tenant.getId(), role.getId())) {
      return ResponseEntity.status(HttpStatus.CONFLICT)
          .body(Map.of("error", "GRANT_EXISTS", "message", "该用户在此子公司已有相同角色授权"));
    }
    UserTenantGrant grant = new UserTenantGrant(user, tenant, role, null);
    grantRepo.save(grant);
    audit.recordSuccess(
        TenantContext.parseLong(tenantHeader),
        TenantContext.parseLong(userIdHeader),
        AuditLog.Action.CREATE,
        "TENANT_GRANT",
        grant.getId(),
        "授权 " + user.getUsername() + " → " + tenant.getName() + "（" + role.getCode() + "）",
        request);
    return ResponseEntity.status(HttpStatus.CREATED).body(toView(grant));
  }

  @Operation(summary = "撤销跨公司授权（PLATFORM_ADMIN）")
  @DeleteMapping("/{id}")
  @Transactional
  public ResponseEntity<?> revoke(
      @PathVariable Long id,
      @RequestHeader(value = "X-User-Roles", required = false) String rolesHeader,
      @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
      @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
      HttpServletRequest request) {
    if (!AuthRoles.hasAny(rolesHeader, AuthRoles.PLATFORM_ADMIN)) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .body(Map.of("error", "FORBIDDEN", "message", "requires PLATFORM_ADMIN"));
    }
    UserTenantGrant grant =
        grantRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("授权不存在: " + id));
    grantRepo.delete(grant);
    audit.recordSuccess(
        TenantContext.parseLong(tenantHeader),
        TenantContext.parseLong(userIdHeader),
        AuditLog.Action.DELETE,
        "TENANT_GRANT",
        grant.getId(),
        "撤销授权 " + grant.getUser().getUsername() + " → " + grant.getTenant().getName(),
        request);
    return ResponseEntity.noContent().build();
  }

  private GrantView toView(UserTenantGrant g) {
    return new GrantView(
        g.getId(),
        g.getUser().getId(),
        g.getUser().getUsername(),
        g.getTenant().getId(),
        g.getTenant().getCode(),
        g.getTenant().getName(),
        g.getRole().getId(),
        g.getRole().getCode(),
        g.getRole().getName(),
        g.getGrantedAt());
  }
}

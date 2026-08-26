package cn.huntercat.lieshoucloudpro.user.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cn.huntercat.lieshoucloudpro.user.domain.Tenant;
import cn.huntercat.lieshoucloudpro.user.domain.TenantRepository;
import cn.huntercat.lieshoucloudpro.user.domain.User;
import cn.huntercat.lieshoucloudpro.user.domain.UserRepository;
import cn.huntercat.lieshoucloudpro.user.domain.UserTenantGrant;
import cn.huntercat.lieshoucloudpro.user.domain.UserTenantGrantRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 租户访问上下文（集团版 · Phase 1 §3.2 子公司切换器数据源）.
 *
 * <p>返回用户可访问的租户列表：主属租户（users.tenant_id，天然可访问）+ 跨公司授权 （user_tenant_grants）。前端子公司切换器与 auth-service
 * 切换租户都依赖此端点。
 *
 * <p>auth-service 内部调用 {@code /api/tenant-access/user/{userId}} 时透传 X-User-Id（同用户） 放行；外部调用须
 * PLATFORM_ADMIN 或本人。
 */
@RestController
@RequestMapping("/api/tenant-access")
@Tag(name = "TenantAccess", description = "用户可访问租户上下文（集团版子公司切换器）")
public class TenantAccessController {

  private final UserRepository userRepo;
  private final TenantRepository tenantRepo;
  private final UserTenantGrantRepository grantRepo;

  public TenantAccessController(
      UserRepository userRepo, TenantRepository tenantRepo, UserTenantGrantRepository grantRepo) {
    this.userRepo = userRepo;
    this.tenantRepo = tenantRepo;
    this.grantRepo = grantRepo;
  }

  /** 可访问租户项 */
  public record TenantAccessItem(
      Long tenantId,
      String tenantCode,
      String tenantName,
      String edition,
      List<String> roles,
      boolean primary) {}

  @Operation(summary = "当前用户可访问租户列表（子公司切换器）")
  @GetMapping("/me")
  public ResponseEntity<?> me(
      @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
    Long uid = parseLong(userIdHeader);
    if (uid == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(Map.of("error", "UNAUTHORIZED", "message", "missing X-User-Id"));
    }
    return ResponseEntity.ok(buildAccess(uid));
  }

  @Operation(summary = "指定用户可访问租户列表（本人或 PLATFORM_ADMIN；auth 内部透传本人）")
  @GetMapping("/user/{userId}")
  public ResponseEntity<?> forUser(
      @PathVariable Long userId,
      @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
      @RequestHeader(value = "X-User-Roles", required = false) String rolesHeader) {
    Long caller = parseLong(userIdHeader);
    boolean self = caller != null && caller.equals(userId);
    if (!self && !AuthRoles.hasAny(rolesHeader, AuthRoles.PLATFORM_ADMIN)) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .body(Map.of("error", "FORBIDDEN", "message", "requires PLATFORM_ADMIN or self"));
    }
    return ResponseEntity.ok(buildAccess(userId));
  }

  /** 组装：主属租户 + 跨公司授权租户（同租户多角色合并；主属在前） */
  private List<TenantAccessItem> buildAccess(Long userId) {
    User user =
        userRepo
            .findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("用户不存在: " + userId));
    Tenant primary =
        tenantRepo
            .findById(user.getTenantId())
            .orElseThrow(() -> new IllegalArgumentException("主属租户不存在: " + user.getTenantId()));
    List<String> primaryRoles =
        user.getRoles() == null
            ? List.of("USER")
            : user.getRoles().stream()
                .map(r -> r.getCode())
                .distinct()
                .collect(Collectors.toList());

    // grants: 租户 → 角色集合（同租户多角色合并）
    Map<Long, List<String>> grantRolesByTenant = new LinkedHashMap<>();
    for (UserTenantGrant g : grantRepo.findByUserId(userId)) {
      grantRolesByTenant
          .computeIfAbsent(g.getTenant().getId(), k -> new ArrayList<>())
          .add(g.getRole().getCode());
    }

    List<TenantAccessItem> items = new ArrayList<>();
    items.add(
        new TenantAccessItem(
            primary.getId(),
            primary.getCode(),
            primary.getName(),
            primary.getEdition() == null ? null : primary.getEdition().name(),
            primaryRoles.isEmpty() ? List.of("USER") : primaryRoles,
            true));
    for (Map.Entry<Long, List<String>> e : grantRolesByTenant.entrySet()) {
      Tenant t =
          tenantRepo
              .findById(e.getKey())
              .orElseThrow(() -> new IllegalArgumentException("子公司租户不存在: " + e.getKey()));
      items.add(
          new TenantAccessItem(
              t.getId(),
              t.getCode(),
              t.getName(),
              t.getEdition() == null ? null : t.getEdition().name(),
              e.getValue().stream().distinct().collect(Collectors.toList()),
              false));
    }
    items.sort(
        Comparator.comparing((TenantAccessItem i) -> i.primary() ? 0 : 1)
            .thenComparing(TenantAccessItem::tenantCode));
    return items;
  }

  private static Long parseLong(String s) {
    if (s == null || s.isBlank()) return null;
    try {
      return Long.parseLong(s);
    } catch (NumberFormatException e) {
      return null;
    }
  }
}

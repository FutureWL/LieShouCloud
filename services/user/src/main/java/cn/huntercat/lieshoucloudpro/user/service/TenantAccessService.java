package cn.huntercat.lieshoucloudpro.user.service;

import org.springframework.stereotype.Service;

import cn.huntercat.lieshoucloudpro.user.domain.PermissionRepository;
import cn.huntercat.lieshoucloudpro.user.domain.Tenant;
import cn.huntercat.lieshoucloudpro.user.domain.TenantRepository;
import cn.huntercat.lieshoucloudpro.user.domain.User;
import cn.huntercat.lieshoucloudpro.user.domain.UserRepository;
import cn.huntercat.lieshoucloudpro.user.domain.UserTenantGrant;
import cn.huntercat.lieshoucloudpro.user.domain.UserTenantGrantRepository;
import cn.huntercat.lieshoucloudpro.user.service.dto.TenantAccessDtos.TenantAccessItem;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 租户访问上下文业务服务（集团版 · Phase 1 §3.2 子公司切换器数据源）.
 *
 * <p>承载"用户可访问租户列表"组装逻辑（主属租户 + user_tenant_grants 跨公司授权，同租户多角色合并，主属在前）， 自 TenantAccessController
 * 下沉（ARCHITECTURE.md §4.2 Port 抽象前置，同 UserService 先例）—— Controller 只保留 HTTP 适配（header 解析 + 权限放行 +
 * 状态码转译），monolith Local 适配器（auth → user）进程内直接注入本服务。
 *
 * <p>业务失败抛 {@link UserBizException}（404 USER_NOT_FOUND / TENANT_NOT_FOUND），由 Controller 转
 * HTTP、Local 适配器转 Feign 语义。
 */
@Service
public class TenantAccessService {

  private final UserRepository userRepo;
  private final TenantRepository tenantRepo;
  private final UserTenantGrantRepository grantRepo;
  private final PermissionRepository permissionRepo;

  public TenantAccessService(
      UserRepository userRepo,
      TenantRepository tenantRepo,
      UserTenantGrantRepository grantRepo,
      PermissionRepository permissionRepo) {
    this.userRepo = userRepo;
    this.tenantRepo = tenantRepo;
    this.grantRepo = grantRepo;
    this.permissionRepo = permissionRepo;
  }

  /** 组装：主属租户 + 跨公司授权租户（同租户多角色合并；主属在前） */
  public List<TenantAccessItem> buildAccess(Long userId) {
    User user =
        userRepo
            .findById(userId)
            .orElseThrow(() -> new UserBizException(404, "USER_NOT_FOUND", "用户不存在: " + userId));
    Tenant primary =
        tenantRepo
            .findById(user.getTenantId())
            .orElseThrow(
                () ->
                    new UserBizException(
                        404, "TENANT_NOT_FOUND", "主属租户不存在: " + user.getTenantId()));
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
    List<String> primaryPermissions =
        permissionRepo.findCodesByUserId(userId) == null
            ? List.of()
            : permissionRepo.findCodesByUserId(userId);
    items.add(
        new TenantAccessItem(
            primary.getId(),
            primary.getCode(),
            primary.getName(),
            primary.getEdition() == null ? null : primary.getEdition().name(),
            primaryRoles.isEmpty() ? List.of("USER") : primaryRoles,
            primaryPermissions,
            true));
    for (Map.Entry<Long, List<String>> e : grantRolesByTenant.entrySet()) {
      Tenant t =
          tenantRepo
              .findById(e.getKey())
              .orElseThrow(
                  () -> new UserBizException(404, "TENANT_NOT_FOUND", "子公司租户不存在: " + e.getKey()));
      List<String> roleCodes = e.getValue().stream().distinct().collect(Collectors.toList());
      List<String> perms = permissionRepo.findCodesByRoleCodes(roleCodes);
      items.add(
          new TenantAccessItem(
              t.getId(),
              t.getCode(),
              t.getName(),
              t.getEdition() == null ? null : t.getEdition().name(),
              roleCodes,
              perms == null ? List.of() : perms,
              false));
    }
    items.sort(
        Comparator.comparing((TenantAccessItem i) -> i.primary() ? 0 : 1)
            .thenComparing(TenantAccessItem::tenantCode));
    return items;
  }
}

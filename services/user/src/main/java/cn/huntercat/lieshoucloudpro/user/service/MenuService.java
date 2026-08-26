package cn.huntercat.lieshoucloudpro.user.service;

import cn.huntercat.lieshoucloudpro.user.domain.Tenant;
import cn.huntercat.lieshoucloudpro.user.domain.TenantMenuConfig;
import cn.huntercat.lieshoucloudpro.user.domain.TenantMenuConfigRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 菜单组装服务（平台基础层 · 菜单数据驱动 · ADR-0024 Phase 2 阶段 4）.
 *
 * <p>流程：默认菜单清单（DEFAULT_MENUS）→ 合并租户覆盖（tenant_menu_configs：enabled/rename/sort）
 * → 按用户权限码过滤（accessKey）→ 排序 → 菜单树。
 *
 * <p>版别差异由租户 edition 裁决：LAYER/LEGALMIND → 今日作战台置顶 + 无通用欢迎页；其余版别
 * → 欢迎 + 工作台（与前端 editions.ts 对齐，但以后端为数据源）。
 */
@Service
public class MenuService {

  /** 菜单项（平台内置清单） */
  public record MenuNode(
      String key,
      String path,
      String name,
      String icon,
      String accessKey, // null = 登录即可见
      int sort,
      List<MenuNode> children) {}

  /** 默认菜单清单（与前端 _defaultProps 对齐；icon 为字符串 key，前端 ICON_MAP 映射） */
  private static final List<MenuNode> DEFAULT_MENUS =
      List.of(
          new MenuNode(
              "welcome", "/welcome", "欢迎", "smile", null, 10, List.of()),
          new MenuNode(
              "today", "/admin", "工作台", "dashboard", null, 20, List.of()),
          new MenuNode(
              "profile", "/profile", "个人中心", "user", null, 30, List.of()),
          new MenuNode(
              "tenant",
              "/tenant",
              "租户管理",
              "cluster",
              "tenant:manage",
              40,
              List.of(
                  new MenuNode("tenant-list", "/tenant/list", "租户列表", "shop", "tenant:manage", 10, List.of()),
                  new MenuNode("role-list", "/role/list", "角色管理", "safety", "tenant:manage", 20, List.of()),
                  new MenuNode("audit-list", "/audit/list", "审计日志", "file-search", "tenant:manage", 30, List.of()))),
          new MenuNode(
              "user",
              "/user",
              "用户中心",
              "team",
              "user:list",
              50,
              List.of(
                  new MenuNode("user-list", "/user/list", "用户列表", "user", "user:list", 10, List.of()))),
          new MenuNode(
              "customer",
              "/customer",
              "CRM 客户",
              "contacts",
              "crm:use",
              60,
              List.of(
                  new MenuNode("customer-list", "/customer/list", "客户列表", "solution", "crm:use", 10, List.of()),
                  new MenuNode("lead-list", "/lead/list", "线索管理", "rise", "crm:use", 20, List.of()))),
          new MenuNode(
              "inventory",
              "/inventory",
              "进销存",
              "shop",
              "inventory:use",
              70,
              List.of(
                  new MenuNode("inventory-list", "/inventory/list", "库存管理", "solution", "inventory:use", 10, List.of()))),
          new MenuNode(
              "finance",
              "/finance",
              "财务记账",
              "fund",
              "finance:use",
              80,
              List.of(
                  new MenuNode("finance-list", "/finance/list", "记账本", "solution", "finance:use", 10, List.of()))),
          new MenuNode(
              "approval",
              "/approval",
              "审批流",
              "audit",
              "approval:use",
              90,
              List.of(
                  new MenuNode("approval-list", "/approval/list", "审批中心", "solution", "approval:use", 10, List.of()))),
          new MenuNode(
              "legal",
              "/legal",
              "案件管理",
              "book",
              "legal:use",
              100,
              List.of(
                  new MenuNode("legal-cases", "/legal/cases", "办案列表", "solution", "legal:use", 10, List.of()),
                  new MenuNode(
                      "legal-clients", "/legal/clients", "客户成功", "team", "legal:use", 15, List.of()),
                  new MenuNode(
                      "legal-calendar", "/legal/calendar", "任务与日程", "calendar", "legal:use", 17, List.of()),
                  new MenuNode(
                      "legal-knowledge", "/legal/knowledge", "知识资产", "bulb", "legal:use", 20, List.of()),
                  new MenuNode(
                      "legal-growth", "/legal/growth", "专业成长", "rise", "legal:use", 30, List.of()))),
          new MenuNode(
              "iot",
              "/iot",
              "物联网",
              "api",
              "iot:monitor",
              110,
              List.of(
                  new MenuNode("iot-cockpit", "/iot/cockpit", "驾驶舱", "radar", "iot:monitor", 10, List.of()),
                  new MenuNode("iot-overview", "/iot/overview", "监控总览", "dashboard", "iot:monitor", 20, List.of()),
                  new MenuNode("iot-topo", "/iot/topo", "电网拓扑", "apartment", "iot:monitor", 30, List.of()),
                  new MenuNode("iot-devices", "/iot/devices", "设备管理", "shop", "iot:config", 40, List.of()),
                  new MenuNode("iot-products", "/iot/products", "产品物模型", "solution", "iot:config", 50, List.of()),
                  new MenuNode("iot-rules", "/iot/rules", "规则配置", "safety", "iot:config", 60, List.of()),
                  new MenuNode("iot-alerts", "/iot/alerts", "告警中心", "file-search", "iot:monitor", 70, List.of()))));

  private final TenantMenuConfigRepository configs;

  public MenuService(TenantMenuConfigRepository configs) {
    this.configs = configs;
  }

  /**
   * 组装当前租户 + 当前用户可见菜单树。
   *
   * @param tenantId 租户（取租户覆盖配置）
   * @param edition 租户版别（LAYER/LEGALMIND → 法律版形态）
   * @param permissions 当前用户权限码（gateway X-User-Permissions 透传；空 = 仅登录可见项）
   */
  public List<MenuNode> buildMenus(Long tenantId, Tenant.Edition edition, List<String> permissions) {
    Map<String, TenantMenuConfig> overrides = new LinkedHashMap<>();
    for (TenantMenuConfig c : configs.findByTenantId(tenantId)) {
      overrides.put(c.getMenuKey(), c);
    }

    List<String> perms = permissions == null ? List.of() : permissions;
    List<MenuNode> result = new ArrayList<>();
    for (MenuNode root : baseMenus(edition)) {
      MenuNode node = mergeNode(root, overrides, perms);
      if (node != null) result.add(node);
    }
    result.sort(Comparator.comparingInt(MenuNode::sort));
    return result;
  }

  /** 版别基菜单：法律版（LAYER/LEGALMIND）置顶今日作战台 + 隐藏欢迎；其余版别含欢迎 + 工作台。 */
  private List<MenuNode> baseMenus(Tenant.Edition edition) {
    boolean legal = edition == Tenant.Edition.LEGALMIND || edition == Tenant.Edition.LAYER;
    List<MenuNode> base = new ArrayList<>(DEFAULT_MENUS);
    if (legal) {
      base =
          base.stream()
              .filter(n -> !"welcome".equals(n.key()))
              .map(
                  n ->
                      "today".equals(n.key())
                          ? new MenuNode(n.key(), n.path(), "今日作战台", n.icon(), n.accessKey(), n.sort(), n.children())
                          : n)
              .toList();
    }
    return base;
  }

  /** 合并租户覆盖 + 权限过滤；被禁用/无权限 → null（移除）。 */
  private MenuNode mergeNode(MenuNode node, Map<String, TenantMenuConfig> overrides, List<String> perms) {
    // 权限过滤（accessKey；null = 登录可见）
    if (node.accessKey() != null && !perms.contains(node.accessKey())) {
      return null;
    }

    TenantMenuConfig cfg = overrides.get(node.key());
    if (cfg != null && !cfg.isEnabled()) {
      return null;
    }

    // 子节点合并
    List<MenuNode> children = new ArrayList<>();
    for (MenuNode child : node.children()) {
      MenuNode merged = mergeNode(child, overrides, perms);
      if (merged != null) children.add(merged);
    }
    children.sort(Comparator.comparingInt(MenuNode::sort));

    // 有子菜单但全被过滤 → 整组隐藏
    if (!node.children().isEmpty() && children.isEmpty()) {
      return null;
    }

    String name = (cfg != null && cfg.getRename() != null && !cfg.getRename().isBlank())
        ? cfg.getRename()
        : node.name();
    int sort = (cfg != null && cfg.getSort() != 0) ? cfg.getSort() : node.sort();

    return new MenuNode(node.key(), node.path(), name, node.icon(), node.accessKey(), sort, children);
  }
}

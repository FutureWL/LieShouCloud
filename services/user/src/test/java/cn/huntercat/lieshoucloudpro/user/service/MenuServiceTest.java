package cn.huntercat.lieshoucloudpro.user.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cn.huntercat.lieshoucloudpro.user.domain.Tenant;
import cn.huntercat.lieshoucloudpro.user.domain.TenantMenuConfig;
import cn.huntercat.lieshoucloudpro.user.domain.TenantMenuConfigRepository;
import java.util.List;

/**
 * MenuService 单测（菜单数据驱动 · ADR-0024 Phase 2 阶段 4）.
 *
 * <p>覆盖：版别基菜单（legal 置顶今日作战台）、权限过滤、租户覆盖（enabled/rename/sort）、 子菜单全过滤整组隐藏。
 */
class MenuServiceTest {

  private TenantMenuConfigRepository configs;
  private MenuService service;

  @BeforeEach
  void setUp() {
    configs = mock(TenantMenuConfigRepository.class);
    service = new MenuService(configs);
  }

  @Test
  @DisplayName("LEGALMIND 版：今日作战台置顶 + 无欢迎页")
  void legalEdition_todayFirst_noWelcome() {
    when(configs.findByTenantId(1L)).thenReturn(List.of());
    var menus = service.buildMenus(1L, Tenant.Edition.LEGALMIND, List.of("legal:use", "user:list"));
    assertThat(menus.get(0).key()).isEqualTo("today");
    assertThat(menus.get(0).name()).isEqualTo("今日作战台");
    assertThat(menus.stream().map(MenuService.MenuNode::key)).doesNotContain("welcome");
    // 有 legal:use → legal 组出现
    assertThat(menus.stream().map(MenuService.MenuNode::key)).contains("legal");
  }

  @Test
  @DisplayName("GENERIC 版：含欢迎 + 工作台")
  void genericEdition_welcomeAndWorkbench() {
    when(configs.findByTenantId(1L)).thenReturn(List.of());
    var menus = service.buildMenus(1L, Tenant.Edition.GENERIC, List.of());
    assertThat(menus.stream().map(MenuService.MenuNode::key))
        .contains("welcome", "today", "profile");
  }

  @Test
  @DisplayName("权限过滤：无 legal:use → legal 组消失；tenant:manage 保留租户组")
  void permissionFilter() {
    when(configs.findByTenantId(1L)).thenReturn(List.of());
    var menus =
        service.buildMenus(1L, Tenant.Edition.GENERIC, List.of("tenant:manage", "user:list"));
    assertThat(menus.stream().map(MenuService.MenuNode::key))
        .contains("tenant", "user")
        .doesNotContain("legal", "customer", "iot");
  }

  @Test
  @DisplayName("租户覆盖：disabled 隐藏 / rename 改名 / sort 重排")
  void tenantOverride() {
    var customer = new TenantMenuConfig(1L, "customer");
    customer.setEnabled(false);
    var finance = new TenantMenuConfig(1L, "finance");
    finance.setRename("律所财务");
    finance.setSort(5);
    when(configs.findByTenantId(1L)).thenReturn(List.of(customer, finance));

    var menus =
        service.buildMenus(
            1L, Tenant.Edition.GENERIC, List.of("crm:use", "finance:use", "user:list"));
    assertThat(menus.stream().map(MenuService.MenuNode::key)).doesNotContain("customer");
    var fin = menus.stream().filter(m -> m.key().equals("finance")).findFirst().orElseThrow();
    assertThat(fin.name()).isEqualTo("律所财务");
    assertThat(fin.sort()).isEqualTo(5);
  }

  @Test
  @DisplayName("子菜单全部被权限过滤 → 整组隐藏")
  void emptyChildren_hideGroup() {
    when(configs.findByTenantId(1L)).thenReturn(List.of());
    // 无 crm:use → customer 组及其子菜单全隐藏
    var menus = service.buildMenus(1L, Tenant.Edition.GENERIC, List.of("user:list"));
    assertThat(menus.stream().map(MenuService.MenuNode::key)).doesNotContain("customer");
  }
}

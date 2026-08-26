package cn.huntercat.lieshoucloudpro.user.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cn.huntercat.lieshoucloudpro.user.PostgresTestSupport;
import cn.huntercat.lieshoucloudpro.user.domain.Role;
import cn.huntercat.lieshoucloudpro.user.domain.Tenant;
import cn.huntercat.lieshoucloudpro.user.domain.TenantRepository;
import cn.huntercat.lieshoucloudpro.user.domain.User;
import cn.huntercat.lieshoucloudpro.user.domain.UserRepository;
import cn.huntercat.lieshoucloudpro.user.domain.UserTenantGrant;
import cn.huntercat.lieshoucloudpro.user.domain.UserTenantGrantRepository;
import java.util.List;
import java.util.Optional;

/**
 * TenantAccessController 租户访问上下文测试（集团版 Phase 1 §3.2 子公司切换器数据源）.
 *
 * <p>照 TenantControllerTest 模式：@SpringBootTest + MockMvc + Mock repo。
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("TenantAccessController（可访问租户上下文 · 集团版 V10）")
class TenantAccessControllerTest extends PostgresTestSupport {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private UserRepository userRepo;

  @MockitoBean private TenantRepository tenantRepo;

  @MockitoBean private UserTenantGrantRepository grantRepo;

  private User userWithRoles(Long id, Long tenantId, Role... roles) {
    User u = new User();
    u.setId(id);
    u.setTenantId(tenantId);
    u.setUsername("hq-admin");
    for (Role r : roles) u.getRoles().add(r);
    return u;
  }

  @Test
  @DisplayName("me：主属租户 + 跨公司授权租户（同租户多角色合并）")
  void me_primaryPlusGrants() throws Exception {
    User u = userWithRoles(10L, 1L, new Role("TENANT_ADMIN", "租户管理员", Role.Scope.TENANT, "", true));
    Tenant primary = new Tenant("海赞集团", "haizan");
    ReflectionTestUtils.setField(primary, "id", 1L);
    Tenant sub = new Tenant("南昌猎手猫", "nanchang");
    ReflectionTestUtils.setField(sub, "id", 2L);
    Role userRole = new Role("USER", "普通用户", Role.Scope.TENANT, "", true);

    UserTenantGrant grant = new UserTenantGrant(u, sub, userRole, null);
    when(userRepo.findById(10L)).thenReturn(Optional.of(u));
    when(tenantRepo.findById(1L)).thenReturn(Optional.of(primary));
    when(tenantRepo.findById(2L)).thenReturn(Optional.of(sub));
    when(grantRepo.findByUserId(10L)).thenReturn(List.of(grant));

    mockMvc
        .perform(get("/api/tenant-access/me").header("X-User-Id", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].tenantCode").value("haizan"))
        .andExpect(jsonPath("$[0].primary").value(true))
        .andExpect(jsonPath("$[0].roles[0]").value("TENANT_ADMIN"))
        .andExpect(jsonPath("$[1].tenantCode").value("nanchang"))
        .andExpect(jsonPath("$[1].primary").value(false))
        .andExpect(jsonPath("$[1].roles[0]").value("USER"));
  }

  @Test
  @DisplayName("forUser：本人可查（X-User-Id 透传）；非本人非平台管理员 → 403")
  void forUser_permission() throws Exception {
    User u = userWithRoles(10L, 1L);
    Tenant primary = new Tenant("海赞集团", "haizan");
    ReflectionTestUtils.setField(primary, "id", 1L);

    when(userRepo.findById(10L)).thenReturn(Optional.of(u));
    when(tenantRepo.findById(1L)).thenReturn(Optional.of(primary));
    when(grantRepo.findByUserId(10L)).thenReturn(List.of());

    // 本人（auth 内部透传 X-User-Id）→ 200
    mockMvc
        .perform(get("/api/tenant-access/user/10").header("X-User-Id", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].tenantCode").value("haizan"));

    // 他人（无平台角色）→ 403
    mockMvc
        .perform(
            get("/api/tenant-access/user/10")
                .header("X-User-Id", "99")
                .header("X-User-Roles", "USER"))
        .andExpect(status().isForbidden());
  }
}

package cn.huntercat.lieshoucloudpro.user.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import cn.huntercat.lieshoucloudpro.user.service.AuditService;
import java.util.Optional;

/**
 * TenantGrantController 统一账号跨公司授权测试（集团版 Phase 1 §3.2）.
 *
 * <p>照 TenantControllerTest 模式：@SpringBootTest + MockMvc + Mock repo/audit。
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("TenantGrantController（统一账号跨公司授权 · 集团版 V10）")
class TenantGrantControllerTest extends PostgresTestSupport {

  private static final String ADMIN = "PLATFORM_ADMIN";

  @Autowired private MockMvc mockMvc;

  @MockitoBean private UserTenantGrantRepository grantRepo;

  @MockitoBean private UserRepository userRepo;

  @MockitoBean private TenantRepository tenantRepo;

  @MockitoBean private cn.huntercat.lieshoucloudpro.user.domain.RoleRepository roleRepo;

  @MockitoBean private AuditService audit;

  private User user(Long id, Long tenantId, String username) {
    User u = new User();
    u.setId(id);
    u.setTenantId(tenantId);
    u.setUsername(username);
    return u;
  }

  @Test
  @DisplayName("grant 成功（PLATFORM_ADMIN）→ 201 + 授权视图")
  void grant_success() throws Exception {
    User u = user(10L, 1L, "hq-admin");
    Tenant sub = new Tenant("南昌猎手猫信息科技", "nanchang");
    ReflectionTestUtils.setField(sub, "id", 2L);
    Role role = new Role("USER", "普通用户", Role.Scope.TENANT, "基础使用", true);
    ReflectionTestUtils.setField(role, "id", 99L);

    when(userRepo.findById(10L)).thenReturn(Optional.of(u));
    when(tenantRepo.findById(2L)).thenReturn(Optional.of(sub));
    when(roleRepo.findById(99L)).thenReturn(Optional.of(role));
    when(grantRepo.existsByUserIdAndTenantIdAndRoleId(10L, 2L, 99L)).thenReturn(false);
    when(grantRepo.save(any(UserTenantGrant.class)))
        .thenAnswer(
            inv -> {
              UserTenantGrant g = inv.getArgument(0);
              return g;
            });

    mockMvc
        .perform(
            post("/api/tenant-grants")
                .header("X-User-Roles", ADMIN)
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"userId": 10, "tenantId": 2, "roleId": 99}
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.userId").value(10))
        .andExpect(jsonPath("$.tenantCode").value("nanchang"))
        .andExpect(jsonPath("$.roleCode").value("USER"));
  }

  @Test
  @DisplayName("grant 非平台管理员 → 403")
  void grant_forbidden() throws Exception {
    mockMvc
        .perform(
            post("/api/tenant-grants")
                .header("X-User-Roles", "USER")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"userId": 10, "tenantId": 2, "roleId": 99}
                    """))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("grant 平台角色（PLATFORM scope）→ 400 INVALID_ROLE_SCOPE")
  void grant_platformScopeRole_rejected() throws Exception {
    User u = user(10L, 1L, "hq-admin");
    Tenant sub = new Tenant("子公司", "sub-x");
    ReflectionTestUtils.setField(sub, "id", 2L);
    Role role = new Role("PLATFORM_ADMIN", "平台管理员", Role.Scope.PLATFORM, "平台运营", true);
    ReflectionTestUtils.setField(role, "id", 99L);

    when(userRepo.findById(10L)).thenReturn(Optional.of(u));
    when(tenantRepo.findById(2L)).thenReturn(Optional.of(sub));
    when(roleRepo.findById(99L)).thenReturn(Optional.of(role));

    mockMvc
        .perform(
            post("/api/tenant-grants")
                .header("X-User-Roles", ADMIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"userId": 10, "tenantId": 2, "roleId": 99}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("INVALID_ROLE_SCOPE"));
  }

  @Test
  @DisplayName("grant 主属租户 → 400 PRIMARY_TENANT_NOT_GRANTABLE")
  void grant_primaryTenant_rejected() throws Exception {
    User u = user(10L, 1L, "hq-admin");
    Tenant primary = new Tenant("总部", "hq");
    ReflectionTestUtils.setField(primary, "id", 1L);
    Role role = new Role("USER", "普通用户", Role.Scope.TENANT, "基础使用", true);
    ReflectionTestUtils.setField(role, "id", 99L);

    when(userRepo.findById(10L)).thenReturn(Optional.of(u));
    when(tenantRepo.findById(1L)).thenReturn(Optional.of(primary));
    when(roleRepo.findById(99L)).thenReturn(Optional.of(role));

    mockMvc
        .perform(
            post("/api/tenant-grants")
                .header("X-User-Roles", ADMIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"userId": 10, "tenantId": 1, "roleId": 99}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("PRIMARY_TENANT_NOT_GRANTABLE"));
  }

  @Test
  @DisplayName("grant 重复授权 → 409 GRANT_EXISTS")
  void grant_duplicate_conflict() throws Exception {
    User u = user(10L, 1L, "hq-admin");
    Tenant sub = new Tenant("子公司", "sub-x");
    ReflectionTestUtils.setField(sub, "id", 2L);
    Role role = new Role("USER", "普通用户", Role.Scope.TENANT, "基础使用", true);
    ReflectionTestUtils.setField(role, "id", 99L);

    when(userRepo.findById(10L)).thenReturn(Optional.of(u));
    when(tenantRepo.findById(2L)).thenReturn(Optional.of(sub));
    when(roleRepo.findById(99L)).thenReturn(Optional.of(role));
    when(grantRepo.existsByUserIdAndTenantIdAndRoleId(10L, 2L, 99L)).thenReturn(true);

    mockMvc
        .perform(
            post("/api/tenant-grants")
                .header("X-User-Roles", ADMIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"userId": 10, "tenantId": 2, "roleId": 99}
                    """))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error").value("GRANT_EXISTS"));
  }

  @Test
  @DisplayName("revoke 非平台管理员 → 403；平台管理员 → 204")
  void revoke_permission() throws Exception {
    mockMvc
        .perform(
            delete("/api/tenant-grants/1")
                .header("X-User-Roles", "USER")
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isForbidden());

    UserTenantGrant g =
        new UserTenantGrant(
            user(10L, 1L, "hq-admin"),
            new Tenant("子公司", "sub-x"),
            new Role("USER", "普通用户", Role.Scope.TENANT, "x", true),
            null);
    when(grantRepo.findById(anyLong())).thenReturn(Optional.of(g));
    mockMvc
        .perform(
            delete("/api/tenant-grants/1")
                .header("X-User-Roles", ADMIN)
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNoContent());
  }
}

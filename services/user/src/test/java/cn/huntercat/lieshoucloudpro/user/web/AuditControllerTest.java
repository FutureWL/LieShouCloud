package cn.huntercat.lieshoucloudpro.user.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cn.huntercat.lieshoucloudpro.user.PostgresTestSupport;
import cn.huntercat.lieshoucloudpro.user.domain.AuditLog;
import cn.huntercat.lieshoucloudpro.user.domain.AuditLogRepository;
import java.util.List;

/**
 * AuditController 集成测试（@SpringBootTest + MockMvc + Mock repo）.
 *
 * <p>覆盖租户作用域：有 X-Tenant-Id → 只返回该租户；无租户上下文 → 需要 PLATFORM_ADMIN； action/resourceType 过滤。
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("AuditController（审计查询 · 租户作用域）")
class AuditControllerTest extends PostgresTestSupport {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private AuditLogRepository repo;

  private AuditLog log(Long tid, String action, String resourceType) {
    AuditLog l = new AuditLog();
    l.setTenantId(tid);
    l.setUserId(1L);
    l.setAction(AuditLog.Action.valueOf(action));
    l.setResourceType(resourceType);
    l.setResourceId(10L);
    l.setOutcome(AuditLog.Outcome.SUCCESS);
    return l;
  }

  @Test
  @DisplayName("带 X-Tenant-Id → 只返回该租户日志")
  void list_tenantScoped() throws Exception {
    when(repo.findByTenantIdOrderByCreatedAtDesc(7L))
        .thenReturn(List.of(log(7L, "CREATE", "USER"), log(7L, "DELETE", "TENANT")));

    mockMvc
        .perform(get("/api/audit-logs").header("X-Tenant-Id", "7"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].resourceType").value("USER"));
  }

  @Test
  @DisplayName("无租户上下文 + 非平台管理员 → 403")
  void list_withoutTenant_requiresPlatformAdmin() throws Exception {
    mockMvc
        .perform(get("/api/audit-logs").header("X-User-Roles", "USER"))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("无租户上下文 + PLATFORM_ADMIN → 全部日志")
  void list_platformAdmin_seesAll() throws Exception {
    when(repo.findAll())
        .thenReturn(List.of(log(1L, "CREATE", "ROLE"), log(2L, "UPDATE", "TENANT")));

    mockMvc
        .perform(get("/api/audit-logs").header("X-User-Roles", "PLATFORM_ADMIN"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2));
  }

  @Test
  @DisplayName("action 过滤只返回匹配行")
  void list_actionFilter() throws Exception {
    when(repo.findByTenantIdOrderByCreatedAtDesc(1L))
        .thenReturn(List.of(log(1L, "CREATE", "USER"), log(1L, "DELETE", "USER")));

    mockMvc
        .perform(get("/api/audit-logs").header("X-Tenant-Id", "1").param("action", "DELETE"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].action").value("DELETE"));
  }

  @Test
  @DisplayName("count 按租户")
  void count_tenantScoped() throws Exception {
    when(repo.countByTenantId(3L)).thenReturn(9L);

    mockMvc
        .perform(get("/api/audit-logs/count").header("X-Tenant-Id", "3"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").value(9));
  }
}

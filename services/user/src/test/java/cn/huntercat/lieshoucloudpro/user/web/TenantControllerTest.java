package cn.huntercat.lieshoucloudpro.user.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cn.huntercat.lieshoucloudpro.user.PostgresTestSupport;
import cn.huntercat.lieshoucloudpro.user.domain.Tenant;
import cn.huntercat.lieshoucloudpro.user.domain.TenantRepository;
import cn.huntercat.lieshoucloudpro.user.service.AuditService;
import java.util.Optional;

/**
 * TenantController 子公司档案测试（集团版 V9 · 工商字段 + parentTenantId 校验）.
 *
 * <p>照 AuditControllerTest 模式：@SpringBootTest + MockMvc + Mock repo/audit。
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("TenantController（子公司档案 · 集团版 V9）")
class TenantControllerTest extends PostgresTestSupport {

  private static final String ADMIN = "PLATFORM_ADMIN";

  @Autowired private MockMvc mockMvc;

  @MockitoBean private TenantRepository repo;

  @MockitoBean private AuditService audit;

  @Test
  @DisplayName("create 带子公司档案字段 → 返回完整档案")
  void create_withCompanyProfile() throws Exception {
    when(repo.findByCode("sub-a")).thenReturn(Optional.empty());
    when(repo.existsById(2L)).thenReturn(true);
    when(repo.save(any(Tenant.class)))
        .thenAnswer(
            inv -> {
              Tenant t = inv.getArgument(0);
              return t;
            });

    mockMvc
        .perform(
            post("/api/tenants")
                .header("X-User-Roles", ADMIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "海赞教育子公司",
                      "code": "sub-a",
                      "creditCode": "91360103MA7XXXXXXX1",
                      "legalPerson": "张三",
                      "registeredCapital": 1000.00,
                      "establishedAt": "2020-01-01",
                      "industry": "教育",
                      "parentTenantId": 2
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("sub-a"))
        .andExpect(jsonPath("$.creditCode").value("91360103MA7XXXXXXX1"))
        .andExpect(jsonPath("$.legalPerson").value("张三"))
        .andExpect(jsonPath("$.registeredCapital").value(1000.00))
        .andExpect(jsonPath("$.establishedAt").value("2020-01-01"))
        .andExpect(jsonPath("$.industry").value("教育"))
        .andExpect(jsonPath("$.parentTenantId").value(2));
  }

  @Test
  @DisplayName("create 不填档案字段 → 默认为空（通用版不受影响）")
  void create_withoutProfile_keepsBlank() throws Exception {
    when(repo.findByCode("plain-t")).thenReturn(Optional.empty());
    when(repo.save(any(Tenant.class)))
        .thenAnswer(
            inv -> {
              Tenant t = inv.getArgument(0);
              return t;
            });

    mockMvc
        .perform(
            post("/api/tenants")
                .header("X-User-Roles", ADMIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"name": "普通租户", "code": "plain-t"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.creditCode").doesNotExist());
  }

  @Test
  @DisplayName("update 更新档案字段（null 字段不动）")
  void update_companyProfile() throws Exception {
    Tenant t = new Tenant("海赞教育子公司", "sub-a", Tenant.Edition.GENERIC);
    when(repo.findById(1L)).thenReturn(Optional.of(t));
    when(repo.save(any(Tenant.class))).thenAnswer(inv -> inv.getArgument(0));

    mockMvc
        .perform(
            put("/api/tenants/1")
                .header("X-User-Roles", ADMIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "legalPerson": "李四",
                      "registeredCapital": 2000.50,
                      "industry": "制造"
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.legalPerson").value("李四"))
        .andExpect(jsonPath("$.registeredCapital").value(2000.50))
        .andExpect(jsonPath("$.industry").value("制造"))
        .andExpect(jsonPath("$.creditCode").doesNotExist());
  }

  @Test
  @DisplayName("parentTenantId 指向不存在的租户 → 400 INVALID_PARENT_TENANT")
  void create_parentTenantMissing_400() throws Exception {
    when(repo.findByCode("sub-b")).thenReturn(Optional.empty());
    when(repo.existsById(999L)).thenReturn(false);

    mockMvc
        .perform(
            post("/api/tenants")
                .header("X-User-Roles", ADMIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"name": "孤儿子公司", "code": "sub-b", "parentTenantId": 999}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("INVALID_PARENT_TENANT"));
  }

  @Test
  @DisplayName("update parentTenantId 指向不存在的租户 → 400")
  void update_parentTenantMissing_400() throws Exception {
    Tenant t = new Tenant("海赞教育子公司", "sub-a", Tenant.Edition.GENERIC);
    when(repo.findById(1L)).thenReturn(Optional.of(t));
    when(repo.existsById(999L)).thenReturn(false);

    mockMvc
        .perform(
            put("/api/tenants/1")
                .header("X-User-Roles", ADMIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"parentTenantId": 999}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("INVALID_PARENT_TENANT"));
  }

  @Test
  @DisplayName("非平台管理员 → 403（不影响既有权限模型）")
  void create_nonPlatformAdmin_403() throws Exception {
    mockMvc
        .perform(
            post("/api/tenants")
                .header("X-User-Roles", "TENANT_ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"name": "x", "code": "x1"}
                    """))
        .andExpect(status().isForbidden());
  }
}

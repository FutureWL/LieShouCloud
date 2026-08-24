package cn.huntercat.lieshoucloudpro.approval.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cn.huntercat.lieshoucloudpro.approval.PostgresTestSupport;
import cn.huntercat.lieshoucloudpro.approval.domain.ApprovalAuditLogRepository;
import cn.huntercat.lieshoucloudpro.approval.feign.UserQueryClient;
import cn.huntercat.lieshoucloudpro.approval.feign.UserView;
import java.util.List;

/** 审批端点集成测试（强制租户 + 状态机 + 权限 · ADR-0025 模式 · ADR-0032）. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional // 静态块共享容器下防跨测试类数据污染（issue #25 · 回滚 approval_requests）
class ApprovalControllerTest extends PostgresTestSupport {

  @Autowired MockMvc mockMvc;
  @Autowired ApprovalAuditLogRepository auditRepo;

  @MockitoBean UserQueryClient userClient;

  @BeforeEach
  void cleanDb() {
    // 审计走 REQUIRES_NEW 独立提交（ADR-0030 模式），事务回滚不到 → 每测试前清审计表
    auditRepo.deleteAll();
    when(userClient.listTenantUsers(any())).thenReturn(List.of());
  }

  private static final String CREATE_BODY =
      "{\"type\":\"EXPENSE\",\"title\":\"报销差旅费\",\"amount\":1280,\"detail\":\"8 月出差\",\"approverId\":10}";

  @Test
  void createWithoutTenant_returns401() throws Exception {
    mockMvc
        .perform(
            post("/api/approvals")
                .header("X-User-Id", "9")
                .contentType(MediaType.APPLICATION_JSON)
                .content(CREATE_BODY))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error").value("TENANT_CONTEXT_REQUIRED"));
  }

  @Test
  void createAndList_tenantScoped() throws Exception {
    mockMvc
        .perform(
            post("/api/approvals")
                .header("X-Tenant-Id", "1")
                .header("X-User-Id", "9")
                .contentType(MediaType.APPLICATION_JSON)
                .content(CREATE_BODY))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.type").value("EXPENSE"))
        .andExpect(jsonPath("$.title").value("报销差旅费"))
        .andExpect(jsonPath("$.status").value("PENDING"))
        .andExpect(jsonPath("$.requesterId").value(9))
        .andExpect(jsonPath("$.approverId").value(10));

    mockMvc
        .perform(
            post("/api/approvals")
                .header("X-Tenant-Id", "2")
                .header("X-User-Id", "9")
                .contentType(MediaType.APPLICATION_JSON)
                .content(CREATE_BODY))
        .andExpect(status().isOk());

    mockMvc
        .perform(get("/api/approvals").header("X-Tenant-Id", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1));
  }

  @Test
  void create_invalidType_returns400() throws Exception {
    mockMvc
        .perform(
            post("/api/approvals")
                .header("X-Tenant-Id", "1")
                .header("X-User-Id", "9")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"FIREWORKS\",\"title\":\"x\",\"approverId\":10}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("INVALID_TYPE"));
  }

  @Test
  void create_missingApprover_autoResolveFails_whenTenantHasNoUsers() throws Exception {
    // 租户无用户（业务挂接自动触发场景）→ 400 APPROVER_RESOLVE_FAILED
    mockMvc
        .perform(
            post("/api/approvals")
                .header("X-Tenant-Id", "1")
                .header("X-User-Id", "9")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"EXPENSE\",\"title\":\"报销\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("APPROVER_RESOLVE_FAILED"));
  }

  @Test
  void create_withoutApprover_autoResolvesTenantAdmin() throws Exception {
    // 阶段 2 · 业务挂接：approverId 可空，自动选租户管理员（TENANT_ADMIN 优先）
    when(userClient.listTenantUsers("1"))
        .thenReturn(
            List.of(
                new UserView(5L, "staff", "员工", "ACTIVE", List.of("USER")),
                new UserView(7L, "boss", "管理员", "ACTIVE", List.of("TENANT_ADMIN"))));
    mockMvc
        .perform(
            post("/api/approvals")
                .header("X-Tenant-Id", "1")
                .header("X-User-Id", "9")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"EXPENSE\",\"title\":\"自动审批\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.approverId").value(7));
  }

  @Test
  void create_withoutApprover_autoResolvesFallbackToAnyUser() throws Exception {
    // 租户无管理员 → 退到任意用户
    when(userClient.listTenantUsers("1"))
        .thenReturn(List.of(new UserView(3L, "alice", "爱丽丝", "ACTIVE", List.of("USER"))));
    mockMvc
        .perform(
            post("/api/approvals")
                .header("X-Tenant-Id", "1")
                .header("X-User-Id", "9")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"OTHER\",\"title\":\"兜底\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.approverId").value(3));
  }

  @Test
  void auditLogs_recordsCreateAndApprove() throws Exception {
    Long id = createAndGetId(1L, 9L, 10L);
    mockMvc
        .perform(
            post("/api/approvals/{id}/approve", id)
                .header("X-Tenant-Id", "1")
                .header("X-User-Id", "10"))
        .andExpect(status().isOk());

    // 审计 append-only：CREATE + APPROVE 两条，新→旧
    mockMvc
        .perform(get("/api/approvals/audit-logs").header("X-Tenant-Id", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].action").value("APPROVE"))
        .andExpect(jsonPath("$[0].actorId").value(10))
        .andExpect(jsonPath("$[1].action").value("CREATE"))
        .andExpect(jsonPath("$[1].actorId").value(9));
  }

  @Test
  void auditLogs_tenantScoped() throws Exception {
    Long id = createAndGetId(1L, 9L, 10L);
    // 租户 2 看不到租户 1 的审计
    mockMvc
        .perform(get("/api/approvals/audit-logs").header("X-Tenant-Id", "2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void approve_flow_approverAndPermissions() throws Exception {
    Long id = createAndGetId(1L, 9L, 10L);

    // 非审批人 → 403
    mockMvc
        .perform(
            post("/api/approvals/{id}/approve", id)
                .header("X-Tenant-Id", "1")
                .header("X-User-Id", "99"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error").value("FORBIDDEN"));

    // 审批人通过 → APPROVED + decidedBy
    mockMvc
        .perform(
            post("/api/approvals/{id}/approve", id)
                .header("X-Tenant-Id", "1")
                .header("X-User-Id", "10")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"comment\":\"同意\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("APPROVED"))
        .andExpect(jsonPath("$.decidedBy").value(10));

    // 重复审批 → 409
    mockMvc
        .perform(
            post("/api/approvals/{id}/approve", id)
                .header("X-Tenant-Id", "1")
                .header("X-User-Id", "10"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error").value("ALREADY_DECIDED"));
  }

  @Test
  void reject_requiresComment_andFlow() throws Exception {
    Long id = createAndGetId(1L, 9L, 10L);

    // 驳回缺 comment → 400
    mockMvc
        .perform(
            post("/api/approvals/{id}/reject", id)
                .header("X-Tenant-Id", "1")
                .header("X-User-Id", "10")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest());

    mockMvc
        .perform(
            post("/api/approvals/{id}/reject", id)
                .header("X-Tenant-Id", "1")
                .header("X-User-Id", "10")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"comment\":\"金额超预算\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("REJECTED"))
        .andExpect(jsonPath("$.comment").value("金额超预算"));
  }

  @Test
  void cancel_onlyRequester() throws Exception {
    Long id = createAndGetId(1L, 9L, 10L);

    // 非发起人撤销 → 403
    mockMvc
        .perform(
            post("/api/approvals/{id}/cancel", id)
                .header("X-Tenant-Id", "1")
                .header("X-User-Id", "10"))
        .andExpect(status().isForbidden());

    mockMvc
        .perform(
            post("/api/approvals/{id}/cancel", id)
                .header("X-Tenant-Id", "1")
                .header("X-User-Id", "9"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CANCELLED"));
  }

  @Test
  void crossTenant_get_returns404() throws Exception {
    Long id = createAndGetId(1L, 9L, 10L);

    mockMvc
        .perform(get("/api/approvals/{id}", id).header("X-Tenant-Id", "2"))
        .andExpect(status().isNotFound());
  }

  @Test
  void counts_returnsInboxAndMine() throws Exception {
    createAndGetId(1L, 9L, 10L);
    createAndGetId(1L, 11L, 10L);

    mockMvc
        .perform(get("/api/approvals/counts").header("X-Tenant-Id", "1").header("X-User-Id", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.inbox").value(2))
        .andExpect(jsonPath("$.mine").value(0));

    mockMvc
        .perform(get("/api/approvals/counts").header("X-Tenant-Id", "1").header("X-User-Id", "9"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.inbox").value(0))
        .andExpect(jsonPath("$.mine").value(1));
  }

  // ============================================================
  // helpers
  // ============================================================

  private Long createAndGetId(Long tenant, Long requester, Long approver) throws Exception {
    String json =
        "{\"type\":\"EXPENSE\",\"title\":\"报销差旅费\",\"amount\":1280,\"approverId\":"
            + approver
            + "}";
    String body =
        mockMvc
            .perform(
                post("/api/approvals")
                    .header("X-Tenant-Id", String.valueOf(tenant))
                    .header("X-User-Id", String.valueOf(requester))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return Long.parseLong(extractId(body));
  }

  private String extractId(String json) {
    return json.replaceAll(".*\"id\"\\s*:\\s*(\\d+).*", "$1");
  }
}

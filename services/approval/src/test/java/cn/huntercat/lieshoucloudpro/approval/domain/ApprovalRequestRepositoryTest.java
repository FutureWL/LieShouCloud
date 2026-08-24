package cn.huntercat.lieshoucloudpro.approval.domain;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

import cn.huntercat.lieshoucloudpro.approval.PostgresTestSupport;
import java.math.BigDecimal;
import java.util.List;

/** 审批 Repository 租户隔离 + 待办/发起查询测试（ADR-0025 模式 · ADR-0032）. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class ApprovalRequestRepositoryTest extends PostgresTestSupport {

  @Autowired ApprovalRequestRepository repo;

  private ApprovalRequest request(Long tenant, Long requester, Long approver, String status) {
    ApprovalRequest a =
        new ApprovalRequest(
            tenant,
            ApprovalRequest.Type.EXPENSE,
            "报销差旅费",
            new BigDecimal("100"),
            requester,
            approver);
    a.setStatus(ApprovalRequest.Status.valueOf(status));
    return a;
  }

  @Test
  void tenantIsolation() {
    repo.save(request(1L, 9L, 10L, "PENDING"));
    repo.save(request(1L, 9L, 10L, "APPROVED"));
    repo.save(request(2L, 9L, 10L, "PENDING"));

    List<ApprovalRequest> t1 = repo.findTenantRequests(1L, null, null);
    assertThat(t1).hasSize(2);
    assertThat(t1).extracting(ApprovalRequest::getTenantId).containsOnly(1L);
  }

  @Test
  void typeAndStatusFilters() {
    repo.save(request(1L, 9L, 10L, "PENDING"));
    repo.save(request(1L, 9L, 10L, "APPROVED"));

    assertThat(repo.findTenantRequests(1L, null, ApprovalRequest.Status.APPROVED)).hasSize(1);
    assertThat(
            repo.findTenantRequests(
                1L, ApprovalRequest.Type.EXPENSE, ApprovalRequest.Status.PENDING))
        .hasSize(1);
    assertThat(repo.findTenantRequests(1L, ApprovalRequest.Type.SALE, null)).isEmpty();
  }

  @Test
  void requesterAndInboxQueries() {
    repo.save(request(1L, 9L, 10L, "PENDING"));
    repo.save(request(1L, 9L, 11L, "PENDING"));
    repo.save(request(1L, 12L, 10L, "PENDING"));
    repo.save(request(1L, 9L, 10L, "APPROVED"));

    // 我发起的（status 过滤）
    assertThat(repo.findByRequester(1L, 9L, null)).hasSize(3);
    assertThat(repo.findByRequester(1L, 9L, ApprovalRequest.Status.APPROVED)).hasSize(1);
    // 待我审批（只看 PENDING）
    assertThat(repo.findInbox(1L, 10L)).hasSize(2);
    assertThat(repo.findInbox(1L, 11L)).hasSize(1);
    // 计数
    assertThat(repo.countByTenantIdAndApproverIdAndStatus(1L, 10L, ApprovalRequest.Status.PENDING))
        .isEqualTo(2);
    assertThat(repo.countByTenantIdAndRequesterIdAndStatus(1L, 9L, ApprovalRequest.Status.PENDING))
        .isEqualTo(2);
  }
}

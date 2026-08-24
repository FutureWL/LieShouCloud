package cn.huntercat.lieshoucloudpro.user.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import cn.huntercat.lieshoucloudpro.user.domain.AuditLog;
import cn.huntercat.lieshoucloudpro.user.feign.AuditClient;
import cn.huntercat.lieshoucloudpro.user.feign.CreateAuditLogRequest;

/** AuditService 单测（Feign → core.audit · 成功投递 + 失败降级不阻塞 · ADR-0030 Stage 2）. */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuditService（统一审计 Feign 投递）")
class AuditServiceTest {

  @Mock private AuditClient auditClient;

  private AuditService audit;

  @BeforeEach
  void setUp() {
    audit = new AuditService(auditClient);
  }

  @Test
  @DisplayName("record：成功投递 core.audit（六要素 + sourceService=user）")
  void record_postsToAuditService() {
    AuditLog log =
        audit.record(
            1L,
            7L,
            AuditLog.Action.CREATE,
            "USER",
            13L,
            "创建用户 auditdemo",
            "172.29.0.1",
            "curl/8.0",
            AuditLog.Outcome.SUCCESS,
            "req-1");

    verify(auditClient)
        .create(
            org.mockito.ArgumentMatchers.argThat(
                req ->
                    req.userId() == 7L
                        && "CREATE".equals(req.action())
                        && "USER".equals(req.resourceType())
                        && req.resourceId() == 13L
                        && "user".equals(req.sourceService())
                        && "SUCCESS".equals(req.outcome())),
            eq("1"));
    assertEquals(AuditLog.Action.CREATE, log.getAction());
    assertEquals(1L, log.getTenantId());
  }

  @Test
  @DisplayName("record：audit 服务不可用 → 降级返回 null，不抛异常")
  void record_auditDown_degradesGracefully() {
    doThrow(new RuntimeException("audit service down"))
        .when(auditClient)
        .create(any(CreateAuditLogRequest.class), any());

    assertDoesNotThrow(
        () ->
            audit.record(
                1L,
                1L,
                AuditLog.Action.UPDATE,
                "ROLE",
                2L,
                "改角色",
                null,
                null,
                AuditLog.Outcome.SUCCESS,
                null));
    verify(auditClient).create(any(), any());
  }

  @Test
  @DisplayName("detail 超长截断到 500 再投递")
  void record_truncatesDetail() {
    audit.record(
        1L,
        1L,
        AuditLog.Action.CREATE,
        "USER",
        1L,
        "x".repeat(800),
        null,
        null,
        AuditLog.Outcome.SUCCESS,
        null);

    verify(auditClient)
        .create(
            org.mockito.ArgumentMatchers.argThat(
                req -> req.detail() != null && req.detail().length() == 500),
            any());
  }

  @Test
  @DisplayName("tenantId 为 null（平台操作）→ 投递不带 X-Tenant-Id")
  void record_nullTenant_postsWithoutTenantHeader() {
    audit.record(
        null,
        1L,
        AuditLog.Action.CREATE,
        "TENANT",
        5L,
        "开租户",
        null,
        null,
        AuditLog.Outcome.SUCCESS,
        null);

    verify(auditClient)
        .create(any(CreateAuditLogRequest.class), org.mockito.ArgumentMatchers.isNull());
    verify(auditClient, never()).create(any(), eq(""));
  }
}

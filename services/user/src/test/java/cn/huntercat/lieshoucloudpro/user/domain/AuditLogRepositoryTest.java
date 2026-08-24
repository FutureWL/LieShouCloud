package cn.huntercat.lieshoucloudpro.user.domain;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

import cn.huntercat.lieshoucloudpro.user.PostgresTestSupport;
import cn.huntercat.lieshoucloudpro.user.domain.AuditLog.Action;
import cn.huntercat.lieshoucloudpro.user.domain.AuditLog.Outcome;

/**
 * AuditLog 仓库切片测试（append-only · DATA_SECURITY §7）.
 *
 * <p>验证 Flyway V6 建表 + 保存/按租户查询/计数；审计表不可更新/删除由仓库 不暴露 update/delete 方法保证（JpaRepository 自带
 * delete，本测试仅验证读路径）。
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("AuditLogRepository（JPA 切片 + 真 PG + append-only）")
class AuditLogRepositoryTest extends PostgresTestSupport {

  @Autowired private AuditLogRepository repo;

  private AuditLog log(
      Long tenantId, Long userId, Action action, String resourceType, Long resourceId) {
    AuditLog l = new AuditLog();
    l.setTenantId(tenantId);
    l.setUserId(userId);
    l.setAction(action);
    l.setResourceType(resourceType);
    l.setResourceId(resourceId);
    l.setDetail("测试操作");
    l.setOutcome(Outcome.SUCCESS);
    return l;
  }

  @Test
  @DisplayName("保存后能按租户查询（新→旧）")
  void save_andFindByTenant_returnsNewestFirst() throws Exception {
    repo.save(log(1L, 10L, Action.CREATE, "USER", 100L));
    Thread.sleep(5); // createdAt 同毫秒可能并列，间隔一下保证顺序稳定
    repo.save(log(1L, 10L, Action.DELETE, "USER", 101L));
    repo.save(log(2L, 20L, Action.CREATE, "TENANT", 200L));

    var tenant1 = repo.findByTenantIdOrderByCreatedAtDesc(1L);
    assertThat(tenant1).hasSize(2);
    assertThat(tenant1.get(0).getAction()).isEqualTo(Action.DELETE); // 新→旧
    assertThat(tenant1).allMatch(l -> l.getTenantId() == 1L);

    var tenant2 = repo.findByTenantIdOrderByCreatedAtDesc(2L);
    assertThat(tenant2).hasSize(1);
    assertThat(tenant2.get(0).getResourceType()).isEqualTo("TENANT");
  }

  @Test
  @DisplayName("countByTenantId 按租户计数")
  void countByTenant_isolation() {
    repo.save(log(1L, 10L, Action.CREATE, "USER", 1L));
    repo.save(log(1L, 10L, Action.UPDATE, "USER", 1L));
    repo.save(log(9L, 99L, Action.CREATE, "ROLE", 5L));

    assertThat(repo.countByTenantId(1L)).isEqualTo(2);
    assertThat(repo.countByTenantId(9L)).isEqualTo(1);
    assertThat(repo.countByTenantId(777L)).isZero();
  }
}

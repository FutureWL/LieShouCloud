package cn.huntercat.lieshoucloudpro.file.domain;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

import cn.huntercat.lieshoucloudpro.file.PostgresTestSupport;
import java.util.List;

/**
 * FileRepository 切片测试（{@code @DataJpaTest} + Testcontainers PostgreSQL）.
 *
 * <p>Flyway V1（files 表，独立 history 表）→ Hibernate validate → 租户隔离断言。
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("FileRepository（JPA 切片 + 真 PG + 租户隔离）")
class FileRepositoryTest extends PostgresTestSupport {

  @Autowired private FileRepository repo;

  @Test
  @DisplayName("保存文件元数据后能找回（含审计字段）")
  void save_andFindById() {
    FileEntity saved = repo.save(new FileEntity(1L, "委托合同.pdf", "abc123", "application/pdf", 1024));

    FileEntity found = repo.findById(saved.getId()).orElseThrow();
    assertThat(found.getTenantId()).isEqualTo(1L);
    assertThat(found.getOriginalName()).isEqualTo("委托合同.pdf");
    assertThat(found.getStoredName()).isEqualTo("abc123");
    assertThat(found.isDeleted()).isFalse();
    assertThat(found.getCreatedAt()).isNotNull();
  }

  @Test
  @DisplayName("findTenantFiles 只返回指定租户、未软删文件")
  void findTenantFiles_isolation() {
    repo.save(new FileEntity(1L, "租户1文件", "a", "text/plain", 1));
    FileEntity t2 = repo.save(new FileEntity(2L, "租户2文件", "b", "text/plain", 1));
    FileEntity deleted = repo.save(new FileEntity(1L, "已删", "c", "text/plain", 1));
    deleted.setDeleted(true);
    repo.save(deleted);

    List<FileEntity> r = repo.findTenantFiles(1L);
    assertThat(r)
        .extracting(FileEntity::getOriginalName)
        .containsExactly("租户1文件")
        .doesNotContain("租户2文件", "已删");
    assertThat(t2.getTenantId()).isEqualTo(2L);
  }

  @Test
  @DisplayName("findByIdAndTenantIdAndDeletedFalse：跨租户/已软删 → 空")
  void findById_scoped() {
    FileEntity t1 = repo.save(new FileEntity(1L, "租户1文件", "a", "text/plain", 1));
    FileEntity deleted = repo.save(new FileEntity(1L, "已删", "d", "text/plain", 1));
    deleted.setDeleted(true);
    repo.save(deleted);

    assertThat(repo.findByIdAndTenantIdAndDeletedFalse(t1.getId(), 1L)).isPresent();
    assertThat(repo.findByIdAndTenantIdAndDeletedFalse(t1.getId(), 2L)).isEmpty(); // 跨租户
    assertThat(repo.findByIdAndTenantIdAndDeletedFalse(deleted.getId(), 1L)).isEmpty(); // 已软删
  }
}

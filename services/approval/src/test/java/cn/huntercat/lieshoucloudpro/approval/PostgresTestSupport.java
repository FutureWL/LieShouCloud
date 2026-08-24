package cn.huntercat.lieshoucloudpro.approval;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Testcontainers PostgreSQL 基类（ADR-0021 / ADR-0025 / ADR-0032）.
 *
 * <p>approval-service 的 Flyway 迁移（V1__init_approval_schema.sql，独立 history 表 {@code
 * flyway_schema_history_approval}）为 PG 方言，必须用真 PG 验证 + JPA validate 对齐。 镜像 {@code
 * postgres:16-alpine} 与 CI 的 postgres service 版本一致。
 *
 * @see .ai/TESTING.md §9
 * @see .ai/decisions/0021-flyway-schema.md
 * @see .ai/decisions/0032-approval-workflow.md
 */
@Testcontainers
public abstract class PostgresTestSupport {

  @Container
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("lieshoucloudpro")
          .withUsername("postgres")
          .withPassword("postgres");

  @DynamicPropertySource
  static void datasourceProps(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
  }
}

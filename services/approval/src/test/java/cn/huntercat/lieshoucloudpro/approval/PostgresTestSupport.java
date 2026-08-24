package cn.huntercat.lieshoucloudpro.approval;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Testcontainers PostgreSQL 基类（ADR-0021 / ADR-0025 / ADR-0032）.
 *
 * <p>approval-service 的 Flyway 迁移（V1__init_approval_schema.sql，独立 history 表 {@code
 * flyway_schema_history_approval}）为 PG 方言，必须用真 PG 验证 + JPA validate 对齐。 镜像 {@code
 * postgres:16-alpine} 与 CI 的 postgres service 版本一致。
 *
 * <p>生命周期：与 user-service 同款（PR #1 教训）——不用 {@code @Testcontainers @Container}（其生命周期按测试类， afterAll 即
 * stop 容器 → 端口漂移 + Spring 上下文缓存复用旧 URL → Connection refused），改静态初始化块只 start 一次，所有继承类共用同一容器同一端口。容器由
 * Ryuk 在 JVM 退出时回收。
 *
 * @see .ai/TESTING.md §9
 * @see .ai/decisions/0021-flyway-schema.md
 * @see .ai/decisions/0032-approval-workflow.md
 */
public abstract class PostgresTestSupport {

  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("lieshoucloudpro")
          .withUsername("postgres")
          .withPassword("postgres");

  static {
    POSTGRES.start();
  }

  @DynamicPropertySource
  static void datasourceProps(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
  }
}

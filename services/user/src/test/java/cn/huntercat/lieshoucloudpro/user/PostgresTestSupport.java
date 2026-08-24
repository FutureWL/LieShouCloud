package cn.huntercat.lieshoucloudpro.user;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Testcontainers PostgreSQL 基类（Phase 6 · ADR-0021）.
 *
 * <p>从 H2 升级到真实 PostgreSQL 的原因：
 *
 * <ul>
 *   <li>Flyway 迁移脚本为 PG 方言（TEXT[] / COMMENT / CHECK），H2 无法验证；
 *   <li>{@code spring.jpa.hibernate.ddl-auto=validate} 需要与 Flyway 建的表严格对齐，必须真 PG；
 *   <li>TESTING.md §9 P2「Testcontainers 真实依赖」为 Phase 6 待办，本类即落地。
 * </ul>
 *
 * <p>子类共享同一个静态容器（JVM 内只起一个 PG），镜像 {@code postgres:16-alpine} 与 CI 的 postgres service 版本一致。
 *
 * <p><b>生命周期说明</b>：不用 {@code @Testcontainers @Container}（其生命周期按测试类——afterAll 即 stop 容器），
 * 改为<b>静态初始化块只 start 一次</b>：所有继承类共用同一容器同一端口，Spring 上下文缓存复用才安全 （否则每个测试类重建容器换端口，旧 Hikari 池指向已停端口 →
 * Connection refused，CI 实测）。 容器由 Testcontainers Ryuk 在 JVM 退出时回收。
 *
 * @see .ai/TESTING.md §9
 * @see .ai/decisions/0021-flyway-schema.md
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

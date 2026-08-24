package cn.huntercat.lieshoucloudpro.user;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import org.junit.jupiter.api.Test;

/**
 * 全上下文 context load 测试（Testcontainers PostgreSQL · Phase 6）.
 *
 * <p>验证：Spring Boot 完整启动链路（Flyway 迁移 → JPA validate → Nacos discovery 注册尝试）不抛异常。
 */
@SpringBootTest
@ActiveProfiles("test")
class UserApplicationTests extends PostgresTestSupport {

  @Test
  void contextLoads() {}
}

package cn.huntercat.lieshoucloudpro.user;

import org.springframework.boot.test.context.SpringBootTest;

import org.junit.jupiter.api.Test;

/**
 * 全上下文 context load 测试（Testcontainers PostgreSQL · Phase 6）.
 *
 * <p>验证：Spring Boot 完整启动链路（Flyway 迁移 → JPA validate → Nacos discovery 注册尝试）不抛异常。
 *
 * <p><b>profile 约定</b>：不硬编码 profile，跟随默认（application.yml 的 {@code spring.profiles.active:
 * ${SPRING_PROFILES_ACTIVE:dev}}）——本地 dev / CI ci。 原因：PostgresTestSupport 的容器是 JVM 内所有测试类共享的，各上下文的
 * Flyway locations 必须一致 （dev 含 db/seed，ci/test 不含），否则先跑的上下文应用 seed 后，后续上下文 validate 报 “applied
 * migration not resolved locally”（本地 Docker 实测，2026-08）。
 */
@SpringBootTest
class UserApplicationTests extends PostgresTestSupport {

  @Test
  void contextLoads() {}
}

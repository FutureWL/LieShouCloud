package cn.huntercat.lieshoucloudpro.auth;

import org.springframework.boot.test.context.SpringBootTest;

import org.junit.jupiter.api.Test;

/**
 * Phase 5: 基础上下文加载测试（验证 Spring 容器 + Security + Feign 都通）.
 *
 * <p>不依赖 Nacos（@EnableDiscoveryClient 默认 Nacos auto-config; 用
 * spring.cloud.nacos.discovery.enabled=false 在测试 profile 关闭）.
 */
@SpringBootTest(
    properties = {
      "spring.cloud.nacos.discovery.enabled=false",
      "spring.cloud.nacos.discovery.register-enabled=false",
      "app.jwt.secret=test-secret-must-be-at-least-32-bytes-long-1234"
    })
class AuthApplicationTests {

  @Test
  void contextLoads() {
    // 验证 Spring 容器能正常启动 (含 SecurityConfig / FeignClients / JwtService bean)
  }
}

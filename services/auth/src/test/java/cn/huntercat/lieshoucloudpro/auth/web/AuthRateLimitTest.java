package cn.huntercat.lieshoucloudpro.auth.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cn.huntercat.lieshoucloudpro.auth.service.AuthService;
import cn.huntercat.lieshoucloudpro.auth.web.dto.AuthDtos.TokenResponse;

/**
 * Resilience4j RateLimiter 限流测试（P1-4 · ADR-0028）.
 *
 * <p>验证：公开端点（/login /send-code）在限流窗口内超过次数 → 429 RATE_LIMITED； 窗口内允许的次数正常放行（200）。AuthService 全程
 * mock（不碰 Feign/DB）， 限流是控制器切面行为，与业务实现无关。
 *
 * <p>注意：@RateLimiter 计数器是单实例内存态，测试共享容器，login 实例 （10 次/分钟）与 send-code 实例（5 次/分钟）各自独立，互不影响。
 */
@SpringBootTest(
    properties = {
      "spring.cloud.nacos.discovery.enabled=false",
      "spring.cloud.nacos.discovery.register-enabled=false",
      "app.jwt.secret=test-secret-must-be-at-least-32-bytes-long-1234"
    })
@AutoConfigureMockMvc
@DisplayName("Auth RateLimiter（Resilience4j 限流）")
class AuthRateLimitTest {

  private static final TokenResponse TOKENS =
      new TokenResponse("at", "rt", 1800L, "Bearer", 1L, "admin", "huntercat", null, "GENERIC");

  @Autowired private MockMvc mockMvc;

  @MockitoBean private AuthService authService;

  private void stubLogin() {
    when(authService.login(any())).thenReturn(TOKENS);
  }

  @Test
  @DisplayName("login 第 11 次调用（>10 次/分钟）→ 429 RATE_LIMITED")
  void login_exceedsLimit_returns429() throws Exception {
    stubLogin();
    String body = "{\"username\":\"admin\",\"password\":\"x\",\"tenantCode\":\"huntercat\"}";

    for (int i = 0; i < 10; i++) {
      mockMvc
          .perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
          .andExpect(status().isOk());
    }

    // 第 11 次：超出 authLogin（10/min）→ 429
    mockMvc
        .perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isTooManyRequests())
        .andExpect(jsonPath("$.error").value("RATE_LIMITED"));
  }

  @Test
  @DisplayName("send-code 第 6 次调用（>5 次/分钟）→ 429 RATE_LIMITED（与 login 实例独立）")
  void sendCode_exceedsLimit_returns429() throws Exception {
    stubLogin(); // login 实例可能已被上一个用例消耗；send-code 实例独立，不受影响
    String body = "{\"channel\":\"EMAIL\",\"target\":\"a@b.cn\",\"purpose\":\"LOGIN\"}";

    for (int i = 0; i < 5; i++) {
      mockMvc
          .perform(
              post("/api/auth/send-code").contentType(MediaType.APPLICATION_JSON).content(body))
          .andExpect(status().isNoContent());
    }

    mockMvc
        .perform(post("/api/auth/send-code").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isTooManyRequests())
        .andExpect(jsonPath("$.error").value("RATE_LIMITED"));
  }
}

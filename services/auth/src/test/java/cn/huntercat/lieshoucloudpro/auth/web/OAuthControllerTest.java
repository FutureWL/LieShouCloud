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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cn.huntercat.lieshoucloudpro.auth.service.AuthService;
import cn.huntercat.lieshoucloudpro.auth.service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import cn.huntercat.lieshoucloudpro.auth.web.dto.AuthDtos.TokenResponse;

/**
 * 可信身份登录（OAuth 演示通道）集成测试.
 *
 * <p>覆盖：providers 注册表、authorize（核验通过签发一次性 code / 未知通道 / 成员禁用）、
 * token（code 换 JWT + memberStatus + 安全会话）、无效 code 401、sessions 需 JWT。
 */
@SpringBootTest(
    properties = {
      "spring.cloud.nacos.discovery.enabled=false",
      "spring.cloud.nacos.discovery.register-enabled=false",
      "app.jwt.secret=test-secret-must-be-at-least-32-bytes-long-1234",
      "resilience4j.ratelimiter.instances.authOAuth.limitForPeriod=100"
    })
@AutoConfigureMockMvc
@DisplayName("OAuth 可信身份登录（SECURE WORKSPACE · 演示通道）")
class OAuthControllerTest {

  private static final ObjectMapper JSON = new ObjectMapper();

  @Autowired private MockMvc mockMvc;

  @Autowired private JwtService jwt;

  @MockitoBean private AuthService authService;

  @Test
  @DisplayName("providers：返回可信身份通道注册表（Sign in with ChatGPT）")
  void providers_list() throws Exception {
    mockMvc
        .perform(get("/api/auth/oauth/providers"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].provider").value("chatgpt"))
        .andExpect(jsonPath("$[0].name").value("Sign in with ChatGPT"));
  }

  @Test
  @DisplayName("authorize：成员核验通过 → 一次性授权码（VERIFIED）")
  void authorize_success() throws Exception {
    when(authService.verifyMember(anyString(), anyString())).thenReturn("ACTIVE");
    mockMvc
        .perform(
            post("/api/auth/oauth/authorize")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"provider\":\"chatgpt\",\"memberUsername\":\"admin\",\"tenantCode\":\"jxlkas\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(org.hamcrest.Matchers.startsWith("oc_")))
        .andExpect(jsonPath("$.expiresInSeconds").value(300))
        .andExpect(jsonPath("$.memberUsername").value("admin"))
        .andExpect(jsonPath("$.tenantCode").value("jxlkas"))
        .andExpect(jsonPath("$.memberStatus").value("VERIFIED"));
  }

  @Test
  @DisplayName("authorize：未知通道 → 401 UNKNOWN_PROVIDER；成员非 ACTIVE → 401 MEMBER_*")
  void authorize_invalid() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/oauth/authorize")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"provider\":\"bogus\",\"memberUsername\":\"admin\",\"tenantCode\":\"jxlkas\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error").value("OAUTH_FAILED"))
        .andExpect(jsonPath("$.message").value("UNKNOWN_PROVIDER"));

    when(authService.verifyMember(anyString(), anyString())).thenReturn("DISABLED");
    mockMvc
        .perform(
            post("/api/auth/oauth/authorize")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"provider\":\"chatgpt\",\"memberUsername\":\"admin\",\"tenantCode\":\"jxlkas\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("MEMBER_DISABLED"));
  }

  @Test
  @DisplayName("token：授权码换 JWT（memberStatus VERIFIED + 安全会话）")
  void token_exchange() throws Exception {
    when(authService.verifyMember(anyString(), anyString())).thenReturn("ACTIVE");
    String code =
        mockMvc
            .perform(
                post("/api/auth/oauth/authorize")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"provider\":\"chatgpt\",\"memberUsername\":\"admin\",\"tenantCode\":\"jxlkas\"}"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String codeValue =
        JSON.readTree(code).get("code").asText();

    when(authService.oauthLogin(anyString(), anyString()))
        .thenReturn(new TokenResponse("at123", "rt123", 1800L, "Bearer", 1L, "admin", "jxlkas", "凌科安时", "LEGALMIND"));

    mockMvc
        .perform(
            post("/api/auth/oauth/token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"" + codeValue + "\",\"tenantCode\":\"jxlkas\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").value("at123"))
        .andExpect(jsonPath("$.provider").value("chatgpt"))
        .andExpect(jsonPath("$.memberStatus").value("VERIFIED"))
        .andExpect(jsonPath("$.sessionAt").isNotEmpty());
  }

  @Test
  @DisplayName("token：无效/一次性 code → 401 INVALID_OAUTH_CODE（重放被拒）")
  void token_invalidCode() throws Exception {
    when(authService.oauthLogin(anyString(), anyString()))
        .thenReturn(new TokenResponse("at", "rt", 1800L, "Bearer", 1L, "admin", "jxlkas", null, "GENERIC"));
    mockMvc
        .perform(
            post("/api/auth/oauth/token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"oc_bogus\",\"tenantCode\":\"jxlkas\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("INVALID_OAUTH_CODE"));

    // 一次性：同一 code 换过一次后再用 → 401
    when(authService.verifyMember(anyString(), anyString())).thenReturn("ACTIVE");
    String code =
        mockMvc
            .perform(
                post("/api/auth/oauth/authorize")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"provider\":\"chatgpt\",\"memberUsername\":\"admin\",\"tenantCode\":\"jxlkas\"}"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String codeValue = JSON.readTree(code).get("code").asText();
    String body = "{\"code\":\"" + codeValue + "\",\"tenantCode\":\"jxlkas\"}";
    mockMvc.perform(post("/api/auth/oauth/token").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isOk());
    mockMvc.perform(post("/api/auth/oauth/token").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("INVALID_OAUTH_CODE"));
  }

  @Test
  @DisplayName("sessions：无 JWT → 401；带 JWT → 安全会话列表")
  void sessions_auth() throws Exception {
    mockMvc.perform(get("/api/auth/oauth/sessions")).andExpect(status().isUnauthorized());

    // sessions 端点用真实 JwtService 校验 → 用真实 JwtService 生成合法 token
    String realAt =
        jwt.generateAccessToken(1L, 1L, "jxlkas", "admin", java.util.List.of("LEGAL_ADMIN"), java.util.List.of());
    when(authService.verifyMember(anyString(), anyString())).thenReturn("ACTIVE");
    when(authService.oauthLogin(anyString(), anyString()))
        .thenReturn(new TokenResponse(realAt, "rt", 1800L, "Bearer", 1L, "admin", "jxlkas", null, "GENERIC"));
    String code =
        mockMvc
            .perform(
                post("/api/auth/oauth/authorize")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"provider\":\"chatgpt\",\"memberUsername\":\"admin\",\"tenantCode\":\"jxlkas\"}"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String codeValue = JSON.readTree(code).get("code").asText();
    String tokenResp =
        mockMvc
            .perform(
                post("/api/auth/oauth/token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"code\":\"" + codeValue + "\",\"tenantCode\":\"jxlkas\"}"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String at = JSON.readTree(tokenResp).get("accessToken").asText();

    mockMvc
        .perform(get("/api/auth/oauth/sessions").header("Authorization", "Bearer " + at))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].provider").value("chatgpt"))
        .andExpect(jsonPath("$[0].username").value("admin"))
        .andExpect(jsonPath("$[0].memberStatus").value("VERIFIED"))
        .andExpect(jsonPath("$[0].at").isNotEmpty());
  }
}

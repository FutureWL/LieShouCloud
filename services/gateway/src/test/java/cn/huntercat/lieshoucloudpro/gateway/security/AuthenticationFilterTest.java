package cn.huntercat.lieshoucloudpro.gateway.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AuthenticationFilter 白名单单测（集团版子公司切换需鉴权）.
 *
 * <p>switch-tenant 必须走鉴权（gateway 注入 X-User-Id/X-User-Roles 供 auth 校验目标租户授权）， 其余公开 auth
 * 端点（login/send-code/refresh 等）保持白名单放行。
 */
class AuthenticationFilterTest {

  @Test
  @DisplayName("switch-tenant 不在白名单（需鉴权注入用户头）")
  void switchTenant_requiresAuth() {
    assertThat(AuthenticationFilter.isWhitelist("/api/auth/switch-tenant")).isFalse();
  }

  @Test
  @DisplayName("公开 auth 端点保持白名单")
  void publicAuthEndpoints() {
    assertThat(AuthenticationFilter.isWhitelist("/api/auth/login")).isTrue();
    assertThat(AuthenticationFilter.isWhitelist("/api/auth/send-code")).isTrue();
    assertThat(AuthenticationFilter.isWhitelist("/api/auth/refresh")).isTrue();
  }
}

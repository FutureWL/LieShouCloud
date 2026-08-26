package cn.huntercat.lieshoucloudpro.auth.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import cn.huntercat.lieshoucloudpro.auth.feign.TenantAccessClient;
import cn.huntercat.lieshoucloudpro.auth.feign.UserAuthClient;
import cn.huntercat.lieshoucloudpro.auth.feign.dto.TenantAccessItem;
import cn.huntercat.lieshoucloudpro.auth.web.dto.AuthDtos.TokenResponse;
import io.jsonwebtoken.Claims;
import java.util.List;

/**
 * AuthService.switchTenant 集团版子公司切换测试（Phase 1 §3.2 统一账号）.
 *
 * <p>mock TenantAccessClient（不碰 user-service）：切换成功重签 token（tcode/roles 更新为目标租户）， 目标租户不可访问 →
 * NO_ACCESS_TO_TENANT。
 */
@SpringBootTest(
    properties = {
      "spring.cloud.nacos.discovery.enabled=false",
      "spring.cloud.nacos.discovery.register-enabled=false",
      "app.jwt.secret=test-secret-must-be-at-least-32-bytes-long-1234"
    })
@DisplayName("AuthService.switchTenant（集团版子公司切换）")
class AuthServiceSwitchTest {

  @Autowired private AuthService authService;

  @Autowired private JwtService jwtService;

  @MockitoBean private TenantAccessClient tenantAccessClient;

  @MockitoBean private UserAuthClient userAuthClient;

  private static final List<TenantAccessItem> ACCESS =
      List.of(
          new TenantAccessItem(1L, "haizan", "海赞集团", "GENERIC", List.of("TENANT_ADMIN"), true),
          new TenantAccessItem(2L, "nanchang", "南昌猎手猫", "GENERIC", List.of("USER"), false));

  @Test
  @DisplayName("切换成功：重签 token 的 tenantCode/tenantName/roles 指向目标子公司")
  void switchTenant_success() {
    when(tenantAccessClient.tenantAccess(10L, 10L)).thenReturn(ACCESS);

    TokenResponse r = authService.switchTenant(10L, "hq-admin", "nanchang");

    assertEquals("nanchang", r.tenantCode());
    assertEquals("南昌猎手猫", r.tenantName());
    assertEquals("GENERIC", r.tenantEdition());
    assertEquals("hq-admin", r.username());
    // token 可解析且含目标租户 claims
    Claims claims = jwtService.parse(r.accessToken());
    assertEquals(2L, claims.get("tid", Long.class));
    assertEquals("nanchang", claims.get("tcode", String.class));
    assertEquals(List.of("USER"), claims.get("roles", List.class));
  }

  @Test
  @DisplayName("目标租户不可访问 → BadCredentialsException（NO_ACCESS_TO_TENANT）")
  void switchTenant_noAccess_rejected() {
    when(tenantAccessClient.tenantAccess(10L, 10L)).thenReturn(ACCESS);

    assertThrows(
        BadCredentialsException.class,
        () -> authService.switchTenant(10L, "hq-admin", "unknown-tenant"));
  }

  @Test
  @DisplayName("切回主属租户：tcode/roles 为主属租户上下文")
  void switchTenant_backToPrimary() {
    when(tenantAccessClient.tenantAccess(10L, 10L)).thenReturn(ACCESS);

    TokenResponse r = authService.switchTenant(10L, "hq-admin", "haizan");

    assertEquals("haizan", r.tenantCode());
    Claims claims = jwtService.parse(r.accessToken());
    assertEquals(1L, claims.get("tid", Long.class));
    assertEquals(List.of("TENANT_ADMIN"), claims.get("roles", List.class));
  }
}

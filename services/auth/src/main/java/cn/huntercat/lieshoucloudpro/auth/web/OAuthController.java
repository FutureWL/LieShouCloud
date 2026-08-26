package cn.huntercat.lieshoucloudpro.auth.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import cn.huntercat.lieshoucloudpro.auth.service.JwtService;
import cn.huntercat.lieshoucloudpro.auth.service.OAuthService;
import cn.huntercat.lieshoucloudpro.auth.web.dto.AuthDtos.OAuthAuthorizeRequest;
import cn.huntercat.lieshoucloudpro.auth.web.dto.AuthDtos.OAuthAuthorizeResponse;
import cn.huntercat.lieshoucloudpro.auth.web.dto.AuthDtos.OAuthProvider;
import cn.huntercat.lieshoucloudpro.auth.web.dto.AuthDtos.OAuthTokenRequest;
import cn.huntercat.lieshoucloudpro.auth.web.dto.AuthDtos.SecureSession;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;

/**
 * 可信身份登录（SECURE WORKSPACE · OAuth 授权码演示通道）.
 *
 * <p>愿景「Sign in with ChatGPT」：可信身份 provider 完成身份验证 → 一次性授权码 →
 * 换取组织 JWT 会话（不保存密码 · 组织成员核验 · 安全会话可见）。
 *
 * @see .ai/decisions/0017-spring-security-jwt.md（JWT 体系不变，OAuth 为其入口之一）
 */
@RestController
@RequestMapping("/api/auth/oauth")
@Tag(name = "Auth · OAuth", description = "可信身份登录（SECURE WORKSPACE · 演示通道）")
public class OAuthController {

  private final OAuthService oauth;
  private final JwtService jwt;

  public OAuthController(OAuthService oauth, JwtService jwt) {
    this.oauth = oauth;
    this.jwt = jwt;
  }

  @Operation(summary = "List trusted identity providers", description = "可信身份通道注册表（Sign in with ChatGPT 等）。")
  @ApiResponse(responseCode = "200", description = "Provider list")
  @GetMapping("/providers")
  public List<OAuthProvider> providers() {
    return oauth.providers();
  }

  @Operation(
      summary = "Authorize (trusted identity channel)",
      description = "演示通道：可信身份 provider 已完成身份验证，绑定组织成员并签发一次性授权码。正式接入时由真实 provider 接管。")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "一次性授权码"),
    @ApiResponse(responseCode = "400", description = "UNKNOWN_PROVIDER / MEMBER_NOT_FOUND / MEMBER_DISABLED"),
    @ApiResponse(responseCode = "429", description = "RATE_LIMITED")
  })
  @PostMapping("/authorize")
  @RateLimiter(name = "authOAuth")
  public OAuthAuthorizeResponse authorize(@Valid @RequestBody OAuthAuthorizeRequest req) {
    return oauth.authorize(req.provider(), req.memberUsername(), req.tenantCode());
  }

  @Operation(summary = "Exchange auth code for token (OAuth authorization code)", description = "一次性授权码 → 组织 JWT 会话 + 记录安全会话。")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "access/refresh token + membership"),
    @ApiResponse(responseCode = "401", description = "INVALID_OAUTH_CODE / ACCOUNT_*"),
    @ApiResponse(responseCode = "404", description = "USER_NOT_FOUND"),
    @ApiResponse(responseCode = "429", description = "RATE_LIMITED")
  })
  @PostMapping("/token")
  @RateLimiter(name = "authOAuth")
  public Map<String, Object> token(@Valid @RequestBody OAuthTokenRequest req) {
    return oauth.token(req.code(), req.tenantCode());
  }

  @Operation(summary = "Current user secure sessions", description = "上次安全登录（最近 10 条 · 身份×职责分别管理可见角色）。")
  @SecurityRequirement(name = "bearerAuth")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Secure session list"),
    @ApiResponse(responseCode = "401", description = "Missing or invalid token")
  })
  @GetMapping("/sessions")
  public List<SecureSession> sessions(
      @RequestHeader(value = "Authorization", required = false) String authorization) {
    if (authorization == null || !authorization.startsWith("Bearer ")) {
      throw new BadCredentialsException("MISSING_BEARER_TOKEN");
    }
    String token = authorization.substring(7);
    if (!jwt.validate(token)) {
      throw new BadCredentialsException("INVALID_TOKEN");
    }
    Claims c = jwt.parse(token);
    Long userId = c.get("uid", Long.class);
    return oauth.sessionsOf(userId);
  }

  // ============================================================
  // 异常处理（OAuth 通道专属；AuthController 的 handler 不跨类生效）
  // ============================================================

  @ExceptionHandler(BadCredentialsException.class)
  public ResponseEntity<Map<String, String>> handleBad(BadCredentialsException e) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(Map.of("error", "OAUTH_FAILED", "message", e.getMessage()));
  }

  @ExceptionHandler(UsernameNotFoundException.class)
  public ResponseEntity<Map<String, String>> handleNotFound(UsernameNotFoundException e) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(Map.of("error", "USER_NOT_FOUND", "message", e.getMessage()));
  }

  @ExceptionHandler(RequestNotPermitted.class)
  public ResponseEntity<Map<String, String>> handleRateLimit(RequestNotPermitted e) {
    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
        .body(Map.of("error", "RATE_LIMITED", "message", "请求过于频繁，请稍后再试"));
  }
}

package cn.huntercat.lieshoucloudpro.auth.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import cn.huntercat.lieshoucloudpro.auth.service.AuthService;
import cn.huntercat.lieshoucloudpro.auth.service.JwtService;
import cn.huntercat.lieshoucloudpro.auth.web.dto.AuthDtos.LoginRequest;
import cn.huntercat.lieshoucloudpro.auth.web.dto.AuthDtos.LoginWithCodeRequest;
import cn.huntercat.lieshoucloudpro.auth.web.dto.AuthDtos.RefreshRequest;
import cn.huntercat.lieshoucloudpro.auth.web.dto.AuthDtos.RegisterRequest;
import cn.huntercat.lieshoucloudpro.auth.web.dto.AuthDtos.ResetPasswordRequest;
import cn.huntercat.lieshoucloudpro.auth.web.dto.AuthDtos.SendCodeRequest;
import cn.huntercat.lieshoucloudpro.auth.web.dto.AuthDtos.SwitchTenantRequest;
import cn.huntercat.lieshoucloudpro.auth.web.dto.AuthDtos.TokenResponse;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;

/**
 * Auth 控制器: login / refresh / logout / me.
 *
 * @see .ai/decisions/0017-spring-security-jwt.md
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "JWT 鉴权 endpoints")
public class AuthController {

  private final AuthService authService;
  private final JwtService jwtService;

  public AuthController(AuthService authService, JwtService jwtService) {
    this.authService = authService;
    this.jwtService = jwtService;
  }

  @Operation(
      summary = "Login with username/password",
      description = "Returns access + refresh tokens.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Authenticated; tokens returned"),
    @ApiResponse(responseCode = "401", description = "INVALID_CREDENTIALS"),
    @ApiResponse(responseCode = "404", description = "USER_NOT_FOUND")
  })
  @PostMapping("/login")
  @RateLimiter(name = "authLogin")
  public TokenResponse login(@Valid @RequestBody LoginRequest req) {
    return authService.login(req);
  }

  @Operation(
      summary = "Switch tenant context (集团版子公司切换)",
      description = "验证当前用户在目标子公司租户有授权（主属或 user_tenant_grants），重签 tid/tcode/roles 的 token。")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Switched; new tokens returned"),
    @ApiResponse(responseCode = "400", description = "NO_ACCESS_TO_TENANT")
  })
  @PostMapping("/switch-tenant")
  public TokenResponse switchTenant(
      @Valid @RequestBody SwitchTenantRequest req,
      @org.springframework.web.bind.annotation.RequestHeader(value = "X-User-Id", required = false)
          Long userId,
      @org.springframework.web.bind.annotation.RequestHeader(
              value = "X-User-Name",
              required = false)
          String username) {
    if (userId == null) {
      throw new BadCredentialsException("UNAUTHENTICATED");
    }
    return authService.switchTenant(userId, username == null ? "" : username, req.tenantCode());
  }

  // ============================================================
  // Phase 8 · 认证体系扩展（ADR-0023）
  // ============================================================

  @Operation(
      summary = "Send one-time code (SMS/EMAIL)",
      description = "Public endpoint for login/register/reset flows.")
  @ApiResponse(responseCode = "204", description = "Sent")
  @ApiResponse(responseCode = "400", description = "SEND_TOO_FREQUENT / invalid channel")
  @PostMapping("/send-code")
  @RateLimiter(name = "authSendCode")
  public ResponseEntity<Void> sendCode(@Valid @RequestBody SendCodeRequest req) {
    authService.sendCode(req);
    return ResponseEntity.noContent().build();
  }

  @Operation(summary = "Login with SMS/EMAIL code")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Tokens returned"),
    @ApiResponse(responseCode = "401", description = "INVALID_CODE"),
    @ApiResponse(responseCode = "404", description = "USER_NOT_FOUND (register first)")
  })
  @PostMapping("/login/code")
  @RateLimiter(name = "authCodeFlow")
  public TokenResponse loginWithCode(@Valid @RequestBody LoginWithCodeRequest req) {
    return authService.loginWithCode(req);
  }

  @Operation(
      summary = "Register with code (auto login)",
      description = "Verify code, create user in tenant, return tokens.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Created + tokens returned"),
    @ApiResponse(responseCode = "400", description = "INVALID_CODE / REGISTER_FAILED")
  })
  @PostMapping("/register")
  @RateLimiter(name = "authCodeFlow")
  public TokenResponse register(@Valid @RequestBody RegisterRequest req) {
    return authService.register(req);
  }

  @Operation(summary = "Reset password via code")
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "Password reset"),
    @ApiResponse(responseCode = "401", description = "INVALID_CODE"),
    @ApiResponse(responseCode = "404", description = "USER_NOT_FOUND")
  })
  @PostMapping("/reset-password")
  @RateLimiter(name = "authCodeFlow")
  public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
    authService.resetPassword(req);
    return ResponseEntity.noContent().build();
  }

  @Operation(summary = "Exchange refresh token for new access token")
  @ApiResponse(responseCode = "200", description = "New access token returned")
  @ApiResponse(responseCode = "401", description = "INVALID_REFRESH_TOKEN / WRONG_TOKEN_TYPE")
  @PostMapping("/refresh")
  public TokenResponse refresh(@Valid @RequestBody RefreshRequest req) {
    return authService.refresh(req);
  }

  @Operation(
      summary = "Logout (no-op stub)",
      description = "Phase 5: 客户端清 cookie/localStorage 即可; Phase 2+ 加服务端黑名单.")
  @ApiResponse(responseCode = "204", description = "Acknowledged")
  @PostMapping("/logout")
  public ResponseEntity<Void> logout() {
    // Phase 2+: 把 access token jti / refresh token 加入服务端黑名单 (Redis)
    return ResponseEntity.noContent().build();
  }

  @Operation(
      summary = "Get current authenticated user (from JWT)",
      description = "Requires Authorization: Bearer header")
  @SecurityRequirement(name = "bearerAuth")
  @ApiResponse(responseCode = "200", description = "Current user info from JWT claims")
  @ApiResponse(responseCode = "401", description = "Missing or invalid token")
  @GetMapping("/me")
  public Map<String, Object> me(
      @org.springframework.web.bind.annotation.RequestHeader(
              value = "Authorization",
              required = false)
          String authorization) {
    if (authorization == null || !authorization.startsWith("Bearer ")) {
      throw new BadCredentialsException("MISSING_BEARER_TOKEN");
    }
    String token = authorization.substring(7);
    if (!jwtService.validate(token)) {
      throw new BadCredentialsException("INVALID_TOKEN");
    }
    Claims c = jwtService.parse(token);
    return authService.viewFromClaims(c);
  }

  // ============================================================
  // 异常处理（与 ResponseEntity 一起返回标准化错误）
  // ============================================================

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException e) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(Map.of("error", "VALIDATION_FAILED", "message", "username 和 password 不能为空"));
  }

  @ExceptionHandler(UsernameNotFoundException.class)
  public ResponseEntity<Map<String, String>> handleNotFound(UsernameNotFoundException e) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(Map.of("error", "USER_NOT_FOUND", "message", e.getMessage()));
  }

  @ExceptionHandler(BadCredentialsException.class)
  public ResponseEntity<Map<String, String>> handleBad(BadCredentialsException e) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(Map.of("error", "INVALID_CREDENTIALS", "message", e.getMessage()));
  }

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<Map<String, String>> handleAuth(AuthenticationException e) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(Map.of("error", "AUTH_FAILED", "message", e.getMessage()));
  }

  @ExceptionHandler(RequestNotPermitted.class)
  public ResponseEntity<Map<String, String>> handleRateLimit(RequestNotPermitted e) {
    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
        .body(Map.of("error", "RATE_LIMITED", "message", "请求过于频繁，请稍后再试"));
  }
}

package cn.huntercat.lieshoucloudpro.auth.web.dto;

import jakarta.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.media.Schema;

/** Auth 三件套 DTO（登录 / 刷新 / 令牌响应）. */
public final class AuthDtos {

  private AuthDtos() {}

  @Schema(description = "Login request body")
  public record LoginRequest(
      @Schema(description = "Tenant code (default huntercat)", example = "huntercat")
          String tenantCode,
      @Schema(description = "Username", example = "futurewl") @NotBlank String username,
      @Schema(
              description = "Plaintext password (over HTTPS only)",
              example = "correct horse battery")
          @NotBlank
          String password) {}

  @Schema(description = "Refresh token request body")
  public record RefreshRequest(
      @Schema(description = "Refresh token from /login response") @NotBlank String refreshToken) {}

  @Schema(description = "Token response (Phase 5: access + refresh + meta)")
  public record TokenResponse(
      @Schema(description = "JWT access token (Bearer)") String accessToken,
      @Schema(description = "JWT refresh token (long-lived)") String refreshToken,
      @Schema(description = "Access token TTL in seconds", example = "1800") long expiresIn,
      @Schema(description = "Token type", example = "Bearer") String tokenType,
      @Schema(description = "User id (uid claim)") Long userId,
      @Schema(description = "Username (sub claim)") String username,
      @Schema(description = "Tenant code (tcode claim)", example = "huntercat") String tenantCode,
      @Schema(description = "Tenant display name", example = "南昌猎手猫数字科技有限公司") String tenantName,
      @Schema(
              description =
                  "Tenant edition: GENERIC | LAYER | LEGALMIND | ZHIYE | JMZZ (ADR-0035/0036)",
              example = "GENERIC")
          String tenantEdition) {}

  @Schema(description = "Error response body")
  public record ErrorResponse(
      @Schema(description = "Error code") String error,
      @Schema(description = "Human-readable message") String message) {}

  // ============================================================
  // Phase 8 · 认证体系扩展（ADR-0023）：验证码登录 / 注册 / 重置密码
  // ============================================================

  @Schema(description = "Send one-time code request")
  public record SendCodeRequest(
      @Schema(description = "Channel: SMS | EMAIL", example = "SMS") @NotBlank String channel,
      @Schema(description = "Phone or email", example = "13800000000") @NotBlank String target,
      @Schema(description = "Purpose: LOGIN | REGISTER | RESET_PASSWORD", example = "LOGIN")
          @NotBlank
          String purpose) {}

  @Schema(description = "Code login request (SMS/EMAIL verification code)")
  public record LoginWithCodeRequest(
      @Schema(description = "Tenant code", example = "huntercat") String tenantCode,
      @Schema(description = "Channel: SMS | EMAIL", example = "SMS") @NotBlank String channel,
      @Schema(description = "Phone or email", example = "13800000000") @NotBlank String target,
      @Schema(description = "6-digit code", example = "123456") @NotBlank String code) {}

  @Schema(description = "Self/invited registration request")
  public record RegisterRequest(
      @Schema(description = "Tenant code (ignored when inviteCode present)", example = "huntercat")
          String tenantCode,
      @Schema(description = "Login username") @NotBlank String username,
      @Schema(description = "Display name") @NotBlank String displayName,
      @Schema(description = "Password") @NotBlank String password,
      @Schema(description = "Channel: SMS | EMAIL") @NotBlank String channel,
      @Schema(description = "Phone or email") @NotBlank String target,
      @Schema(description = "6-digit code") @NotBlank String code,
      @Schema(
              description =
                  "Invite code (optional; auto-joins tenant with invite role · ADR-0023 P2)",
              example = "AB12CD34")
          String inviteCode) {}

  @Schema(description = "Reset password request (via code)")
  public record ResetPasswordRequest(
      @Schema(description = "Channel: SMS | EMAIL", example = "SMS") @NotBlank String channel,
      @Schema(description = "Phone or email", example = "13800000000") @NotBlank String target,
      @Schema(description = "6-digit code", example = "123456") @NotBlank String code,
      @Schema(description = "New password") @NotBlank String newPassword) {}

  // ============================================================
  // 可信身份登录（SECURE WORKSPACE · OAuth 授权码演示通道 · 愿景「Sign in with ChatGPT」）
  // ============================================================

  /** 可信身份通道描述（OAuth provider 注册表） */
  @Schema(description = "可信身份通道（OAuth provider）")
  public record OAuthProvider(
      @Schema(description = "通道标识", example = "chatgpt") String provider,
      @Schema(description = "展示名称", example = "Sign in with ChatGPT") String name,
      @Schema(description = "理念说明（不保存密码 / 组织成员核验）") String hint,
      @Schema(description = "授权范围") java.util.List<String> permissions) {}

  @Schema(description = "OAuth 授权请求（可信身份通道 · 演示：provider 已完成身份验证）")
  public record OAuthAuthorizeRequest(
      @Schema(description = "通道标识", example = "chatgpt") @NotBlank String provider,
      @Schema(
              description =
                  "组织成员用户名（演示语义：可信身份 provider 已核验该成员身份，此处绑定组织账号）",
              example = "admin")
          @NotBlank
          String memberUsername,
      @Schema(description = "租户编码（默认 huntercat）", example = "jxlkas")
          String tenantCode) {}

  @Schema(description = "OAuth 授权响应（一次性授权码）")
  public record OAuthAuthorizeResponse(
      @Schema(description = "一次性授权码（5 分钟有效 · 仅可用一次）", example = "oc_xxxxxxxx") String code,
      @Schema(description = "防 CSRF state（演示语义）", example = "st_xxxxxxxx") String state,
      @Schema(description = "授权码有效期（秒）", example = "300") long expiresInSeconds,
      @Schema(description = "成员用户名", example = "admin") String memberUsername,
      @Schema(description = "租户编码", example = "jxlkas") String tenantCode,
      @Schema(description = "组织成员核验结果（AUTH REQUIRED）", example = "VERIFIED") String memberStatus) {}

  @Schema(description = "OAuth 授权码换取令牌请求")
  public record OAuthTokenRequest(
      @Schema(description = "一次性授权码", example = "oc_xxxxxxxx") @NotBlank String code,
      @Schema(description = "租户编码", example = "jxlkas") String tenantCode) {}

  /** 安全会话记录（上次安全登录 · 内存保留最近 10 条/用户） */
  @Schema(description = "安全会话记录（上次安全登录）")
  public record SecureSession(
      @Schema(description = "可信身份通道", example = "chatgpt") String provider,
      @Schema(description = "成员用户名", example = "admin") String username,
      @Schema(description = "租户编码", example = "jxlkas") String tenantCode,
      @Schema(description = "角色", example = "LEGAL_ADMIN") java.util.List<String> roles,
      @Schema(description = "登录时间") java.time.Instant at,
      @Schema(description = "组织成员核验结果", example = "VERIFIED") String memberStatus) {}
}

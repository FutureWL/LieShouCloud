package cn.huntercat.lieshoucloudpro.auth.service;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

<<<<<<< HEAD
=======
import cn.huntercat.lieshoucloudpro.auth.feign.TenantAccessClient;
import cn.huntercat.lieshoucloudpro.auth.feign.UserAuthClient;
import cn.huntercat.lieshoucloudpro.auth.feign.dto.TenantAccessItem;
>>>>>>> origin/dev
import cn.huntercat.lieshoucloudpro.auth.feign.dto.UserAuthView;
import cn.huntercat.lieshoucloudpro.auth.port.UserAuthPort;
import cn.huntercat.lieshoucloudpro.auth.web.dto.AuthDtos.LoginRequest;
import cn.huntercat.lieshoucloudpro.auth.web.dto.AuthDtos.LoginWithCodeRequest;
import cn.huntercat.lieshoucloudpro.auth.web.dto.AuthDtos.RefreshRequest;
import cn.huntercat.lieshoucloudpro.auth.web.dto.AuthDtos.RegisterRequest;
import cn.huntercat.lieshoucloudpro.auth.web.dto.AuthDtos.ResetPasswordRequest;
import cn.huntercat.lieshoucloudpro.auth.web.dto.AuthDtos.SendCodeRequest;
import cn.huntercat.lieshoucloudpro.auth.web.dto.AuthDtos.TokenResponse;
import io.jsonwebtoken.Claims;
import java.util.List;
import java.util.Map;

/**
 * Auth 业务服务: login + refresh + me.
 *
 * <p>不持有 user 表；通过 Feign 调 user-service 验证.
 */
@Service
public class AuthService {

  /** 默认租户编码（未传 tenantCode 时） · ADR-0022 */
  private static final String DEFAULT_TENANT_CODE = "huntercat";

  private final JwtService jwt;
<<<<<<< HEAD
  private final UserAuthPort userClient;
  private final PasswordEncoder passwordEncoder;

  public AuthService(JwtService jwt, UserAuthPort userClient, PasswordEncoder passwordEncoder) {
=======
  private final UserAuthClient userClient;
  private final TenantAccessClient tenantAccessClient;
  private final PasswordEncoder passwordEncoder;

  public AuthService(
      JwtService jwt,
      UserAuthClient userClient,
      TenantAccessClient tenantAccessClient,
      PasswordEncoder passwordEncoder) {
>>>>>>> origin/dev
    this.jwt = jwt;
    this.userClient = userClient;
    this.tenantAccessClient = tenantAccessClient;
    this.passwordEncoder = passwordEncoder;
  }

  /**
   * 登录: tenantCode + username + password → access + refresh tokens.
   *
   * <p>Phase 6: 校验账户 status（非 ACTIVE 拒绝登录）；登录成功回写 last_login_at.
   *
   * <p>Phase 8（ADR-0022）: 按租户鉴权，JWT 带 tid/tcode.
   *
   * @throws UsernameNotFoundException 用户不存在
   * @throws BadCredentialsException 密码错误或账户被禁用/锁定
   */
  public TokenResponse login(LoginRequest req) {
    String tenantCode =
        (req.tenantCode() == null || req.tenantCode().isBlank())
            ? DEFAULT_TENANT_CODE
            : req.tenantCode();
    UserAuthView user;
    try {
      user = userClient.findByTenantAndUsername(tenantCode, req.username());
    } catch (feign.FeignException e) {
      // 403 = 租户被停用（user-service 返回 TENANT_DISABLED）
      if (e.status() == 403) {
        throw new BadCredentialsException("TENANT_DISABLED");
      }
      throw new UsernameNotFoundException("USER_NOT_FOUND: " + req.username());
    } catch (Exception e) {
      throw new UsernameNotFoundException("USER_NOT_FOUND: " + req.username());
    }
    if (user == null || user.passwordHash() == null) {
      throw new UsernameNotFoundException("USER_NOT_FOUND: " + req.username());
    }
    if (!passwordEncoder.matches(req.password(), user.passwordHash())) {
      throw new BadCredentialsException("INVALID_CREDENTIALS");
    }
    // Phase 6: 账户状态校验（null 兜底 ACTIVE，兼容旧 user-service）
    String status = user.status() == null ? "ACTIVE" : user.status();
    if (!"ACTIVE".equals(status)) {
      throw new BadCredentialsException("ACCOUNT_" + status);
    }
    List<String> roles =
        user.roles() == null || user.roles().isEmpty() ? List.of("USER") : user.roles();
    List<String> permissions = user.permissions() == null ? List.of() : user.permissions();
    String access =
        jwt.generateAccessToken(
            user.id(), user.tenantId(), tenantCode, user.username(), roles, permissions);
    String refresh = jwt.generateRefreshToken(user.id(), user.username());
    markLastLogin(user.id());
    return new TokenResponse(
        access,
        refresh,
        jwt.getAccessTtlSeconds(),
        "Bearer",
        user.id(),
        user.username(),
        tenantCode,
        user.tenantName(),
        user.tenantEdition());
  }

  /** 登录成功回写 last_login_at（失败静默，不影响登录主流程）. */
  private void markLastLogin(Long userId) {
    try {
      userClient.markLastLogin(userId);
    } catch (Exception ignored) {
      // 回写失败不阻断登录（user-service 暂不可达时降级）
    }
  }

  // ============================================================
  // 可信身份登录（SECURE WORKSPACE · OAuth 通道）
  // ============================================================

  /**
   * 组织成员核验（AUTH REQUIRED）：返回成员状态 ACTIVE / DISABLED / NOT_FOUND.
   *
   * <p>供 OAuth 授权前置校验：可信身份 provider 已完成身份验证后，核对组织成员资格与有效期。
   */
  public String verifyMember(String tenantCode, String username) {
    String tcode = (tenantCode == null || tenantCode.isBlank()) ? DEFAULT_TENANT_CODE : tenantCode;
    try {
      UserAuthView u = userClient.findByTenantAndUsername(tcode, username);
      if (u == null || u.id() == null) return "NOT_FOUND";
      return u.status() == null ? "ACTIVE" : u.status();
    } catch (Exception e) {
      return "NOT_FOUND";
    }
  }

  /**
   * 可信身份登录（OAuth token 阶段）：按成员用户名签发 JWT（不校验密码—— 身份已由可信身份 provider 验证，密码通道不参与，符合「不保存密码」理念）。
   *
   * <p>仍执行组织成员核验：成员不存在 / 非 ACTIVE 拒绝签发。
   */
  public TokenResponse oauthLogin(String tenantCode, String username) {
    String tcode = (tenantCode == null || tenantCode.isBlank()) ? DEFAULT_TENANT_CODE : tenantCode;
    UserAuthView user;
    try {
      user = userClient.findByTenantAndUsername(tcode, username);
    } catch (Exception e) {
      throw new UsernameNotFoundException("USER_NOT_FOUND: " + username);
    }
    if (user == null || user.id() == null) {
      // OAuth 通道不依赖密码（身份已由可信身份 provider 验证 · 不保存密码理念）
      throw new UsernameNotFoundException("USER_NOT_FOUND: " + username);
    }
    String status = user.status() == null ? "ACTIVE" : user.status();
    if (!"ACTIVE".equals(status)) {
      throw new BadCredentialsException("ACCOUNT_" + status);
    }
    return issueTokens(user, tcode);
  }

  // ============================================================
  // Phase 8 · 认证体系扩展（ADR-0023）：验证码登录 / 注册 / 重置密码
  // ============================================================

  /** 发送验证码（短信/邮箱） */
  public void sendCode(SendCodeRequest req) {
    try {
      userClient.sendVerificationCode(
          Map.of("channel", req.channel(), "target", req.target(), "purpose", req.purpose()));
    } catch (feign.FeignException e) {
      if (e.status() == 400) {
        throw new BadCredentialsException("SEND_CODE_FAILED");
      }
      throw new BadCredentialsException("CODE_SERVICE_UNAVAILABLE");
    }
  }

  /** 验证码登录：校验 code → 按 phone/email 查用户 → JWT */
  public TokenResponse loginWithCode(LoginWithCodeRequest req) {
    verifyCode(req.channel(), req.target(), "LOGIN", req.code());
    UserAuthView user = findUserByTarget(req.channel(), req.target());
    if (user == null || user.id() == null) {
      throw new UsernameNotFoundException("USER_NOT_FOUND: " + req.target());
    }
    return issueTokens(user, req.tenantCode());
  }

  /** 注册（验证码）→ 创建用户 → 注册即登录 */
  public TokenResponse register(RegisterRequest req) {
    verifyCode(req.channel(), req.target(), "REGISTER", req.code());
    Map<String, String> createBody = new java.util.HashMap<>();
    createBody.put("username", req.username());
    createBody.put("displayName", req.displayName());
    createBody.put("password", req.password());
    if (req.inviteCode() != null && !req.inviteCode().isBlank()) {
      // 邀请注册：租户/角色由 user-service 按邀请码解析（ADR-0023 P2）
      createBody.put("inviteCode", req.inviteCode());
    } else {
      createBody.put(
          "tenantCode", req.tenantCode() == null ? DEFAULT_TENANT_CODE : req.tenantCode());
    }
    if ("SMS".equals(req.channel())) {
      createBody.put("phone", req.target());
    } else {
      createBody.put("email", req.target());
    }
    Map<String, Object> created;
    try {
      created = userClient.createUser(createBody);
    } catch (feign.FeignException e) {
      if (e.status() == 400) {
        // 透传具体错误（INVALID_INVITE / USERNAME_TAKEN / TENANT_NOT_FOUND 等）
        String detail = extractError(e);
        throw new BadCredentialsException(detail == null ? "REGISTER_FAILED" : detail);
      }
      throw new BadCredentialsException("USER_SERVICE_UNAVAILABLE");
    }
    Number uid = (Number) created.get("id");
    Number tid = (Number) created.get("tenantId");
    String tcode = (String) created.getOrDefault("tenantCode", DEFAULT_TENANT_CODE);
    String tname = (String) created.get("tenantName");
    String tedition = (String) created.getOrDefault("tenantEdition", "GENERIC");
    return new TokenResponse(
        jwt.generateAccessToken(
            uid.longValue(),
            tid == null ? 0L : tid.longValue(),
            tcode,
            req.username(),
            List.of("USER"),
            List.of()),
        jwt.generateRefreshToken(uid.longValue(), req.username()),
        jwt.getAccessTtlSeconds(),
        "Bearer",
        uid.longValue(),
        req.username(),
        tcode,
        tname,
        tedition);
  }

  /** 忘记密码：校验 code → 按 phone/email 查用户 → 改密 */
  public void resetPassword(ResetPasswordRequest req) {
    verifyCode(req.channel(), req.target(), "RESET_PASSWORD", req.code());
    UserAuthView user = findUserByTarget(req.channel(), req.target());
    if (user == null || user.id() == null) {
      throw new UsernameNotFoundException("USER_NOT_FOUND: " + req.target());
    }
    try {
      userClient.updateUserPassword(user.id(), Map.of("password", req.newPassword()));
    } catch (feign.FeignException e) {
      throw new BadCredentialsException("RESET_FAILED");
    }
  }

  /** 从 Feign 400 响应体提取 error 字段（{error: ...}） */
  private String extractError(feign.FeignException e) {
    try {
      if (e.responseBody().isPresent()) {
        byte[] bytes = e.responseBody().get().array();
        String body = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
        var obj = new com.fasterxml.jackson.databind.ObjectMapper().readTree(body);
        if (obj.has("error")) return obj.get("error").asText();
      }
    } catch (Exception ignored) {
      // 解析失败忽略
    }
    return null;
  }

  /** 校验验证码（失败 → BadCredentialsException） */
  private void verifyCode(String channel, String target, String purpose, String code) {
    try {
      userClient.verifyVerificationCode(
          Map.of("channel", channel, "target", target, "purpose", purpose, "code", code));
    } catch (feign.FeignException e) {
      if (e.status() == 400) {
        throw new BadCredentialsException("INVALID_CODE");
      }
      throw new BadCredentialsException("CODE_SERVICE_UNAVAILABLE");
    }
  }

  /** 按渠道查用户（SMS→phone / EMAIL→email） */
  private UserAuthView findUserByTarget(String channel, String target) {
    try {
      if ("SMS".equals(channel)) {
        return userClient.findByPhone(target);
      }
      return userClient.findByEmail(target);
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * 集团版子公司切换（Phase 1 §3.2 统一账号）：验证目标租户可访问后重签 token.
   *
   * <p>从 user-service {@code /api/tenant-access/user/{userId}} 拿可访问租户列表（主属 + 跨公司授权）， 目标租户不在列表 →
   * {@link BadCredentialsException}（NO_ACCESS_TO_TENANT）。 切换后 JWT 的 tid/tcode/roles
   * 更新为目标子公司租户上下文，各服务请求链自然按新租户处理。
   */
  public TokenResponse switchTenant(Long userId, String username, String tenantCode) {
    List<TenantAccessItem> access = tenantAccessClient.tenantAccess(userId, userId);
    TenantAccessItem target =
        access.stream()
            .filter(i -> i.tenantCode() != null && i.tenantCode().equals(tenantCode))
            .findFirst()
            .orElseThrow(() -> new BadCredentialsException("NO_ACCESS_TO_TENANT"));
    List<String> roles =
        target.roles() == null || target.roles().isEmpty() ? List.of("USER") : target.roles();
    List<String> permissions = target.permissions() == null ? List.of() : target.permissions();
    String accessToken =
        jwt.generateAccessToken(
            userId, target.tenantId(), target.tenantCode(), username, roles, permissions);
    String refresh = jwt.generateRefreshToken(userId, username);
    return new TokenResponse(
        accessToken,
        refresh,
        jwt.getAccessTtlSeconds(),
        "Bearer",
        userId,
        username,
        target.tenantCode(),
        target.tenantName(),
        target.edition());
  }

  /** 签发 access + refresh（含租户维度） */
  private TokenResponse issueTokens(UserAuthView user, String tenantCode) {
    String tcode =
        (tenantCode == null || tenantCode.isBlank())
            ? (user.tenantCode() == null ? DEFAULT_TENANT_CODE : user.tenantCode())
            : tenantCode;
    List<String> roles =
        user.roles() == null || user.roles().isEmpty() ? List.of("USER") : user.roles();
    List<String> permissions = user.permissions() == null ? List.of() : user.permissions();
    String access =
        jwt.generateAccessToken(
            user.id(), user.tenantId(), tcode, user.username(), roles, permissions);
    String refresh = jwt.generateRefreshToken(user.id(), user.username());
    markLastLogin(user.id());
    return new TokenResponse(
        access,
        refresh,
        jwt.getAccessTtlSeconds(),
        "Bearer",
        user.id(),
        user.username(),
        tcode,
        user.tenantName(),
        user.tenantEdition());
  }

  /**
   * 刷新: refresh token → 新 access token.
   *
   * <p>Phase 5 简化：不做服务端黑名单（access 过期前有效; refresh 默认 7 天）; Phase 2+ 加 Redis 黑名单.
   *
   * @throws BadCredentialsException refresh token 无效 / 过期 / 类型错
   */
  public TokenResponse refresh(RefreshRequest req) {
    if (!jwt.validate(req.refreshToken())) {
      throw new BadCredentialsException("INVALID_REFRESH_TOKEN");
    }
    Claims c = jwt.parse(req.refreshToken());
    if (!"refresh".equals(c.get("typ"))) {
      throw new BadCredentialsException("WRONG_TOKEN_TYPE");
    }
    Long userId = c.get("uid", Long.class);
    Long tenantId = c.get("tid", Long.class);
    String tenantCode = c.get("tcode", String.class);
    String username = c.getSubject();
    @SuppressWarnings("unchecked")
    List<String> roles = c.get("roles", List.class);
    if (roles == null) roles = List.of("USER");
    @SuppressWarnings("unchecked")
    List<String> permissions = c.get("permissions", List.class);
    if (permissions == null) permissions = List.of();
    String access =
        jwt.generateAccessToken(userId, tenantId, tenantCode, username, roles, permissions);
    // refresh 保持纯 JWT 校验：tenantName/tenantEdition 未知，置 null（前端刷新不覆盖租户信息）
    return new TokenResponse(
        access,
        req.refreshToken(),
        jwt.getAccessTtlSeconds(),
        "Bearer",
        userId,
        username,
        tenantCode,
        null,
        null);
  }

  /** 给 AuthController.me 用：从已验证的 JWT Claims 提取用户信息. */
  public Map<String, Object> viewFromClaims(Claims claims) {
    return Map.of(
        "userId", claims.get("uid", Long.class),
        "tenantId", claims.get("tid", Long.class),
        "tenantCode", claims.get("tcode", String.class),
        "username", claims.getSubject(),
        "roles", claims.get("roles", List.class),
        "permissions", claims.get("permissions", List.class));
  }
}

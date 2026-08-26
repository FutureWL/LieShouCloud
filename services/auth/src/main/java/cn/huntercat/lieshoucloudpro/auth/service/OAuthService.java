package cn.huntercat.lieshoucloudpro.auth.service;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

import cn.huntercat.lieshoucloudpro.auth.web.dto.AuthDtos.OAuthAuthorizeResponse;
import cn.huntercat.lieshoucloudpro.auth.web.dto.AuthDtos.OAuthProvider;
import cn.huntercat.lieshoucloudpro.auth.web.dto.AuthDtos.SecureSession;
import cn.huntercat.lieshoucloudpro.auth.web.dto.AuthDtos.TokenResponse;
import io.jsonwebtoken.Claims;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 可信身份登录（SECURE WORKSPACE · OAuth 授权码演示通道）.
 *
 * <p>愿景「Sign in with ChatGPT」：可信身份 provider 完成身份验证后，本服务签发组织会话。
 * 语义落地：\n
 * - 不保存密码：OAuth 通道全程无密码字段（身份由 provider 验证）；\n
 * - 组织成员核验（AUTH REQUIRED）：authorize/token 均核验成员状态 ACTIVE；\n
 * - 安全会话：内存保留每用户最近 10 条安全登录记录（上次安全登录可见）；\n
 * - 一次性授权码：5 分钟有效，仅可兑换一次（防重放）。\n
 * <p>演示边界：正式接入真实 OAuth provider（ChatGPT/企业微信等）时，授权端点由 provider
 * 接管（本类 authorize 为演示通道：假设 provider 已完成身份验证，直接绑定组织成员）。
 */
@Service
public class OAuthService {

  private static final long CODE_TTL_SECONDS = 300L;
  private static final int MAX_SESSIONS_PER_USER = 10;

  private final AuthService authService;
  private final JwtService jwt;

  /** 一次性授权码（code → 授权信息）。 */
  private final Map<String, OAuthCode> codes = new ConcurrentHashMap<>();

  /** 安全会话（userId → 最近 N 条）。 */
  private final Map<Long, Deque<SecureSession>> sessions = new ConcurrentHashMap<>();

  public OAuthService(AuthService authService, JwtService jwt) {
    this.authService = authService;
    this.jwt = jwt;
  }

  /** 可信身份通道注册表（未来可信身份通道之一）。 */
  public List<OAuthProvider> providers() {
    return List.of(
        new OAuthProvider(
            "chatgpt",
            "Sign in with ChatGPT",
            "可信身份通道 · 不保存密码 · 组织成员核验",
            List.of("member:verify")),
        new OAuthProvider(
            "wecom",
            "企业微信扫码",
            "组织成员关系（AUTH REQUIRED）· 登录即进入受控工作区",
            List.of("member:verify")));
  }

  /** 授权：核验组织成员 → 签发一次性授权码。 */
  public OAuthAuthorizeResponse authorize(String provider, String memberUsername, String tenantCode) {
    if (providers().stream().noneMatch(p -> p.provider().equals(provider))) {
      throw new BadCredentialsException("UNKNOWN_PROVIDER");
    }
    String status = authService.verifyMember(tenantCode, memberUsername);
    if (!"ACTIVE".equals(status)) {
      throw new BadCredentialsException("MEMBER_" + status);
    }
    String code = "oc_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    codes.put(code, new OAuthCode(provider, tenantCode, memberUsername, Instant.now().plusSeconds(CODE_TTL_SECONDS)));
    return new OAuthAuthorizeResponse(
        code,
        "st_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12),
        CODE_TTL_SECONDS,
        memberUsername,
        tenantCode == null || tenantCode.isBlank() ? "huntercat" : tenantCode,
        "VERIFIED");
  }

  /** 授权码换令牌（OAuth 授权码模式）：一次性 code → JWT + 记录安全会话。 */
  public Map<String, Object> token(String code, String tenantCode) {
    OAuthCode oc = codes.remove(code); // 一次性：无论成败都消费，防重放
    if (oc == null || oc.expiresAt().isBefore(Instant.now())) {
      throw new BadCredentialsException("INVALID_OAUTH_CODE");
    }
    TokenResponse tr = authService.oauthLogin(oc.tenantCode(), oc.username());
    List<String> roles = List.of("USER");
    try {
      Claims claims = jwt.parse(tr.accessToken());
      List<String> r = claims.get("roles", List.class);
      if (r != null) roles = r;
    } catch (Exception ignored) {
      // 令牌解析失败降级 USER（会话记录展示用，不影响签发）
    }
    recordSession(tr.userId(), oc.provider(), tr.username(), tr.tenantCode(), roles);
    Map<String, Object> result = new java.util.LinkedHashMap<>();
    result.put("accessToken", tr.accessToken());
    result.put("refreshToken", tr.refreshToken());
    result.put("expiresIn", tr.expiresIn());
    result.put("tokenType", tr.tokenType());
    result.put("userId", tr.userId());
    result.put("username", tr.username());
    result.put("tenantCode", tr.tenantCode());
    result.put("tenantName", tr.tenantName());
    result.put("tenantEdition", tr.tenantEdition());
    result.put("provider", oc.provider());
    result.put("memberStatus", "VERIFIED");
    result.put("sessionAt", Instant.now().toString());
    return result;
  }

  /** 当前用户安全会话（最近 N 条 · 上次安全登录可见）。 */
  public List<SecureSession> sessionsOf(Long userId) {
    Deque<SecureSession> q = sessions.get(userId);
    if (q == null) return List.of();
    return List.copyOf(q);
  }

  private void recordSession(Long userId, String provider, String username, String tenantCode, List<String> roles) {
    SecureSession s = new SecureSession(
        provider,
        username,
        tenantCode,
        roles == null ? List.of() : roles,
        Instant.now(),
        "VERIFIED");
    sessions.compute(
        userId,
        (k, q) -> {
          Deque<SecureSession> deque = q == null ? new ArrayDeque<>() : q;
          deque.addFirst(s);
          while (deque.size() > MAX_SESSIONS_PER_USER) deque.removeLast();
          return deque;
        });
  }

  /** 授权码承载信息（不可变）。 */
  private record OAuthCode(String provider, String tenantCode, String username, Instant expiresAt) {}
}

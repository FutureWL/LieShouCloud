package cn.huntercat.lieshoucloudpro.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.crypto.SecretKey;

/**
 * JWT 签发 + 验证服务 (HS256).
 *
 * <p>secret 必须在生产 ≥ 32 字节（256 bit）; HS256 算法要求. secret 共享给 auth-service 与 gateway 的 JwtService
 * (同源环境变量).
 *
 * @see .ai/decisions/0017-spring-security-jwt.md
 */
@Service
public class JwtService {

  private static final String CLAIM_UID = "uid";
  private static final String CLAIM_TID = "tid";
  private static final String CLAIM_TCODE = "tcode";
  private static final String CLAIM_ROLES = "roles";
  private static final String CLAIM_PERMISSIONS = "permissions";
  private static final String CLAIM_TYPE = "typ";

  private final String secret;
  private final long accessTtlSeconds;
  private final long refreshTtlSeconds;
  private final String issuer;

  private SecretKey signingKey;

  public JwtService(
      @Value("${app.jwt.secret}") String secret,
      @Value("${app.jwt.access-ttl-seconds:1800}") long accessTtlSeconds,
      @Value("${app.jwt.refresh-ttl-seconds:604800}") long refreshTtlSeconds,
      @Value("${app.jwt.issuer:lieshoucloud}") String issuer) {
    this.secret = secret;
    this.accessTtlSeconds = accessTtlSeconds;
    this.refreshTtlSeconds = refreshTtlSeconds;
    this.issuer = issuer;
  }

  @PostConstruct
  void init() {
    if (secret == null || secret.length() < 32) {
      throw new IllegalStateException(
          "JWT_SECRET 长度不足（HS256 至少 32 字节）；当前长度=" + (secret == null ? 0 : secret.length()));
    }
    this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
  }

  /** 生成 access token (typ=access, 含租户 tid/tcode · ADR-0022；权限码 permissions · ADR-0024 Phase 2). */
  public String generateAccessToken(
      Long userId,
      Long tenantId,
      String tenantCode,
      String username,
      List<String> roles,
      List<String> permissions) {
    Instant now = Instant.now();
    return Jwts.builder()
        .issuer(issuer)
        .subject(username)
        .claims(
            Map.of(
                CLAIM_UID,
                userId,
                CLAIM_TID,
                tenantId == null ? 0L : tenantId,
                CLAIM_TCODE,
                tenantCode == null ? "" : tenantCode,
                CLAIM_ROLES,
                roles == null ? List.of() : roles,
                CLAIM_PERMISSIONS,
                permissions == null ? List.of() : permissions,
                CLAIM_TYPE,
                "access"))
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plusSeconds(accessTtlSeconds)))
        .signWith(signingKey, Jwts.SIG.HS256)
        .compact();
  }

  /** 生成 refresh token (typ=refresh, 仅含 uid + username, 不带 roles). */
  public String generateRefreshToken(Long userId, String username) {
    Instant now = Instant.now();
    return Jwts.builder()
        .issuer(issuer)
        .subject(username)
        .claims(Map.of(CLAIM_UID, userId, CLAIM_TYPE, "refresh"))
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plusSeconds(refreshTtlSeconds)))
        .signWith(signingKey, Jwts.SIG.HS256)
        .compact();
  }

  /** 解析 token; 失败抛 JwtException. */
  public Claims parse(String token) {
    return Jwts.parser()
        .verifyWith(signingKey)
        .requireIssuer(issuer)
        .build()
        .parseSignedClaims(token)
        .getPayload();
  }

  /** 仅验证 token 是否合法 + 未过期. */
  public boolean validate(String token) {
    try {
      parse(token);
      return true;
    } catch (JwtException | IllegalArgumentException e) {
      return false;
    }
  }

  public long getAccessTtlSeconds() {
    return accessTtlSeconds;
  }

  public long getRefreshTtlSeconds() {
    return refreshTtlSeconds;
  }
}

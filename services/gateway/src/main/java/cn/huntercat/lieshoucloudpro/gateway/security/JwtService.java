package cn.huntercat.lieshoucloudpro.gateway.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;

/**
 * gateway 的 JWT 验证服务.
 *
 * <p>与 auth-service 共享 {@code app.jwt.secret} (同源环境变量). 验证 gateway 收到的 Bearer token 合法性,
 * 并把用户信息转发到下游请求头 (X-User-Id / X-User-Name / X-User-Roles).
 *
 * @see .ai/decisions/0017-spring-security-jwt.md
 */
@Service
public class JwtService {

  private final String secret;
  private final String issuer;
  private SecretKey signingKey;

  public JwtService(
      @Value("${app.jwt.secret}") String secret,
      @Value("${app.jwt.issuer:lieshoucloud}") String issuer) {
    this.secret = secret;
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

  public Claims parse(String token) {
    return Jwts.parser()
        .verifyWith(signingKey)
        .requireIssuer(issuer)
        .build()
        .parseSignedClaims(token)
        .getPayload();
  }

  public boolean validate(String token) {
    try {
      parse(token);
      return true;
    } catch (JwtException | IllegalArgumentException e) {
      return false;
    }
  }
}

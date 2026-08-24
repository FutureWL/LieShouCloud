package cn.huntercat.lieshoucloudpro.gateway.security;

import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jsonwebtoken.Claims;
import java.nio.charset.StandardCharsets;
import java.util.List;
import reactor.core.publisher.Mono;

/**
 * gateway 全局鉴权过滤器 (Phase 5 闭环鉴权).
 *
 * <p>白名单: {@code /api/auth/**}, {@code /v3/api-docs/**}, {@code /swagger-ui/**}, {@code
 * /actuator/health}. 其他路径必须带有效 Bearer JWT.
 *
 * <p>验证通过后向请求注入 {@code X-User-Id}, {@code X-User-Name}, {@code X-User-Roles} header, 下游服务可直接读取.
 *
 * @see .ai/decisions/0017-spring-security-jwt.md
 */
@Component
public class AuthenticationFilter implements GlobalFilter, Ordered {

  private static final Logger log = LoggerFactory.getLogger(AuthenticationFilter.class);
  private static final String BEARER_PREFIX = "Bearer ";
  private static final String HDR_X_USER_ID = "X-User-Id";
  private static final String HDR_X_USER_NAME = "X-User-Name";
  private static final String HDR_X_USER_ROLES = "X-User-Roles";
  private static final String HDR_X_TENANT_ID = "X-Tenant-Id";
  private static final String HDR_X_TENANT_CODE = "X-Tenant-Code";

  private final JwtService jwt;

  public AuthenticationFilter(JwtService jwt) {
    this.jwt = jwt;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    String path = exchange.getRequest().getPath().value();

    // 白名单 —— 不需要鉴权
    if (isWhitelist(path)) {
      return chain.filter(exchange);
    }

    // 鉴权
    String auth = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
    if (auth == null || !auth.startsWith(BEARER_PREFIX)) {
      return onError(exchange, HttpStatus.UNAUTHORIZED, "{\"error\":\"MISSING_BEARER_TOKEN\"}");
    }
    String token = auth.substring(BEARER_PREFIX.length()).trim();
    if (!jwt.validate(token)) {
      return onError(exchange, HttpStatus.UNAUTHORIZED, "{\"error\":\"INVALID_OR_EXPIRED_TOKEN\"}");
    }

    Claims claims;
    try {
      claims = jwt.parse(token);
    } catch (Exception e) {
      log.warn("Failed to parse JWT claims for path {}: {}", path, e.getMessage());
      return onError(exchange, HttpStatus.UNAUTHORIZED, "{\"error\":\"MALFORMED_TOKEN\"}");
    }

    Long uid = claims.get("uid", Long.class);
    Long tenantId = claims.get("tid", Long.class);
    String tenantCode = claims.get("tcode", String.class);
    String username = claims.getSubject();
    @SuppressWarnings("unchecked")
    List<String> roles = claims.get("roles", List.class);

    // 注入用户信息到请求头（让下游服务可直接读；Phase 8 含租户维度 ADR-0022）
    ServerHttpRequest mutated =
        exchange
            .getRequest()
            .mutate()
            .header(HDR_X_USER_ID, uid == null ? "" : String.valueOf(uid))
            .header(HDR_X_USER_NAME, username == null ? "" : username)
            .header(HDR_X_USER_ROLES, roles == null ? "" : String.join(",", roles))
            .header(HDR_X_TENANT_ID, tenantId == null ? "" : String.valueOf(tenantId))
            .header(HDR_X_TENANT_CODE, tenantCode == null ? "" : tenantCode)
            .build();
    return chain.filter(exchange.mutate().request(mutated).build());
  }

  @Override
  public int getOrder() {
    // 在路由之前执行
    return Ordered.HIGHEST_PRECEDENCE + 10;
  }

  private static boolean isWhitelist(String path) {
    return path.startsWith("/api/auth/")
        || path.startsWith("/v3/api-docs/")
        || path.startsWith("/swagger-ui/")
        || path.startsWith("/webjars/")
        // actuator: 顶层 + 下游服务的 actuator/health 探针
        || path.startsWith("/actuator")
        || path.contains("/actuator/")
        // 下游服务自定义 health 端点 (user-service: /api/users/_health 等)
        || path.endsWith("/_health")
        // 服务间调用 (auth-service 调 user-service 拉鉴权视图)
        || path.startsWith("/api/users/auth/")
        // 租户自助开通（公开 · SaaS 增长路径 · issue #24）
        || path.startsWith("/api/tenants/register")
        // 设备 HTTP 接入（ADR-0040 · X-Device-Secret 设备级认证，不经 JWT）
        || path.startsWith("/api/devices/v1/")
        // Nacos 注册回调 (Spring Cloud 心跳/注册)
        || path.startsWith("/nacos/");
  }

  private Mono<Void> onError(ServerWebExchange exchange, HttpStatus status, String jsonBody) {
    exchange.getResponse().setStatusCode(status);
    exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
    DataBuffer buffer =
        exchange.getResponse().bufferFactory().wrap(jsonBody.getBytes(StandardCharsets.UTF_8));
    return exchange.getResponse().writeWith(Mono.just(buffer));
  }
}

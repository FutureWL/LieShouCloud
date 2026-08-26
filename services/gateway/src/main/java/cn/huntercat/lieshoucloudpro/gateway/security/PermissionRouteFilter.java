package cn.huntercat.lieshoucloudpro.gateway.security;

import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;

import java.util.List;
import java.util.Map;
import reactor.core.publisher.Mono;

/**
 * 权限路由过滤器（平台基础层 · ADR-0024 Phase 2 阶段 3 接口级鉴权）.
 *
 * <p>在 {@link AuthenticationFilter} 之后执行：按路径 → 权限码映射校验当前用户的 {@code X-User-Permissions}（来自 JWT
 * permissions claim）。菜单隐藏与接口拒绝从此共用同一数据源。
 *
 * <p>安全默认（渐进迁移，不破坏存量）：
 *
 * <ul>
 *   <li>白名单路径（/api/auth/**、服务间 /api/users/auth/** 等）→ 放行
 *   <li>{@code X-User-Permissions} 为空（旧 token 无 permissions claim）→ 放行（兼容旧登录）
 *   <li>路径未配置权限映射 → 放行（避免误拦未迁移端点）
 *   <li>路径有映射且用户权限码不足 → 403 FORBIDDEN
 * </ul>
 *
 * <p>路径 → 权限码映射（粗粒度域级；最长前缀匹配）：后续可外置 Nacos 配置。
 */
@Component
public class PermissionRouteFilter implements GlobalFilter, Ordered {

  private static final String HDR_X_USER_PERMISSIONS = "X-User-Permissions";

  /** 路径前缀 → 权限码（最长前缀匹配；/api/iot 细粒度：配置类子路径需 iot:config） */
  static final Map<String, String> PATH_PERMISSION =
      Map.ofEntries(
          Map.entry("/api/legal", "legal:use"),
          Map.entry("/api/crm", "crm:use"),
          Map.entry("/api/inventory", "inventory:use"),
          Map.entry("/api/finance", "finance:use"),
          Map.entry("/api/approval", "approval:use"),
          Map.entry("/api/tenants", "tenant:manage"),
          Map.entry("/api/roles", "tenant:manage"),
          Map.entry("/api/audit", "tenant:manage"),
          Map.entry("/api/iot/devices", "iot:config"),
          Map.entry("/api/iot/products", "iot:config"),
          Map.entry("/api/iot/rules", "iot:config"),
          Map.entry("/api/iot", "iot:monitor"));

  /** 解析路径所需权限码（最长前缀匹配；无映射 → null = 放行）。包可见供单测。 */
  static String requiredPermission(String path) {
    String best = null;
    int bestLen = -1;
    for (Map.Entry<String, String> e : PATH_PERMISSION.entrySet()) {
      if (path.startsWith(e.getKey()) && e.getKey().length() > bestLen) {
        best = e.getValue();
        bestLen = e.getKey().length();
      }
    }
    return best;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    String path = exchange.getRequest().getPath().value();

    // 白名单（鉴权过滤器已放行的路径）不再重复校验
    if (isWhitelist(path)) {
      return chain.filter(exchange);
    }

    String required = requiredPermission(path);
    if (required == null) {
      return chain.filter(exchange);
    }

    // 旧 token（无 permissions claim → header 为空）→ 放行（渐进迁移兼容）
    String permsHeader = exchange.getRequest().getHeaders().getFirst(HDR_X_USER_PERMISSIONS);
    if (permsHeader == null || permsHeader.isBlank()) {
      return chain.filter(exchange);
    }

    List<String> perms = List.of(permsHeader.split(","));
    if (!perms.contains(required)) {
      return onError(
          exchange,
          HttpStatus.FORBIDDEN,
          "{\"error\":\"FORBIDDEN\",\"message\":\"缺少权限 " + required + "\"}");
    }
    return chain.filter(exchange);
  }

  private Mono<Void> onError(ServerWebExchange exchange, HttpStatus status, String body) {
    exchange.getResponse().setStatusCode(status);
    exchange
        .getResponse()
        .getHeaders()
        .setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
    byte[] bytes = body.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    return exchange
        .getResponse()
        .writeWith(
            reactor.core.publisher.Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
  }

  /** 与 AuthenticationFilter 相同的白名单（服务间调用 / 公开端点不校验权限） */
  private static boolean isWhitelist(String path) {
    return path.startsWith("/api/auth/")
        || path.startsWith("/v3/api-docs/")
        || path.startsWith("/swagger-ui/")
        || path.startsWith("/webjars/")
        || path.startsWith("/actuator")
        || path.contains("/actuator/")
        || path.endsWith("/_health")
        || path.startsWith("/api/users/auth/");
  }

  @Override
  public int getOrder() {
    // 在 AuthenticationFilter（HIGHEST_PRECEDENCE + 10）之后
    return Ordered.HIGHEST_PRECEDENCE + 20;
  }
}

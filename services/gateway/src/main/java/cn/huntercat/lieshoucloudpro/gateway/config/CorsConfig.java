package cn.huntercat.lieshoucloudpro.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * CORS 白名单配置（详见 .ai/SECURITY.md §5）。
 *
 * <p>Spring Cloud Gateway 基于 WebFlux，必须用 {@code reactive.*} 包下的 CORS 类； 用 {@code CorsWebFilter}
 * 注册到全局路由。
 *
 * <p>关键：禁止 {@code Access-Control-Allow-Origin: *} —— 前端必须经由白名单进入；allowCredentials = true 时 origin
 * 也不能是 *。
 */
@Configuration
public class CorsConfig {

  @Value("${app.cors.allowed-origins:http://localhost:5173,http://localhost:8080}")
  private String allowedOriginsCsv;

  @Value("${app.cors.allowed-methods:GET,POST,PUT,DELETE,OPTIONS}")
  private String allowedMethodsCsv;

  @Value("${app.cors.allowed-headers:Authorization,Content-Type,X-Requested-With}")
  private String allowedHeadersCsv;

  @Value("${app.cors.allow-credentials:true}")
  private boolean allowCredentials;

  @Bean
  public CorsWebFilter corsWebFilter() {
    CorsConfiguration config = new CorsConfiguration();

    // 1. 显式 origin 列表（不能用 *）
    List<String> origins = Arrays.asList(allowedOriginsCsv.split("\\s*,\\s*"));
    origins.forEach(config::addAllowedOrigin);

    // 2. 显式 methods
    Arrays.stream(allowedMethodsCsv.split("\\s*,\\s*")).forEach(config::addAllowedMethod);

    // 3. 显式 headers
    Arrays.stream(allowedHeadersCsv.split("\\s*,\\s*")).forEach(config::addAllowedHeader);

    // 4. credentials 显式
    config.setAllowCredentials(allowCredentials);

    // 5. 预检有效期（秒）—— 1 day
    config.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return new CorsWebFilter(source);
  }
}

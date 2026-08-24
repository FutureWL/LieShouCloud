package cn.huntercat.lieshoucloudpro.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 配置 - Phase 5 闭环鉴权.
 *
 * <p>auth-service 自己只暴露 /api/auth/**; gateway 已在外层验 JWT, 本服务内的 /me 端点 仍需鉴权（直接从 Bearer token 解析
 * username).
 *
 * <p>白名单:
 *
 * <ul>
 *   <li>/api/auth/login, /api/auth/refresh, /api/auth/logout —— 公开
 *   <li>/v3/api-docs/**, /swagger-ui/** —— SpringDoc
 *   <li>/actuator/health —— 健康检查
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.csrf(csrf -> csrf.disable())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth
                    // 整个 auth-service 端点都 permitAll（gateway 外层已验 JWT）
                    // /me 端点由 AuthController 手动解析 Authorization header;
                    // 缺/无效 token 抛 BadCredentialsException, 由 @ExceptionHandler 返 401.
                    .requestMatchers("/api/auth/**")
                    .permitAll()
                    .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                    .permitAll()
                    .requestMatchers("/actuator/health", "/actuator/info")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .httpBasic(b -> b.disable())
        .formLogin(f -> f.disable());
    return http.build();
  }

  /** BCrypt 密码Encoder. auth-service 用其校验从 user-service 拉到的 passwordHash. */
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}

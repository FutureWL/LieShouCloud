package cn.huntercat.lieshoucloudpro.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * OpenAPI SecurityScheme 配置 - Phase 5 闭环鉴权.
 *
 * <p>在 OpenAPI spec 中暴露 bearerAuth scheme, 前端 codegen 客户端会带 Bearer token 中间件.
 */
@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI customOpenAPI() {
    final String bearerAuth = "bearerAuth";
    return new OpenAPI()
        .info(
            new Info()
                .title("LieShou Cloud · Auth Service")
                .version("0.0.1")
                .description("JWT 鉴权服务 (login/refresh/me)")
                .contact(new Contact().name("FutureWL").email("624263934@qq.com"))
                .license(new License().name("MIT")))
        .addSecurityItem(new SecurityRequirement().addList(bearerAuth))
        .components(
            new Components()
                .addSecuritySchemes(
                    bearerAuth,
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description(
                            "在 Authorization header 中带 'Bearer <accessToken>'，由 /api/auth/login 获取.")));
  }
}

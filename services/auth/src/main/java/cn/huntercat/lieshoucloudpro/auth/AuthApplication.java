package cn.huntercat.lieshoucloudpro.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.servers.Server;

/**
 * 猎手云 Pro · Auth 服务入口.
 *
 * <ul>
 *   <li>{@link EnableDiscoveryClient} —— 注册到 Nacos，被 gateway 发现
 *   <li>{@link EnableFeignClients} —— 调 user-service 验证 username/password
 *   <li>{@link OpenAPIDefinition} —— Phase 5 SpringDoc 元信息（含 bearerAuth scheme）
 * </ul>
 *
 * @see .ai/decisions/0017-spring-security-jwt.md
 */
@SpringBootApplication(scanBasePackages = "cn.huntercat.lieshoucloudpro.auth")
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "cn.huntercat.lieshoucloudpro.auth.feign")
@OpenAPIDefinition(
    info =
        @Info(
            title = "LieShou Cloud · Auth Service",
            version = "0.0.1",
            description = "JWT 鉴权服务：login + refresh + me. SpringDoc 自动暴露 bearerAuth scheme.",
            contact = @Contact(name = "FutureWL", email = "624263934@qq.com"),
            license = @License(name = "MIT")),
    servers = {
      @Server(url = "http://localhost:9000", description = "via Gateway (recommended)"),
      @Server(url = "http://localhost:8083", description = "direct (dev only)")
    })
public class AuthApplication {

  public static void main(String[] args) {
    SpringApplication.run(AuthApplication.class, args);
  }
}

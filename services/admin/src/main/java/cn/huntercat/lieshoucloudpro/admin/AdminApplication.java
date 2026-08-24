package cn.huntercat.lieshoucloudpro.admin;

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
 * 猎手云 Pro · Admin 服务入口。
 *
 * <ul>
 *   <li>{@link EnableDiscoveryClient} —— 注册到 Nacos，被 gateway 发现
 *   <li>{@link EnableFeignClients} —— 扫描 {@code cn.huntercat.lieshoucloudpro.admin.feign} 包下的
 *       {@code @FeignClient} 接口，跨服务调用 user-service
 *   <li>{@link OpenAPIDefinition} —— Phase 5 SpringDoc 元信息
 * </ul>
 */
@SpringBootApplication(scanBasePackages = "cn.huntercat.lieshoucloudpro.admin")
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "cn.huntercat.lieshoucloudpro.admin.feign")
@OpenAPIDefinition(
    info =
        @Info(
            title = "LieShou Cloud · Admin Service",
            version = "0.0.1",
            description =
                "Admin-facing API: cross-service orchestration via Feign + Resilience4j circuit breaker",
            contact = @Contact(name = "FutureWL", email = "624263934@qq.com"),
            license = @License(name = "MIT")),
    servers = {
      @Server(url = "http://localhost:9000", description = "via Gateway (recommended)"),
      @Server(url = "http://localhost:8082", description = "direct (dev only)")
    })
public class AdminApplication {

  public static void main(String[] args) {
    SpringApplication.run(AdminApplication.class, args);
  }
}

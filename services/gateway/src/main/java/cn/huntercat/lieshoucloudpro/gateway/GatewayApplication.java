package cn.huntercat.lieshoucloudpro.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;

/**
 * 猎手云 Pro · Gateway 入口。
 *
 * <p>Spring Cloud Gateway（WebFlux-based），负责：
 *
 * <ul>
 *   <li>从 Nacos 拉取服务列表，做 lb:// 服务发现
 *   <li>按路径把请求路由到 user-service / admin-service
 *   <li>对外唯一入口（前端 → gateway:9000 → user/admin）
 *   <li>转发 OpenAPI doc 路径（{@code /v3/api-docs/user → user-service}, {@code /v3/api-docs/admin →
 *       admin-service}）
 * </ul>
 */
@SpringBootApplication
@EnableDiscoveryClient
@OpenAPIDefinition(
    info =
        @Info(
            title = "LieShou Cloud · API Gateway",
            version = "0.0.1",
            description =
                "Spring Cloud Gateway entry-point; routes OpenAPI docs from downstream services.",
            contact = @Contact(name = "FutureWL", email = "624263934@qq.com"),
            license = @License(name = "MIT")))
public class GatewayApplication {

  public static void main(String[] args) {
    SpringApplication.run(GatewayApplication.class, args);
  }
}

package cn.huntercat.lieshoucloudpro.approval;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.servers.Server;

/**
 * 猎手云 Pro · 审批流服务入口（Phase 9 · ADR-0032）.
 *
 * <p>沿用 crm-service 的 ADR-0025 模式：所有业务端点强制读取 gateway 注入的 {@code X-Tenant-Id} header，缺失/非法 →
 * 401；跨租户访问 → 404。
 */
@SpringBootApplication(scanBasePackages = "cn.huntercat.lieshoucloudpro.approval")
@EntityScan(basePackages = "cn.huntercat.lieshoucloudpro.approval.domain")
@EnableJpaRepositories(basePackages = "cn.huntercat.lieshoucloudpro.approval.domain")
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "cn.huntercat.lieshoucloudpro.approval.feign")
@EnableAsync // 审批邮件通知异步发送（不阻塞审批主流程 · ADR-0032）
@OpenAPIDefinition(
    info =
        @Info(
            title = "LieShou Cloud · Approval Service",
            version = "0.0.1",
            description = "审批流 API（通用审批请求：发起/审批/撤销 · 强制租户过滤）",
            contact = @Contact(name = "FutureWL", email = "624263934@qq.com"),
            license = @License(name = "MIT")),
    servers = {
      @Server(url = "http://localhost:9000", description = "via Gateway (recommended)"),
      @Server(url = "http://localhost:8087", description = "direct (dev only)")
    })
public class ApprovalApplication {

  public static void main(String[] args) {
    SpringApplication.run(ApprovalApplication.class, args);
  }
}

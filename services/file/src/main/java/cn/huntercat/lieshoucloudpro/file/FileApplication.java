package cn.huntercat.lieshoucloudpro.file;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.servers.Server;

/**
 * 猎手云 Pro · 文件服务入口（上传/下载/预览/回收站 · 强制 X-Tenant-Id）.
 *
 * <p>与 crm/legal 一致（安全关键）：所有端点强制读取 gateway 注入的 {@code X-Tenant-Id} header（来自 JWT {@code tid}
 * claim），缺失/非法直接 401，不存在"无租户 = 平台"的放行路径（ADR-0025）。
 *
 * <p>存储：本地磁盘（{@code file.store.dir}，容器内 /data/files volume），DB 仅存元数据；生产可切换 OSS。
 */
@SpringBootApplication(scanBasePackages = "cn.huntercat.lieshoucloudpro.file")
@EntityScan(basePackages = "cn.huntercat.lieshoucloudpro.file.domain")
@EnableJpaRepositories(basePackages = "cn.huntercat.lieshoucloudpro.file.domain")
@EnableScheduling
@EnableDiscoveryClient
@OpenAPIDefinition(
    info =
        @Info(
            title = "LieShou Cloud · File Service",
            version = "0.0.1",
            description = "文件服务 API（上传/下载/预览 · 强制 X-Tenant-Id 过滤 · ADR-0025）",
            contact = @Contact(name = "FutureWL", email = "624263934@qq.com"),
            license = @License(name = "MIT")),
    servers = {
      @Server(url = "http://localhost:9000", description = "via Gateway (recommended)"),
      @Server(url = "http://localhost:8099", description = "direct (dev only)")
    })
public class FileApplication {

  public static void main(String[] args) {
    SpringApplication.run(FileApplication.class, args);
  }
}

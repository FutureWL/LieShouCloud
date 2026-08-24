package cn.huntercat.lieshoucloudpro.admin.feign;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import org.springframework.cloud.openfeign.FeignClient;

import cn.huntercat.lieshoucloudpro.admin.feign.dto.UserDTO;

/**
 * 调用 user-service 的 Feign client。
 *
 * <p>{@code name = "lieshoucloud-user"} 必须与 user-service 在 Nacos 注册的 spring.application.name 一致，由
 * Spring Cloud OpenFeign 通过 {@code lb://} 软负载均衡。
 *
 * <p>{@code fallback}：当 user-service 不可用或熔断打开时，返回 fallback 实现—— Resilience4j 触发降级而非 5xx 上抛，保证 admin
 * 服务自身可用。
 */
@FeignClient(name = "lieshoucloud-user", fallback = UserFeignClientFallback.class)
public interface UserFeignClient {

  @GetMapping("/api/users/count")
  Long count();

  @GetMapping("/api/users/{id}")
  UserDTO findById(@PathVariable Long id);

  @GetMapping("/api/users/by-username/{username}")
  UserDTO findByUsername(@PathVariable String username);
}

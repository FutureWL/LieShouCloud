package cn.huntercat.lieshoucloudpro.admin.feign;

import org.springframework.stereotype.Component;

import cn.huntercat.lieshoucloudpro.admin.feign.dto.UserDTO;

/**
 * Feign fallback：user-service 不可用时返回兜底值，避免 admin 服务连锁崩溃。
 *
 * <p>由 {@code spring.cloud.openfeign.circuitbreaker.enabled=true} + Resilience4j 熔断器共同触发。当熔断器进入
 * OPEN 状态，本 fallback 被调用。
 */
@Component
public class UserFeignClientFallback implements UserFeignClient {

  @Override
  public Long count() {
    // -1 表示「user-service 不可用」；上游据需要决定如何呈现
    return -1L;
  }

  @Override
  public UserDTO findById(Long id) {
    return null;
  }

  @Override
  public UserDTO findByUsername(String username) {
    return null;
  }
}

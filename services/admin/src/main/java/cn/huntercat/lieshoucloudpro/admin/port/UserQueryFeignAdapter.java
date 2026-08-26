package cn.huntercat.lieshoucloudpro.admin.port;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import cn.huntercat.lieshoucloudpro.admin.feign.UserFeignClient;
import cn.huntercat.lieshoucloudpro.admin.feign.dto.UserDTO;

/**
 * msa 模式适配器（ARCHITECTURE.md §4.2 ③）：委托 {@link UserFeignClient}（含 Resilience4j 熔断 fallback） 跨进程 HTTP
 * 调用 user-service。
 *
 * <p>激活条件：{@code app.deploy-mode=msa}（缺省即 msa —— 当前 SaaS 形态默认，未配置时保持现状行为）。
 */
@Component
@ConditionalOnProperty(name = "app.deploy-mode", havingValue = "msa", matchIfMissing = true)
public class UserQueryFeignAdapter implements UserQueryPort {

  private final UserFeignClient delegate;

  public UserQueryFeignAdapter(UserFeignClient delegate) {
    this.delegate = delegate;
  }

  @Override
  public Long count() {
    return delegate.count();
  }

  @Override
  public UserDTO findById(Long id) {
    return delegate.findById(id);
  }

  @Override
  public UserDTO findByUsername(String username) {
    return delegate.findByUsername(username);
  }
}

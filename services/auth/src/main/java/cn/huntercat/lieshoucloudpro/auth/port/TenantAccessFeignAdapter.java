package cn.huntercat.lieshoucloudpro.auth.port;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import cn.huntercat.lieshoucloudpro.auth.feign.TenantAccessClient;
import cn.huntercat.lieshoucloudpro.auth.feign.dto.TenantAccessItem;
import java.util.List;

/**
 * msa 模式适配器（ARCHITECTURE.md §4.2 ③）：委托 {@link TenantAccessClient} 跨进程 HTTP 调用 user-service。
 *
 * <p>激活条件：{@code app.deploy-mode=msa}（缺省即 msa —— 当前 SaaS 形态默认，未配置时保持现状行为）。
 */
@Component
@ConditionalOnProperty(name = "app.deploy-mode", havingValue = "msa", matchIfMissing = true)
public class TenantAccessFeignAdapter implements TenantAccessPort {

  private final TenantAccessClient delegate;

  public TenantAccessFeignAdapter(TenantAccessClient delegate) {
    this.delegate = delegate;
  }

  @Override
  public List<TenantAccessItem> tenantAccess(Long userId, Long callerId) {
    return delegate.tenantAccess(userId, callerId);
  }
}

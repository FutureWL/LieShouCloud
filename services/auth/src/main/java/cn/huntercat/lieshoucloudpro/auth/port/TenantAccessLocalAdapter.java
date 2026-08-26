package cn.huntercat.lieshoucloudpro.auth.port;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import cn.huntercat.lieshoucloudpro.auth.feign.dto.TenantAccessItem;
import cn.huntercat.lieshoucloudpro.user.service.TenantAccessService;
import cn.huntercat.lieshoucloudpro.user.service.UserBizException;
import feign.FeignException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * monolith 模式适配器（ARCHITECTURE.md §4.2 ②）：进程内直接调用 user 领域服务（{@link TenantAccessService}）， 零网络开销。
 *
 * <p>激活条件：{@code app.deploy-mode=monolith}。错误语义转译为 {@link FeignException} 子类（404 / 403 / 400， body
 * 带标准化错误码）——AuthService 现有 {@code catch (FeignException e) { e.status() ... }} 逻辑零修改。
 */
@Component
@ConditionalOnProperty(name = "app.deploy-mode", havingValue = "monolith")
public class TenantAccessLocalAdapter implements TenantAccessPort {

  private final TenantAccessService tenantAccess;

  public TenantAccessLocalAdapter(TenantAccessService tenantAccess) {
    this.tenantAccess = tenantAccess;
  }

  @Override
  public List<TenantAccessItem> tenantAccess(Long userId, Long callerId) {
    try {
      return tenantAccess.buildAccess(userId).stream()
          .map(TenantAccessLocalAdapter::toItem)
          .toList();
    } catch (UserBizException e) {
      throw feign(e.getStatus(), e.getError());
    }
  }

  private static TenantAccessItem toItem(
      cn.huntercat.lieshoucloudpro.user.service.dto.TenantAccessDtos.TenantAccessItem v) {
    return new TenantAccessItem(
        v.tenantId(),
        v.tenantCode(),
        v.tenantName(),
        v.edition(),
        v.roles(),
        v.permissions(),
        v.primary());
  }

  /** 构造与 msa 等价的 FeignException（body 带标准化错误码，供 AuthService.extractError 透传）。 */
  private static FeignException feign(int status, String error) {
    byte[] body = ("{\"error\":\"" + error + "\"}").getBytes(StandardCharsets.UTF_8);
    Map<String, java.util.Collection<String>> headers = Map.of();
    return switch (status) {
      case 403 -> new FeignException.Forbidden(error, null, body, headers);
      case 404 -> new FeignException.NotFound(error, null, body, headers);
      default -> new FeignException.BadRequest(error, null, body, headers);
    };
  }
}

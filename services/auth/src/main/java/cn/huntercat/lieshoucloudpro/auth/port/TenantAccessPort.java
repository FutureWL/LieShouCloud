package cn.huntercat.lieshoucloudpro.auth.port;

import cn.huntercat.lieshoucloudpro.auth.feign.dto.TenantAccessItem;
import java.util.List;

/**
 * 服务间调用抽象层（ARCHITECTURE.md §4.2 · Port 模式）。
 *
 * <p>auth → user 租户访问契约（集团版子公司切换器数据源）。业务代码只依赖本接口（= 未来服务契约），不感知单体（monolith） 还是微服务（msa）形态：
 *
 * <ul>
 *   <li>{@code monolith}：进程内直接调用 user 领域服务（{@link TenantAccessLocalAdapter}）
 *   <li>{@code msa}（缺省）：Feign 适配器跨进程调用（{@link TenantAccessFeignAdapter}）
 * </ul>
 *
 * <p>错误语义与 msa 的 HTTP 状态一致，由适配器转译为 {@link feign.FeignException} 子类（404 / 403 / 400）——
 * 调用方（AuthService）现有 catch 逻辑零修改。
 */
public interface TenantAccessPort {

  /** 用户可访问租户列表（主属 + 跨公司授权）；用户/租户不存在 → 404 */
  List<TenantAccessItem> tenantAccess(Long userId, Long callerId);
}

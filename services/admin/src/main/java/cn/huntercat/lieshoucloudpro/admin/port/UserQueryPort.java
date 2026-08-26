package cn.huntercat.lieshoucloudpro.admin.port;

import cn.huntercat.lieshoucloudpro.admin.feign.dto.UserDTO;

/**
 * 服务间调用抽象层（ARCHITECTURE.md §4.2 · Port 模式）。
 *
 * <p>admin → user 用户查询契约。业务代码只依赖本接口（= 未来服务契约），不感知单体（monolith）还是微服务（msa）形态：
 *
 * <ul>
 *   <li>{@code monolith}：进程内直接调用 user 领域服务（{@link UserQueryLocalAdapter}）
 *   <li>{@code msa}（缺省）：Feign 适配器跨进程调用（{@link UserQueryFeignAdapter}，含熔断 fallback）
 * </ul>
 *
 * <p>查询失败（用户不存在 / user-service 不可用）返回 {@code null}——与 Feign fallback 语义一致，admin 侧据此呈现
 * USER_NOT_FOUND / USER_SERVICE_UNAVAILABLE。
 */
public interface UserQueryPort {

  /** 用户总数（无租户上下文需平台管理员，否则 403——与 msa 下 HTTP 语义一致） */
  Long count();

  /** 按 id 查用户；不存在 → null */
  UserDTO findById(Long id);

  /** 按 username 查用户；不存在 → null */
  UserDTO findByUsername(String username);
}

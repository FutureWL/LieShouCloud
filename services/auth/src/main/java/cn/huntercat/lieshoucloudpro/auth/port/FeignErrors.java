package cn.huntercat.lieshoucloudpro.auth.port;

import feign.FeignException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;

/**
 * Feign 错误转译工具（ARCHITECTURE.md §4.2 · Local 适配器公共件）。
 *
 * <p>monolith 模式 Local 适配器把 user 域异常（{@code UserBizException}）转译为与 msa 等价的 {@link FeignException}
 * 子类（404 / 403 / 400，body 带标准化错误码）——调用方（AuthService）现有 {@code catch (FeignException e) { e.status()
 * ... }} 逻辑零修改。UserAuthLocalAdapter / TenantAccessLocalAdapter 共用。
 */
public final class FeignErrors {

  private FeignErrors() {}

  /** 构造与 msa HTTP 状态等价的 FeignException（body 带标准化错误码，供 AuthService.extractError 透传）。 */
  public static FeignException from(int status, String error) {
    byte[] body = ("{\"error\":\"" + error + "\"}").getBytes(StandardCharsets.UTF_8);
    Map<String, Collection<String>> headers = Map.of();
    return switch (status) {
      case 403 -> new FeignException.Forbidden(error, null, body, headers);
      case 404 -> new FeignException.NotFound(error, null, body, headers);
      default -> new FeignException.BadRequest(error, null, body, headers);
    };
  }
}

package cn.huntercat.lieshoucloudpro.user.service;

/**
 * 用户域业务异常（UserService 下沉后 Controller / Local 适配器统一转译）.
 *
 * <p>{@code status} 对齐原 UserController 的 HTTP 语义（400 参数/业务拒绝、403 越权/租户停用、404 不存在），{@code error}
 * 为标准化错误码（INVALID_INVITE / USERNAME_TAKEN / TENANT_NOT_FOUND / TENANT_NOT_ACTIVE / TENANT_DISABLED
 * / FORBIDDEN / INVALID_STATUS 等），前端与 auth-service 均依赖该错误码透传。
 */
public class UserBizException extends RuntimeException {

  private final int status;
  private final String error;

  public UserBizException(int status, String error, String message) {
    super(message);
    this.status = status;
    this.error = error;
  }

  public int getStatus() {
    return status;
  }

  public String getError() {
    return error;
  }
}

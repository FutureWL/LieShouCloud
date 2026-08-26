package cn.huntercat.lieshoucloudpro.user.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import cn.huntercat.lieshoucloudpro.user.service.UserBizException;
import java.util.Map;

/**
 * 用户域业务异常全局转译（自 UserController.okOrError / TenantAccessController.build 的重复 catch 下沉）.
 *
 * <p>统一 {@link UserBizException} → HTTP 状态（400 参数/业务拒绝、403 越权/租户停用、404 不存在）+ 标准化错误码 body（前端与
 * auth-service 均依赖错误码透传）。Controller 不再手写 try/catch。
 *
 * <p>仅处理 HTTP 请求路径；monolith Local 适配器的进程内调用由各适配器自行转译 FeignException，不受影响。
 */
@RestControllerAdvice
public class UserBizExceptionAdvice {

  @ExceptionHandler(UserBizException.class)
  public ResponseEntity<Map<String, String>> handle(UserBizException e) {
    return ResponseEntity.status(e.getStatus()).body(Map.of("error", e.getError()));
  }
}

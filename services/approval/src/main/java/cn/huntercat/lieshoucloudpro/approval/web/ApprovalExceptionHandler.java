package cn.huntercat.lieshoucloudpro.approval.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import cn.huntercat.lieshoucloudpro.approval.service.ApproverResolveException;
import cn.huntercat.lieshoucloudpro.approval.web.ApprovalController.NotFoundException;
import java.util.Map;

/** approval-service 统一异常映射（ADR-0025 模式 · ADR-0032）. */
@RestControllerAdvice
public class ApprovalExceptionHandler {

  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<Map<String, String>> onNotFound(NotFoundException e) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(Map.of("error", "NOT_FOUND", "message", e.getMessage()));
  }

  @ExceptionHandler(TenantContextRequiredException.class)
  public ResponseEntity<Map<String, String>> onMissingTenant(TenantContextRequiredException e) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(Map.of("error", "TENANT_CONTEXT_REQUIRED", "message", e.getMessage()));
  }

  @ExceptionHandler(InvalidTypeException.class)
  public ResponseEntity<Map<String, String>> onInvalidType(InvalidTypeException e) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(Map.of("error", "INVALID_TYPE", "message", e.getMessage()));
  }

  @ExceptionHandler(ApproverResolveException.class)
  public ResponseEntity<Map<String, String>> onApproverResolve(ApproverResolveException e) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(Map.of("error", "APPROVER_RESOLVE_FAILED", "message", e.getMessage()));
  }

  @ExceptionHandler(ApprovalForbiddenException.class)
  public ResponseEntity<Map<String, String>> onForbidden(ApprovalForbiddenException e) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(Map.of("error", "FORBIDDEN", "message", e.getMessage()));
  }

  @ExceptionHandler(AlreadyDecidedException.class)
  public ResponseEntity<Map<String, String>> onAlreadyDecided(AlreadyDecidedException e) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(Map.of("error", "ALREADY_DECIDED", "message", e.getMessage()));
  }
}

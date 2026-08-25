package cn.huntercat.lieshoucloudpro.file.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Map;

/** 文件服务异常 → 统一错误体 {@code {error, message}}。 */
@RestControllerAdvice
public class FileExceptionHandler {

  @ExceptionHandler(TenantContextRequiredException.class)
  public ResponseEntity<Map<String, String>> tenantRequired(TenantContextRequiredException e) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(Map.of("error", "TENANT_CONTEXT_REQUIRED", "message", e.getMessage()));
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ResponseEntity<Map<String, String>> tooLarge(MaxUploadSizeExceededException e) {
    return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
        .body(Map.of("error", "FILE_TOO_LARGE", "message", "文件超过大小限制"));
  }

  @ExceptionHandler(FileStorageException.class)
  public ResponseEntity<Map<String, String>> storage(FileStorageException e) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(Map.of("error", "FILE_STORAGE", "message", e.getMessage()));
  }

  @ExceptionHandler(FileNotFoundException.class)
  public ResponseEntity<Void> notFound(FileNotFoundException e) {
    return ResponseEntity.notFound().build();
  }
}

/** 文件不存在/跨租户/已软删 → 404（不泄露存在性）。 */
class FileNotFoundException extends RuntimeException {
  FileNotFoundException(Long id) {
    super("文件不存在: " + id);
  }
}

package cn.huntercat.lieshoucloudpro.admin.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cn.huntercat.lieshoucloudpro.admin.feign.UserFeignClient;
import cn.huntercat.lieshoucloudpro.admin.feign.dto.UserDTO;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Admin 服务 REST 端点.
 *
 * <p>完整路径（含上下文）：{@code /api/admin/**}（由 gateway 转发）。
 *
 * <p>每个方法都通过 Feign 调 user-service，受 Resilience4j 熔断器保护.
 *
 * @see .ai/decisions/0016-springdoc-openapi.md
 */
@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin", description = "Cross-service admin endpoints (Feign + circuit breaker)")
public class AdminController {

  private final UserFeignClient userClient;

  public AdminController(UserFeignClient userClient) {
    this.userClient = userClient;
  }

  @Operation(
      summary = "Admin health (via Feign call to user-service)",
      description =
          "Aggregates admin status with a live call to user-service; falls back on circuit-open.")
  @ApiResponse(responseCode = "200", description = "OK (or degraded when user-service is down)")
  @GetMapping("/health")
  @CircuitBreaker(name = "userService", fallbackMethod = "healthFallback")
  public Map<String, Object> health() {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("status", "UP");
    body.put("app", "lieshoucloud-admin");
    body.put("timestamp", Instant.now().toString());
    body.put("userServiceUserCount", userClient.count()); // 触发 Feign 调用
    return body;
  }

  /** 健康检查的熔断 fallback：当 user-service 不可用时仍能给出本服务状态。 */
  @SuppressWarnings("unused")
  public Map<String, Object> healthFallback(Throwable t) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("status", "UP");
    body.put("app", "lieshoucloud-admin");
    body.put("degraded", true);
    body.put("userServiceError", t.getClass().getSimpleName() + ": " + t.getMessage());
    return body;
  }

  @Operation(summary = "Get user by id (cross-service via Feign)")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "User found or USER_NOT_FOUND error"),
    @ApiResponse(responseCode = "200", description = "USER_SERVICE_UNAVAILABLE (circuit open)")
  })
  @GetMapping("/users/{id}")
  @CircuitBreaker(name = "userService", fallbackMethod = "getUserFallback")
  public Map<String, Object> getUser(
      @Parameter(description = "User id", example = "1") @PathVariable Long id) {
    UserDTO u = userClient.findById(id);
    return u == null
        ? Map.of("error", "USER_NOT_FOUND")
        : Map.of(
            "id", u.id(),
            "username", u.username(),
            "displayName", u.displayName());
  }

  @SuppressWarnings("unused")
  public Map<String, Object> getUserFallback(Long id, Throwable t) {
    return Map.of(
        "error",
        "USER_SERVICE_UNAVAILABLE",
        "userId",
        id,
        "reason",
        t.getClass().getSimpleName() + ": " + t.getMessage());
  }

  @Operation(summary = "Get user by username (cross-service via Feign)")
  @GetMapping("/users/by-username/{username}")
  public Map<String, Object> getUserByUsername(
      @Parameter(description = "Username", example = "futurewl") @PathVariable String username) {
    UserDTO u = userClient.findByUsername(username);
    return u == null
        ? Map.of("error", "USER_NOT_FOUND")
        : Map.of(
            "id", u.id(),
            "username", u.username(),
            "displayName", u.displayName());
  }
}

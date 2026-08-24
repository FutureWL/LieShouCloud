package cn.huntercat.lieshoucloudpro.user.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import cn.huntercat.lieshoucloudpro.user.domain.VerificationCode;
import cn.huntercat.lieshoucloudpro.user.service.VerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;

/**
 * 验证码端点（service-to-service，auth-service 通过 Feign 调用）.
 *
 * <p>完整路径：{@code /api/users/verification/**}。前端不直接调用，走 {@code /api/auth/send-code} 等。
 */
@RestController
@RequestMapping("/api/users/verification")
@Tag(name = "Verification", description = "One-time code send/verify (service-to-service)")
public class VerificationController {

  private final VerificationService service;

  public VerificationController(VerificationService service) {
    this.service = service;
  }

  @Operation(summary = "Send one-time code (SMS/EMAIL)")
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "Sent"),
    @ApiResponse(responseCode = "400", description = "SEND_TOO_FREQUENT / invalid channel")
  })
  @PostMapping("/send")
  public ResponseEntity<?> send(@Valid @RequestBody SendCodeRequest body) {
    VerificationCode.Channel channel;
    VerificationCode.Purpose purpose;
    try {
      channel = VerificationCode.Channel.valueOf(body.channel());
      purpose = VerificationCode.Purpose.valueOf(body.purpose());
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(Map.of("error", "INVALID_CHANNEL_OR_PURPOSE"));
    }
    try {
      service.send(channel, body.target(), purpose);
    } catch (IllegalStateException e) {
      return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
    return ResponseEntity.noContent().build();
  }

  @Operation(summary = "Verify one-time code (marks used)")
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "Valid, consumed"),
    @ApiResponse(
        responseCode = "400",
        description = "CODE_NOT_FOUND / CODE_EXPIRED / CODE_MISMATCH")
  })
  @PostMapping("/verify")
  public ResponseEntity<?> verify(@Valid @RequestBody VerifyCodeRequest body) {
    try {
      service.verify(
          VerificationCode.Channel.valueOf(body.channel()),
          body.target(),
          VerificationCode.Purpose.valueOf(body.purpose()),
          body.code());
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
    return ResponseEntity.noContent().build();
  }

  public record SendCodeRequest(
      @jakarta.validation.constraints.NotBlank String channel,
      @jakarta.validation.constraints.NotBlank String target,
      @jakarta.validation.constraints.NotBlank String purpose) {}

  public record VerifyCodeRequest(
      @jakarta.validation.constraints.NotBlank String channel,
      @jakarta.validation.constraints.NotBlank String target,
      @jakarta.validation.constraints.NotBlank String purpose,
      @jakarta.validation.constraints.NotBlank String code) {}
}

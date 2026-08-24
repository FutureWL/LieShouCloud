package cn.huntercat.lieshoucloudpro.approval.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.huntercat.lieshoucloudpro.approval.domain.ApprovalRequest;
import cn.huntercat.lieshoucloudpro.approval.feign.UserQueryClient;
import cn.huntercat.lieshoucloudpro.approval.feign.UserView;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/** ApprovalNotifier 单测（邮件通知 · 失败降级 · 不阻塞主流程）. */
@ExtendWith(MockitoExtension.class)
@DisplayName("ApprovalNotifier（审批邮件通知）")
class ApprovalNotifierTest {

  @Mock private JavaMailSender mailSender;
  @Mock private UserQueryClient users;

  private ApprovalNotifier notifier;

  private ApprovalRequest request(Long requesterId, Long approverId) {
    return new ApprovalRequest(
        1L, ApprovalRequest.Type.EXPENSE, "采购服务器", new BigDecimal("5000"), requesterId, approverId);
  }

  @BeforeEach
  void setUp() {
    notifier = new ApprovalNotifier(Optional.of(mailSender), users);
  }

  @Test
  @DisplayName("notifyApprover：查到审批人邮箱 → 发送待办邮件")
  void notifyApprover_sendsMail() {
    when(users.getUserById(2L, "1"))
        .thenReturn(
            new UserView(2L, "bob", "Bob", "bob@huntercat.cn", "ACTIVE", List.of("TENANT_ADMIN")));
    ApprovalRequest r = request(1L, 2L);

    notifier.notifyApprover(1L, r);

    verify(mailSender).send(any(SimpleMailMessage.class));
  }

  @Test
  @DisplayName("notifyRequester：查到提交人邮箱 → 发送结果邮件")
  void notifyRequester_sendsMail() {
    when(users.getUserById(1L, "1"))
        .thenReturn(
            new UserView(1L, "alice", "Alice", "alice@huntercat.cn", "ACTIVE", List.of("USER")));
    ApprovalRequest r = request(1L, 2L);

    notifier.notifyRequester(1L, r, "通过");

    verify(mailSender).send(any(SimpleMailMessage.class));
  }

  @Test
  @DisplayName("用户无邮箱 → 不发信（不抛异常）")
  void userWithoutEmail_skipsSend() {
    when(users.getUserById(1L, "1"))
        .thenReturn(new UserView(1L, "alice", "Alice", null, "ACTIVE", List.of("USER")));
    ApprovalRequest r = request(1L, 2L);

    notifier.notifyRequester(1L, r, "通过");

    verify(mailSender, never()).send(any(SimpleMailMessage.class));
  }

  @Test
  @DisplayName("查用户失败（Feign 异常）→ 降级跳过，不抛异常")
  void userQueryFailure_degradesGracefully() {
    when(users.getUserById(any(), any())).thenThrow(new RuntimeException("user service down"));
    ApprovalRequest r = request(1L, 2L);

    assertDoesNotThrow(() -> notifier.notifyApprover(1L, r));
    verify(mailSender, never()).send(any(SimpleMailMessage.class));
  }

  @Test
  @DisplayName("发送失败 → 降级日志，不抛异常")
  void sendFailure_degradesGracefully() {
    when(users.getUserById(2L, "1"))
        .thenReturn(
            new UserView(2L, "bob", "Bob", "bob@huntercat.cn", "ACTIVE", List.of("TENANT_ADMIN")));
    org.mockito.Mockito.doThrow(new RuntimeException("smtp down"))
        .when(mailSender)
        .send(any(SimpleMailMessage.class));
    ApprovalRequest r = request(1L, 2L);

    assertDoesNotThrow(() -> notifier.notifyApprover(1L, r));
  }

  @Test
  @DisplayName("SMTP 未配置（mailSender 空）→ 旁路，不发信")
  void noMailSender_bypasses() {
    notifier = new ApprovalNotifier(Optional.empty(), users);
    when(users.getUserById(1L, "1"))
        .thenReturn(
            new UserView(1L, "alice", "Alice", "alice@huntercat.cn", "ACTIVE", List.of("USER")));
    ApprovalRequest r = request(1L, 2L);

    assertDoesNotThrow(() -> notifier.notifyRequester(1L, r, "通过"));
    verify(mailSender, never()).send(any(SimpleMailMessage.class));
  }
}

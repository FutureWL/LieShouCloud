package cn.huntercat.lieshoucloudpro.user.service;

import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Properties;

/** SmtpEmailSender 单元测试（mock JavaMailSender，不启动 Spring 上下文）. */
@DisplayName("SmtpEmailSender（SMTP 邮件发送）")
class SmtpEmailSenderTest {

  private static final String FROM = "lieshoucloud@huntercat.cn";
  private static final String TO = "boss@huntercat.cn";

  private static JavaMailSender mockMailSender(MimeMessage message) {
    JavaMailSender mailSender = mock(JavaMailSender.class);
    when(mailSender.createMimeMessage()).thenReturn(message);
    return mailSender;
  }

  private static MimeMessage newMessage() {
    return new MimeMessage(Session.getInstance(new Properties()));
  }

  @Test
  @DisplayName("发送成功：收件人/主题/发件人正确，验证码不落日志")
  void ok() throws Exception {
    MimeMessage message = newMessage();
    JavaMailSender mailSender = mockMailSender(message);
    SmtpEmailSender sender = new SmtpEmailSender(mailSender, "猎手云服务", FROM);

    sender.sendVerificationEmail(TO, "123456");

    verify(mailSender).send(message);
    assertThat(message.getAllRecipients()[0].toString()).isEqualTo(TO);
    assertThat(message.getSubject()).contains("验证码");
    assertThat(message.getFrom()[0].toString()).contains(FROM);
  }

  @Test
  @DisplayName("发件人地址未配置 → IllegalStateException")
  void missingFromAddr() {
    MimeMessage message = newMessage();
    SmtpEmailSender sender = new SmtpEmailSender(mockMailSender(message), "猎手云服务", "");

    assertThatThrownBy(() -> sender.sendVerificationEmail(TO, "123456"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("EMAIL_FROM_ADDR");
  }

  @Test
  @DisplayName("SMTP 发送失败（MailException）→ IllegalStateException")
  void smtpFailure() throws Exception {
    MimeMessage message = newMessage();
    JavaMailSender mailSender = mockMailSender(message);
    doThrow(new MailSendException("connection refused"))
        .when(mailSender)
        .send(any(MimeMessage.class));
    SmtpEmailSender sender = new SmtpEmailSender(mailSender, "猎手云服务", FROM);

    assertThatThrownBy(() -> sender.sendVerificationEmail(TO, "123456"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("connection refused");
  }

  @Test
  @DisplayName("邮箱脱敏：b***@huntercat.cn / 无 @ 返回 ***")
  void maskEmail() {
    assertThat(SmtpEmailSender.maskEmail("boss@huntercat.cn")).isEqualTo("b***@huntercat.cn");
    assertThat(SmtpEmailSender.maskEmail("not-an-email")).isEqualTo("***");
  }
}

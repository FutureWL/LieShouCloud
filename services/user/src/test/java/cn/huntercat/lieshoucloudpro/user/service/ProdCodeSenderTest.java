package cn.huntercat.lieshoucloudpro.user.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import cn.huntercat.lieshoucloudpro.user.domain.VerificationCode;

/** ProdCodeSender 通道分发单元测试（SMS → 阿里云 / EMAIL → SMTP）. */
@DisplayName("ProdCodeSender（生产通道分发）")
class ProdCodeSenderTest {

  @Test
  @DisplayName("SMS 通道 → 委托 AliyunSmsSender")
  void smsDelegates() {
    AliyunSmsSender sms = mock(AliyunSmsSender.class);
    SmtpEmailSender email = mock(SmtpEmailSender.class);
    ProdCodeSender sender = new ProdCodeSender(sms, email);

    sender.send(VerificationCode.Channel.SMS, "13800138000", "123456");

    verify(sms).sendSms("13800138000", "123456");
  }

  @Test
  @DisplayName("EMAIL 通道 → 委托 SmtpEmailSender")
  void emailDelegates() {
    AliyunSmsSender sms = mock(AliyunSmsSender.class);
    SmtpEmailSender email = mock(SmtpEmailSender.class);
    ProdCodeSender sender = new ProdCodeSender(sms, email);

    sender.send(VerificationCode.Channel.EMAIL, "a@b.com", "123456");

    verify(email).sendVerificationEmail("a@b.com", "123456");
  }
}

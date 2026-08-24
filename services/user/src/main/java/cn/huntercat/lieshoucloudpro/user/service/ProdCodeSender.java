package cn.huntercat.lieshoucloudpro.user.service;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import cn.huntercat.lieshoucloudpro.user.domain.VerificationCode;

/**
 * 生产验证码发送器（ADR-0023 Phase 2 落地）.
 *
 * <p>通道分发：
 *
 * <ul>
 *   <li><b>SMS</b> —— 阿里云短信（{@link AliyunSmsSender}，dysmsapi 真实调用）；
 *   <li><b>EMAIL</b> —— 飞书邮箱 SMTP（{@link SmtpEmailSender}，smtp.feishu.cn）。
 * </ul>
 *
 * <p>安全纪律：生产**绝不**把验证码打进日志（与 {@link DevCodeSender} 相反——那是 dev 联调特权）。 接入真实通道时同步更新 {@code
 * deploy/nacos-config} 中相关配置项。
 */
@Component
@Profile("prod")
public class ProdCodeSender implements CodeSender {

  private final AliyunSmsSender smsSender;
  private final SmtpEmailSender emailSender;

  public ProdCodeSender(AliyunSmsSender smsSender, SmtpEmailSender emailSender) {
    this.smsSender = smsSender;
    this.emailSender = emailSender;
  }

  @Override
  public void send(VerificationCode.Channel channel, String target, String code) {
    switch (channel) {
      case SMS:
        smsSender.sendSms(target, code);
        return;
      case EMAIL:
        emailSender.sendVerificationEmail(target, code);
        return;
      default:
        throw new UnsupportedOperationException("未知验证码通道: " + channel);
    }
  }
}

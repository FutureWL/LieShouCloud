package cn.huntercat.lieshoucloudpro.user.service;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.huntercat.lieshoucloudpro.user.domain.VerificationCode;

/**
 * dev 验证码发送器：打印到日志（生产接入阿里云短信 / SMTP 后替换）.
 *
 * <p>日志格式固定为 {@code [LSCP-CODE] channel=... target=... code=...}，便于联调抓取。
 */
@Component
@Profile({"dev", "docker", "test", "ci"})
public class DevCodeSender implements CodeSender {

  private static final Logger log = LoggerFactory.getLogger(DevCodeSender.class);

  @Override
  public void send(VerificationCode.Channel channel, String target, String code) {
    log.info(
        "[LSCP-CODE] channel={} target={} code={} (dev sender - 生产环境接入短信/邮件服务商)",
        channel,
        target,
        code);
  }
}

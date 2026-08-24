package cn.huntercat.lieshoucloudpro.user.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;

/**
 * 阿里云短信发送器（生产验证码通道 · ADR-0023 Phase 2）.
 *
 * <p>封装 dysmsapi 2017-05-25 真实调用，模板变量固定 {@code ${code}}（当前账号模板：登录确认验证码）。
 *
 * <p>安全约定（ADR-0028 / 生产纪律）：
 *
 * <ul>
 *   <li>成功仅记日志 requestId，**不落验证码**；失败仅记 masked 手机号 + 阿里云错误 message，同样不落验证码；
 *   <li>非 OK 响应码 → {@link IllegalStateException}（调用方按业务失败处理，验证码不落库）。
 * </ul>
 *
 * <p>仅 prod profile 生效；dev/docker/test 走 {@link DevCodeSender}（日志旁路）。
 */
@Component
@Profile("prod")
public class AliyunSmsSender {

  private static final Logger log = LoggerFactory.getLogger(AliyunSmsSender.class);

  private static final String OK_CODE = "OK";

  private final Client client;
  private final String signName;
  private final String templateCode;

  public AliyunSmsSender(
      Client client,
      @Value("${ALIYUN_SMS_SIGN_NAME:}") String signName,
      @Value("${ALIYUN_SMS_TEMPLATE_CODE:}") String templateCode) {
    this.client = client;
    this.signName = signName;
    this.templateCode = templateCode;
  }

  /**
   * 发送短信验证码。
   *
   * @param phone 国内手机号（11 位）
   * @param code 6 位数字验证码
   * @throws IllegalStateException 配置缺失 / 阿里云返回非 OK / 网络异常
   */
  public void sendSms(String phone, String code) {
    if (signName.isBlank() || templateCode.isBlank()) {
      throw new IllegalStateException(
          "阿里云短信未完整配置：需要 ALIYUN_SMS_SIGN_NAME + ALIYUN_SMS_TEMPLATE_CODE");
    }
    SendSmsRequest request =
        new SendSmsRequest()
            .setPhoneNumbers(phone)
            .setSignName(signName)
            .setTemplateCode(templateCode)
            .setTemplateParam("{\"code\":\"" + code + "\"}");
    try {
      SendSmsResponse response = client.sendSms(request);
      String respCode =
          response == null || response.getBody() == null ? null : response.getBody().getCode();
      if (!OK_CODE.equals(respCode)) {
        String message =
            response != null && response.getBody() != null
                ? response.getBody().getMessage()
                : "空响应";
        log.error("阿里云短信发送失败 phone={} respCode={} message={}", maskPhone(phone), respCode, message);
        throw new IllegalStateException("阿里云短信发送失败: " + message);
      }
      log.info(
          "阿里云短信发送成功 phone={} requestId={}", maskPhone(phone), response.getBody().getRequestId());
    } catch (IllegalStateException e) {
      throw e;
    } catch (Exception e) {
      log.error("阿里云短信调用异常 phone={}", maskPhone(phone), e);
      throw new IllegalStateException("阿里云短信调用异常: " + e.getMessage(), e);
    }
  }

  /** 手机号脱敏：138****8000。 */
  static String maskPhone(String phone) {
    if (phone == null || phone.length() < 7) {
      return "***";
    }
    return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
  }
}

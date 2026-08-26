package com.csa.official.modules.sys.service;

import com.csa.official.common.cache.KeyValueStore;
import com.csa.official.common.exception.ApiErrorCode;
import com.csa.official.common.exception.CsaException;
import com.csa.official.common.util.AccountNormalizer;
import com.csa.official.modules.sys.entity.MailDelivery;
import com.csa.official.modules.sys.mapper.MailDeliveryMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class MailService {

    public static final String REGISTRATION = "REGISTRATION";
    public static final String PASSWORD_RESET = "PASSWORD_RESET";

    private static final SecureRandom RANDOM = new SecureRandom();

    @Resource
    private KeyValueStore keyValueStore;

    @Resource
    private AsyncMailSender asyncMailSender;

    @Resource
    private MailDeliveryMapper mailDeliveryMapper;

    @Resource
    private MailRecoveryStore mailRecoveryStore;

    public void sendCode(String to) {
        sendCode(to, REGISTRATION);
    }

    public void sendPasswordResetCode(String to) {
        sendCode(to, PASSWORD_RESET);
    }

    public void sendCode(String to, String messageType) {
        String normalizedTo = AccountNormalizer.email(to);
        String codeKey = codeKey(normalizedTo, messageType);
        String limitKey = limitKey(normalizedTo, messageType);
        if (keyValueStore.hasKey(limitKey)) {
            throw new CsaException(ApiErrorCode.RATE_LIMITED, "请勿频繁发送验证码");
        }

        String code = String.valueOf(RANDOM.nextInt(900000) + 100000);

        // 先落库验证码与限流标记（同步、快），再把耗时的 SMTP 发送交给异步线程池，
        // 避免阻塞 HTTP 请求线程。验证码 5 分钟自动失效，发送失败用户可稍后重试。
        keyValueStore.set(codeKey, code, 5, TimeUnit.MINUTES);
        keyValueStore.set(limitKey, "1", 60, TimeUnit.SECONDS);

        MailDelivery delivery = new MailDelivery();
        delivery.setRecipientHash(sha256(normalizedTo));
        delivery.setRecipientMasked(maskEmail(normalizedTo));
        delivery.setMessageType(messageType);
        delivery.setStatus("PENDING");
        delivery.setAttemptCount(0);
        try {
            mailDeliveryMapper.insert(delivery);
            mailRecoveryStore.save(
                    delivery.getId(), normalizedTo, messageType, codeKey, limitKey, code);
        } catch (RuntimeException e) {
            keyValueStore.delete(codeKey);
            keyValueStore.delete(limitKey);
            markRecoveryUnavailable(delivery.getId());
            throw new CsaException(ApiErrorCode.SERVICE_UNAVAILABLE, "邮件服务暂时不可用", e);
        }

        asyncMailSender.sendVerifyCode(normalizedTo, code, messageType, codeKey, limitKey, delivery.getId());
    }

    private void markRecoveryUnavailable(Long deliveryId) {
        if (deliveryId == null) {
            return;
        }
        try {
            MailDelivery failed = new MailDelivery();
            failed.setId(deliveryId);
            failed.setStatus("FAILED");
            failed.setLastErrorCode("RECOVERY_STATE_UNAVAILABLE");
            failed.setLastErrorMessage("Temporary recovery state could not be persisted");
            mailDeliveryMapper.updateById(failed);
        } catch (RuntimeException updateFailure) {
            log.error("Mail recovery state failure could not be recorded: deliveryId={}", deliveryId,
                    updateFailure);
        }
    }

    public void verifyCode(String email, String inputCode) {
        verifyCode(email, inputCode, REGISTRATION);
    }

    public void verifyPasswordResetCode(String email, String inputCode) {
        verifyCode(email, inputCode, PASSWORD_RESET);
    }

    public void verifyCode(String email, String inputCode, String messageType) {
        String normalizedEmail = AccountNormalizer.email(email);
        String codeKey = codeKey(normalizedEmail, messageType);
        String realCode = (String) keyValueStore.get(codeKey);

        if (realCode == null) {
            throw new CsaException(ApiErrorCode.BAD_REQUEST, "验证码已过期，请重新获取");
        }
        if (inputCode == null || !constantTimeEquals(realCode, inputCode)) {
            throw new CsaException(ApiErrorCode.BAD_REQUEST, "验证码错误");
        }

        keyValueStore.delete(codeKey);
    }

    private String codeKey(String email, String messageType) {
        return "verify:code:" + messageType + ":" + email;
    }

    private String limitKey(String email, String messageType) {
        return "verify:limit:" + messageType + ":" + email;
    }

    private String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 0) {
            return "***";
        }
        String local = email.substring(0, at);
        String visible = local.length() <= 2 ? local.substring(0, 1) : local.substring(0, 2);
        return visible + "***" + email.substring(at);
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    /** 常量时间比较，避免通过响应耗时侧信道逐位猜测验证码 */
    private boolean constantTimeEquals(String expected, String actual) {
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
    }
}

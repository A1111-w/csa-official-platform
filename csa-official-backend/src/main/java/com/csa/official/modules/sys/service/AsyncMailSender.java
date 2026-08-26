package com.csa.official.modules.sys.service;

import com.csa.official.common.cache.KeyValueStore;
import com.csa.official.modules.sys.entity.MailDelivery;
import com.csa.official.modules.sys.mapper.MailDeliveryMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 独立的异步邮件发送组件。
 * 单独成 Bean 是为了让 {@code @Async} 的 Spring 代理生效（同类内自调用会绕过代理）。
 */
@Slf4j
@Component
public class AsyncMailSender {

    private final JavaMailSender mailSender;
    private final KeyValueStore keyValueStore;
    private final MailDeliveryMapper mailDeliveryMapper;
    private final String from;
    private final int maxAttempts;

    public AsyncMailSender(JavaMailSender mailSender,
                           KeyValueStore keyValueStore,
                           MailDeliveryMapper mailDeliveryMapper,
                           @Value("${spring.mail.username}") String from,
                           @Value("${csa.mail.max-attempts:3}") int maxAttempts) {
        this.mailSender = mailSender;
        this.keyValueStore = keyValueStore;
        this.mailDeliveryMapper = mailDeliveryMapper;
        this.from = from;
        this.maxAttempts = Math.max(1, Math.min(maxAttempts, 5));
    }

    @Async("mailTaskExecutor")
    public void sendVerifyCode(String to, String code) {
        sendVerifyCode(to, code, MailService.REGISTRATION,
                "verify:code:" + MailService.REGISTRATION + ":" + to,
                "verify:limit:" + MailService.REGISTRATION + ":" + to, null);
    }

    @Async("mailTaskExecutor")
    public void sendVerifyCode(String to, String code, String messageType,
                               String codeKey, String limitKey, Long deliveryId) {
        Exception lastFailure = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            markAttempt(deliveryId, attempt);
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(from);
                message.setTo(to);
                message.setSubject("CSA account verification");
                message.setText("Your CSA verification code is " + code
                        + ". It expires in 5 minutes. If you did not request it, ignore this message.");
                mailSender.send(message);
                markSent(deliveryId, attempt);
                log.info("Mail sent: type={}, attempt={}", messageType, attempt);
                return;
            } catch (Exception e) {
                lastFailure = e;
                if (attempt < maxAttempts) {
                    sleepBeforeRetry(attempt);
                }
            }
        }

        keyValueStore.delete(codeKey);
        keyValueStore.delete(limitKey);
        markFailed(deliveryId, maxAttempts, lastFailure);
        log.error("Mail send failed after {} attempts: type={}", maxAttempts, messageType, lastFailure);
    }

    private void markAttempt(Long deliveryId, int attempt) {
        if (deliveryId == null) {
            return;
        }
        MailDelivery update = new MailDelivery();
        update.setId(deliveryId);
        update.setStatus("SENDING");
        update.setAttemptCount(attempt);
        mailDeliveryMapper.updateById(update);
    }

    private void markSent(Long deliveryId, int attempt) {
        if (deliveryId == null) {
            return;
        }
        MailDelivery update = new MailDelivery();
        update.setId(deliveryId);
        update.setStatus("SENT");
        update.setAttemptCount(attempt);
        update.setSentTime(java.time.LocalDateTime.now());
        mailDeliveryMapper.updateById(update);
    }

    private void markFailed(Long deliveryId, int attempts, Exception failure) {
        if (deliveryId == null) {
            return;
        }
        MailDelivery update = new MailDelivery();
        update.setId(deliveryId);
        update.setStatus("FAILED");
        update.setAttemptCount(attempts);
        update.setLastErrorCode(failure == null ? "UNKNOWN" : failure.getClass().getSimpleName());
        update.setLastErrorMessage(safeMessage(failure));
        mailDeliveryMapper.updateById(update);
    }

    private String safeMessage(Exception failure) {
        if (failure == null || !StringUtils.hasText(failure.getMessage())) {
            return null;
        }
        String message = failure.getMessage().replaceAll("(?i)\\b[\\w.+-]+@[\\w.-]+\\b", "[redacted]");
        return message.length() <= 500 ? message : message.substring(0, 500);
    }

    private void sleepBeforeRetry(int attempt) {
        try {
            Thread.sleep(250L * attempt);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

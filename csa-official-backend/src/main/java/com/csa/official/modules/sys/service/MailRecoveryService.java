package com.csa.official.modules.sys.service;

import com.csa.official.common.cache.KeyValueStore;
import com.csa.official.modules.sys.entity.MailDelivery;
import com.csa.official.modules.sys.mapper.MailDeliveryMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MailRecoveryService {

    private final MailDeliveryMapper mailDeliveryMapper;
    private final MailRecoveryStore recoveryStore;
    private final KeyValueStore keyValueStore;
    private final AsyncMailSender asyncMailSender;

    public MailRecoveryService(MailDeliveryMapper mailDeliveryMapper,
                               MailRecoveryStore recoveryStore,
                               KeyValueStore keyValueStore,
                               AsyncMailSender asyncMailSender) {
        this.mailDeliveryMapper = mailDeliveryMapper;
        this.recoveryStore = recoveryStore;
        this.keyValueStore = keyValueStore;
        this.asyncMailSender = asyncMailSender;
    }

    public int recoverStale(LocalDateTime before, int batchSize) {
        List<MailDelivery> candidates = mailDeliveryMapper.selectRecoverable(before, batchSize);
        int dispatched = 0;
        for (MailDelivery delivery : candidates) {
            if (mailDeliveryMapper.claimRecovery(delivery.getId(), before) != 1) {
                continue;
            }

            MailRecoveryStore.RecoveryPayload payload = recoveryStore.find(delivery.getId()).orElse(null);
            if (payload == null) {
                fail(delivery.getId(), "RECOVERY_PAYLOAD_MISSING", "Temporary recovery state expired");
                continue;
            }
            if (!recoveryStore.matchesDelivery(
                    payload, delivery.getRecipientHash(), delivery.getMessageType())) {
                recoveryStore.delete(delivery.getId());
                fail(delivery.getId(), "RECOVERY_PAYLOAD_MISMATCH", "Recovery state did not match delivery");
                continue;
            }

            String code = keyValueStore.getString(payload.codeKey());
            if (code == null) {
                recoveryStore.delete(delivery.getId());
                fail(delivery.getId(), "VERIFICATION_CODE_EXPIRED", "Verification code expired before recovery");
                continue;
            }
            if (!recoveryStore.matchesCode(payload, code)) {
                recoveryStore.delete(delivery.getId());
                fail(delivery.getId(), "VERIFICATION_CODE_REPLACED", "Verification code changed before recovery");
                continue;
            }

            int previousAttempts = delivery.getAttemptCount() == null ? 0 : delivery.getAttemptCount();
            dispatchAfterCommit(() -> asyncMailSender.resumeVerifyCode(
                    payload.recipient(), code, payload.messageType(), payload.codeKey(), payload.limitKey(),
                    delivery.getId(), previousAttempts));
            dispatched++;
        }
        return dispatched;
    }

    private void fail(Long deliveryId, String errorCode, String message) {
        mailDeliveryMapper.markRecoveryFailed(deliveryId, errorCode, message);
    }

    private void dispatchAfterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }
}

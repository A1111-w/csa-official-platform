package com.csa.official.modules.sys.service;

import com.csa.official.common.cache.KeyValueStore;
import com.csa.official.modules.sys.entity.MailDelivery;
import com.csa.official.modules.sys.mapper.MailDeliveryMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MailRecoveryServiceTest {

    @Test
    void dispatchesOnlyWhenTemporaryStateStillMatchesDelivery() {
        MailDeliveryMapper mapper = mock(MailDeliveryMapper.class);
        MailRecoveryStore recoveryStore = mock(MailRecoveryStore.class);
        KeyValueStore keyValueStore = mock(KeyValueStore.class);
        AsyncMailSender sender = mock(AsyncMailSender.class);
        MailDelivery delivery = new MailDelivery();
        delivery.setId(7L);
        delivery.setRecipientHash("recipient-hash");
        delivery.setMessageType(MailService.REGISTRATION);
        delivery.setAttemptCount(1);
        MailRecoveryStore.RecoveryPayload payload = new MailRecoveryStore.RecoveryPayload(
                "student@example.invalid", MailService.REGISTRATION, "code-key", "limit-key", "code-hash");

        when(mapper.selectRecoverable(any(LocalDateTime.class), eq(100))).thenReturn(List.of(delivery));
        when(mapper.claimRecovery(eq(7L), any(LocalDateTime.class))).thenReturn(1);
        when(recoveryStore.find(7L)).thenReturn(java.util.Optional.of(payload));
        when(recoveryStore.matchesDelivery(payload, "recipient-hash", MailService.REGISTRATION)).thenReturn(true);
        when(keyValueStore.getString("code-key")).thenReturn("123456");
        when(recoveryStore.matchesCode(payload, "123456")).thenReturn(true);

        MailRecoveryService service = new MailRecoveryService(mapper, recoveryStore, keyValueStore, sender);
        int count = service.recoverStale(LocalDateTime.now().minusMinutes(2), 100);

        assertThat(count).isEqualTo(1);
        verify(sender).resumeVerifyCode(
                "student@example.invalid", "123456", MailService.REGISTRATION,
                "code-key", "limit-key", 7L, 1);
    }

    @Test
    void marksDeliveryFailedWhenRecoveryPayloadExpired() {
        MailDeliveryMapper mapper = mock(MailDeliveryMapper.class);
        MailRecoveryStore recoveryStore = mock(MailRecoveryStore.class);
        KeyValueStore keyValueStore = mock(KeyValueStore.class);
        AsyncMailSender sender = mock(AsyncMailSender.class);
        MailDelivery delivery = new MailDelivery();
        delivery.setId(8L);
        when(mapper.selectRecoverable(any(LocalDateTime.class), eq(50))).thenReturn(List.of(delivery));
        when(mapper.claimRecovery(eq(8L), any(LocalDateTime.class))).thenReturn(1);
        when(recoveryStore.find(8L)).thenReturn(java.util.Optional.empty());

        MailRecoveryService service = new MailRecoveryService(mapper, recoveryStore, keyValueStore, sender);
        int count = service.recoverStale(LocalDateTime.now().minusMinutes(2), 50);

        assertThat(count).isZero();
        verify(mapper).markRecoveryFailed(8L, "RECOVERY_PAYLOAD_MISSING", "Temporary recovery state expired");
    }
}

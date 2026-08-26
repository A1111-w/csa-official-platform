package com.csa.official.modules.sys.service;

import com.csa.official.modules.sys.entity.MailDelivery;
import com.csa.official.modules.sys.mapper.MailDeliveryMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class AsyncMailSenderTest {

    @Test
    void marksDeliverySentAfterOneSuccessfulAttempt() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        MailDeliveryMapper mapper = mock(MailDeliveryMapper.class);
        AsyncMailSender sender = new AsyncMailSender(mailSender, mock(com.csa.official.common.cache.KeyValueStore.class),
                mapper, mock(MailRecoveryStore.class), "noreply@example.invalid", 3);

        sender.sendVerifyCode("student@example.invalid", "verification-placeholder",
                MailService.PASSWORD_RESET, "code-key", "limit-key", 7L);

        var captor = org.mockito.ArgumentCaptor.forClass(MailDelivery.class);
        verify(mapper, times(2)).updateById(captor.capture());
        List<MailDelivery> updates = captor.getAllValues();
        assertThat(updates.get(0).getStatus()).isEqualTo("SENDING");
        assertThat(updates.get(1).getStatus()).isEqualTo("SENT");
        assertThat(updates.get(1).getAttemptCount()).isEqualTo(1);
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void retriesTwiceBeforeMarkingThirdAttemptSuccessful() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        MailDeliveryMapper mapper = mock(MailDeliveryMapper.class);
        doThrow(new IllegalStateException("temporary smtp failure"))
                .doThrow(new IllegalStateException("temporary smtp failure"))
                .doNothing().when(mailSender).send(any(SimpleMailMessage.class));
        AsyncMailSender sender = new AsyncMailSender(mailSender, mock(com.csa.official.common.cache.KeyValueStore.class),
                mapper, mock(MailRecoveryStore.class), "noreply@example.invalid", 3);

        sender.sendVerifyCode("student@example.invalid", "verification-placeholder",
                MailService.REGISTRATION, "code-key", "limit-key", 8L);

        var captor = org.mockito.ArgumentCaptor.forClass(MailDelivery.class);
        verify(mapper, times(4)).updateById(captor.capture());
        MailDelivery finalUpdate = captor.getAllValues().get(3);
        assertThat(finalUpdate.getStatus()).isEqualTo("SENT");
        assertThat(finalUpdate.getAttemptCount()).isEqualTo(3);
        verify(mailSender, times(3)).send(any(SimpleMailMessage.class));
    }

    @Test
    void clearsVerificationKeysAndMarksFailedAfterRetryLimit() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        MailDeliveryMapper mapper = mock(MailDeliveryMapper.class);
        com.csa.official.common.cache.KeyValueStore keyValueStore =
                mock(com.csa.official.common.cache.KeyValueStore.class);
        doThrow(new IllegalStateException("permanent smtp failure"))
                .when(mailSender).send(any(SimpleMailMessage.class));
        AsyncMailSender sender = new AsyncMailSender(mailSender, keyValueStore, mapper,
                mock(MailRecoveryStore.class), "noreply@example.invalid", 3);

        sender.sendVerifyCode("student@example.invalid", "verification-placeholder",
                MailService.REGISTRATION, "code-key", "limit-key", 9L);

        var captor = org.mockito.ArgumentCaptor.forClass(MailDelivery.class);
        verify(mapper, times(4)).updateById(captor.capture());
        MailDelivery finalUpdate = captor.getAllValues().get(3);
        assertThat(finalUpdate.getStatus()).isEqualTo("FAILED");
        assertThat(finalUpdate.getAttemptCount()).isEqualTo(3);
        verify(keyValueStore).delete("code-key");
        verify(keyValueStore).delete("limit-key");
        verify(mailSender, times(3)).send(any(SimpleMailMessage.class));
    }
}

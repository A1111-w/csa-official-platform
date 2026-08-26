package com.csa.official.modules.sys.service;

import com.csa.official.common.cache.KeyValueStore;
import com.csa.official.modules.sys.entity.MailDelivery;
import com.csa.official.modules.sys.mapper.MailDeliveryMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MailServiceTest {

    @Test
    void storesOnlyRecipientHashAndMaskAndKeepsCodeOutOfDeliveryRecord() {
        KeyValueStore keyValueStore = mock(KeyValueStore.class);
        AsyncMailSender asyncMailSender = mock(AsyncMailSender.class);
        MailDeliveryMapper mapper = mock(MailDeliveryMapper.class);
        when(keyValueStore.hasKey(any())).thenReturn(false);
        when(mapper.insert(any(MailDelivery.class))).thenReturn(1);

        MailService mailService = new MailService();
        ReflectionTestUtils.setField(mailService, "keyValueStore", keyValueStore);
        ReflectionTestUtils.setField(mailService, "asyncMailSender", asyncMailSender);
        ReflectionTestUtils.setField(mailService, "mailDeliveryMapper", mapper);

        mailService.sendCode(" Student@Example.EDU ");

        ArgumentCaptor<MailDelivery> deliveryCaptor = ArgumentCaptor.forClass(MailDelivery.class);
        verify(mapper).insert(deliveryCaptor.capture());
        MailDelivery delivery = deliveryCaptor.getValue();
        assertThat(delivery.getRecipientHash()).hasSize(64);
        assertThat(delivery.getRecipientHash()).doesNotContain("student@example.edu");
        assertThat(delivery.getRecipientMasked()).doesNotContain("student@example.edu");
        assertThat(delivery.getRecipientMasked()).contains("***");
        assertThat(delivery.getStatus()).isEqualTo("PENDING");
        verify(asyncMailSender).sendVerifyCode(
                org.mockito.ArgumentMatchers.eq("student@example.edu"),
                any(String.class),
                org.mockito.ArgumentMatchers.eq(MailService.REGISTRATION),
                any(String.class), any(String.class), org.mockito.ArgumentMatchers.nullable(Long.class));
    }
}

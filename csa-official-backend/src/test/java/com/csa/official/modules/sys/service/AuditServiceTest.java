package com.csa.official.modules.sys.service;

import com.csa.official.modules.sys.mapper.AuditLogMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class AuditServiceTest {

    @Test
    void rejectsSensitiveKeysAtAnyNestingLevel() {
        AuditLogMapper mapper = mock(AuditLogMapper.class);
        AuditService service = new AuditService(mapper, new ObjectMapper());

        assertThatThrownBy(() -> service.record(
                "CONFIG_UPDATE", "CONFIG", "mail", Map.of("nested", Map.of("access_token", "redacted"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("access_token");

        verifyNoInteractions(mapper);
    }
}

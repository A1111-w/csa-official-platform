package com.csa.official.modules.sys.service;

import com.csa.official.modules.resume.mapper.ResumeMapper;
import com.csa.official.modules.sys.entity.User;
import com.csa.official.modules.sys.mapper.AuditLogMapper;
import com.csa.official.modules.sys.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountAnonymizationServiceTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private ResumeMapper resumeMapper;
    @Mock
    private AuditLogMapper auditLogMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UserAccountCacheService accountCacheService;
    @Mock
    private AuditService auditService;

    private AccountAnonymizationService service;

    @BeforeEach
    void setUp() {
        service = new AccountAnonymizationService(
                userMapper, resumeMapper, auditLogMapper, passwordEncoder,
                accountCacheService, auditService);
    }

    @Test
    void anonymizesExpiredAccountAndDependentPersonalData() {
        User candidate = new User();
        candidate.setId(42L);
        candidate.setUsername("member");
        when(userMapper.selectDeletionCandidates(any(LocalDateTime.class), eq(100)))
                .thenReturn(List.of(candidate));
        when(passwordEncoder.encode(anyString())).thenReturn("unusable-hash");
        when(userMapper.anonymizeAccount(eq(42L), anyString(), eq("unusable-hash"), any(LocalDateTime.class)))
                .thenReturn(1);

        int count = service.anonymizeExpired(LocalDateTime.now().minusDays(30), 100, 30);

        assertThat(count).isEqualTo(1);
        verify(resumeMapper).anonymizeByUserId(42L);
        verify(auditLogMapper).anonymizeActorUsername(eq(42L), anyString());
        verify(accountCacheService).evict("member");
        verify(auditService).recordBestEffort(
                eq("ACCOUNT_ANONYMIZE"), eq("USER"), eq("42"), eq("SUCCESS"),
                anyString(), eq(Map.of("retentionDays", 30)));
    }

    @Test
    void skipsDependentWritesWhenCandidateWasAlreadyChanged() {
        User candidate = new User();
        candidate.setId(42L);
        candidate.setUsername("member");
        when(userMapper.selectDeletionCandidates(any(LocalDateTime.class), eq(100)))
                .thenReturn(List.of(candidate));
        when(passwordEncoder.encode(anyString())).thenReturn("unusable-hash");
        when(userMapper.anonymizeAccount(eq(42L), anyString(), eq("unusable-hash"), any(LocalDateTime.class)))
                .thenReturn(0);

        int count = service.anonymizeExpired(LocalDateTime.now().minusDays(30), 100, 30);

        assertThat(count).isZero();
        verify(resumeMapper, never()).anonymizeByUserId(any());
        verify(auditLogMapper, never()).anonymizeActorUsername(any(), anyString());
        verify(accountCacheService, never()).evict(anyString());
    }
}

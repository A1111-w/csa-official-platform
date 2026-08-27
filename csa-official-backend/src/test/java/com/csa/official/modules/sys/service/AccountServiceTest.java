package com.csa.official.modules.sys.service;

import com.csa.official.common.constant.AccountStatus;
import com.csa.official.common.exception.CsaException;
import com.csa.official.modules.sys.entity.User;
import com.csa.official.modules.sys.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UserAccountCacheService accountCacheService;
    @Mock
    private AuditService auditService;

    private AccountService accountService;
    private User user;

    @BeforeEach
    void setUp() {
        accountService = new AccountService(userMapper, passwordEncoder, accountCacheService, auditService);
        user = new User();
        user.setId(42L);
        user.setUsername("member");
        user.setPassword("stored-hash");
        user.setAccountStatus(AccountStatus.ACTIVE);
    }

    @Test
    void changePasswordUsesAtomicPasswordUpdateAndSessionRevocation() {
        when(passwordEncoder.matches("current-value", "stored-hash")).thenReturn(true);
        when(passwordEncoder.matches("next-value", "stored-hash")).thenReturn(false);
        when(passwordEncoder.encode("next-value")).thenReturn("next-hash");
        when(userMapper.updatePasswordAndRevokeSessions(eq(42L), eq("next-hash"), any(LocalDateTime.class)))
                .thenReturn(1);

        accountService.changePassword(user, "current-value", "next-value");

        verify(userMapper).updatePasswordAndRevokeSessions(eq(42L), eq("next-hash"), any(LocalDateTime.class));
        verify(accountCacheService).evict("member");
        verify(auditService).record("PASSWORD_CHANGE", "USER", "42", java.util.Map.of());
    }

    @Test
    void resetPasswordRevokesEveryExistingSession() {
        when(passwordEncoder.encode("replacement-value")).thenReturn("replacement-hash");
        when(userMapper.updatePasswordAndRevokeSessions(
                eq(42L), eq("replacement-hash"), any(LocalDateTime.class))).thenReturn(1);

        accountService.resetPassword(user, "replacement-value");

        verify(userMapper).updatePasswordAndRevokeSessions(
                eq(42L), eq("replacement-hash"), any(LocalDateTime.class));
        verify(accountCacheService).evict("member");
    }

    @Test
    void resetPasswordRejectsDisabledAccountWithoutWriting() {
        user.setAccountStatus(AccountStatus.DISABLED);

        assertThatThrownBy(() -> accountService.resetPassword(user, "replacement-value"))
                .isInstanceOf(CsaException.class);

        verify(userMapper, never()).updatePasswordAndRevokeSessions(any(), any(), any());
    }

    @Test
    void deactivateAndDeletionRequestBothRevokeSessionsInDatabase() {
        when(userMapper.deactivateAccount(eq(42L), any(LocalDateTime.class))).thenReturn(1);
        accountService.deactivate(user);
        verify(userMapper).deactivateAccount(eq(42L), any(LocalDateTime.class));

        when(userMapper.requestAccountDeletion(eq(42L), any(LocalDateTime.class))).thenReturn(1);
        accountService.requestDeletion(user);
        verify(userMapper).requestAccountDeletion(eq(42L), any(LocalDateTime.class));
        verify(accountCacheService, org.mockito.Mockito.times(2)).evict("member");
    }
}

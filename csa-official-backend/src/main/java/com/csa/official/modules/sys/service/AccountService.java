package com.csa.official.modules.sys.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.csa.official.common.constant.AccountStatus;
import com.csa.official.common.exception.ApiErrorCode;
import com.csa.official.common.exception.CsaException;
import com.csa.official.common.util.AccountNormalizer;
import com.csa.official.modules.sys.entity.User;
import com.csa.official.modules.sys.mapper.UserMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class AccountService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final UserAccountCacheService accountCacheService;
    private final AuditService auditService;

    public AccountService(UserMapper userMapper, PasswordEncoder passwordEncoder,
                          UserAccountCacheService accountCacheService, AuditService auditService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.accountCacheService = accountCacheService;
        this.auditService = auditService;
    }

    public User findByNormalizedEmail(String email) {
        return userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, AccountNormalizer.email(email)));
    }

    @Transactional(rollbackFor = Exception.class)
    public void changePassword(User user, String currentPassword, String newPassword) {
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            auditService.record("PASSWORD_CHANGE", "USER", String.valueOf(user.getId()),
                    "FAILURE", null, Map.of("reason", "CURRENT_PASSWORD_MISMATCH"));
            throw new CsaException(ApiErrorCode.AUTHENTICATION_FAILED, "当前密码错误");
        }
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new CsaException(ApiErrorCode.BAD_REQUEST, "新密码不能与当前密码相同");
        }

        int rows = updatePasswordAndRevokeSessions(user.getId(), passwordEncoder.encode(newPassword));
        if (rows != 1) {
            throw new CsaException(ApiErrorCode.CONFLICT, "账号已发生变化，请重新登录后再试");
        }
        accountCacheService.evict(user.getUsername());
        auditService.record("PASSWORD_CHANGE", "USER", String.valueOf(user.getId()), Map.of());
    }

    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(User user, String newPassword) {
        if (!AccountStatus.ACTIVE.equals(user.getAccountStatus())) {
            throw new CsaException(HttpStatus.CONFLICT.value(), "账号当前不可重置密码，请联系管理员");
        }
        int rows = updatePasswordAndRevokeSessions(user.getId(), passwordEncoder.encode(newPassword));
        if (rows != 1) {
            throw new CsaException(ApiErrorCode.CONFLICT, "账号已发生变化，请重新发起找回密码");
        }
        accountCacheService.evict(user.getUsername());
        auditService.recordBestEffort("PASSWORD_RESET", "USER", String.valueOf(user.getId()),
                "SUCCESS", user.getUsername(), Map.of());
    }

    @Transactional(rollbackFor = Exception.class)
    public void revokeAllSessions(User user) {
        int rows = userMapper.incrementSessionVersion(user.getId());
        if (rows != 1) {
            throw new CsaException(ApiErrorCode.CONFLICT, "会话状态更新失败，请重试");
        }
        accountCacheService.evict(user.getUsername());
        auditService.record("SESSION_REVOKE_ALL", "USER", String.valueOf(user.getId()), Map.of());
    }

    @Transactional(rollbackFor = Exception.class)
    public void deactivate(User user) {
        int rows = userMapper.deactivateAccount(user.getId(), LocalDateTime.now());
        if (rows != 1) {
            throw new CsaException(ApiErrorCode.CONFLICT, "账号状态已发生变化");
        }
        accountCacheService.evict(user.getUsername());
        auditService.record("ACCOUNT_DEACTIVATE", "USER", String.valueOf(user.getId()), Map.of());
    }

    @Transactional(rollbackFor = Exception.class)
    public void requestDeletion(User user) {
        int rows = userMapper.requestAccountDeletion(user.getId(), LocalDateTime.now());
        if (rows != 1) {
            throw new CsaException(ApiErrorCode.CONFLICT, "账号状态已发生变化");
        }
        accountCacheService.evict(user.getUsername());
        auditService.record("ACCOUNT_DELETE_REQUEST", "USER", String.valueOf(user.getId()), Map.of());
    }

    private int updatePasswordAndRevokeSessions(Long userId, String passwordHash) {
        return userMapper.updatePasswordAndRevokeSessions(userId, passwordHash, LocalDateTime.now());
    }
}

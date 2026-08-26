package com.csa.official.modules.sys.service;

import com.csa.official.modules.resume.mapper.ResumeMapper;
import com.csa.official.modules.sys.entity.User;
import com.csa.official.modules.sys.mapper.AuditLogMapper;
import com.csa.official.modules.sys.mapper.UserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AccountAnonymizationService {

    private final UserMapper userMapper;
    private final ResumeMapper resumeMapper;
    private final AuditLogMapper auditLogMapper;
    private final PasswordEncoder passwordEncoder;
    private final UserAccountCacheService accountCacheService;
    private final AuditService auditService;

    public AccountAnonymizationService(UserMapper userMapper,
                                       ResumeMapper resumeMapper,
                                       AuditLogMapper auditLogMapper,
                                       PasswordEncoder passwordEncoder,
                                       UserAccountCacheService accountCacheService,
                                       AuditService auditService) {
        this.userMapper = userMapper;
        this.resumeMapper = resumeMapper;
        this.auditLogMapper = auditLogMapper;
        this.passwordEncoder = passwordEncoder;
        this.accountCacheService = accountCacheService;
        this.auditService = auditService;
    }

    @Transactional(rollbackFor = Exception.class)
    public int anonymizeExpired(LocalDateTime before, int batchSize, int retentionDays) {
        List<User> candidates = userMapper.selectDeletionCandidates(before, batchSize);
        int anonymized = 0;
        for (User candidate : candidates) {
            String anonymousUsername = anonymousUsername(candidate.getId());
            String unusablePassword = passwordEncoder.encode(UUID.randomUUID().toString());
            LocalDateTime anonymizedAt = LocalDateTime.now();

            int updated = userMapper.anonymizeAccount(
                    candidate.getId(), anonymousUsername, unusablePassword, anonymizedAt);
            if (updated != 1) {
                continue;
            }

            resumeMapper.anonymizeByUserId(candidate.getId());
            auditLogMapper.anonymizeActorUsername(candidate.getId(), anonymousUsername);
            auditService.recordBestEffort("ACCOUNT_ANONYMIZE", "USER", String.valueOf(candidate.getId()),
                    "SUCCESS", anonymousUsername, Map.of("retentionDays", retentionDays));
            accountCacheService.evict(candidate.getUsername());
            anonymized++;
        }
        return anonymized;
    }

    private String anonymousUsername(Long userId) {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        return "deleted_" + userId + "_" + suffix;
    }
}

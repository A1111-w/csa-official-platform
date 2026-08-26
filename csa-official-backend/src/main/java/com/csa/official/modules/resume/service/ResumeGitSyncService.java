package com.csa.official.modules.resume.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.csa.official.common.exception.ApiErrorCode;
import com.csa.official.common.exception.CsaException;
import com.csa.official.modules.biz.service.GitService;
import com.csa.official.modules.resume.entity.Resume;
import com.csa.official.modules.resume.mapper.ResumeMapper;
import com.csa.official.modules.resume.vo.ResumeGitSyncVO;
import com.csa.official.modules.sys.service.AuditService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class ResumeGitSyncService {

    private final ResumeMapper resumeMapper;
    private final GitService gitService;
    private final ResumeGitSyncWorker worker;
    private final AuditService auditService;
    private final long staleSeconds;

    public ResumeGitSyncService(
            ResumeMapper resumeMapper,
            GitService gitService,
            ResumeGitSyncWorker worker,
            AuditService auditService,
            @Value("${csa.git.stale-seconds:300}") long configuredStaleSeconds) {
        this.resumeMapper = resumeMapper;
        this.gitService = gitService;
        this.worker = worker;
        this.auditService = auditService;
        this.staleSeconds = Math.max(30L, Math.min(configuredStaleSeconds, 3600L));
    }

    public ResumeGitSyncVO getMyStatus(Long userId) {
        return ResumeGitSyncVO.from(findByUserId(userId));
    }

    @Transactional(rollbackFor = Exception.class)
    public ResumeGitSyncVO startMySync(Long userId) {
        Resume resume = findByUserId(userId);
        if (resume == null) {
            throw new CsaException(ApiErrorCode.RESOURCE_NOT_FOUND, "Resume not found");
        }
        if (!StringUtils.hasText(resume.getGitRepoUrl())) {
            throw new CsaException(ApiErrorCode.BAD_REQUEST, "Git repository URL is required");
        }
        gitService.validateRepositoryUrl(resume.getGitRepoUrl());

        String runId = UUID.randomUUID().toString();
        LocalDateTime startedAt = LocalDateTime.now();
        int claimed = resumeMapper.claimGitSync(
                userId, resume.getGitRepoUrl(), runId,
                startedAt, startedAt.minusSeconds(staleSeconds));
        if (claimed != 1) {
            throw new CsaException(ApiErrorCode.CONFLICT,
                    "Repository synchronization is already running");
        }

        auditService.recordBestEffort(
                "RESUME_GIT_SYNC_REQUEST",
                "RESUME",
                String.valueOf(resume.getId()),
                "ACCEPTED",
                null,
                Map.of("subjectUserId", userId));
        dispatchAfterCommit(userId, resume.getGitRepoUrl(), runId);
        return ResumeGitSyncVO.syncing();
    }

    private Resume findByUserId(Long userId) {
        if (userId == null) {
            throw new CsaException(ApiErrorCode.AUTHENTICATION_REQUIRED, "Authentication required");
        }
        return resumeMapper.selectOne(new LambdaQueryWrapper<Resume>()
                .eq(Resume::getUserId, userId));
    }

    private void dispatchAfterCommit(Long userId, String repoUrl, String runId) {
        Runnable dispatch = () -> dispatch(userId, repoUrl, runId);
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            dispatch.run();
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                dispatch.run();
            }
        });
    }

    private void dispatch(Long userId, String repoUrl, String runId) {
        try {
            worker.sync(userId, repoUrl, runId);
        } catch (TaskRejectedException e) {
            worker.rejectBeforeStart(userId, runId);
            log.warn("Git sync queue is full: userId={}, runId={}", userId, runId);
        }
    }
}

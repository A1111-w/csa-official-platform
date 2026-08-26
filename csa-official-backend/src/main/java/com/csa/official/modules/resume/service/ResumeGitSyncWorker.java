package com.csa.official.modules.resume.service;

import com.csa.official.common.exception.CsaException;
import com.csa.official.modules.biz.service.GitService;
import com.csa.official.modules.resume.mapper.ResumeMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
public class ResumeGitSyncWorker {

    private final GitService gitService;
    private final ResumeMapper resumeMapper;

    public ResumeGitSyncWorker(GitService gitService, ResumeMapper resumeMapper) {
        this.gitService = gitService;
        this.resumeMapper = resumeMapper;
    }

    @Async("gitSyncTaskExecutor")
    public void sync(Long userId, String repoUrl, String runId) {
        try {
            GitService.GitSyncResult result = gitService.syncRepository(userId, repoUrl);
            int updated = resumeMapper.completeGitSync(
                    userId,
                    runId,
                    LocalDateTime.now(),
                    limit(result.branch(), 128),
                    result.commit(),
                    result.sizeBytes());
            if (updated == 0) {
                log.info("Ignoring superseded Git sync completion: userId={}, runId={}", userId, runId);
            }
        } catch (CsaException e) {
            markFailed(userId, runId, e.getErrorCode());
            log.warn("Resume Git synchronization failed: userId={}, runId={}, errorCode={}",
                    userId, runId, e.getErrorCode());
        } catch (RuntimeException e) {
            markFailed(userId, runId, "INTERNAL_ERROR");
            log.error("Unexpected resume Git synchronization failure: userId={}, runId={}",
                    userId, runId, e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void rejectBeforeStart(Long userId, String runId) {
        resumeMapper.failGitSync(
                userId, runId, LocalDateTime.now(), "GIT_SYNC_QUEUE_FULL");
    }

    private void markFailed(Long userId, String runId, String errorCode) {
        try {
            resumeMapper.failGitSync(
                    userId, runId, LocalDateTime.now(), limit(errorCode, 64));
        } catch (RuntimeException updateFailure) {
            log.error("Could not persist Git sync failure: userId={}, runId={}",
                    userId, runId, updateFailure);
        }
    }

    private String limit(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return "INTERNAL_ERROR";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}

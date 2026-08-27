package com.csa.official.modules.resume.service;

import com.csa.official.common.exception.ApiErrorCode;
import com.csa.official.common.exception.CsaException;
import com.csa.official.modules.biz.service.GitService;
import com.csa.official.modules.resume.mapper.ResumeMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResumeGitSyncWorkerTest {

    @Mock
    private GitService gitService;

    @Mock
    private ResumeMapper resumeMapper;

    @InjectMocks
    private ResumeGitSyncWorker worker;

    @Test
    void storesRepositoryMetadataAfterSuccessfulSync() {
        when(gitService.syncRepository(101L, "https://github.com/csa/example.git"))
                .thenReturn(new GitService.GitSyncResult("main", "abc123", 4096L));

        worker.sync(101L, "https://github.com/csa/example.git", "run-1");

        verify(resumeMapper).completeGitSync(
                eq(101L), eq("run-1"), any(), eq("main"), eq("abc123"), eq(4096L));
    }

    @Test
    void storesStableBusinessErrorCode() {
        when(gitService.syncRepository(101L, "https://github.com/csa/example.git"))
                .thenThrow(new CsaException(ApiErrorCode.UPSTREAM_ERROR, "unavailable"));

        worker.sync(101L, "https://github.com/csa/example.git", "run-2");

        verify(resumeMapper).failGitSync(
                eq(101L), eq("run-2"), any(), eq("UPSTREAM_ERROR"));
    }

    @Test
    void storesInternalErrorForUnexpectedFailure() {
        when(gitService.syncRepository(101L, "https://github.com/csa/example.git"))
                .thenThrow(new IllegalStateException("boom"));

        worker.sync(101L, "https://github.com/csa/example.git", "run-3");

        verify(resumeMapper).failGitSync(
                eq(101L), eq("run-3"), any(), eq("INTERNAL_ERROR"));
    }

    @Test
    void queueRejectionUsesRunIdGuardedFailureUpdate() {
        worker.rejectBeforeStart(101L, "run-4");

        verify(resumeMapper).failGitSync(
                eq(101L), eq("run-4"), any(), eq("GIT_SYNC_QUEUE_FULL"));
    }

    @Test
    void failurePersistenceErrorDoesNotEscapeWorker() {
        when(gitService.syncRepository(101L, "https://github.com/csa/example.git"))
                .thenThrow(new CsaException(ApiErrorCode.UPSTREAM_ERROR, "unavailable"));
        doThrow(new IllegalStateException("database unavailable"))
                .when(resumeMapper).failGitSync(eq(101L), eq("run-5"), any(), eq("UPSTREAM_ERROR"));

        worker.sync(101L, "https://github.com/csa/example.git", "run-5");
    }
}

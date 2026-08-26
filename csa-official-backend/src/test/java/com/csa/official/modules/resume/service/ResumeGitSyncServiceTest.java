package com.csa.official.modules.resume.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.csa.official.common.exception.CsaException;
import com.csa.official.modules.biz.service.GitService;
import com.csa.official.modules.resume.entity.Resume;
import com.csa.official.modules.resume.mapper.ResumeMapper;
import com.csa.official.modules.sys.service.AuditService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.TaskRejectedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResumeGitSyncServiceTest {

    @BeforeAll
    static void initializeMybatisMetadata() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), "resume-git-sync-test"),
                Resume.class);
    }

    @Mock
    private ResumeMapper resumeMapper;

    @Mock
    private GitService gitService;

    @Mock
    private ResumeGitSyncWorker worker;

    @Mock
    private AuditService auditService;

    private ResumeGitSyncService service;

    @BeforeEach
    void setUp() {
        service = new ResumeGitSyncService(resumeMapper, gitService, worker, auditService, 300);
    }

    @Test
    void returnsNotSyncedWhenResumeDoesNotExist() {
        when(resumeMapper.selectOne(any())).thenReturn(null);

        assertThat(service.getMyStatus(101L).getStatus()).isEqualTo("NOT_SYNCED");
        assertThat(service.getMyStatus(101L).isConfigured()).isFalse();
    }

    @Test
    void rejectsMissingResume() {
        when(resumeMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> service.startMySync(101L))
                .isInstanceOf(CsaException.class)
                .extracting(error -> ((CsaException) error).getCode())
                .isEqualTo(404);

        verify(resumeMapper, never()).claimGitSync(anyLong(), anyString(), anyString(), any(), any());
    }

    @Test
    void validatesRepositoryBeforeClaimingTask() {
        Resume resume = resume(101L, "https://untrusted.invalid/example.git");
        when(resumeMapper.selectOne(any())).thenReturn(resume);
        doThrow(new CsaException(400, "Repository host is not allowed"))
                .when(gitService).validateRepositoryUrl(resume.getGitRepoUrl());

        assertThatThrownBy(() -> service.startMySync(101L))
                .isInstanceOf(CsaException.class)
                .extracting(error -> ((CsaException) error).getCode())
                .isEqualTo(400);

        verify(resumeMapper, never()).claimGitSync(anyLong(), anyString(), anyString(), any(), any());
    }

    @Test
    void rejectsConcurrentSynchronization() {
        Resume resume = resume(101L, "https://github.com/csa/example.git");
        when(resumeMapper.selectOne(any())).thenReturn(resume);
        when(resumeMapper.claimGitSync(anyLong(), anyString(), anyString(), any(), any()))
                .thenReturn(0);

        assertThatThrownBy(() -> service.startMySync(101L))
                .isInstanceOf(CsaException.class)
                .extracting(error -> ((CsaException) error).getCode())
                .isEqualTo(409);

        verify(worker, never()).sync(anyLong(), anyString(), anyString());
    }

    @Test
    void claimsAndDispatchesSynchronization() {
        Resume resume = resume(101L, "https://github.com/csa/example.git");
        when(resumeMapper.selectOne(any())).thenReturn(resume);
        when(resumeMapper.claimGitSync(anyLong(), anyString(), anyString(), any(), any()))
                .thenReturn(1);

        assertThat(service.startMySync(101L).getStatus()).isEqualTo("SYNCING");

        verify(gitService).validateRepositoryUrl(resume.getGitRepoUrl());
        verify(worker).sync(org.mockito.ArgumentMatchers.eq(101L),
                org.mockito.ArgumentMatchers.eq(resume.getGitRepoUrl()), anyString());
        verify(auditService).recordBestEffort(
                org.mockito.ArgumentMatchers.eq("RESUME_GIT_SYNC_REQUEST"),
                org.mockito.ArgumentMatchers.eq("RESUME"),
                org.mockito.ArgumentMatchers.eq("10"),
                org.mockito.ArgumentMatchers.eq("ACCEPTED"),
                org.mockito.ArgumentMatchers.isNull(),
                any());
    }

    @Test
    void marksTaskFailedWhenExecutorQueueRejectsIt() {
        Resume resume = resume(101L, "https://github.com/csa/example.git");
        when(resumeMapper.selectOne(any())).thenReturn(resume);
        when(resumeMapper.claimGitSync(anyLong(), anyString(), anyString(), any(), any()))
                .thenReturn(1);
        doThrow(new TaskRejectedException("queue full"))
                .when(worker).sync(anyLong(), anyString(), anyString());

        service.startMySync(101L);

        verify(worker).rejectBeforeStart(org.mockito.ArgumentMatchers.eq(101L), anyString());
    }

    private Resume resume(Long userId, String repoUrl) {
        Resume resume = new Resume();
        resume.setId(10L);
        resume.setUserId(userId);
        resume.setGitRepoUrl(repoUrl);
        return resume;
    }
}

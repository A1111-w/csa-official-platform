package com.csa.official.modules.resume.vo;

import com.csa.official.modules.resume.entity.Resume;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ResumeGitSyncVO {

    private boolean configured;
    private String status;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String errorCode;
    private String branch;
    private String commit;
    private Long sizeBytes;

    public static ResumeGitSyncVO from(Resume resume) {
        ResumeGitSyncVO vo = new ResumeGitSyncVO();
        boolean configured = resume != null
                && resume.getGitRepoUrl() != null
                && !resume.getGitRepoUrl().isBlank();
        vo.setConfigured(configured);
        vo.setStatus(resume == null || resume.getGitSyncStatus() == null
                ? "NOT_SYNCED" : resume.getGitSyncStatus());
        if (resume != null) {
            vo.setStartedAt(resume.getGitSyncStartedAt());
            vo.setCompletedAt(resume.getGitSyncCompletedAt());
            vo.setErrorCode(resume.getGitSyncErrorCode());
            vo.setBranch(resume.getGitSyncBranch());
            vo.setCommit(resume.getGitSyncCommit());
            vo.setSizeBytes(resume.getGitSyncSizeBytes());
        }
        return vo;
    }

    public static ResumeGitSyncVO syncing() {
        ResumeGitSyncVO vo = new ResumeGitSyncVO();
        vo.setConfigured(true);
        vo.setStatus("SYNCING");
        return vo;
    }
}

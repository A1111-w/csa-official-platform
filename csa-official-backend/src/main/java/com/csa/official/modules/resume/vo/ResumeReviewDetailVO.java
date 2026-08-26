package com.csa.official.modules.resume.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Full resume review view for ministers and administrators.
 *
 * <p>Only identity fields needed for review are exposed. Passwords, session data,
 * payment fields and private contact fields never leave the persistence layer.
 */
@Data
public class ResumeReviewDetailVO {

    private Long id;
    private Long applicantId;
    private String username;
    private String realName;
    private String avatar;
    private String email;
    private String studentId;
    private String college;
    private String className;
    private Long departmentId;
    private String departmentName;

    private String content;
    private String gitRepoUrl;
    private String gitSyncStatus;
    private LocalDateTime gitSyncCompletedAt;
    private String gitSyncErrorCode;
    private String gitSyncBranch;
    private String gitSyncCommit;
    private Long gitSyncSizeBytes;
    private Integer status;
    private String rejectReason;

    private Long auditBy;
    private String auditorName;
    private LocalDateTime auditTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

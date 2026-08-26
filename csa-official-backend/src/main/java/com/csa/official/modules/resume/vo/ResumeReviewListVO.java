package com.csa.official.modules.resume.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Minister-facing resume review queue item.
 *
 * <p>The list deliberately contains only a short content preview. Reviewers fetch the
 * full resume from the detail endpoint, which keeps the paginated response bounded.
 */
@Data
public class ResumeReviewListVO {

    private Long id;
    private Long applicantId;
    private String username;
    private String realName;
    private String avatar;
    private Long departmentId;
    private String departmentName;
    private Integer status;
    private String contentSummary;
    private String gitRepoUrl;
    private String gitSyncStatus;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private LocalDateTime auditTime;
}

package com.csa.official.modules.resume.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.csa.official.modules.resume.enums.ResumeStatusEnum;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("biz_resume")
public class Resume implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    // 存储 Markdown 内容或自我介绍
    private String content;

    // 硬核模式：Git 仓库地址
    private String gitRepoUrl;
    private String gitSyncStatus;
    private String gitSyncRunId;
    private LocalDateTime gitSyncStartedAt;
    private LocalDateTime gitSyncCompletedAt;
    private String gitSyncErrorCode;
    private String gitSyncBranch;
    private String gitSyncCommit;
    private Long gitSyncSizeBytes;

    // 状态 (使用枚举)
    private ResumeStatusEnum status;

    private String rejectReason;

    private Long auditBy;

    private LocalDateTime auditTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}

package com.csa.official.modules.biz.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.csa.official.modules.biz.enums.CompetitionStatusEnum;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("biz_competition")
public class Competition implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    @NotBlank(message = "比赛标题不能为空")
    private String title;
    @NotBlank(message = "比赛详情不能为空")
    private String content;
    private String coverImg;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    private Long publisherId;
    // import com.csa.official.modules.biz.enums.CompetitionStatusEnum;
    private CompetitionStatusEnum status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
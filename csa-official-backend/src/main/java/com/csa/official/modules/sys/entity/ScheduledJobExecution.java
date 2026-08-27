package com.csa.official.modules.sys.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_scheduled_job_execution")
public class ScheduledJobExecution {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String jobName;
    private String idempotencyKey;
    private String status;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
}

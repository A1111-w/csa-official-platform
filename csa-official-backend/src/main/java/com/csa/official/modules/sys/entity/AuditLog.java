package com.csa.official.modules.sys.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_audit_log")
public class AuditLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long actorUserId;
    private String actorUsername;
    private String action;
    private String targetType;
    private String targetId;
    private String result;
    private String ipAddress;
    private String userAgent;
    private String requestId;
    private String detailsJson;
    private LocalDateTime createTime;
}

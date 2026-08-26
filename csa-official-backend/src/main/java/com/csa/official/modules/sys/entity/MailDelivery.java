package com.csa.official.modules.sys.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_mail_delivery")
public class MailDelivery {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String recipientHash;
    private String recipientMasked;
    private String messageType;
    private String status;
    private Integer attemptCount;
    private String lastErrorCode;
    private String lastErrorMessage;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private LocalDateTime sentTime;
}

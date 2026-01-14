package com.csa.official.modules.sys.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("sys_invite_code")
public class InviteCode implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    private Long creatorId;
    private Integer maxUsage;
    private Integer currentUsage;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableLogic
    private Integer deleted;
}
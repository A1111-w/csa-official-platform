package com.csa.official.modules.sys.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("sys_proposal")
public class Proposal implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String type; // ROOT_APPLY, CODE_DEPLOY
    private String title;
    private String reason;
    private Long proposerId;
    
    // 0:VOTING, 1:PASSED, 2:REJECTED
    private Integer status; 
    
    private LocalDateTime expireTime;
    private String finalResultJson; // 存类似 {"agree": 5, "reject": 1}

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}

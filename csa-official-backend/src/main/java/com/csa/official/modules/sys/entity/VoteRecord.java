package com.csa.official.modules.sys.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.csa.official.modules.sys.enums.VoteResultEnum;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("sys_vote_record")
public class VoteRecord implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long proposalId;
    private Long voterId;
    // import com.csa.official.modules.sys.enums.VoteResultEnum;
    private VoteResultEnum result;
    private Integer weight; // 权重
    private String comment;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
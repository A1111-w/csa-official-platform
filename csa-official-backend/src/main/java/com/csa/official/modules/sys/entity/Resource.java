package com.csa.official.modules.sys.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("sys_resource")
public class Resource implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String summary;
    private String fileUrl;
    private String category;
    private Long uploaderId;
    private Integer downloadCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableLogic
    private Integer deleted;
}
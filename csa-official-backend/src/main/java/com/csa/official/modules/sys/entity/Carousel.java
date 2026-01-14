package com.csa.official.modules.sys.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("sys_carousel")
public class Carousel implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String imgUrl;
    private String targetUrl;
    private String title;
    private Integer sortOrder;
    private Integer status; // 1:启用, 0:禁用

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}

package com.csa.official.modules.biz.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("biz_comp_editor")
public class CompEditor {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long competitionId;
    private Long userId;
}

package com.csa.official.modules.biz.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum CompetitionStatusEnum {
    UNPUBLISHED(0, "未发布"),
    ONGOING(1, "进行中"),
    FINISHED(2, "已结束");

    @EnumValue
    private final int code;
    private final String desc;

    CompetitionStatusEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}

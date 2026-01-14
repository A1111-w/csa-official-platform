package com.csa.official.modules.sys.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum VoteResultEnum {
    REJECT(0, "反对"),
    AGREE(1, "赞成");

    @EnumValue
    private final int code;
    private final String desc;

    VoteResultEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
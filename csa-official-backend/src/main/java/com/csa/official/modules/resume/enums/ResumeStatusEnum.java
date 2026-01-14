package com.csa.official.modules.resume.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum ResumeStatusEnum {
    DRAFT(0, "草稿"),
    PENDING(1, "待审核"),
    APPROVED(2, "已通过"),
    REJECTED(3, "已驳回");

    @EnumValue // 告诉 MyBatis-Plus 存数据库时存这个 code (0,1,2...)
    private final int code;
    private final String desc;

    ResumeStatusEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
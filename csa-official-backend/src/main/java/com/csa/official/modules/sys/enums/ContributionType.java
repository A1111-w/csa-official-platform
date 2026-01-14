package com.csa.official.modules.sys.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ContributionType {
    DEV("官网建设"), // 按百分比/分值
    RES("资源贡献"), // 按条数
    COMP("发布比赛"), // 按条数
    OPS("首页维护"); // 按次数 (Carousel, Notice等)

    private final String desc;
}
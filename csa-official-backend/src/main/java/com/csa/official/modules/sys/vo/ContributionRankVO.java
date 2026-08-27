package com.csa.official.modules.sys.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ContributionRankVO {
    private Long userId;
    private String username;
    private String realName;
    private String avatar;
    private String deptName;
    private BigDecimal score;
    private Long contributionCount;
}

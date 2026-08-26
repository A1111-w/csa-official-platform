package com.csa.official.modules.sys.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ContributionWallVO {
    private Long userId;
    private String realName;
    private String avatar;
    private String deptName;
    private BigDecimal devScore;
    private Integer resCount;
    private Integer compCount;
    private Integer opsCount;
    private BigDecimal totalSortScore;
}

package com.csa.official.modules.sys.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ContributionAwardVO {
    private Long id;
    private Long userId;
    private String username;
    private String realName;
    private String departmentName;
    private String type;
    private String typeLabel;
    private BigDecimal score;
    private String reason;
    private String source;
    private String sourceLabel;
    private Long awardedBy;
    private String awardedByUsername;
    private LocalDateTime createTime;
}

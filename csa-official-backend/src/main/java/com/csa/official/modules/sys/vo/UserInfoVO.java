package com.csa.official.modules.sys.vo;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class UserInfoVO {
    // 基础信息
    private Long id;
    private String username;
    private String realName;
    private String avatar;
    private String email;
    private String phone;
    private String contact; // QQ/微信

    // 身份信息
    private Integer roleLevel;
    private Integer positionType;
    private BigDecimal balance; // 余额 (前端可能要展示)

    // 学籍信息
    private String college;
    private String className;
    private String studentId;

    // 注意：这里绝对没有 password, openid, deleted 等敏感字段
}
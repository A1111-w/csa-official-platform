package com.csa.official.modules.sys.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data // 核心：自动生成 getPhone(), getContact() 等方法
@TableName("sys_user")
public class User implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;
    private String password;
    private String realName;
    private String email;
    private String accountStatus;
    private Long sessionVersion;
    private LocalDateTime passwordChangedAt;
    private LocalDateTime lastLoginAt;
    private LocalDateTime deactivatedAt;
    private LocalDateTime deletionRequestedAt;
    private LocalDateTime anonymizedAt;
    private String privacyConsentVersion;
    private LocalDateTime privacyConsentAt;

    // 权限与职位
    private Integer roleLevel;    // 0-99
    private Integer positionType; // 0:无, 1:成员, 2:副职, 3:正职
    private Long departmentId;
    private BigDecimal balance;

    // Gitea 信息
    private String giteaUsername;

    // === 👇 这次报错缺失的字段 (社交信息) ===
    private String avatar;
    private String phone;         // 对应 getPhone()
    private String wxOpenId;
    private String contact;       // 对应 getContact()
    private String address;

    // === 👇 之前注册功能新增的字段 (学籍与支付) ===
    private String studentId;     // 学号
    private String college;       // 学院
    private String className;     // 班级
    private String merchantNo;    // 微信支付单号
    private String usedInviteCode;// 使用的邀请码

    // 审计字段
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @TableLogic
    private Integer deleted;
}

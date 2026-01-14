package com.csa.official.common.constant;

public interface RoleConsts {
    // 定义等级常量，以后改数字只在这里改
    int GUEST = 0;       // 路人
    int MEMBER = 1;      // 会员
    int CORE_MEMBER = 2; // 成员 (核心)
    int MINISTER = 3;    // 部长
    int PRESIDENT = 4;   // 会长
    int ROOT = 99;       // 超级管理员
}
// 如果修改等级常量，请同步更新旧数据
// UPDATE sys_user SET role_level = 5 WHERE role_level = 4; -- 会长升5
// UPDATE sys_user SET role_level = 4 WHERE role_level = 3; -- 部长升4
// UPDATE sys_user SET role_level = 3 WHERE role_level = 2; -- 成员升3
// UPDATE sys_user SET role_level = 2 WHERE role_level = 1; -- 会员升2
// UPDATE sys_user SET role_level = 1 WHERE role_level = 0; -- 路人升1
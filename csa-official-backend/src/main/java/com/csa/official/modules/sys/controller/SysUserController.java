package com.csa.official.modules.sys.controller;

import cn.hutool.core.bean.BeanUtil;
import com.csa.official.common.result.R;
import com.csa.official.common.util.SecurityUtils;
import com.csa.official.modules.sys.entity.User;
import com.csa.official.modules.sys.vo.UserInfoVO;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sys/user")
public class SysUserController {

    @GetMapping("/info")
    public R<UserInfoVO> getUserInfo() {
        // 1. 获取当前登录用户实体 (SecurityUtils 已经处理了查库和空判断)
        User user = SecurityUtils.getCurrentUser();

        // 2. 转换成 VO
        UserInfoVO vo = new UserInfoVO();

        // 使用 BeanUtil.copyProperties 自动把同名字段的值复制过去
        BeanUtil.copyProperties(user, vo);

        return R.ok(vo);
    }

    // 示例：仅限 Level 99 访问的接口
    @PreAuthorize("hasRole('LEVEL_99')")
    @GetMapping("/admin-test")
    public R<String> onlyForRoot() {
        return R.ok("只有 Root 能看到这句话");
    }

    // 示例：仅限 Level 1 (会员) 及以上访问的接口
    // 因为我们做了权限累加，Level 2,3,4,99 都有 ROLE_LEVEL_1，所以他们也能访问
    @PreAuthorize("hasRole('LEVEL_1')")
    @GetMapping("/member-test")
    public R<String> onlyForMember() {
        return R.ok("会员资源区内容");
    }
}
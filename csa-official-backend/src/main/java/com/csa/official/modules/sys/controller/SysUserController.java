package com.csa.official.modules.sys.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import cn.hutool.core.bean.BeanUtil;
import com.csa.official.common.result.R;
import com.csa.official.common.util.PageUtils;
import com.csa.official.common.util.SecurityUtils;
import com.csa.official.modules.sys.entity.Dept;
import com.csa.official.modules.sys.entity.User;
import com.csa.official.modules.sys.mapper.DeptMapper;
import com.csa.official.modules.sys.mapper.UserMapper;
import com.csa.official.modules.sys.vo.UserDirectoryVO;
import com.csa.official.modules.sys.vo.UserInfoVO;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/sys/user")
public class SysUserController {

    private final UserMapper userMapper;
    private final DeptMapper deptMapper;

    public SysUserController(UserMapper userMapper, DeptMapper deptMapper) {
        this.userMapper = userMapper;
        this.deptMapper = deptMapper;
    }

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

    @PreAuthorize("hasRole('LEVEL_3')")
    @GetMapping("/list")
    public R<List<UserDirectoryVO>> listUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Integer minRoleLevel,
            @RequestParam(required = false) Integer size) {
        int limit = PageUtils.clampLimit(size, 100);

        LambdaQueryWrapper<User> query = new LambdaQueryWrapper<>();
        query.eq(departmentId != null, User::getDepartmentId, departmentId);
        query.ge(minRoleLevel != null, User::getRoleLevel, minRoleLevel);

        if (StringUtils.hasText(keyword)) {
            query.and(wrapper -> wrapper.like(User::getUsername, keyword)
                    .or()
                    .like(User::getRealName, keyword)
                    .or()
                    .like(User::getEmail, keyword));
        }

        query.orderByDesc(User::getRoleLevel)
                .orderByDesc(User::getPositionType)
                .orderByAsc(User::getUsername)
                .last("LIMIT " + limit);

        List<User> users = userMapper.selectList(query);
        Set<Long> deptIds = users.stream()
                .map(User::getDepartmentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, String> deptNames = deptIds.isEmpty()
                ? Collections.emptyMap()
                : deptMapper.selectBatchIds(deptIds).stream()
                        .collect(Collectors.toMap(Dept::getId, Dept::getName));

        List<UserDirectoryVO> list = users.stream().map(user -> {
            UserDirectoryVO vo = new UserDirectoryVO();
            vo.setId(user.getId());
            vo.setUsername(user.getUsername());
            vo.setRealName(user.getRealName());
            vo.setAvatar(user.getAvatar());
            vo.setEmail(user.getEmail());
            vo.setRoleLevel(user.getRoleLevel());
            vo.setPositionType(user.getPositionType());
            vo.setDepartmentId(user.getDepartmentId());
            vo.setDepartmentName(deptNames.get(user.getDepartmentId()));
            return vo;
        }).collect(Collectors.toList());

        return R.ok(list);
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

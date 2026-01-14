package com.csa.official.modules.sys.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.csa.official.common.constant.RoleConsts;
import com.csa.official.common.result.R;
import com.csa.official.common.util.SecurityUtils;
import com.csa.official.modules.sys.entity.Dept;
import com.csa.official.modules.sys.entity.SysConfig;
import com.csa.official.modules.sys.entity.User;
import com.csa.official.modules.sys.mapper.DeptMapper;
import com.csa.official.modules.sys.mapper.SysConfigMapper;
import com.csa.official.modules.sys.mapper.UserMapper;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class PublicController {

    @Autowired
    private SysConfigMapper configMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private DeptMapper deptMapper;

    // ================== 1. 了解计协 (About) ==================

    /**
     * 获取协会介绍 (公开)
     */
    @GetMapping("/public/about")
    public R<String> getAbout() {
        SysConfig config = configMapper.selectOne(new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getConfigKey, "CSA_INTRO"));
        return R.ok(config != null ? config.getConfigValue() : "");
    }

    /**
     * 修改协会介绍 (仅限会长/Root)
     */
    @PreAuthorize("hasRole('LEVEL_4')")
    @PostMapping("/sys/config/update-about")
    public R<String> updateAbout(@RequestBody ConfigDto dto) {
        SysConfig config = configMapper.selectOne(new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getConfigKey, "CSA_INTRO"));

        if (config == null) {
            config = new SysConfig();
            config.setConfigKey("CSA_INTRO");
            config.setDescription("协会介绍");
            config.setConfigValue(dto.getContent());
            configMapper.insert(config);
        } else {
            config.setConfigValue(dto.getContent());
            config.setUpdateBy(SecurityUtils.getUserId());
            configMapper.updateById(config);
        }
        return R.ok("更新成功");
    }

    // ================== 2. 贡献墙 (Contributors) ==================

    /**
     * 获取贡献者名单 (公开)
     * 目前逻辑：展示所有核心成员 (Level >= 2)，按等级排序
     */
    @GetMapping("/public/contributors")
    public R<List<ContributorVo>> getContributors() {
        // 查所有核心成员
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>()
                .ge(User::getRoleLevel, RoleConsts.CORE_MEMBER) // Level 2+
                .orderByDesc(User::getRoleLevel)); // 大佬排前面

        List<ContributorVo> list = users.stream().map(u -> {
            ContributorVo vo = new ContributorVo();
            vo.setId(u.getId());
            vo.setRealName(u.getRealName());
            vo.setAvatar(u.getAvatar());
            vo.setRoleLevel(u.getRoleLevel());

            // 查部门名称
            if (u.getDepartmentId() != null) {
                Dept dept = deptMapper.selectById(u.getDepartmentId());
                if (dept != null)
                    vo.setDeptName(dept.getName());
            }

            // 设置头衔
            vo.setTitle(getTitle(u.getRoleLevel(), u.getPositionType()));
            return vo;
        }).collect(Collectors.toList());

        return R.ok(list);
    }

    // 辅助：生成头衔字符串
    private String getTitle(int level, Integer posType) {
        if (level == RoleConsts.ROOT)
            return "创始人/运维";
        if (level == RoleConsts.PRESIDENT)
            return posType == 3 ? "会长" : "副会长";
        if (level == RoleConsts.MINISTER)
            return posType == 3 ? "部长" : "副部长";
        return "核心成员";
    }

    @Data
    static class ConfigDto {
        private String content;
    }

    @Data
    static class ContributorVo {
        private Long id;
        private String realName;
        private String avatar;
        private String deptName;
        private String title; // 头衔 (如: 开发部部长)
        private Integer roleLevel;
    }
}
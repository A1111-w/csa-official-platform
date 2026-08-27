package com.csa.official.modules.sys.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.csa.official.common.constant.RoleConsts;
import com.csa.official.common.result.R;
import com.csa.official.common.util.PageUtils;
import com.csa.official.common.util.SecurityUtils;
import com.csa.official.modules.sys.entity.Dept;
import com.csa.official.modules.sys.entity.SysConfig;
import com.csa.official.modules.sys.entity.User;
import com.csa.official.modules.sys.mapper.DeptMapper;
import com.csa.official.modules.sys.mapper.SysConfigMapper;
import com.csa.official.modules.sys.mapper.UserMapper;
import com.csa.official.modules.sys.service.AuditService;
import com.csa.official.modules.sys.vo.PrivacyNoticeVO;
import lombok.Data;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class PublicController {

    private static final Safelist ABOUT_CONTENT_SAFELIST = Safelist.basicWithImages()
            .addTags("h1", "h2", "h3", "blockquote", "pre", "code")
            .addAttributes("a", "target", "rel");

    @Autowired
    private SysConfigMapper configMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private DeptMapper deptMapper;
    @Autowired
    private AuditService auditService;

    @Value("${csa.privacy.current-version:2026-01}")
    private String privacyVersion;

    @Value("${csa.privacy.contact-email:privacy@localhost.invalid}")
    private String privacyContactEmail;

    @Value("${csa.privacy.account-retention-days:30}")
    private int accountRetentionDays;

    // ================== 1. 了解计协 (About) ==================

    /**
     * 获取协会介绍 (公开)
     */
    @GetMapping("/public/about")
    @Cacheable(value = "public_about", key = "'content'")
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
    @CacheEvict(value = "public_about", allEntries = true)
    public R<String> updateAbout(@RequestBody ConfigDto dto) {
        String safeContent = Jsoup.clean(dto.getContent() == null ? "" : dto.getContent(), ABOUT_CONTENT_SAFELIST);
        SysConfig config = configMapper.selectOne(new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getConfigKey, "CSA_INTRO"));

        if (config == null) {
            config = new SysConfig();
            config.setConfigKey("CSA_INTRO");
            config.setDescription("协会介绍");
            config.setConfigValue(safeContent);
            configMapper.insert(config);
            auditService.recordBestEffort("CONFIG_UPDATE", "SYSTEM_CONFIG", "CSA_INTRO",
                    "SUCCESS", null, Map.of("contentLength", safeContent.length()));
        } else {
            config.setConfigValue(safeContent);
            config.setUpdateBy(SecurityUtils.getUserId());
            configMapper.updateById(config);
            auditService.recordBestEffort("CONFIG_UPDATE", "SYSTEM_CONFIG", "CSA_INTRO",
                    "SUCCESS", null, Map.of("contentLength", safeContent.length()));
        }
        return R.ok("更新成功");
    }

    // ================== 2. 贡献墙 (Contributors) ==================

    /**
     * 获取贡献者名单 (公开)
     * 目前逻辑：展示所有核心成员 (Level >= 2)，按等级排序
     */
    @GetMapping("/public/contributors")
    @Cacheable(value = "public_contributors", key = "'all'")
    public R<List<ContributorVo>> getContributors() {
        // 查所有核心成员。加 LIMIT 是因为这是未登录可访问的公开接口，
        // 协会规模变大后不能让它无上限地把整张用户表拉出来。
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>()
                .select(User::getId, User::getRealName, User::getAvatar,
                        User::getRoleLevel, User::getPositionType, User::getDepartmentId)
                .ge(User::getRoleLevel, RoleConsts.CORE_MEMBER) // Level 2+
                .orderByDesc(User::getRoleLevel) // 大佬排前面
                .orderByAsc(User::getId) // 同等级下保证顺序稳定，避免翻页/缓存前后不一致
                .last("LIMIT " + PageUtils.MAX_LIST_LIMIT));

        Set<Long> deptIds = users.stream()
                .map(User::getDepartmentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> deptNames = deptIds.isEmpty()
                ? Collections.emptyMap()
                : deptMapper.selectBatchIds(deptIds).stream()
                        .collect(Collectors.toMap(Dept::getId, Dept::getName));

        List<ContributorVo> list = users.stream().map(u -> {
            ContributorVo vo = new ContributorVo();
            vo.setId(u.getId());
            vo.setRealName(u.getRealName());
            vo.setAvatar(u.getAvatar());
            vo.setRoleLevel(u.getRoleLevel());
            vo.setDeptName(deptNames.get(u.getDepartmentId()));
            vo.setTitle(getTitle(u.getRoleLevel(), u.getPositionType()));
            return vo;
        }).collect(Collectors.toList());

        return R.ok(list);
    }

    @GetMapping("/public/privacy")
    public R<PrivacyNoticeVO> getPrivacyNotice() {
        return R.ok(PrivacyNoticeVO.current(privacyVersion, privacyContactEmail, accountRetentionDays));
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
    public static class ContributorVo {
        private Long id;
        private String realName;
        private String avatar;
        private String deptName;
        private String title; // 头衔 (如: 开发部部长)
        private Integer roleLevel;
    }
}

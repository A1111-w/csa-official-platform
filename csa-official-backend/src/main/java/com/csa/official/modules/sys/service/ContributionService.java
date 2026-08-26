package com.csa.official.modules.sys.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.csa.official.common.constant.AccountStatus;
import com.csa.official.common.exception.ApiErrorCode;
import com.csa.official.common.exception.CsaException;
import com.csa.official.common.util.PageUtils;
import com.csa.official.modules.sys.dto.ContributionAwardRequest;
import com.csa.official.modules.sys.entity.ContributionLog;
import com.csa.official.modules.sys.entity.Dept;
import com.csa.official.modules.sys.entity.User;
import com.csa.official.modules.sys.enums.ContributionType;
import com.csa.official.modules.sys.mapper.ContributionLogMapper;
import com.csa.official.modules.sys.mapper.DeptMapper;
import com.csa.official.modules.sys.mapper.UserMapper;
import com.csa.official.modules.sys.vo.ContributionAwardVO;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ContributionService {

    private static final String SOURCE_AUTO = "AUTO";
    private static final String SOURCE_MANUAL = "MANUAL";
    private static final String SOURCE_LEGACY = "LEGACY";

    private final ContributionLogMapper contributionLogMapper;
    private final UserMapper userMapper;
    private final DeptMapper deptMapper;
    private final AuditService auditService;

    public ContributionService(ContributionLogMapper contributionLogMapper,
                               UserMapper userMapper, DeptMapper deptMapper,
                               AuditService auditService) {
        this.contributionLogMapper = contributionLogMapper;
        this.userMapper = userMapper;
        this.deptMapper = deptMapper;
        this.auditService = auditService;
    }

    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "public_contribution_rank", allEntries = true)
    public Long award(ContributionAwardRequest request, Long operatorUserId) {
        String type = normalizeType(request.getType());
        validateScore(request.getScore());

        User target = userMapper.selectById(request.getUserId());
        if (target == null || Objects.equals(target.getDeleted(), 1)) {
            throw new CsaException(ApiErrorCode.RESOURCE_NOT_FOUND, "成员不存在");
        }
        if (AccountStatus.ANONYMIZED.equals(target.getAccountStatus())) {
            throw new CsaException(ApiErrorCode.BUSINESS_RULE_VIOLATION, "匿名化账号不能新增贡献记录");
        }
        if (StringUtils.hasText(target.getAccountStatus())
                && !AccountStatus.ACTIVE.equals(target.getAccountStatus())) {
            throw new CsaException(ApiErrorCode.BUSINESS_RULE_VIOLATION, "非正常状态账号不能新增贡献记录");
        }

        ContributionLog log = new ContributionLog();
        log.setUserId(target.getId());
        log.setType(type);
        log.setScore(request.getScore());
        log.setDetail(request.getReason().trim());
        log.setSource(SOURCE_MANUAL);
        log.setAwardedBy(operatorUserId);
        if (contributionLogMapper.insert(log) <= 0) {
            throw new CsaException(ApiErrorCode.DATABASE_ERROR, "贡献记录写入失败");
        }

        auditService.recordBestEffort("CONTRIBUTION_MANUAL_AWARD", "USER",
                String.valueOf(target.getId()), "SUCCESS", null,
                Map.of("contributionId", String.valueOf(log.getId()),
                        "targetUserId", target.getId(),
                        "type", type,
                        "score", request.getScore(),
                        "reasonLength", request.getReason().trim().length()));
        return log.getId();
    }

    public Page<ContributionAwardVO> listAwards(Integer page, Integer size, String keyword,
                                                 String type, String source) {
        String normalizedType = StringUtils.hasText(type) ? normalizeType(type) : null;
        String normalizedSource = normalizeSource(source);
        Set<Long> matchedUserIds = findUserIds(keyword);

        LambdaQueryWrapper<ContributionLog> query = Wrappers.lambdaQuery();
        query.eq(StringUtils.hasText(normalizedType), ContributionLog::getType, normalizedType)
                .eq(StringUtils.hasText(normalizedSource), ContributionLog::getSource, normalizedSource)
                .orderByDesc(ContributionLog::getCreateTime)
                .orderByDesc(ContributionLog::getId);
        if (matchedUserIds != null) {
            query.in(ContributionLog::getUserId,
                    matchedUserIds.isEmpty() ? Collections.singleton(-1L) : matchedUserIds);
        }

        Page<ContributionLog> entityPage = contributionLogMapper.selectPage(PageUtils.of(page, size), query);
        Set<Long> userIds = entityPage.getRecords().stream()
                .flatMap(log -> java.util.stream.Stream.of(log.getUserId(), log.getAwardedBy()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, User> users = loadUsers(userIds);
        Map<Long, Dept> departments = loadDepartments(users.values());

        Page<ContributionAwardVO> result = new Page<>(
                entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        result.setRecords(entityPage.getRecords().stream()
                .map(log -> toVO(log, users.get(log.getUserId()),
                        users.get(log.getAwardedBy()), departments))
                .toList());
        return result;
    }

    private Set<Long> findUserIds(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }

        String normalizedKeyword = keyword.trim();
        if (normalizedKeyword.length() > 64) {
            throw new CsaException(ApiErrorCode.BUSINESS_RULE_VIOLATION, "搜索关键词不能超过 64 个字符");
        }
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>()
                .and(wrapper -> wrapper
                        .like(User::getUsername, normalizedKeyword)
                        .or()
                        .like(User::getRealName, normalizedKeyword)
                        .or()
                        .like(User::getStudentId, normalizedKeyword))
                .select(User::getId));
        return users.stream().map(User::getId).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    private Map<Long, User> loadUsers(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return userMapper.selectBatchIds(ids).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(User::getId, Function.identity(), (left, right) -> left));
    }

    private Map<Long, Dept> loadDepartments(Collection<User> users) {
        Set<Long> departmentIds = users.stream()
                .map(User::getDepartmentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (departmentIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return deptMapper.selectBatchIds(departmentIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Dept::getId, Function.identity(), (left, right) -> left));
    }

    private ContributionAwardVO toVO(ContributionLog log, User target, User operator,
                                     Map<Long, Dept> departments) {
        ContributionAwardVO vo = new ContributionAwardVO();
        vo.setId(log.getId());
        vo.setUserId(log.getUserId());
        vo.setUsername(target == null ? null : target.getUsername());
        vo.setRealName(target == null ? null : target.getRealName());
        Dept department = target == null ? null : departments.get(target.getDepartmentId());
        vo.setDepartmentName(department == null ? null : department.getName());
        vo.setType(log.getType());
        vo.setTypeLabel(typeLabel(log.getType()));
        vo.setScore(log.getScore());
        vo.setReason(log.getDetail());
        vo.setSource(log.getSource());
        vo.setSourceLabel(sourceLabel(log.getSource()));
        vo.setAwardedBy(log.getAwardedBy());
        vo.setAwardedByUsername(operator == null ? null : operator.getUsername());
        vo.setCreateTime(log.getCreateTime());
        return vo;
    }

    private String normalizeType(String type) {
        String normalized = type == null ? "" : type.trim().toUpperCase(Locale.ROOT);
        try {
            return ContributionType.valueOf(normalized).name();
        } catch (IllegalArgumentException ex) {
            throw new CsaException(ApiErrorCode.BUSINESS_RULE_VIOLATION,
                    "贡献类型必须是 DEV、RES、COMP 或 OPS");
        }
    }

    private String normalizeSource(String source) {
        if (!StringUtils.hasText(source)) {
            return null;
        }
        String normalized = source.trim().toUpperCase(Locale.ROOT);
        if (!SOURCE_AUTO.equals(normalized) && !SOURCE_MANUAL.equals(normalized)
                && !SOURCE_LEGACY.equals(normalized)) {
            throw new CsaException(ApiErrorCode.BUSINESS_RULE_VIOLATION,
                    "记录来源必须是 AUTO、MANUAL 或 LEGACY");
        }
        return normalized;
    }

    private void validateScore(BigDecimal score) {
        if (score == null || score.signum() <= 0 || score.scale() > 2
                || score.precision() - score.scale() > 8) {
            throw new CsaException(ApiErrorCode.BUSINESS_RULE_VIOLATION,
                    "贡献分值必须大于 0，最多 8 位整数和 2 位小数");
        }
    }

    private String typeLabel(String type) {
        if (type == null) {
            return "未知类型";
        }
        try {
            return ContributionType.valueOf(type).getDesc();
        } catch (IllegalArgumentException ex) {
            return type;
        }
    }

    private String sourceLabel(String source) {
        if (SOURCE_MANUAL.equals(source)) {
            return "人工补录";
        }
        if (SOURCE_AUTO.equals(source)) {
            return "系统自动";
        }
        return "历史记录";
    }
}

package com.csa.official.modules.sys.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.csa.official.common.constant.RoleConsts;
import com.csa.official.common.exception.ApiErrorCode;
import com.csa.official.common.exception.CsaException;
import com.csa.official.common.exception.ResourceNotFoundException;
import com.csa.official.modules.sys.entity.Dept;
import com.csa.official.modules.sys.entity.User;
import com.csa.official.modules.sys.mapper.DeptMapper;
import com.csa.official.modules.sys.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeptService {

    private final DeptMapper deptMapper;
    private final UserMapper userMapper;
    private final AuditService auditService;

    /**
     * 任命正部长 (包含自动降级逻辑)
     */
    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {
            @CacheEvict(value = "dept_list", allEntries = true),
            @CacheEvict(value = "auth_user", allEntries = true),
            @CacheEvict(value = "public_contributors", allEntries = true)
    })
    public void appointLeader(Long deptId, Long newLeaderId) {
        try {
            // 1. 获取部门信息
            Dept dept = deptMapper.selectById(deptId);
            if (dept == null)
                throw new ResourceNotFoundException("部门不存在");

            // 2. 获取新部长信息
            User newLeader = userMapper.selectById(newLeaderId);
            if (newLeader == null)
                throw new CsaException(HttpStatus.NOT_FOUND.value(), "用户不存在");
            if (newLeader.getRoleLevel() != null && newLeader.getRoleLevel() >= RoleConsts.PRESIDENT) {
                throw new CsaException(HttpStatus.BAD_REQUEST.value(), "会长或 Root 账号不能被任命为部长");
            }

            log.info("开始执行任命: 部门[{}] -> 新部长[{}]", dept.getName(), newLeader.getUsername());

            Long oldLeaderId = dept.getLeaderId();

            // 3. 【旧王废黜】逻辑
            if (oldLeaderId != null && !oldLeaderId.equals(newLeaderId)) {
                User oldLeader = userMapper.selectById(oldLeaderId);
                if (oldLeader != null) {
                    oldLeader.setRoleLevel(RoleConsts.CORE_MEMBER);
                    oldLeader.setPositionType(1);
                    userMapper.updateById(oldLeader);
                    log.info("自动降级: 原部长 [{}] 已降级为成员", oldLeader.getUsername());
                }
            }

            // 4. 【新王登基】逻辑
            // 先检查他是不是已经是别的部门的部长了（防止一人兼多职）
            if (newLeader.getRoleLevel() == RoleConsts.MINISTER && newLeader.getDepartmentId() != null
                    && !newLeader.getDepartmentId().equals(deptId)) {
                throw new CsaException(HttpStatus.CONFLICT.value(), "该成员已是其他部门的部长，请先卸任！");
            }

            newLeader.setRoleLevel(RoleConsts.MINISTER);
            newLeader.setDepartmentId(deptId);
            newLeader.setPositionType(3);
            userMapper.updateById(newLeader);

            // 5. 更新部门表
            dept.setLeaderId(newLeaderId);
            deptMapper.updateById(dept);

            auditService.recordBestEffort("ROLE_CHANGE", "USER", String.valueOf(newLeaderId),
                    "SUCCESS", null, Map.of(
                            "departmentId", deptId,
                            "newRoleLevel", RoleConsts.MINISTER,
                            "previousLeaderChanged", oldLeaderId != null && !oldLeaderId.equals(newLeaderId)));

            log.info("任命成功: [{}] 正式就任 [{}] 部长", newLeader.getUsername(), dept.getName());

        } catch (CsaException e) {
            throw e; // 业务异常直接抛出
        } catch (Exception e) {
            log.error("任命过程发生未知错误", e);
            throw new CsaException(ApiErrorCode.INTERNAL_ERROR, "任命失败，系统已回滚", e);
        }
    }

    /**
     * 批量提拔
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"auth_user", "public_contributors"}, allEntries = true)
    public void batchPromoteToMember(Long deptId, List<Long> userIds) {
        if (deptMapper.selectById(deptId) == null)
            throw new ResourceNotFoundException("部门不存在");
        if (userIds == null || userIds.isEmpty())
            return;

        // 单条 UPDATE ... WHERE id IN (...) AND role_level < CORE_MEMBER，取代逐条 updateById
        int count = userMapper.update(null, new LambdaUpdateWrapper<User>()
                .set(User::getRoleLevel, RoleConsts.CORE_MEMBER)
                .set(User::getPositionType, 1)
                .set(User::getDepartmentId, deptId)
                .in(User::getId, userIds)
                .lt(User::getRoleLevel, RoleConsts.CORE_MEMBER));

        auditService.recordBestEffort("ROLE_CHANGE_BATCH", "DEPARTMENT", String.valueOf(deptId),
                "SUCCESS", null, Map.of(
                        "newRoleLevel", RoleConsts.CORE_MEMBER,
                        "requestedCount", userIds.size(),
                        "updatedCount", count));

        log.info("批量提拔完成: 部门ID={}, 实操人数={}", deptId, count);
    }

    /**
     * 获取所有部门列表 (带缓存)
     * 第一次查库，放入 Redis；以后直接读 Redis。
     */
    @Cacheable(value = "dept_list", key = "'all'")
    public List<Dept> getAllDepts() {
        return deptMapper.selectList(null);
    }
}

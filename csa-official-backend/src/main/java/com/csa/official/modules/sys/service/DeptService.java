package com.csa.official.modules.sys.service;

import com.csa.official.common.constant.RoleConsts;
import com.csa.official.common.exception.CsaException;
import com.csa.official.common.exception.ResourceNotFoundException;
import com.csa.official.modules.sys.entity.Dept;
import com.csa.official.modules.sys.entity.User;
import com.csa.official.modules.sys.mapper.DeptMapper;
import com.csa.official.modules.sys.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeptService {

    private final DeptMapper deptMapper;
    private final UserMapper userMapper;

    /**
     * 任命正部长 (包含自动降级逻辑)
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "dept_list", key = "'all'")
    public void appointLeader(Long deptId, Long newLeaderId) {
        try {
            // 1. 获取部门信息
            Dept dept = deptMapper.selectById(deptId);
            if (dept == null)
                throw new ResourceNotFoundException("部门不存在");

            // 2. 获取新部长信息
            User newLeader = userMapper.selectById(newLeaderId);
            if (newLeader == null)
                throw new CsaException("用户不存在");

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
                throw new CsaException("该成员已是其他部门的部长，请先卸任！");
            }

            newLeader.setRoleLevel(RoleConsts.MINISTER);
            newLeader.setDepartmentId(deptId);
            newLeader.setPositionType(3);
            userMapper.updateById(newLeader);

            // 5. 更新部门表
            dept.setLeaderId(newLeaderId);
            deptMapper.updateById(dept);

            log.info("任命成功: [{}] 正式就任 [{}] 部长", newLeader.getUsername(), dept.getName());

        } catch (CsaException e) {
            throw e; // 业务异常直接抛出
        } catch (Exception e) {
            log.error("任命过程发生未知错误", e);
            throw new CsaException("任命失败，系统已回滚"); // 包装成业务异常抛出，触发事务回滚
        }
    }

    /**
     * 批量提拔
     */
    @Transactional(rollbackFor = Exception.class)
    public void batchPromoteToMember(Long deptId, List<Long> userIds) {
        if (deptMapper.selectById(deptId) == null)
            throw new CsaException("部门不存在");
        if (userIds == null || userIds.isEmpty())
            return;

        List<User> users = userMapper.selectBatchIds(userIds);
        int count = 0;

        for (User user : users) {
            if (user.getRoleLevel() < RoleConsts.CORE_MEMBER) {
                user.setRoleLevel(RoleConsts.CORE_MEMBER);
                user.setPositionType(1);
                user.setDepartmentId(deptId);
                userMapper.updateById(user);
                count++;
            }
        }
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
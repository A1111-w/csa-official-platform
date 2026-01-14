package com.csa.official.modules.sys.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.csa.official.modules.sys.entity.Proposal;
import com.csa.official.modules.sys.entity.User;
import com.csa.official.modules.sys.entity.VoteRecord;
import com.csa.official.modules.sys.mapper.ProposalMapper;
import com.csa.official.modules.sys.mapper.UserMapper;
import com.csa.official.modules.sys.mapper.VoteRecordMapper;
import com.csa.official.common.constant.RoleConsts;
import com.csa.official.common.exception.CsaException;
import com.csa.official.modules.sys.enums.VoteResultEnum;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class VoteService {

    @Autowired
    private ProposalMapper proposalMapper;
    @Autowired
    private VoteRecordMapper voteRecordMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private com.csa.official.common.util.JwtUtils jwtUtils;

    // 1. 发起提案
    public Proposal createProposal(String type, String title, String reason, Long userId) {
        Proposal p = new Proposal();
        p.setType(type);
        p.setTitle(title);
        p.setReason(reason);
        p.setProposerId(userId);
        p.setStatus(0); // 进行中
        p.setExpireTime(LocalDateTime.now().plusDays(1)); // 默认24小时有效期
        proposalMapper.insert(p); // 插入后，MyBatis 会自动把生成的 ID 填回 p 对象
        return p;
    }

    // 2. 投票核心逻辑
    @Transactional(rollbackFor = Exception.class)
    public String vote(Long proposalId, Long userId, boolean agree, String comment) {
        Proposal p = proposalMapper.selectById(proposalId);
        if (p == null || p.getStatus() != 0)
            throw new CsaException("提案不存在或已结束");
        if (LocalDateTime.now().isAfter(p.getExpireTime()))
            throw new CsaException("提案已过期");

        if (p.getProposerId().equals(userId)) {
            throw new CsaException("发起人必须避嫌，不能参与投票");
        }
        // 检查是否投过
        if (voteRecordMapper.exists(new LambdaQueryWrapper<VoteRecord>()
                .eq(VoteRecord::getProposalId, proposalId)
                .eq(VoteRecord::getVoterId, userId))) {
            throw new CsaException("你已经投过票了");
        }

        // 获取用户权重
        User voter = userMapper.selectById(userId);
        int weight = 0;
        if (voter.getRoleLevel() == RoleConsts.PRESIDENT)
            weight = 2; // 替换 4
        else if (voter.getRoleLevel() == RoleConsts.MINISTER)
            weight = 1; // 替换 3
        else
            throw new CsaException("无权投票");
        // 记录投票
        VoteRecord record = new VoteRecord();
        record.setProposalId(proposalId);
        record.setVoterId(userId);
        record.setResult(agree ? VoteResultEnum.AGREE : VoteResultEnum.REJECT);
        record.setWeight(weight);
        record.setComment(comment);
        voteRecordMapper.insert(record);
        log.info("收到投票: 提案ID={}, 投票人={}, 态度={}, 权重={}",
                proposalId, voter.getUsername(), agree ? "赞成" : "反对", record.getWeight());
        // 3. 实时结算：检查是否满足通过条件
        return checkResult(p);
    }

    private String checkResult(Proposal p) {
        // 1. 获取当前的实际投票结果
        List<VoteRecord> records = voteRecordMapper.selectList(new LambdaQueryWrapper<VoteRecord>()
                .eq(VoteRecord::getProposalId, p.getId()));

        int currentAgreeWeight = records.stream()
                .filter(r -> r.getResult() == VoteResultEnum.AGREE)
                .mapToInt(VoteRecord::getWeight).sum();

        int currentRejectWeight = records.stream()
                .filter(r -> r.getResult() == VoteResultEnum.REJECT)
                .mapToInt(VoteRecord::getWeight).sum();

        // 规则：只统计 Level 3 (部长/副部长) 和 Level 4 (会长/副会长)
        // (Root) 参与分母计算
        List<User> cabinetMembers = userMapper.selectList(new LambdaQueryWrapper<User>()
                .in(User::getRoleLevel, 3, 4) // 只查 3 和 4
                .eq(User::getDeleted, 0));

        int totalPossibleWeight = 0;
        for (User u : cabinetMembers) {
            // 排除发起人自己 (避嫌)
            if (u.getId().equals(p.getProposerId())) {
                continue;
            }

            // 权重计算：
            // Level 4 (会长/副会长) = 2票
            // Level 3 (部长/副部长) = 1票
            if (u.getRoleLevel() == RoleConsts.PRESIDENT) {
                totalPossibleWeight += 2;
            } else if (u.getRoleLevel() == RoleConsts.MINISTER) {
                totalPossibleWeight += 1;
            }
        }

        // 3. 计算过半数阈值
        int passThreshold = (totalPossibleWeight / 2) + 1;

        log.info("【投票结算】当前同意:" + currentAgreeWeight + " / 阈值:" + passThreshold + " (有效总池:" + totalPossibleWeight + ")");

        // 4. 判定结果
        if (currentAgreeWeight >= passThreshold) {
            p.setStatus(1); // 通过
            p.setFinalResultJson("同意:" + currentAgreeWeight + ", 反对:" + currentRejectWeight + ", 阈值:" + passThreshold);
            proposalMapper.updateById(p);
            log.info("提案[{}] 已通过! 最终结果: {}", p.getTitle(), p.getFinalResultJson());

            // 申请 Root 权限通过，发放令牌
            if ("ROOT_APPLY".equals(p.getType())) {
                User proposer = userMapper.selectById(p.getProposerId());
                long threeDays = 3L * 24 * 60 * 60 * 1000;
                proposer.setRoleLevel(99);
                String rootToken = jwtUtils.generateToken(proposer, threeDays);
                return "提案通过！您的临时Root令牌(3天有效): " + rootToken;
            }
            return "投票成功，提案已通过！";
        }

        if (currentRejectWeight >= passThreshold) {
            p.setStatus(2); // 驳回
            p.setFinalResultJson("同意:" + currentAgreeWeight + ", 反对:" + currentRejectWeight + ", 阈值:" + passThreshold);
            proposalMapper.updateById(p);
            log.info("提案[{}] 被驳回! 最终结果: {}", p.getTitle(), p.getFinalResultJson());
            return "投票成功，提案被驳回。";
        }

        return "投票成功，当前进度 (" + currentAgreeWeight + "/" + passThreshold + ")，等待更多人表决...";
    }
}

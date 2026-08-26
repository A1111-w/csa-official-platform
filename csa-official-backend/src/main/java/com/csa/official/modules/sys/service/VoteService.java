package com.csa.official.modules.sys.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.csa.official.common.constant.RoleConsts;
import com.csa.official.common.exception.ApiErrorCode;
import com.csa.official.common.exception.CsaException;
import com.csa.official.modules.sys.entity.Proposal;
import com.csa.official.modules.sys.entity.User;
import com.csa.official.modules.sys.entity.VoteRecord;
import com.csa.official.modules.sys.enums.VoteResultEnum;
import com.csa.official.modules.sys.mapper.ProposalMapper;
import com.csa.official.modules.sys.mapper.UserMapper;
import com.csa.official.modules.sys.mapper.VoteRecordMapper;
import com.csa.official.modules.sys.vo.VoteTallyVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;

@Slf4j
@Service
public class VoteService {

    private final ProposalMapper proposalMapper;
    private final VoteRecordMapper voteRecordMapper;
    private final UserMapper userMapper;

    public VoteService(
            ProposalMapper proposalMapper,
            VoteRecordMapper voteRecordMapper,
            UserMapper userMapper) {
        this.proposalMapper = proposalMapper;
        this.voteRecordMapper = voteRecordMapper;
        this.userMapper = userMapper;
    }

    public Proposal createProposal(String type, String title, String reason, Long userId) {
        String normalizedType = normalizeProposalType(type);
        if (isRootApplyProposal(normalizedType)) {
            throw new CsaException(ApiErrorCode.BAD_REQUEST, "ROOT_APPLY proposals require manual handling");
        }

        Proposal proposal = new Proposal();
        proposal.setType(normalizedType);
        proposal.setTitle(title);
        proposal.setReason(reason);
        proposal.setProposerId(userId);
        proposal.setStatus(0);
        proposal.setExpireTime(LocalDateTime.now().plusDays(1));
        proposalMapper.insert(proposal);
        return proposal;
    }

    @Transactional(rollbackFor = Exception.class)
    public String vote(Long proposalId, Long userId, boolean agree, String comment) {
        Proposal proposal = proposalMapper.selectById(proposalId);
        if (proposal == null || proposal.getStatus() != 0) {
            throw new CsaException(ApiErrorCode.CONFLICT, "Proposal not found or already closed");
        }
        if (LocalDateTime.now().isAfter(proposal.getExpireTime())) {
            throw new CsaException(ApiErrorCode.CONFLICT, "Proposal has expired");
        }
        if (proposal.getProposerId().equals(userId)) {
            throw new CsaException(ApiErrorCode.ACCESS_DENIED, "Proposers cannot vote on their own proposal");
        }
        if (voteRecordMapper.exists(new LambdaQueryWrapper<VoteRecord>()
                .eq(VoteRecord::getProposalId, proposalId)
                .eq(VoteRecord::getVoterId, userId))) {
            throw new CsaException(ApiErrorCode.CONFLICT, "You have already voted");
        }

        User voter = userMapper.selectById(userId);
        if (voter == null || voter.getRoleLevel() == null) {
            throw new CsaException(ApiErrorCode.RESOURCE_NOT_FOUND, "Voter does not exist");
        }

        int weight;
        if (voter.getRoleLevel() == RoleConsts.PRESIDENT) {
            weight = 2;
        } else if (voter.getRoleLevel() == RoleConsts.MINISTER) {
            weight = 1;
        } else {
            throw new CsaException(ApiErrorCode.ACCESS_DENIED, "You are not allowed to vote");
        }

        VoteRecord record = new VoteRecord();
        record.setProposalId(proposalId);
        record.setVoterId(userId);
        record.setResult(agree ? VoteResultEnum.AGREE : VoteResultEnum.REJECT);
        record.setWeight(weight);
        record.setComment(comment);
        voteRecordMapper.insert(record);

        log.info("Vote received: proposalId={}, voter={}, agree={}, weight={}",
                proposalId, voter.getUsername(), agree, weight);
        return checkResult(proposal);
    }

    private String checkResult(Proposal proposal) {
        VoteTallyVO tally = voteRecordMapper.selectTally(proposal.getId());
        int currentAgreeWeight = tally == null || tally.getAgreeWeight() == null
                ? 0
                : tally.getAgreeWeight();
        int currentRejectWeight = tally == null || tally.getRejectWeight() == null
                ? 0
                : tally.getRejectWeight();

        Integer eligibleWeight = userMapper.selectEligibleVoteWeight(
                proposal.getProposerId(), RoleConsts.MINISTER, RoleConsts.PRESIDENT);
        int totalPossibleWeight = eligibleWeight == null ? 0 : eligibleWeight;
        int passThreshold = (totalPossibleWeight / 2) + 1;

        log.info("Vote tally: agree={} threshold={} pool={}",
                currentAgreeWeight, passThreshold, totalPossibleWeight);

        if (currentAgreeWeight >= passThreshold) {
            proposal.setStatus(1);
            proposal.setFinalResultJson("agree:" + currentAgreeWeight
                    + ", reject:" + currentRejectWeight
                    + ", threshold:" + passThreshold);
            proposalMapper.updateById(proposal);

            if (isRootApplyProposal(proposal.getType())) {
                log.warn("ROOT_APPLY proposal {} passed but automatic elevation is disabled", proposal.getId());
                return "ROOT_APPLY passed, but automatic elevation is disabled. Please handle manually.";
            }
            return "Proposal passed";
        }

        if (currentRejectWeight >= passThreshold) {
            proposal.setStatus(2);
            proposal.setFinalResultJson("agree:" + currentAgreeWeight
                    + ", reject:" + currentRejectWeight
                    + ", threshold:" + passThreshold);
            proposalMapper.updateById(proposal);
            return "Proposal rejected";
        }

        return "Vote recorded (" + currentAgreeWeight + "/" + passThreshold + ")";
    }

    private boolean isRootApplyProposal(String type) {
        return "ROOT_APPLY".equals(normalizeProposalType(type));
    }

    private String normalizeProposalType(String type) {
        return type == null ? "" : type.trim().toUpperCase(Locale.ROOT);
    }
}

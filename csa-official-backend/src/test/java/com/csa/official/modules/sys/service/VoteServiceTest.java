package com.csa.official.modules.sys.service;

import com.csa.official.common.constant.RoleConsts;
import com.csa.official.common.exception.CsaException;
import com.csa.official.modules.sys.entity.Proposal;
import com.csa.official.modules.sys.entity.User;
import com.csa.official.modules.sys.mapper.ProposalMapper;
import com.csa.official.modules.sys.mapper.UserMapper;
import com.csa.official.modules.sys.mapper.VoteRecordMapper;
import com.csa.official.modules.sys.vo.VoteTallyVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VoteServiceTest {

    @Mock
    private ProposalMapper proposalMapper;

    @Mock
    private VoteRecordMapper voteRecordMapper;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private VoteService voteService;

    @Test
    void createProposalRejectsRootApply() {
        assertThatThrownBy(() -> voteService.createProposal("root_apply", "title", "reason", 1L))
                .isInstanceOf(CsaException.class)
                .hasMessageContaining("ROOT_APPLY");
    }

    @Test
    void rootApplyVotePassesWithoutAutomaticElevation() {
        Proposal proposal = new Proposal();
        proposal.setId(1L);
        proposal.setType("ROOT_APPLY");
        proposal.setTitle("Root");
        proposal.setProposerId(2L);
        proposal.setStatus(0);
        proposal.setExpireTime(LocalDateTime.now().plusHours(1));

        User voter = new User();
        voter.setId(1L);
        voter.setUsername("president");
        voter.setRoleLevel(RoleConsts.PRESIDENT);
        voter.setDeleted(0);

        VoteTallyVO tally = new VoteTallyVO();
        tally.setAgreeWeight(2);
        tally.setRejectWeight(0);

        when(proposalMapper.selectById(1L)).thenReturn(proposal);
        when(voteRecordMapper.exists(any())).thenReturn(false);
        when(userMapper.selectById(1L)).thenReturn(voter);
        when(voteRecordMapper.selectTally(1L)).thenReturn(tally);
        when(userMapper.selectEligibleVoteWeight(2L, RoleConsts.MINISTER, RoleConsts.PRESIDENT))
                .thenReturn(2);

        String result = voteService.vote(1L, 1L, true, "approve");

        assertThat(result).contains("automatic elevation is disabled");
    }
}

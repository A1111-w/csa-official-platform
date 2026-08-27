package com.csa.official.modules.sys.vo;

import com.csa.official.modules.sys.entity.Proposal;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 提案对外视图，屏蔽 {@code deleted} 等持久层字段。
 */
@Data
public class ProposalVO {

    private Long id;
    private String type;
    private String title;
    private String reason;
    private Long proposerId;
    /** 0:VOTING, 1:PASSED, 2:REJECTED */
    private Integer status;
    private LocalDateTime expireTime;
    private String finalResultJson;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public static ProposalVO from(Proposal proposal) {
        ProposalVO vo = new ProposalVO();
        vo.setId(proposal.getId());
        vo.setType(proposal.getType());
        vo.setTitle(proposal.getTitle());
        vo.setReason(proposal.getReason());
        vo.setProposerId(proposal.getProposerId());
        vo.setStatus(proposal.getStatus());
        vo.setExpireTime(proposal.getExpireTime());
        vo.setFinalResultJson(proposal.getFinalResultJson());
        vo.setCreateTime(proposal.getCreateTime());
        vo.setUpdateTime(proposal.getUpdateTime());
        return vo;
    }
}

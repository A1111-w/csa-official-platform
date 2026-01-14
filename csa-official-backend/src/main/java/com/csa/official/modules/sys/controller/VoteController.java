package com.csa.official.modules.sys.controller;

import com.csa.official.common.result.R;
import com.csa.official.common.util.SecurityUtils;
import com.csa.official.modules.sys.entity.Proposal;
import com.csa.official.modules.sys.mapper.ProposalMapper;
import com.csa.official.modules.sys.service.VoteService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sys/vote")
public class VoteController {

    @Autowired
    private VoteService voteService;
    @Autowired
    private ProposalMapper proposalMapper;

    // 发起提案
    @PreAuthorize("hasRole('LEVEL_3')")
    @PostMapping("/create")
    public R<Proposal> create(@RequestBody ProposalDto dto) {

        Proposal p = voteService.createProposal(dto.getType(), dto.getTitle(), dto.getReason(),
                SecurityUtils.getUserId());
        return R.ok(p);
    }

    // 投票
    @PreAuthorize("hasRole('LEVEL_3')")
    @PostMapping("/submit")
    public R<String> submit(@RequestBody VoteDto dto) {

        String resultMsg = voteService.vote(dto.getProposalId(), SecurityUtils.getUserId(), dto.isAgree(),
                dto.getComment());
        return R.ok(resultMsg);
    }

    @GetMapping("/list")
    public R<List<Proposal>> list() {
        return R.ok(proposalMapper.selectList(null));
    }

    @Data
    static class ProposalDto {
        private String type;
        private String title;
        private String reason;
    }

    @Data
    static class VoteDto {
        private Long proposalId;
        private boolean agree;
        private String comment;
    }
}
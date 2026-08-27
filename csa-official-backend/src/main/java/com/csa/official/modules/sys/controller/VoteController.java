package com.csa.official.modules.sys.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.csa.official.common.exception.ApiErrorCode;
import com.csa.official.common.exception.CsaException;
import com.csa.official.common.result.R;
import com.csa.official.common.util.PageUtils;
import com.csa.official.common.util.SecurityUtils;
import com.csa.official.modules.sys.entity.Proposal;
import com.csa.official.modules.sys.mapper.ProposalMapper;
import com.csa.official.modules.sys.service.VoteService;
import com.csa.official.modules.sys.vo.ProposalVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/sys/vote")
public class VoteController {

    @Autowired
    private VoteService voteService;

    @Autowired
    private ProposalMapper proposalMapper;

    @PreAuthorize("hasRole('LEVEL_3')")
    @PostMapping("/create")
    public R<ProposalVO> create(@RequestBody @Valid ProposalDto dto) {
        if (dto.getType() != null && "ROOT_APPLY".equals(dto.getType().toUpperCase(Locale.ROOT))) {
            throw new CsaException(ApiErrorCode.BAD_REQUEST, "ROOT_APPLY proposals require manual handling");
        }

        Proposal proposal = voteService.createProposal(dto.getType(), dto.getTitle(), dto.getReason(),
                SecurityUtils.getUserId());
        return R.ok(ProposalVO.from(proposal));
    }

    @PreAuthorize("hasRole('LEVEL_3')")
    @PostMapping("/submit")
    public R<String> submit(@RequestBody VoteDto dto) {
        String resultMsg = voteService.vote(dto.getProposalId(), SecurityUtils.getUserId(), dto.isAgree(),
                dto.getComment());
        return R.ok(resultMsg);
    }

    @PreAuthorize("hasRole('LEVEL_3')")
    @GetMapping("/list")
    public R<List<ProposalVO>> list(@RequestParam(required = false) Integer size) {
        int safeSize = PageUtils.clampLimit(size, 100);
        return R.ok(proposalMapper.selectList(new LambdaQueryWrapper<Proposal>()
                        .orderByDesc(Proposal::getCreateTime)
                        .last("LIMIT " + safeSize))
                .stream()
                .map(ProposalVO::from)
                .toList());
    }

    @Data
    static class ProposalDto {
        @NotBlank(message = "提案类型不能为空")
        private String type;

        @NotBlank(message = "提案标题不能为空")
        @Size(max = 200, message = "提案标题不能超过 200 个字符")
        private String title;

        @Size(max = 2000, message = "提案理由不能超过 2000 个字符")
        private String reason;
    }

    @Data
    static class VoteDto {
        private Long proposalId;
        private boolean agree;
        private String comment;
    }
}

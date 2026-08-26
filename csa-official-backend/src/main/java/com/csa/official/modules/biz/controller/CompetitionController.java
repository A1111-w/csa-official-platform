package com.csa.official.modules.biz.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.csa.official.common.annotation.LogContribution;
import com.csa.official.common.exception.CsaException;
import com.csa.official.common.result.R;
import com.csa.official.common.util.SecurityUtils;
import com.csa.official.modules.biz.entity.Competition;
import com.csa.official.modules.biz.enums.CompetitionStatusEnum;
import com.csa.official.modules.biz.service.CompetitionService;
import com.csa.official.modules.biz.vo.CompetitionDetailVO;
import com.csa.official.modules.biz.vo.CompetitionListVO;
import com.csa.official.modules.sys.enums.ContributionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/biz/comp")
public class CompetitionController {

    @Autowired
    private CompetitionService compService;

    @PreAuthorize("(#dto.id == null and @csaSec.canCreateCompetition()) or (#dto.id != null and @csaSec.canEditCompetition(#dto.id))")
    @LogContribution(type = ContributionType.COMP, detail = "publish or update competition")
    @PostMapping("/save")
    public R<String> save(@RequestBody @Valid SaveCompetitionDto dto) {
        Competition comp = new Competition();
        comp.setId(dto.getId());
        comp.setTitle(dto.getTitle().trim());
        comp.setContent(dto.getContent().trim());
        comp.setCoverImg(dto.getCoverImg());
        comp.setStartTime(dto.getStartTime());
        comp.setEndTime(dto.getEndTime());
        comp.setStatus(dto.getStatus());
        compService.saveCompetition(comp, SecurityUtils.getUserId());
        return R.ok("Success");
    }

    @PreAuthorize("@csaSec.canGrantCompetitionEditor(#dto.compId)")
    @PostMapping("/grant")
    public R<String> grant(@RequestBody GrantDto dto) {
        if (dto.getCompId() == null || dto.getTargetUserId() == null) {
            throw new CsaException(HttpStatus.BAD_REQUEST.value(), "Missing required parameters");
        }
        compService.addEditor(dto.getCompId(), dto.getTargetUserId(), SecurityUtils.getUserId());
        return R.ok("Success");
    }

    @GetMapping("/list")
    public R<Page<CompetitionListVO>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(required = false) Integer size) {
        return R.ok(compService.getCompetitionPage(page, size));
    }

    /**
     * 竞赛详情。列表接口只返回摘要，编辑弹窗需要完整正文时调这个。
     */
    @GetMapping("/{id}")
    public R<CompetitionDetailVO> detail(@PathVariable Long id) {
        return R.ok(compService.getCompetitionDetail(id));
    }

    @Data
    static class SaveCompetitionDto {
        private Long id;

        @NotBlank(message = "Competition title is required")
        private String title;

        @NotBlank(message = "Competition content is required")
        private String content;

        private String coverImg;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private CompetitionStatusEnum status;
    }

    @Data
    static class GrantDto {
        private Long compId;
        private Long targetUserId;
    }
}

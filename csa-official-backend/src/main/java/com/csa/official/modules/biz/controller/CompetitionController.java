package com.csa.official.modules.biz.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.csa.official.common.annotation.LogContribution;
import com.csa.official.common.result.R;
import com.csa.official.common.util.SecurityUtils;
import com.csa.official.modules.biz.entity.Competition;
import com.csa.official.modules.biz.service.CompetitionService;
import com.csa.official.modules.sys.enums.ContributionType;

import jakarta.validation.Valid;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/biz/comp")
public class CompetitionController {

    @Autowired
    private CompetitionService compService;

    @PreAuthorize("#comp.id == null or @csaSec.canEditCompetition(#comp.id)")
    @LogContribution(type = ContributionType.COMP, detail = "发布/更新比赛")
    @PostMapping("/save")
    public R<String> save(@RequestBody @Valid Competition comp) {
        compService.saveCompetition(comp, SecurityUtils.getUserId());
        return R.ok("保存成功");
    }

    @PreAuthorize("hasRole('LEVEL_3')")
    @PostMapping("/grant")
    public R<String> grant(@RequestBody GrantDto dto) {
        compService.addEditor(dto.getCompId(), dto.getTargetUserId(), SecurityUtils.getUserId());
        return R.ok("授权成功");
    }

    @GetMapping("/list")
    public R<Page<Competition>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return R.ok(compService.getCompetitionPage(page, size));
    }

    @Data
    static class GrantDto {
        private Long compId;
        private Long targetUserId;
    }
}
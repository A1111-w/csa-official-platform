package com.csa.official.modules.resume.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.csa.official.common.result.R;
import com.csa.official.common.util.SecurityUtils;
import com.csa.official.modules.resume.service.ResumeGitSyncService;
import com.csa.official.modules.resume.service.ResumeService;
import com.csa.official.modules.resume.vo.ResumeGitSyncVO;
import com.csa.official.modules.resume.vo.ResumeReviewDetailVO;
import com.csa.official.modules.resume.vo.ResumeReviewListVO;
import com.csa.official.modules.resume.vo.ResumeVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/resume")
public class ResumeController {

    @Autowired
    private ResumeService resumeService;

    @Autowired
    private ResumeGitSyncService resumeGitSyncService;

    // ================= 核心成员接口 (Level 2+) =================

    @PreAuthorize("hasRole('LEVEL_2')")
    @GetMapping("/my")
    public R<ResumeVO> getMyResume() {

        return R.ok(ResumeVO.from(resumeService.getMyResume(SecurityUtils.getUserId())));
    }

    @PreAuthorize("hasRole('LEVEL_2')")
    @PostMapping("/save")
    public R<String> save(@RequestBody @Valid ResumeDto dto) {

        resumeService.saveMyResume(SecurityUtils.getUserId(), dto.getContent(), dto.getGitRepoUrl());
        return R.ok("保存成功");
    }

    @PreAuthorize("hasRole('LEVEL_2')")
    @PostMapping("/submit")
    public R<String> submit() {

        resumeService.submitForAudit(SecurityUtils.getUserId());
        return R.ok("已提交审核");
    }

    @PreAuthorize("hasRole('LEVEL_2')")
    @GetMapping("/git-sync")
    public R<ResumeGitSyncVO> gitSyncStatus() {
        return R.ok(resumeGitSyncService.getMyStatus(SecurityUtils.getUserId()));
    }

    @PreAuthorize("hasRole('LEVEL_2')")
    @PostMapping("/git-sync")
    public R<ResumeGitSyncVO> syncGitRepository() {
        return R.ok(resumeGitSyncService.startMySync(SecurityUtils.getUserId()));
    }

    // ================= 部长/管理员接口 =================

    @PreAuthorize("hasRole('LEVEL_3')")
    @GetMapping("/reviews")
    public R<Page<ResumeReviewListVO>> reviewList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(defaultValue = "1") Integer status) {
        return R.ok(resumeService.getReviewPage(page, size, status));
    }

    @PreAuthorize("hasRole('LEVEL_3')")
    @GetMapping("/reviews/{id}")
    public R<ResumeReviewDetailVO> reviewDetail(@PathVariable Long id) {
        return R.ok(resumeService.getReviewDetail(id));
    }

    @PreAuthorize("hasRole('LEVEL_3')")
    @PostMapping("/audit")
    public R<String> audit(@RequestBody @Valid AuditDto dto) {
        resumeService.auditResume(dto.getResumeId(), dto.getPass(), dto.getReason(), SecurityUtils.getUserId());
        return R.ok("审核完成");
    }

    @Data
    static class ResumeDto {
        @Size(max = 50_000, message = "简历内容不能超过 50000 个字符")
        private String content;

        @Size(max = 255, message = "仓库链接不能超过 255 个字符")
        private String gitRepoUrl;
    }

    @Data
    static class AuditDto {
        @NotNull(message = "必须指定简历ID")
        private Long resumeId;

        @NotNull(message = "必须指定审核结果")
        private Boolean pass;

        @Size(max = 500, message = "审核原因不能超过 500 个字符")
        private String reason;
    }
}

package com.csa.official.modules.resume.controller;

import com.csa.official.common.result.R;
import com.csa.official.common.util.SecurityUtils;
import com.csa.official.modules.resume.entity.Resume;
import com.csa.official.modules.resume.service.ResumeService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/resume")
public class ResumeController {

    @Autowired
    private ResumeService resumeService;

    // ================= 核心成员接口 (Level 2+) =================

    @PreAuthorize("hasRole('LEVEL_2')")
    @GetMapping("/my")
    public R<Resume> getMyResume() {

        return R.ok(resumeService.getMyResume(SecurityUtils.getUserId()));
    }

    @PreAuthorize("hasRole('LEVEL_2')")
    @PostMapping("/save")
    public R<String> save(@RequestBody ResumeDto dto) {

        resumeService.saveMyResume(SecurityUtils.getUserId(), dto.getContent(), dto.getGitRepoUrl());
        return R.ok("保存成功");
    }

    @PreAuthorize("hasRole('LEVEL_2')")
    @PostMapping("/submit")
    public R<String> submit() {

        resumeService.submitForAudit(SecurityUtils.getUserId());
        return R.ok("已提交审核");
    }

    // ================= 部长/管理员接口 =================

    @PreAuthorize("hasRole('LEVEL_3')")
    @PostMapping("/audit")
    public R<String> audit(@RequestBody AuditDto dto) {
        if (dto.getResumeId() == null)
            return R.fail("必须指定简历ID");

        resumeService.auditResume(dto.getResumeId(), dto.isPass(), dto.getReason(), SecurityUtils.getUserId());
        return R.ok("审核完成");
    }

    @Data
    static class ResumeDto {
        private String content;
        private String gitRepoUrl;
    }

    @Data
    static class AuditDto {
        private Long resumeId;
        private boolean pass;
        private String reason;
    }
}
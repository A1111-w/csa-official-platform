package com.csa.official.modules.resume.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.csa.official.modules.resume.entity.Resume;
import com.csa.official.modules.resume.enums.ResumeStatusEnum;
import com.csa.official.modules.resume.mapper.ResumeMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ResumeService {

    @Autowired
    private ResumeMapper resumeMapper;

    // 1. 获取我的简历
    public Resume getMyResume(Long userId) {
        return resumeMapper.selectOne(new LambdaQueryWrapper<Resume>()
                .eq(Resume::getUserId, userId));
    }

    // 2. 保存简历 (支持 内容 和 Git地址)
    public void saveMyResume(Long userId, String content, String gitUrl) {
        Resume resume = getMyResume(userId);
        if (resume == null) {
            resume = new Resume();
            resume.setUserId(userId);
            resume.setStatus(ResumeStatusEnum.DRAFT);
        }
        
        // 如果之前已经通过了，现在修改了内容，需要重置为待审核或草稿
        if (resume.getStatus() == ResumeStatusEnum.APPROVED) {
            resume.setStatus(ResumeStatusEnum.PENDING); // 或者 DRAFT，看你业务要求
        }
        
        resume.setContent(content);
        resume.setGitRepoUrl(gitUrl);
        
        if (resume.getId() == null) {
            resumeMapper.insert(resume);
        } else {
            resumeMapper.updateById(resume);
        }
    }
    
    // 3. 提交审核
    public void submitForAudit(Long userId) {
        Resume resume = getMyResume(userId);
        if (resume != null) {
            resume.setStatus(ResumeStatusEnum.PENDING);
            resumeMapper.updateById(resume);
        }
    }

    // 4. 部长审核 (核心)
    public void auditResume(Long resumeId, boolean pass, String reason, Long ministerId) {
        Resume resume = resumeMapper.selectById(resumeId);
        if (resume == null) return;

        if (pass) {
            resume.setStatus(ResumeStatusEnum.APPROVED);
        } else {
            resume.setStatus(ResumeStatusEnum.REJECTED);
            resume.setRejectReason(reason);
        }
        
        resume.setAuditBy(ministerId);
        resume.setAuditTime(LocalDateTime.now());
        resumeMapper.updateById(resume);
    }
}

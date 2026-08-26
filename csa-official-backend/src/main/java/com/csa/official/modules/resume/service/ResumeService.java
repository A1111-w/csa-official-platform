package com.csa.official.modules.resume.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.csa.official.common.cache.KeyValueStore;
import com.csa.official.common.exception.ApiErrorCode;
import com.csa.official.common.exception.CsaException;
import com.csa.official.common.util.PageUtils;
import com.csa.official.modules.resume.entity.Resume;
import com.csa.official.modules.resume.enums.ResumeStatusEnum;
import com.csa.official.modules.resume.mapper.ResumeMapper;
import com.csa.official.modules.resume.vo.ResumeReviewDetailVO;
import com.csa.official.modules.resume.vo.ResumeReviewListVO;
import com.csa.official.modules.sys.entity.Dept;
import com.csa.official.modules.sys.entity.User;
import com.csa.official.modules.sys.mapper.DeptMapper;
import com.csa.official.modules.sys.mapper.UserMapper;
import com.csa.official.modules.sys.service.AuditService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ResumeService {

    private final ResumeMapper resumeMapper;
    private final KeyValueStore keyValueStore;
    private final AuditService auditService;
    private final UserMapper userMapper;
    private final DeptMapper deptMapper;

    public ResumeService(ResumeMapper resumeMapper,
                         KeyValueStore keyValueStore,
                         AuditService auditService,
                         UserMapper userMapper,
                         DeptMapper deptMapper) {
        this.resumeMapper = resumeMapper;
        this.keyValueStore = keyValueStore;
        this.auditService = auditService;
        this.userMapper = userMapper;
        this.deptMapper = deptMapper;
    }

    public Resume getMyResume(Long userId) {
        return resumeMapper.selectOne(new LambdaQueryWrapper<Resume>()
                .eq(Resume::getUserId, userId));
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveMyResume(Long userId, String content, String gitUrl) {
        String normalizedContent = content == null ? null : content.trim();
        String normalizedGitUrl = normalizeGitUrl(gitUrl);
        if (!StringUtils.hasText(normalizedContent) && !StringUtils.hasText(normalizedGitUrl)) {
            throw new CsaException(ApiErrorCode.BAD_REQUEST, "Resume content or Git repository URL is required");
        }

        String lockKey = "lock:resume:user:" + userId;
        String lockToken = UUID.randomUUID().toString();
        if (!keyValueStore.setIfAbsent(lockKey, lockToken, 30, TimeUnit.SECONDS)) {
            throw new CsaException(HttpStatus.CONFLICT.value(), "Resume is being updated; please retry");
        }

        boolean releaseAfterTransaction = registerLockRelease(lockKey, lockToken);
        try {
            Resume resume = getMyResume(userId);
            if (resume == null) {
                resume = new Resume();
                resume.setUserId(userId);
                resume.setStatus(ResumeStatusEnum.DRAFT);
            }

            if (resume.getStatus() == ResumeStatusEnum.PENDING) {
                throw new CsaException(ApiErrorCode.CONFLICT, "Resume is under review and cannot be edited");
            }

            if (resume.getStatus() == ResumeStatusEnum.APPROVED
                    || resume.getStatus() == ResumeStatusEnum.REJECTED) {
                resume.setStatus(ResumeStatusEnum.DRAFT);
                resume.setRejectReason(null);
                resume.setAuditBy(null);
                resume.setAuditTime(null);
            }

            boolean gitUrlChanged = !Objects.equals(resume.getGitRepoUrl(), normalizedGitUrl);
            resume.setContent(normalizedContent);
            resume.setGitRepoUrl(normalizedGitUrl);
            if (gitUrlChanged) {
                resetGitSync(resume);
            }

            if (resume.getId() == null) {
                resumeMapper.insert(resume);
            } else {
                resumeMapper.updateById(resume);
            }
        } finally {
            if (!releaseAfterTransaction) {
                releaseLock(lockKey, lockToken);
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void submitForAudit(Long userId) {
        Resume resume = getMyResume(userId);
        if (resume == null) {
            throw new CsaException(HttpStatus.NOT_FOUND.value(), "Resume not found");
        }

        if (resume.getStatus() == ResumeStatusEnum.PENDING) {
            throw new CsaException(ApiErrorCode.CONFLICT, "Resume is already pending review");
        }

        if (!StringUtils.hasText(resume.getContent()) && !StringUtils.hasText(resume.getGitRepoUrl())) {
            throw new CsaException(ApiErrorCode.BAD_REQUEST, "Resume content or Git repository URL is required");
        }

        resume.setStatus(ResumeStatusEnum.PENDING);
        resume.setRejectReason(null);
        resume.setAuditBy(null);
        resume.setAuditTime(null);
        resumeMapper.updateById(resume);
    }

    public Page<ResumeReviewListVO> getReviewPage(Integer page, Integer size, Integer statusCode) {
        ResumeStatusEnum status = requireReviewStatus(statusCode);
        Page<Resume> resumePage = resumeMapper.selectPage(
                PageUtils.of(page, size),
                new LambdaQueryWrapper<Resume>()
                        .eq(Resume::getStatus, status)
                        .orderByDesc(Resume::getUpdateTime)
                        .orderByDesc(Resume::getId));

        Map<Long, User> users = loadUsers(resumePage.getRecords().stream()
                .map(Resume::getUserId)
                .toList());
        Map<Long, Dept> departments = loadDepartments(users.values());

        Page<ResumeReviewListVO> result = new Page<>(
                resumePage.getCurrent(), resumePage.getSize(), resumePage.getTotal());
        result.setRecords(resumePage.getRecords().stream()
                .map(resume -> toReviewListVO(
                        resume,
                        users.get(resume.getUserId()),
                        departments))
                .toList());
        return result;
    }

    public ResumeReviewDetailVO getReviewDetail(Long resumeId) {
        if (resumeId == null) {
            throw new CsaException(ApiErrorCode.BAD_REQUEST, "Resume id is required");
        }

        Resume resume = resumeMapper.selectById(resumeId);
        if (resume == null || !isReviewVisible(resume.getStatus())) {
            throw new CsaException(ApiErrorCode.RESOURCE_NOT_FOUND, "Resume not found");
        }

        User applicant = userMapper.selectById(resume.getUserId());
        User auditor = resume.getAuditBy() == null ? null : userMapper.selectById(resume.getAuditBy());
        Dept department = applicant == null || applicant.getDepartmentId() == null
                ? null
                : deptMapper.selectById(applicant.getDepartmentId());
        return toReviewDetailVO(resume, applicant, auditor, department);
    }

    @Transactional(rollbackFor = Exception.class)
    public void auditResume(Long resumeId, boolean pass, String reason, Long ministerId) {
        String normalizedReason = reason == null ? null : reason.trim();
        if (!pass && !StringUtils.hasText(normalizedReason)) {
            throw new CsaException(ApiErrorCode.BAD_REQUEST, "Reject reason is required");
        }
        if (normalizedReason != null && normalizedReason.length() > 500) {
            throw new CsaException(ApiErrorCode.BAD_REQUEST, "Reject reason cannot exceed 500 characters");
        }

        Resume resume = resumeMapper.selectById(resumeId);
        if (resume == null) {
            throw new CsaException(HttpStatus.NOT_FOUND.value(), "Resume not found");
        }

        if (resume.getStatus() != ResumeStatusEnum.PENDING) {
            throw new CsaException(ApiErrorCode.CONFLICT, "Only pending resumes can be reviewed");
        }

        ResumeStatusEnum decision = pass ? ResumeStatusEnum.APPROVED : ResumeStatusEnum.REJECTED;
        int updated = resumeMapper.update(null, new LambdaUpdateWrapper<Resume>()
                .set(Resume::getStatus, decision)
                .set(Resume::getRejectReason, pass ? null : normalizedReason)
                .set(Resume::getAuditBy, ministerId)
                .set(Resume::getAuditTime, LocalDateTime.now())
                .eq(Resume::getId, resumeId)
                .eq(Resume::getStatus, ResumeStatusEnum.PENDING));
        if (updated <= 0) {
            throw new CsaException(ApiErrorCode.CONFLICT, "Resume review state changed; refresh and retry");
        }

        auditService.recordBestEffort("RESUME_REVIEW", "RESUME", String.valueOf(resumeId),
                "SUCCESS", null, Map.of(
                        "decision", decision.name(),
                        "subjectUserId", resume.getUserId(),
                        "reviewerUserId", ministerId));
    }

    static String buildContentSummary(String content) {
        if (!StringUtils.hasText(content)) {
            return "";
        }

        String normalized = content.replaceAll("\\s+", " ").trim();
        int maxLength = 160;
        return normalized.length() <= maxLength
                ? normalized
                : normalized.substring(0, maxLength) + "…";
    }

    private ResumeStatusEnum requireReviewStatus(Integer statusCode) {
        if (statusCode == null) {
            return ResumeStatusEnum.PENDING;
        }

        ResumeStatusEnum status = java.util.Arrays.stream(ResumeStatusEnum.values())
                .filter(candidate -> candidate.getCode() == statusCode)
                .findFirst()
                .orElse(null);
        if (!isReviewVisible(status)) {
            throw new CsaException(ApiErrorCode.BAD_REQUEST, "Unsupported resume review status");
        }
        return status;
    }

    private boolean isReviewVisible(ResumeStatusEnum status) {
        return status == ResumeStatusEnum.PENDING
                || status == ResumeStatusEnum.APPROVED
                || status == ResumeStatusEnum.REJECTED;
    }

    private Map<Long, User> loadUsers(Collection<Long> userIds) {
        Set<Long> ids = userIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }

        return userMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private Map<Long, Dept> loadDepartments(Collection<User> users) {
        Set<Long> departmentIds = users.stream()
                .map(User::getDepartmentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (departmentIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return deptMapper.selectBatchIds(departmentIds).stream()
                .collect(Collectors.toMap(Dept::getId, Function.identity()));
    }

    private ResumeReviewListVO toReviewListVO(
            Resume resume,
            User applicant,
            Map<Long, Dept> departments) {
        ResumeReviewListVO vo = new ResumeReviewListVO();
        vo.setId(resume.getId());
        vo.setApplicantId(resume.getUserId());
        if (applicant != null) {
            vo.setUsername(applicant.getUsername());
            vo.setRealName(applicant.getRealName());
            vo.setAvatar(applicant.getAvatar());
            vo.setDepartmentId(applicant.getDepartmentId());
            Dept department = departments.get(applicant.getDepartmentId());
            vo.setDepartmentName(department == null ? null : department.getName());
        }
        vo.setStatus(resume.getStatus() == null ? null : resume.getStatus().getCode());
        vo.setContentSummary(buildContentSummary(resume.getContent()));
        vo.setGitRepoUrl(resume.getGitRepoUrl());
        vo.setGitSyncStatus(resume.getGitSyncStatus());
        vo.setCreateTime(resume.getCreateTime());
        vo.setUpdateTime(resume.getUpdateTime());
        vo.setAuditTime(resume.getAuditTime());
        return vo;
    }

    private ResumeReviewDetailVO toReviewDetailVO(
            Resume resume,
            User applicant,
            User auditor,
            Dept department) {
        ResumeReviewDetailVO vo = new ResumeReviewDetailVO();
        vo.setId(resume.getId());
        vo.setApplicantId(resume.getUserId());
        if (applicant != null) {
            vo.setUsername(applicant.getUsername());
            vo.setRealName(applicant.getRealName());
            vo.setAvatar(applicant.getAvatar());
            vo.setEmail(applicant.getEmail());
            vo.setStudentId(applicant.getStudentId());
            vo.setCollege(applicant.getCollege());
            vo.setClassName(applicant.getClassName());
            vo.setDepartmentId(applicant.getDepartmentId());
        }
        vo.setDepartmentName(department == null ? null : department.getName());
        vo.setContent(resume.getContent());
        vo.setGitRepoUrl(resume.getGitRepoUrl());
        vo.setGitSyncStatus(resume.getGitSyncStatus());
        vo.setGitSyncCompletedAt(resume.getGitSyncCompletedAt());
        vo.setGitSyncErrorCode(resume.getGitSyncErrorCode());
        vo.setGitSyncBranch(resume.getGitSyncBranch());
        vo.setGitSyncCommit(resume.getGitSyncCommit());
        vo.setGitSyncSizeBytes(resume.getGitSyncSizeBytes());
        vo.setStatus(resume.getStatus() == null ? null : resume.getStatus().getCode());
        vo.setRejectReason(resume.getRejectReason());
        vo.setAuditBy(resume.getAuditBy());
        vo.setAuditorName(auditor == null
                ? null
                : (StringUtils.hasText(auditor.getRealName()) ? auditor.getRealName() : auditor.getUsername()));
        vo.setAuditTime(resume.getAuditTime());
        vo.setCreateTime(resume.getCreateTime());
        vo.setUpdateTime(resume.getUpdateTime());
        return vo;
    }

    private String normalizeGitUrl(String gitUrl) {
        if (!StringUtils.hasText(gitUrl)) {
            return null;
        }

        String normalized = gitUrl.trim();
        if (normalized.length() > 255) {
            throw new CsaException(ApiErrorCode.BAD_REQUEST, "Git repository URL cannot exceed 255 characters");
        }

        try {
            URI uri = new URI(normalized);
            String scheme = uri.getScheme();
            if (uri.getHost() == null || !"https".equalsIgnoreCase(scheme)
                    || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null) {
                throw new CsaException(ApiErrorCode.BAD_REQUEST, "Git repository URL must use HTTPS");
            }
        } catch (URISyntaxException e) {
            throw new CsaException(ApiErrorCode.BAD_REQUEST, "Git repository URL is invalid");
        }
        return normalized;
    }

    private void resetGitSync(Resume resume) {
        resume.setGitSyncStatus("NOT_SYNCED");
        resume.setGitSyncRunId(null);
        resume.setGitSyncStartedAt(null);
        resume.setGitSyncCompletedAt(null);
        resume.setGitSyncErrorCode(null);
        resume.setGitSyncBranch(null);
        resume.setGitSyncCommit(null);
        resume.setGitSyncSizeBytes(null);
    }

    private boolean registerLockRelease(String lockKey, String lockToken) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return false;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                releaseLock(lockKey, lockToken);
            }
        });
        return true;
    }

    private void releaseLock(String lockKey, String lockToken) {
        try {
            keyValueStore.deleteIfValue(lockKey, lockToken);
        } catch (RuntimeException e) {
            log.warn("Failed to release resume lock {}", lockKey, e);
        }
    }
}

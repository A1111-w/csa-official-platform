package com.csa.official.modules.sys.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.csa.official.common.exception.CsaException;
import com.csa.official.modules.resume.entity.Resume;
import com.csa.official.modules.resume.mapper.ResumeMapper;
import com.csa.official.modules.sys.entity.AuditLog;
import com.csa.official.modules.sys.entity.StoredFile;
import com.csa.official.modules.sys.entity.User;
import com.csa.official.modules.sys.mapper.AuditLogMapper;
import com.csa.official.modules.sys.mapper.StoredFileMapper;
import com.csa.official.modules.sys.mapper.UserMapper;
import com.csa.official.modules.sys.vo.PersonalDataExportVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PersonalDataExportService {

    private final UserMapper userMapper;
    private final ResumeMapper resumeMapper;
    private final StoredFileMapper storedFileMapper;
    private final AuditLogMapper auditLogMapper;
    private final AuditService auditService;

    public PersonalDataExportService(UserMapper userMapper, ResumeMapper resumeMapper,
                                     StoredFileMapper storedFileMapper, AuditLogMapper auditLogMapper,
                                     AuditService auditService) {
        this.userMapper = userMapper;
        this.resumeMapper = resumeMapper;
        this.storedFileMapper = storedFileMapper;
        this.auditLogMapper = auditLogMapper;
        this.auditService = auditService;
    }

    @Transactional(rollbackFor = Exception.class)
    public PersonalDataExportVO exportFor(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new CsaException(404, "用户不存在");
        }

        Resume resume = resumeMapper.selectOne(new LambdaQueryWrapper<Resume>()
                .eq(Resume::getUserId, userId));
        List<StoredFile> files = storedFileMapper.selectList(new LambdaQueryWrapper<StoredFile>()
                .eq(StoredFile::getOwnerUserId, userId)
                .orderByDesc(StoredFile::getCreateTime));
        List<AuditLog> events = auditLogMapper.selectList(new LambdaQueryWrapper<AuditLog>()
                .eq(AuditLog::getActorUserId, userId)
                .orderByDesc(AuditLog::getCreateTime));

        PersonalDataExportVO result = new PersonalDataExportVO();
        result.setExportVersion("1");
        result.setGeneratedAt(LocalDateTime.now());
        result.setAccount(toAccountData(user));
        result.setResume(toResumeData(resume));
        result.setUploadedFiles(files.stream().map(this::toFileData).collect(Collectors.toList()));
        result.setSecurityEvents(events.stream().map(this::toSecurityEventData).collect(Collectors.toList()));

        auditService.recordBestEffort("DATA_EXPORT", "USER", String.valueOf(userId),
                "SUCCESS", null, Map.of(
                        "scope", "SELF",
                        "fileCount", result.getUploadedFiles().size(),
                        "eventCount", result.getSecurityEvents().size()));
        return result;
    }

    private PersonalDataExportVO.AccountData toAccountData(User user) {
        PersonalDataExportVO.AccountData data = new PersonalDataExportVO.AccountData();
        data.setId(user.getId());
        data.setUsername(user.getUsername());
        data.setRealName(user.getRealName());
        data.setEmail(user.getEmail());
        data.setPhone(user.getPhone());
        data.setContact(user.getContact());
        data.setWxOpenId(user.getWxOpenId());
        data.setAddress(user.getAddress());
        data.setAvatar(user.getAvatar());
        data.setGiteaUsername(user.getGiteaUsername());
        data.setRoleLevel(user.getRoleLevel());
        data.setPositionType(user.getPositionType());
        data.setDepartmentId(user.getDepartmentId());
        data.setBalance(user.getBalance());
        data.setStudentId(user.getStudentId());
        data.setCollege(user.getCollege());
        data.setClassName(user.getClassName());
        data.setMerchantNo(user.getMerchantNo());
        data.setUsedInviteCode(user.getUsedInviteCode());
        data.setAccountStatus(user.getAccountStatus());
        data.setPrivacyConsentVersion(user.getPrivacyConsentVersion());
        data.setPrivacyConsentAt(user.getPrivacyConsentAt());
        data.setPasswordChangedAt(user.getPasswordChangedAt());
        data.setLastLoginAt(user.getLastLoginAt());
        data.setDeactivatedAt(user.getDeactivatedAt());
        data.setDeletionRequestedAt(user.getDeletionRequestedAt());
        data.setCreateTime(user.getCreateTime());
        data.setUpdateTime(user.getUpdateTime());
        return data;
    }

    private PersonalDataExportVO.ResumeData toResumeData(Resume resume) {
        if (resume == null) {
            return null;
        }
        PersonalDataExportVO.ResumeData data = new PersonalDataExportVO.ResumeData();
        data.setId(resume.getId());
        data.setContent(resume.getContent());
        data.setGitRepoUrl(resume.getGitRepoUrl());
        data.setStatus(resume.getStatus() == null ? null : resume.getStatus().getCode());
        data.setRejectReason(resume.getRejectReason());
        data.setAuditTime(resume.getAuditTime());
        data.setCreateTime(resume.getCreateTime());
        data.setUpdateTime(resume.getUpdateTime());
        return data;
    }

    private PersonalDataExportVO.StoredFileData toFileData(StoredFile file) {
        PersonalDataExportVO.StoredFileData data = new PersonalDataExportVO.StoredFileData();
        data.setId(file.getId());
        data.setOriginalName(file.getOriginalName());
        data.setExtension(file.getExtension());
        data.setContentType(file.getContentType());
        data.setSizeBytes(file.getSizeBytes());
        data.setSha256(file.getSha256());
        data.setStorageProvider(file.getStorageProvider());
        data.setStatus(file.getStatus());
        data.setCreateTime(file.getCreateTime());
        data.setLastAccessTime(file.getLastAccessTime());
        data.setDeletedAt(file.getDeletedAt());
        return data;
    }

    private PersonalDataExportVO.SecurityEventData toSecurityEventData(AuditLog event) {
        PersonalDataExportVO.SecurityEventData data = new PersonalDataExportVO.SecurityEventData();
        data.setAction(event.getAction());
        data.setTargetType(event.getTargetType());
        data.setResult(event.getResult());
        data.setIpAddress(event.getIpAddress());
        data.setUserAgent(event.getUserAgent());
        data.setRequestId(event.getRequestId());
        data.setCreateTime(event.getCreateTime());
        return data;
    }
}

package com.csa.official.modules.sys.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Explicit allow-list for the data subject export. Never serialize User directly:
 * it contains password and other internal authentication fields.
 */
@Data
public class PersonalDataExportVO {

    private String exportVersion;
    private LocalDateTime generatedAt;
    private AccountData account;
    private ResumeData resume;
    private List<StoredFileData> uploadedFiles;
    private List<SecurityEventData> securityEvents;

    @Data
    public static class AccountData {
        private Long id;
        private String username;
        private String realName;
        private String email;
        private String phone;
        private String contact;
        private String wxOpenId;
        private String address;
        private String avatar;
        private String giteaUsername;
        private Integer roleLevel;
        private Integer positionType;
        private Long departmentId;
        private BigDecimal balance;
        private String studentId;
        private String college;
        private String className;
        private String merchantNo;
        private String usedInviteCode;
        private String accountStatus;
        private String privacyConsentVersion;
        private LocalDateTime privacyConsentAt;
        private LocalDateTime passwordChangedAt;
        private LocalDateTime lastLoginAt;
        private LocalDateTime deactivatedAt;
        private LocalDateTime deletionRequestedAt;
        private LocalDateTime createTime;
        private LocalDateTime updateTime;
    }

    @Data
    public static class ResumeData {
        private Long id;
        private String content;
        private String gitRepoUrl;
        private Integer status;
        private String rejectReason;
        private LocalDateTime auditTime;
        private LocalDateTime createTime;
        private LocalDateTime updateTime;
    }

    @Data
    public static class StoredFileData {
        private Long id;
        private String originalName;
        private String extension;
        private String contentType;
        private Long sizeBytes;
        private String sha256;
        private String storageProvider;
        private String status;
        private LocalDateTime createTime;
        private LocalDateTime lastAccessTime;
        private LocalDateTime deletedAt;
    }

    @Data
    public static class SecurityEventData {
        private String action;
        private String targetType;
        private String result;
        private String ipAddress;
        private String userAgent;
        private String requestId;
        private LocalDateTime createTime;
    }
}

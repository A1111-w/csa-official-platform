package com.csa.official.modules.sys.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public class PrivacyNoticeVO {

    private String policyVersion;
    private List<String> collectedFields;
    private Map<String, String> purposes;
    private Map<String, String> retention;
    private List<String> userRights;
    private String contactEmail;

    public static PrivacyNoticeVO current(String policyVersion, String contactEmail, int accountRetentionDays) {
        return new PrivacyNoticeVO(
                policyVersion,
                List.of(
                        "username", "realName", "email", "studentId", "college", "className",
                        "phone", "contact", "avatar", "resume", "uploadedFiles", "securityEvents"),
                Map.of(
                        "account", "Provide identity, membership and permission management",
                        "operations", "Provide competitions, resources, resumes and association workflows",
                        "security", "Prevent abuse, investigate incidents and protect accounts"),
                Map.of(
                        "activeAccount", "Retained while the account is active",
                        "deletionRequest", "Deletion requests are retained during the "
                                + accountRetentionDays
                                + "-day retention window; eligible accounts are then anonymized by a scheduled controlled job",
                        "audit", "Security and management audit records follow the documented retention policy",
                        "backups", "Expired backup copies are removed by the backup retention schedule"),
                List.of("access", "export", "correction", "sessionRevocation", "deactivation", "deletionRequest"),
                contactEmail);
    }
}

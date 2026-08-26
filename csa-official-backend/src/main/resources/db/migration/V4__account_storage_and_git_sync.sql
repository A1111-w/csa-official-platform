-- Phase 2 completion: account anonymization, atomic file quotas and resume Git sync.
-- V1/V2/V3 are immutable. No credentials or tokens are stored by this migration.

ALTER TABLE sys_user
    ADD COLUMN anonymized_at DATETIME NULL AFTER deletion_requested_at;

CREATE TABLE sys_file_usage (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    scope_type  VARCHAR(16)  NOT NULL,
    scope_id    BIGINT       NOT NULL,
    used_bytes  BIGINT       NOT NULL DEFAULT 0,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_file_usage_scope (scope_type, scope_id),
    KEY idx_file_usage_scope_type (scope_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='atomic upload quota counters';

INSERT INTO sys_file_usage (scope_type, scope_id, used_bytes)
SELECT 'USER', owner_user_id, COALESCE(SUM(size_bytes), 0)
FROM sys_stored_file
WHERE status = 'ACTIVE'
GROUP BY owner_user_id;

INSERT INTO sys_file_usage (scope_type, scope_id, used_bytes)
SELECT 'SCHOOL', 0, COALESCE(SUM(size_bytes), 0)
FROM sys_stored_file
WHERE status = 'ACTIVE';

ALTER TABLE biz_resume
    ADD COLUMN git_sync_status VARCHAR(24) NOT NULL DEFAULT 'NOT_SYNCED' AFTER git_repo_url,
    ADD COLUMN git_sync_run_id VARCHAR(64) NULL AFTER git_sync_status,
    ADD COLUMN git_sync_started_at DATETIME NULL AFTER git_sync_run_id,
    ADD COLUMN git_sync_completed_at DATETIME NULL AFTER git_sync_started_at,
    ADD COLUMN git_sync_error_code VARCHAR(64) NULL AFTER git_sync_completed_at,
    ADD COLUMN git_sync_branch VARCHAR(128) NULL AFTER git_sync_error_code,
    ADD COLUMN git_sync_commit CHAR(40) NULL AFTER git_sync_branch,
    ADD COLUMN git_sync_size_bytes BIGINT NULL AFTER git_sync_commit;

ALTER TABLE biz_resume
    ADD KEY idx_resume_git_sync (git_sync_status, git_sync_started_at);

ALTER TABLE sys_mail_delivery
    ADD KEY idx_mail_recovery (status, update_time, id);

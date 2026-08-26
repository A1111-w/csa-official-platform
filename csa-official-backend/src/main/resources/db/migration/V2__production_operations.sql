-- Phase 2 production operations. V1 remains immutable.

UPDATE sys_user
SET email = NULLIF(LOWER(TRIM(email)), '')
WHERE email IS NOT NULL;

UPDATE sys_user
SET student_id = NULLIF(TRIM(student_id), '')
WHERE student_id IS NOT NULL;

ALTER TABLE sys_user MODIFY COLUMN email VARCHAR(254) NULL COMMENT 'normalized email';
ALTER TABLE sys_user ADD COLUMN account_status VARCHAR(24) NOT NULL DEFAULT 'ACTIVE' AFTER email;
ALTER TABLE sys_user ADD COLUMN session_version BIGINT NOT NULL DEFAULT 0 AFTER account_status;
ALTER TABLE sys_user ADD COLUMN password_changed_at DATETIME NULL AFTER session_version;
ALTER TABLE sys_user ADD COLUMN last_login_at DATETIME NULL AFTER password_changed_at;
ALTER TABLE sys_user ADD COLUMN deactivated_at DATETIME NULL AFTER last_login_at;
ALTER TABLE sys_user ADD COLUMN deletion_requested_at DATETIME NULL AFTER deactivated_at;
ALTER TABLE sys_user ADD COLUMN privacy_consent_version VARCHAR(32) NULL AFTER deletion_requested_at;
ALTER TABLE sys_user ADD COLUMN privacy_consent_at DATETIME NULL AFTER privacy_consent_version;
ALTER TABLE sys_user DROP INDEX idx_user_email;
ALTER TABLE sys_user ADD UNIQUE KEY uk_user_email (email);
ALTER TABLE sys_user ADD UNIQUE KEY uk_user_student_id (student_id);
ALTER TABLE sys_user ADD KEY idx_user_account_status (account_status, deletion_requested_at);

CREATE TABLE sys_audit_log (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    actor_user_id  BIGINT           NULL,
    actor_username VARCHAR(64)      NULL,
    action         VARCHAR(64)  NOT NULL,
    target_type    VARCHAR(64)      NULL,
    target_id      VARCHAR(128)     NULL,
    result         VARCHAR(16)  NOT NULL DEFAULT 'SUCCESS',
    ip_address     VARCHAR(64)      NULL,
    user_agent     VARCHAR(500)     NULL,
    request_id     VARCHAR(128)     NULL,
    details_json   TEXT             NULL,
    create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_audit_actor_time (actor_user_id, create_time),
    KEY idx_audit_action_time (action, create_time),
    KEY idx_audit_target (target_type, target_id, create_time),
    KEY idx_audit_request_id (request_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='immutable management and security audit log';

CREATE TABLE sys_stored_file (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    owner_user_id    BIGINT       NOT NULL,
    storage_key      VARCHAR(500) NOT NULL,
    original_name    VARCHAR(255) NOT NULL,
    extension        VARCHAR(16)  NOT NULL,
    content_type     VARCHAR(128) NOT NULL,
    size_bytes       BIGINT       NOT NULL,
    sha256           CHAR(64)     NOT NULL,
    storage_provider VARCHAR(32)  NOT NULL DEFAULT 'LOCAL',
    status           VARCHAR(24)  NOT NULL DEFAULT 'ACTIVE',
    create_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_access_time DATETIME         NULL,
    deleted_at       DATETIME         NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_stored_file_key (storage_key),
    KEY idx_stored_file_owner_status (owner_user_id, status, create_time),
    KEY idx_stored_file_orphan (status, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='uploaded file metadata';

CREATE TABLE sys_mail_delivery (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    recipient_hash    CHAR(64)     NOT NULL,
    recipient_masked  VARCHAR(254) NOT NULL,
    message_type      VARCHAR(32)  NOT NULL,
    status            VARCHAR(16)  NOT NULL DEFAULT 'PENDING',
    attempt_count     INT          NOT NULL DEFAULT 0,
    last_error_code   VARCHAR(64)      NULL,
    last_error_message VARCHAR(500)    NULL,
    create_time       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    sent_time         DATETIME         NULL,
    PRIMARY KEY (id),
    KEY idx_mail_status_time (status, create_time),
    KEY idx_mail_recipient_time (recipient_hash, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='mail delivery status without message secrets';

CREATE TABLE sys_scheduled_job_execution (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    job_name        VARCHAR(128) NOT NULL,
    idempotency_key VARCHAR(191) NOT NULL,
    status          VARCHAR(16)  NOT NULL DEFAULT 'RUNNING',
    started_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_at     DATETIME         NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_job_idempotency (job_name, idempotency_key),
    KEY idx_job_started_at (job_name, started_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='database idempotency keys for scheduled jobs';

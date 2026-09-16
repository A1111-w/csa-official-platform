-- Resume review CAS version. V1-V5 are immutable.
ALTER TABLE biz_resume
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0 AFTER status;

ALTER TABLE biz_resume
    ADD KEY idx_resume_user_version (user_id, version);

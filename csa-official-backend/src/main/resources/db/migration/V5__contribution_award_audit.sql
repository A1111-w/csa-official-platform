-- Distinguish new automatic and manual contribution records without guessing the
-- origin of historical rows. LEGACY is intentionally retained for pre-V5 data.

ALTER TABLE sys_contribution_log
    ADD COLUMN source VARCHAR(16) NOT NULL DEFAULT 'LEGACY' AFTER detail,
    ADD COLUMN awarded_by BIGINT NULL AFTER source,
    ADD KEY idx_contribution_admin_history (source, create_time, id);

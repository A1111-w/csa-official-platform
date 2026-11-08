CREATE TABLE IF NOT EXISTS ai_evaluation (
    id VARCHAR(36) NOT NULL,
    owner_id BIGINT NOT NULL,
    report_json LONGTEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_ai_evaluation_owner (owner_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

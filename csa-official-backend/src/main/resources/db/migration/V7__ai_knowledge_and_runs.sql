CREATE TABLE ai_document (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    source VARCHAR(500) NOT NULL,
    min_role_level INT NOT NULL,
    chunk_count INT NOT NULL,
    owner_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_ai_document_role (min_role_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE ai_chunk (
    id VARCHAR(48) NOT NULL PRIMARY KEY,
    document_id VARCHAR(36) NOT NULL,
    chunk_index INT NOT NULL,
    content TEXT NOT NULL,
    embedding_json MEDIUMTEXT NULL,
    embedding_model VARCHAR(200) NULL,
    KEY idx_ai_chunk_document (document_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE ai_run (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    result_json MEDIUMTEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_ai_run_owner (owner_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE migration_drill_marker (
    id BIGINT NOT NULL PRIMARY KEY,
    note VARCHAR(64) NOT NULL
);

INSERT INTO migration_drill_marker (id, note) VALUES (1, 'v1-complete');

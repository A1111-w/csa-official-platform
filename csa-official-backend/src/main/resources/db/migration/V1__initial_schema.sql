-- CSA Official initial schema. Flyway creates the database schema history.
-- The database itself and its application user are provisioned outside Flyway.

CREATE TABLE sys_user (
    id                BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    username          VARCHAR(64)   NOT NULL COMMENT '登录用户名',
    password          VARCHAR(100)  NOT NULL COMMENT 'BCrypt 密码哈希',
    real_name         VARCHAR(64)       NULL COMMENT '真实姓名',
    email             VARCHAR(128)      NULL COMMENT '邮箱',
    role_level        INT           NOT NULL DEFAULT 0 COMMENT '权限等级',
    position_type     INT           NOT NULL DEFAULT 0 COMMENT '职位类型',
    department_id     BIGINT            NULL COMMENT '所属部门ID',
    balance           DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '账户余额',
    gitea_username    VARCHAR(64)       NULL COMMENT 'Gitea 用户名',
    avatar            VARCHAR(500)      NULL COMMENT '头像URL',
    phone             VARCHAR(32)       NULL COMMENT '手机号',
    wx_open_id        VARCHAR(64)       NULL COMMENT '微信 OpenID',
    contact           VARCHAR(128)      NULL COMMENT '联系方式',
    address           VARCHAR(255)      NULL COMMENT '地址',
    student_id        VARCHAR(32)       NULL COMMENT '学号',
    college           VARCHAR(64)       NULL COMMENT '学院',
    class_name        VARCHAR(64)       NULL COMMENT '班级',
    merchant_no       VARCHAR(64)       NULL COMMENT '支付单号',
    used_invite_code  VARCHAR(64)       NULL COMMENT '注册邀请码',
    create_time       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted           TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_username (username),
    KEY idx_user_email (email),
    KEY idx_user_department (department_id),
    KEY idx_user_role_level (role_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

CREATE TABLE sys_dept (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(64)  NOT NULL,
    intro       VARCHAR(500)     NULL,
    leader_id   BIGINT           NULL,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='部门表';

CREATE TABLE sys_carousel (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    img_url     VARCHAR(500) NOT NULL,
    target_url  VARCHAR(500)     NULL,
    title       VARCHAR(128)     NULL,
    sort_order  INT          NOT NULL DEFAULT 0,
    status      INT          NOT NULL DEFAULT 1,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_carousel_status_sort (status, sort_order, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='首页轮播图表';

CREATE TABLE sys_invite_code (
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    code          VARCHAR(64) NOT NULL,
    creator_id    BIGINT          NULL,
    max_usage     INT         NOT NULL DEFAULT 1,
    current_usage INT         NOT NULL DEFAULT 0,
    create_time   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted       TINYINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_invite_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='邀请码表';

CREATE TABLE sys_proposal (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    type              VARCHAR(32)  NOT NULL,
    title             VARCHAR(128) NOT NULL,
    reason            TEXT             NULL,
    proposer_id       BIGINT           NULL,
    status            INT          NOT NULL DEFAULT 0,
    expire_time       DATETIME         NULL,
    final_result_json VARCHAR(500)     NULL,
    create_time       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted           TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_proposal_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='投票提案表';

CREATE TABLE sys_resource (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    title          VARCHAR(128) NOT NULL,
    summary        VARCHAR(500)     NULL,
    file_url       VARCHAR(500)     NULL,
    category       VARCHAR(64)      NULL,
    uploader_id    BIGINT           NULL,
    download_count INT          NOT NULL DEFAULT 0,
    create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted        TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_resource_category_time (category, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='资源库表';

CREATE TABLE sys_config (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    config_key   VARCHAR(64)  NOT NULL,
    config_value TEXT             NULL,
    description  VARCHAR(255)     NULL,
    update_by    BIGINT           NULL,
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';

CREATE TABLE sys_vote_record (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    proposal_id BIGINT       NOT NULL,
    voter_id    BIGINT       NOT NULL,
    result      INT          NOT NULL,
    weight      INT          NOT NULL DEFAULT 1,
    comment     VARCHAR(500)     NULL,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_vote_proposal_voter (proposal_id, voter_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='投票记录表';

CREATE TABLE sys_contribution_log (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    user_id     BIGINT        NOT NULL,
    type        VARCHAR(16)   NOT NULL,
    score       DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    detail      VARCHAR(500)      NULL,
    create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_contrib_user_type (user_id, type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='贡献流水表';

CREATE TABLE biz_competition (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    title        VARCHAR(128) NOT NULL,
    content      TEXT             NULL,
    cover_img    VARCHAR(500)     NULL,
    start_time   DATETIME         NULL,
    end_time     DATETIME         NULL,
    publisher_id BIGINT           NULL,
    status       INT          NOT NULL DEFAULT 0,
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted      TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_comp_status_time (status, update_time, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='竞赛表';

CREATE TABLE biz_comp_editor (
    id             BIGINT NOT NULL AUTO_INCREMENT,
    competition_id BIGINT NOT NULL,
    user_id        BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_editor_comp_user (competition_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='竞赛授权编辑者表';

CREATE TABLE biz_resume (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    user_id       BIGINT       NOT NULL,
    content       TEXT             NULL,
    git_repo_url  VARCHAR(255)     NULL,
    status        INT          NOT NULL DEFAULT 0,
    reject_reason VARCHAR(500)     NULL,
    audit_by      BIGINT           NULL,
    audit_time    DATETIME         NULL,
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted       TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_resume_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='简历表';

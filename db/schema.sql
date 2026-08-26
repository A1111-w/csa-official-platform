-- =====================================================================
-- CSA Official —— 数据库结构脚本 (schema.sql)
-- =====================================================================
-- 生成来源：Flyway V1 + V2，并严格对照 MyBatis-Plus 实体类
-- (modules/**/entity/*.java)。生产环境只运行 Flyway；本文件是当前结构快照，
-- 供本地查阅和显式创建空库使用，不能代替版本化 migration。
--   - 表名取自 @TableName
--   - 列名为 Java 字段的 snake_case（application.yml 开启
--     map-underscore-to-camel-case: true）
--   - 所有 @TableId(type = IdType.AUTO) 均为 BIGINT AUTO_INCREMENT 主键
--
-- 约定说明：
--   1. 仅声明了 @TableLogic private Integer deleted 的实体才有 deleted 列。
--      sys_config / sys_contribution_log / sys_vote_record / biz_comp_editor
--      这四张表【没有】 deleted 列。deleted 统一 NOT NULL DEFAULT 0。
--      （application.yml: global-config.db-config.logic-delete-field: deleted）
--   2. create_time / update_time 严格对应实体上的 @TableField(fill=...)：
--        INSERT        -> 仅 create_time
--        INSERT_UPDATE -> 该列存在（update_time 通常成对，但需逐表核对）
--      例如 sys_contribution_log 只有 create_time；sys_config 只有 update_time；
--      biz_comp_editor 两者都没有。审计时间由 MyBatis-Plus MetaObjectHandler
--      自动填充，这里同时给出 DB 层 DEFAULT 作为手工 SQL 的兜底，二者不冲突。
--   3. 枚举列的存储类型：
--      CompetitionStatusEnum / ResumeStatusEnum / VoteResultEnum 三个枚举都在
--      int code 字段上标注了 @EnumValue（不是 IEnum，但效果相同：MyBatis-Plus
--      落库时存 @EnumValue 标注的值）。因此这些列用 INT 存整数 code，而不是
--      VARCHAR 存枚举名。反例：ContributionLog.type 是【纯 String】，直接存
--      枚举名字符串（'DEV'/'RES'/'COMP'/'OPS'），所以它用 VARCHAR。
--   4. BigDecimal -> DECIMAL(10,2)：ContributionLog.score、User.balance。
--
-- 关于外键：本项目使用逻辑删除 + MyBatis-Plus，不做数据库物理外键。物理 FK
-- 会与逻辑删除（父行 deleted=1 但物理仍在）以及框架的级联行为相互打架，因此
-- 表间关系仅通过下方注释描述，不建立 FOREIGN KEY 约束。详见 docs/database.md。
--
-- 初始化：mysql -u root -p < db/schema.sql
-- =====================================================================

CREATE DATABASE IF NOT EXISTS csa_db
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE csa_db;

-- =====================================================================
-- sys_user —— 用户（sys 模块）
-- 关系：department_id 逻辑指向 sys_dept.id；used_invite_code 逻辑指向
--       sys_invite_code.code（均不建物理外键）
-- =====================================================================
CREATE TABLE IF NOT EXISTS sys_user (
    id                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    username          VARCHAR(64)  NOT NULL               COMMENT '登录用户名',
    password          VARCHAR(100) NOT NULL               COMMENT 'BCrypt 密码哈希',
    real_name         VARCHAR(64)      NULL               COMMENT '真实姓名',
    email             VARCHAR(254)     NULL               COMMENT '规范化邮箱（小写、去首尾空格）',
    account_status    VARCHAR(24)  NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/DISABLED/DELETION_PENDING/ANONYMIZED',
    session_version   BIGINT       NOT NULL DEFAULT 0     COMMENT '全会话吊销版本',
    password_changed_at DATETIME       NULL               COMMENT '最近修改密码时间',
    last_login_at     DATETIME         NULL               COMMENT '最近成功登录时间',
    deactivated_at    DATETIME         NULL               COMMENT '账号停用时间',
    deletion_requested_at DATETIME     NULL               COMMENT '账号删除申请时间',
    privacy_consent_version VARCHAR(32) NULL               COMMENT '隐私同意版本',
    privacy_consent_at DATETIME         NULL               COMMENT '隐私同意时间',
    role_level        INT          NOT NULL DEFAULT 0     COMMENT '权限等级 0路人/1会员/2核心成员/3部长/4会长/99Root',
    position_type     INT          NOT NULL DEFAULT 0     COMMENT '职位类型 0无/1成员/2副职/3正职',
    department_id     BIGINT           NULL               COMMENT '所属部门ID（逻辑关联 sys_dept.id）',
    balance           DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '账户余额',
    gitea_username    VARCHAR(64)      NULL               COMMENT 'Gitea 用户名',
    avatar            VARCHAR(500)     NULL               COMMENT '头像URL',
    phone             VARCHAR(32)      NULL               COMMENT '手机号',
    wx_open_id        VARCHAR(64)      NULL               COMMENT '微信 OpenID',
    contact           VARCHAR(128)     NULL               COMMENT '联系方式',
    address           VARCHAR(255)     NULL               COMMENT '地址',
    student_id        VARCHAR(32)      NULL               COMMENT '学号',
    college           VARCHAR(64)      NULL               COMMENT '学院',
    class_name        VARCHAR(64)      NULL               COMMENT '班级',
    merchant_no       VARCHAR(64)      NULL               COMMENT '微信支付单号',
    used_invite_code  VARCHAR(64)      NULL               COMMENT '注册时使用的邀请码',
    create_time       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP                           COMMENT '创建时间',
    update_time       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted           TINYINT      NOT NULL DEFAULT 0     COMMENT '逻辑删除 0正常/1删除',
    PRIMARY KEY (id),
    -- 注册查重 & 登录：AuthController exists(username) / SecurityUtils selectOne(username)
    UNIQUE KEY uk_user_username (username),
    -- 邮箱和学号在规范化后全局唯一；MySQL 允许唯一索引中存在多个 NULL。
    UNIQUE KEY uk_user_email (email),
    UNIQUE KEY uk_user_student_id (student_id),
    -- 用户目录按部门筛选：SysUserController query.eq(departmentId)
    KEY idx_user_department (department_id),
    -- 贡献者名单/排序：/public/contributors 过滤 role_level>=2 且按 role_level DESC 排序
    KEY idx_user_role_level (role_level),
    KEY idx_user_account_status (account_status, deletion_requested_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- =====================================================================
-- sys_dept —— 部门（sys 模块）
-- 关系：leader_id 逻辑指向 sys_user.id（当前正部长）
-- 审计：create_time + update_time；有 deleted
-- =====================================================================
CREATE TABLE IF NOT EXISTS sys_dept (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    name        VARCHAR(64)  NOT NULL               COMMENT '部门名称',
    intro       VARCHAR(500)     NULL               COMMENT '部门简介',
    leader_id   BIGINT           NULL               COMMENT '正部长用户ID（逻辑关联 sys_user.id）',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP                           COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT      NOT NULL DEFAULT 0     COMMENT '逻辑删除 0正常/1删除',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='部门表';

-- =====================================================================
-- sys_carousel —— 首页轮播图（sys 模块）
-- 审计：create_time + update_time；有 deleted
-- =====================================================================
CREATE TABLE IF NOT EXISTS sys_carousel (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    img_url     VARCHAR(500) NOT NULL               COMMENT '图片URL',
    target_url  VARCHAR(500)     NULL               COMMENT '点击跳转URL',
    title       VARCHAR(128)     NULL               COMMENT '标题',
    sort_order  INT          NOT NULL DEFAULT 0     COMMENT '排序号（升序）',
    status      INT          NOT NULL DEFAULT 1     COMMENT '状态 1启用/0禁用',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP                           COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT      NOT NULL DEFAULT 0     COMMENT '逻辑删除 0正常/1删除',
    PRIMARY KEY (id),
    -- 公开轮播列表：CarouselController 过滤 status=1，按 sort_order ASC, create_time DESC
    KEY idx_carousel_status_sort (status, sort_order, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='首页轮播图表';

-- =====================================================================
-- sys_invite_code —— 邀请码（sys 模块）
-- 关系：creator_id 逻辑指向 sys_user.id
-- 审计：仅 create_time（无 update_time）；有 deleted
-- =====================================================================
CREATE TABLE IF NOT EXISTS sys_invite_code (
    id            BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    code          VARCHAR(64) NOT NULL               COMMENT '邀请码',
    creator_id    BIGINT          NULL               COMMENT '创建者用户ID（逻辑关联 sys_user.id）',
    max_usage     INT         NOT NULL DEFAULT 1     COMMENT '最大可用次数',
    current_usage INT         NOT NULL DEFAULT 0     COMMENT '已使用次数',
    create_time   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted       TINYINT     NOT NULL DEFAULT 0     COMMENT '逻辑删除 0正常/1删除',
    PRIMARY KEY (id),
    -- 注册时按邀请码核销：AuthController selectOne(code)，唯一保证不重复
    UNIQUE KEY uk_invite_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='邀请码表';

-- =====================================================================
-- sys_proposal —— 投票提案（sys 模块）
-- 关系：proposer_id 逻辑指向 sys_user.id
-- 审计：create_time + update_time；有 deleted
-- status 为普通 Integer（0投票中/1通过/2驳回），非 @EnumValue 枚举，用 INT
-- =====================================================================
CREATE TABLE IF NOT EXISTS sys_proposal (
    id                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    type              VARCHAR(32)  NOT NULL               COMMENT '提案类型 ROOT_APPLY/CODE_DEPLOY 等',
    title             VARCHAR(128) NOT NULL               COMMENT '标题',
    reason            TEXT             NULL               COMMENT '理由',
    proposer_id       BIGINT           NULL               COMMENT '发起人用户ID（逻辑关联 sys_user.id）',
    status            INT          NOT NULL DEFAULT 0     COMMENT '状态 0投票中/1通过/2驳回',
    expire_time       DATETIME         NULL               COMMENT '截止时间',
    final_result_json VARCHAR(500)     NULL               COMMENT '结果快照，如 agree:5, reject:1, threshold:3',
    create_time       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP                           COMMENT '创建时间',
    update_time       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted           TINYINT      NOT NULL DEFAULT 0     COMMENT '逻辑删除 0正常/1删除',
    PRIMARY KEY (id),
    -- 提案列表：VoteController 按 create_time DESC 排序
    KEY idx_proposal_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='投票提案表';

-- =====================================================================
-- sys_resource —— 资源库（sys 模块）
-- 关系：uploader_id 逻辑指向 sys_user.id
-- 审计：仅 create_time（无 update_time）；有 deleted
-- =====================================================================
CREATE TABLE IF NOT EXISTS sys_resource (
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    title          VARCHAR(128) NOT NULL               COMMENT '标题',
    summary        VARCHAR(500)     NULL               COMMENT '摘要',
    file_url       VARCHAR(500)     NULL               COMMENT '文件URL',
    category       VARCHAR(64)      NULL               COMMENT '分类',
    uploader_id    BIGINT           NULL               COMMENT '上传者用户ID（逻辑关联 sys_user.id）',
    download_count INT          NOT NULL DEFAULT 0     COMMENT '下载次数',
    create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted        TINYINT      NOT NULL DEFAULT 0     COMMENT '逻辑删除 0正常/1删除',
    PRIMARY KEY (id),
    -- 资源列表：ResourceController 按 category 过滤并按 create_time DESC 排序
    KEY idx_resource_category_time (category, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='资源库表';

-- =====================================================================
-- sys_config —— 系统配置（sys 模块）
-- 关系：update_by 逻辑指向 sys_user.id
-- 审计：仅 update_time（无 create_time）；【无】 deleted
-- =====================================================================
CREATE TABLE IF NOT EXISTS sys_config (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    config_key   VARCHAR(64)  NOT NULL               COMMENT '配置键，如 CSA_INTRO',
    config_value TEXT             NULL               COMMENT '配置值',
    description  VARCHAR(255)     NULL               COMMENT '说明',
    update_by    BIGINT           NULL               COMMENT '最后修改人用户ID（逻辑关联 sys_user.id）',
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    -- 按键取配置：PublicController / ContributionTask selectOne(config_key)，唯一保证一键一值
    UNIQUE KEY uk_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';

-- =====================================================================
-- sys_vote_record —— 投票记录（sys 模块）
-- 关系：proposal_id 逻辑指向 sys_proposal.id；voter_id 逻辑指向 sys_user.id
-- 审计：仅 create_time（无 update_time）；【无】 deleted
-- result 为 VoteResultEnum（0反对/1赞成），@EnumValue 标注 int code，用 INT
-- =====================================================================
CREATE TABLE IF NOT EXISTS sys_vote_record (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    proposal_id BIGINT       NOT NULL               COMMENT '提案ID（逻辑关联 sys_proposal.id）',
    voter_id    BIGINT       NOT NULL               COMMENT '投票人用户ID（逻辑关联 sys_user.id）',
    result      INT          NOT NULL               COMMENT '投票结果 0反对/1赞成（VoteResultEnum @EnumValue code）',
    weight      INT          NOT NULL DEFAULT 1     COMMENT '票权重',
    comment     VARCHAR(500)     NULL               COMMENT '投票留言',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    -- 防重复投票：VoteService.vote 用 exists(proposalId, voterId) 判断“已投过”，
    -- 唯一约束把这条业务规则下沉到 DB 层，杜绝并发下的重复投票
    UNIQUE KEY uk_vote_proposal_voter (proposal_id, voter_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='投票记录表';

-- =====================================================================
-- sys_contribution_log —— 贡献流水（sys 模块）
-- 关系：user_id 逻辑指向 sys_user.id
-- 审计：仅 create_time（无 update_time）；【无】 deleted
-- type 是【纯 String】，存枚举名字符串 'DEV'/'RES'/'COMP'/'OPS'，用 VARCHAR
-- score 是 BigDecimal -> DECIMAL(10,2)
-- =====================================================================
CREATE TABLE IF NOT EXISTS sys_contribution_log (
    id          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id     BIGINT        NOT NULL               COMMENT '用户ID（逻辑关联 sys_user.id）',
    type        VARCHAR(16)   NOT NULL               COMMENT '贡献类型 DEV/RES/COMP/OPS（存枚举名字符串）',
    score       DECIMAL(10,2) NOT NULL DEFAULT 0.00  COMMENT '分值（DEV 按分，其余按条计数）',
    detail      VARCHAR(500)      NULL               COMMENT '明细描述',
    source      VARCHAR(16)   NOT NULL DEFAULT 'LEGACY' COMMENT '来源 AUTO/MANUAL/LEGACY',
    awarded_by  BIGINT            NULL               COMMENT '自动触发人或人工补录操作人',
    create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    -- 贡献墙聚合：ContributionLogMapper.selectWall 按 user_id 分组并对 type 做条件聚合，
    -- (user_id, type) 覆盖 GROUP BY + CASE WHEN type=... 过滤
    KEY idx_contrib_user_type (user_id, type),
    KEY idx_contribution_admin_history (source, create_time, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='贡献流水表';

-- =====================================================================
-- biz_competition —— 竞赛（biz 模块）
-- 关系：publisher_id 逻辑指向 sys_user.id
-- 审计：create_time + update_time；有 deleted
-- status 为 CompetitionStatusEnum（0未发布/1进行中/2已结束），@EnumValue int code，用 INT
-- =====================================================================
CREATE TABLE IF NOT EXISTS biz_competition (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    title        VARCHAR(128) NOT NULL               COMMENT '比赛标题',
    content      TEXT             NULL               COMMENT '比赛详情',
    cover_img    VARCHAR(500)     NULL               COMMENT '封面图URL',
    start_time   DATETIME         NULL               COMMENT '开始时间',
    end_time     DATETIME         NULL               COMMENT '结束时间',
    publisher_id BIGINT           NULL               COMMENT '发布者用户ID（逻辑关联 sys_user.id）',
    status       INT          NOT NULL DEFAULT 0     COMMENT '状态 0未发布/1进行中/2已结束（CompetitionStatusEnum @EnumValue code）',
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP                           COMMENT '创建时间',
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted      TINYINT      NOT NULL DEFAULT 0     COMMENT '逻辑删除 0正常/1删除',
    PRIMARY KEY (id),
    -- 公开竞赛列表：CompetitionService.getPublicCompetitionPage 过滤 status<>0(UNPUBLISHED)，
    -- 按 update_time DESC, create_time DESC 排序
    KEY idx_comp_status_time (status, update_time, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='竞赛表';

-- =====================================================================
-- biz_comp_editor —— 竞赛授权编辑者（biz 模块）
-- 关系：competition_id 逻辑指向 biz_competition.id；user_id 逻辑指向 sys_user.id
-- 审计：【无】 create_time / update_time；【无】 deleted（实体最精简）
-- =====================================================================
CREATE TABLE IF NOT EXISTS biz_comp_editor (
    id             BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    competition_id BIGINT NOT NULL               COMMENT '竞赛ID（逻辑关联 biz_competition.id）',
    user_id        BIGINT NOT NULL               COMMENT '编辑者用户ID（逻辑关联 sys_user.id）',
    PRIMARY KEY (id),
    -- 授权查重：CompetitionService.addEditor / CsaSecurityService 用 exists(competition_id, user_id)，
    -- 唯一约束把“同一竞赛不重复授权同一人”下沉到 DB 层
    UNIQUE KEY uk_editor_comp_user (competition_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='竞赛授权编辑者表';

-- =====================================================================
-- biz_resume —— 简历（resume 模块）
-- 关系：user_id 逻辑指向 sys_user.id（投递人）；audit_by 逻辑指向 sys_user.id（审核人）
-- 审计：create_time + update_time；有 deleted
-- status 为 ResumeStatusEnum（0草稿/1待审核/2已通过/3已驳回），@EnumValue int code，用 INT
-- =====================================================================
CREATE TABLE IF NOT EXISTS biz_resume (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id       BIGINT       NOT NULL               COMMENT '投递人用户ID（逻辑关联 sys_user.id）',
    content       TEXT             NULL               COMMENT '简历内容（Markdown/自我介绍）',
    git_repo_url  VARCHAR(255)     NULL               COMMENT '硬核模式 Git 仓库地址',
    status        INT          NOT NULL DEFAULT 0     COMMENT '状态 0草稿/1待审核/2已通过/3已驳回（ResumeStatusEnum @EnumValue code）',
    reject_reason VARCHAR(500)     NULL               COMMENT '驳回原因',
    audit_by      BIGINT           NULL               COMMENT '审核人用户ID（逻辑关联 sys_user.id）',
    audit_time    DATETIME         NULL               COMMENT '审核时间',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP                           COMMENT '创建时间',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted       TINYINT      NOT NULL DEFAULT 0     COMMENT '逻辑删除 0正常/1删除',
    PRIMARY KEY (id),
    -- 我的简历：ResumeService.getMyResume 用 selectOne(user_id)。一人一份，唯一保证
    -- selectOne 不会因多行抛 TooManyResultsException
    UNIQUE KEY uk_resume_user (user_id),
    -- 部长审核队列按 status 过滤并按 update_time 倒序分页
    KEY idx_resume_status_update (status, update_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='简历表';

-- =====================================================================
-- Phase 2 production operations
-- =====================================================================
CREATE TABLE IF NOT EXISTS sys_audit_log (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='不可变管理与安全审计日志';

CREATE TABLE IF NOT EXISTS sys_stored_file (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='上传文件元数据';

CREATE TABLE IF NOT EXISTS sys_mail_delivery (
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    recipient_hash     CHAR(64)     NOT NULL,
    recipient_masked   VARCHAR(254) NOT NULL,
    message_type       VARCHAR(32)  NOT NULL,
    status             VARCHAR(16)  NOT NULL DEFAULT 'PENDING',
    attempt_count      INT          NOT NULL DEFAULT 0,
    last_error_code    VARCHAR(64)      NULL,
    last_error_message VARCHAR(500)     NULL,
    create_time        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    sent_time          DATETIME         NULL,
    PRIMARY KEY (id),
    KEY idx_mail_status_time (status, create_time),
    KEY idx_mail_recipient_time (recipient_hash, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='不含邮件秘密的发送状态';

CREATE TABLE IF NOT EXISTS sys_scheduled_job_execution (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    job_name        VARCHAR(128) NOT NULL,
    idempotency_key VARCHAR(191) NOT NULL,
    status          VARCHAR(16)  NOT NULL DEFAULT 'RUNNING',
    started_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_at     DATETIME         NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_job_idempotency (job_name, idempotency_key),
    KEY idx_job_started_at (job_name, started_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='定时任务数据库幂等键';

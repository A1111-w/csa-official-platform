-- =====================================================================
-- CSA Official —— 演示种子数据 (seed.sql)
-- =====================================================================
-- 前置：先让后端 Flyway 完成 V1/V2；本文件不负责建库或生产迁移。
--   运行时由 dev/test profile 的 DevSeedDataInitializer 加载，
--   只有显式启用 DEMO_SEED_ENABLED 并注入 DEMO_SEED_PASSWORD 才会执行。
--
-- 幂等：所有 INSERT 均带显式主键 + ON DUPLICATE KEY UPDATE，重复执行安全，
--       不会因主键/唯一键冲突报错。
--
-- 仅允许 dev/test profile 的 DevSeedDataInitializer 执行本文件。
-- __DEMO_PASSWORD_HASH__ 会在内存中替换为 DEMO_SEED_PASSWORD 的 BCrypt 哈希；
-- 仓库、日志和测试均不保存演示口令或固定哈希。生产 profile 无法加载该初始化器。
-- =====================================================================

-- =====================================================================
-- 1. 部门 sys_dept
-- =====================================================================
INSERT INTO sys_dept (id, name, intro, leader_id, create_time, update_time, deleted) VALUES
  (1, '技术部', '负责官网、资源平台与竞赛系统的开发运维', 3, '2026-01-01 09:00:00', '2026-01-01 09:00:00', 0),
  (2, '宣传部', '负责公众号、海报设计与活动宣传',       NULL, '2026-01-01 09:00:00', '2026-01-01 09:00:00', 0),
  (3, '外联部', '负责企业合作、赞助与校外资源对接',     NULL, '2026-01-01 09:00:00', '2026-01-01 09:00:00', 0)
ON DUPLICATE KEY UPDATE name = VALUES(name), intro = VALUES(intro), leader_id = VALUES(leader_id);

-- =====================================================================
-- 2. 用户 sys_user —— 覆盖全部 role_level：99/4/3/2/1/0
-- =====================================================================
INSERT INTO sys_user
  (id, username, password, real_name, email, role_level, position_type, department_id, balance, create_time, update_time, deleted)
VALUES
  (1, 'root',      '__DEMO_PASSWORD_HASH__', '超级管理员', 'root@csa.dev',      99, 0, NULL, 0.00,  '2026-01-01 09:00:00', '2026-01-01 09:00:00', 0),
  (2, 'president', '__DEMO_PASSWORD_HASH__', '张会长',    'president@csa.dev',  4, 3, NULL, 0.00,  '2026-01-01 09:00:00', '2026-01-01 09:00:00', 0),
  (3, 'minister',  '__DEMO_PASSWORD_HASH__', '李部长',    'minister@csa.dev',   3, 3, 1,    50.00, '2026-01-01 09:00:00', '2026-01-01 09:00:00', 0),
  (4, 'core',      '__DEMO_PASSWORD_HASH__', '王核心',    'core@csa.dev',       2, 1, 1,    20.00, '2026-01-01 09:00:00', '2026-01-01 09:00:00', 0),
  (5, 'member',    '__DEMO_PASSWORD_HASH__', '赵会员',    'member@csa.dev',     1, 0, 2,    0.00,  '2026-01-01 09:00:00', '2026-01-01 09:00:00', 0),
  (6, 'guest',     '__DEMO_PASSWORD_HASH__', '陈路人',    'guest@csa.dev',      0, 0, NULL, 0.00,  '2026-01-01 09:00:00', '2026-01-01 09:00:00', 0)
ON DUPLICATE KEY UPDATE
  password = VALUES(password), real_name = VALUES(real_name), email = VALUES(email),
  role_level = VALUES(role_level), position_type = VALUES(position_type),
  department_id = VALUES(department_id), balance = VALUES(balance);

-- =====================================================================
-- 3. 系统配置 sys_config —— CSA_INTRO（/api/public/about 读取此键）
-- =====================================================================
INSERT INTO sys_config (id, config_key, config_value, description, update_by, update_time) VALUES
  (1, 'CSA_INTRO',
      '<p>计算机学生协会（CSA）是一个面向全体在校学生的技术社团，聚焦编程实践、竞赛培训与项目开发。我们提供资源共享、竞赛组织、简历投递与内部投票治理等能力。</p>',
      '协会介绍', 1, '2026-01-01 09:00:00')
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), description = VALUES(description), update_by = VALUES(update_by);

-- =====================================================================
-- 4. 邀请码 sys_invite_code
-- =====================================================================
INSERT INTO sys_invite_code (id, code, creator_id, max_usage, current_usage, create_time, deleted) VALUES
  (1, 'CSA2026', 1, 100, 1, '2026-01-01 09:00:00', 0)
ON DUPLICATE KEY UPDATE max_usage = VALUES(max_usage), current_usage = VALUES(current_usage);

-- =====================================================================
-- 5. 轮播图 sys_carousel
-- =====================================================================
INSERT INTO sys_carousel (id, img_url, target_url, title, sort_order, status, create_time, update_time, deleted) VALUES
  (1, 'https://picsum.photos/seed/csa1/1200/400', '/competitions', '2026 校内算法赛报名开启', 1, 1, '2026-01-01 09:00:00', '2026-01-01 09:00:00', 0),
  (2, 'https://picsum.photos/seed/csa2/1200/400', '/resources',    '资源库上新：期末复习合集', 2, 1, '2026-01-01 09:00:00', '2026-01-01 09:00:00', 0)
ON DUPLICATE KEY UPDATE title = VALUES(title), sort_order = VALUES(sort_order), status = VALUES(status);

-- =====================================================================
-- 6. 资源库 sys_resource —— 跨多个分类
-- =====================================================================
INSERT INTO sys_resource (id, title, summary, file_url, category, uploader_id, download_count, create_time, deleted) VALUES
  (1, '数据结构复习提纲', '涵盖线性表、树、图与常见考点', 'https://files.csa.dev/ds-outline.pdf',  '课件', 3, 128, '2026-02-01 10:00:00', 0),
  (2, '操作系统笔记',     '进程、内存、文件系统整理',     'https://files.csa.dev/os-notes.pdf',    '课件', 4,  76, '2026-02-05 10:00:00', 0),
  (3, '期末算法真题',     '近三年期末算法真题与题解',     'https://files.csa.dev/algo-exam.zip',   '真题', 3,  95, '2026-02-10 10:00:00', 0),
  (4, 'IDEA 配置指南',    'Java 开发环境一键配置',         'https://files.csa.dev/idea-setup.pdf',  '工具', 4,  40, '2026-02-15 10:00:00', 0),
  (5, '蓝桥杯备赛资料',   '真题分类 + 模板代码',           'https://files.csa.dev/lanqiao.zip',     '竞赛', 2,  60, '2026-02-20 10:00:00', 0)
ON DUPLICATE KEY UPDATE title = VALUES(title), summary = VALUES(summary), category = VALUES(category), download_count = VALUES(download_count);

-- =====================================================================
-- 7. 竞赛 biz_competition —— 三种状态 0未发布 / 1进行中 / 2已结束
-- =====================================================================
INSERT INTO biz_competition (id, title, content, cover_img, start_time, end_time, publisher_id, status, create_time, update_time, deleted) VALUES
  (1, '2026 校内算法程序设计赛', '<p>面向全校的算法竞赛，个人赛，ACM 赛制。</p>', 'https://picsum.photos/seed/comp1/800/400', '2026-05-01 09:00:00', '2026-05-01 14:00:00', 2, 1, '2026-04-01 10:00:00', '2026-04-20 10:00:00', 0),
  (2, '2025 秋季程序设计赛',     '<p>上一届已顺利结束，感谢参与。</p>',           'https://picsum.photos/seed/comp2/800/400', '2025-10-01 09:00:00', '2025-10-01 14:00:00', 3, 2, '2025-09-01 10:00:00', '2025-10-02 10:00:00', 0),
  (3, '新生编程挑战赛（草稿）',   '<p>筹备中，尚未发布。</p>',                     NULL,                                       NULL,                  NULL,                  2, 0, '2026-06-01 10:00:00', '2026-06-01 10:00:00', 0)
ON DUPLICATE KEY UPDATE title = VALUES(title), content = VALUES(content), status = VALUES(status), publisher_id = VALUES(publisher_id);

-- =====================================================================
-- 8. 竞赛授权编辑者 biz_comp_editor —— (competition_id, user_id) 唯一
-- =====================================================================
INSERT INTO biz_comp_editor (id, competition_id, user_id) VALUES
  (1, 1, 3),
  (2, 1, 4)
ON DUPLICATE KEY UPDATE competition_id = VALUES(competition_id), user_id = VALUES(user_id);

-- =====================================================================
-- 9. 贡献流水 sys_contribution_log —— 让贡献墙非空
--    type 存枚举名字符串：DEV(按分) / RES/COMP/OPS(按条计数)
-- =====================================================================
INSERT INTO sys_contribution_log (id, user_id, type, score, detail, create_time) VALUES
  (1, 3, 'DEV',  30.00, '官网首页与登录模块开发',   '2026-03-01 10:00:00'),
  (2, 3, 'COMP',  1.00, '发布 2026 校内算法赛',      '2026-04-01 10:00:00'),
  (3, 3, 'OPS',   1.00, '更新首页轮播与公告',        '2026-04-05 10:00:00'),
  (4, 4, 'DEV',  20.00, '资源库模块开发',            '2026-03-10 10:00:00'),
  (5, 4, 'RES',   1.00, '上传数据结构复习提纲',      '2026-02-01 10:00:00'),
  (6, 4, 'RES',   1.00, '上传操作系统笔记',          '2026-02-05 10:00:00'),
  (7, 2, 'COMP',  1.00, '发布 2025 秋季程序设计赛',  '2025-09-01 10:00:00')
ON DUPLICATE KEY UPDATE user_id = VALUES(user_id), type = VALUES(type), score = VALUES(score), detail = VALUES(detail);

-- =====================================================================
-- 10. 投票提案 sys_proposal —— status 0投票中 / 1通过 / 2驳回
-- =====================================================================
INSERT INTO sys_proposal (id, type, title, reason, proposer_id, status, expire_time, final_result_json, create_time, update_time, deleted) VALUES
  (1, 'CODE_DEPLOY', '部署新版官网到生产环境', '新版已完成测试，申请上线',   3, 0, '2026-12-31 23:59:59', NULL,                                    '2026-06-10 10:00:00', '2026-06-10 10:00:00', 0),
  (2, 'CODE_DEPLOY', '上线资源下载统计功能',   '需要统计各资源下载量',       4, 1, '2026-06-05 23:59:59', 'agree:3, reject:0, threshold:2',       '2026-06-01 10:00:00', '2026-06-04 10:00:00', 0)
ON DUPLICATE KEY UPDATE title = VALUES(title), reason = VALUES(reason), status = VALUES(status), final_result_json = VALUES(final_result_json);

-- =====================================================================
-- 11. 投票记录 sys_vote_record —— (proposal_id, voter_id) 唯一
--     result: 0反对 / 1赞成（VoteResultEnum code）；weight 会长2/部长1
--     对应提案2（已通过），提案发起人 user 4 不参与投票
-- =====================================================================
INSERT INTO sys_vote_record (id, proposal_id, voter_id, result, weight, comment, create_time) VALUES
  (1, 2, 2, 1, 2, '同意上线', '2026-06-02 10:00:00'),
  (2, 2, 3, 1, 1, '支持',     '2026-06-03 10:00:00')
ON DUPLICATE KEY UPDATE result = VALUES(result), weight = VALUES(weight), comment = VALUES(comment);

-- =====================================================================
-- 12. 简历 biz_resume —— user_id 唯一（一人一份）
--     status: 0草稿 / 1待审核 / 2已通过 / 3已驳回（ResumeStatusEnum code）
-- =====================================================================
INSERT INTO biz_resume (id, user_id, content, git_repo_url, status, reject_reason, audit_by, audit_time, create_time, update_time, deleted) VALUES
  (1, 4, '## 王核心\n后端方向，熟悉 Spring Boot 与 MySQL。', 'https://github.com/example/csa-demo', 1, NULL, NULL, NULL, '2026-06-08 10:00:00', '2026-06-08 10:00:00', 0)
ON DUPLICATE KEY UPDATE content = VALUES(content), git_repo_url = VALUES(git_repo_url), status = VALUES(status);

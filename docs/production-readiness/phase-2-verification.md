# Phase 2 验证记录

记录时间：2026-07-30（Asia/Shanghai）
最后按当前工作区复核：2026-08-26（Asia/Shanghai）

本记录对应当前未提交工作区，不把历史 Phase 1 镜像结果当作当前源码结果。所有命令使用隔离数据和运行时临时值，记录中不包含密码、Token、Cookie 或邮件凭据。

## 1. 实际执行结果

| 检查 | 结果 | 证据 |
| --- | --- | --- |
| `mvnw.cmd test` | 通过 | 2026-08-26：133 tests，0 failures，0 errors，1 skipped |
| 显式 Testcontainers | 待健康 Docker/staging 重跑 | 当前测试目标为 MySQL 8.0.36、Redis 7.2、Flyway V1→V3；本机 Docker 不可用 |
| `npm run lint` | 通过 | ESLint exit code 0 |
| `npm run test` | 通过 | Vitest 1 file、3 tests |
| `npm run build` | 通过 | Next.js 16.2.12；21 个静态页面单元；123 秒 |
| 开发 Compose config | 通过 | 配置可解析 |
| 生产 Compose 缺变量 | 通过 | 缺少必填变量时按预期失败 |
| 生产 Compose 静态展开 | 通过 | 进程内随机值；只有 Caddy 发布端口；data 网络 internal |
| 当前 Docker 镜像构建 | 环境阻断 | Docker 数据盘 I/O 错误，EXT4 journal aborted，dockerd SIGBUS |
| 当前 Playwright | 未完成 | 依赖最新可运行栈；未拿旧镜像冒充通过 |
| 当前备份恢复复演 | 未完成 | Docker 数据盘故障后停止，保护已有卷 |
| Trivy / GitHub Actions 远端运行 | 未执行 | 工作流已建立，本地未伪造 CI 成功状态 |

默认后端全测跳过的 1 项就是 Testcontainers。历史记录曾在 V2 版本链上通过；当前测试已增加 V3 和审核索引断言，必须在健康 Docker/staging 重跑，不能把旧结果当作当前源码证据。

## 2. 账号与会话

已由单元/控制器测试覆盖：

- 邮箱 lowercase + trim、学号 trim，V2 建立各自唯一索引。
- 修改密码、重置密码和吊销全部会话递增 `session_version`。
- JWT 缺少 `sessionVersion` 的旧格式不再被接受，避免绕过全会话吊销。
- 认证格式错误按未认证处理；数据库、Redis 等鉴权依赖故障返回 HTTP 500 和稳定错误外壳，不伪装为 401。
- 停用和删除申请会立即递增会话版本、清缓存并退出当前会话。

删除申请目前只进入 `DELETION_PENDING`。仓库没有自动最终匿名化/物理删除执行器，所以隐私文案已改成“进入保留窗口并由受控操作流程完成”，不能对外承诺系统会自动在 N 天内完成删除。

## 3. 数据、审计、上传、邮件与任务

- 个人数据导出使用 VO 白名单，不返回密码哈希、Token、验证码、`storageKey` 或完整审计详情。
- `AuditService` 拒绝密码、Token、Cookie、Authorization、验证码等敏感键，并覆盖登录安全、角色、导出、配置、删除、简历和竞赛授权事件。
- 上传校验扩展名、文件头、大小与配额，写入 `sys_stored_file` 元数据、SHA-256 和 provider；`FileStorage` 为未来对象存储保留边界。
- 邮件发送在独立执行器中有限重试，维护 `PENDING/SENDING/SENT/FAILED` 状态；最终失败清理验证码和限流 key。
- 定时任务同时使用 Redis 锁和数据库唯一幂等键，测试覆盖锁冲突、已完成跳过和数据库兜底。

## 4. 数据库影响

`V2__production_operations.sql`：

- 扩展 `sys_user`：`account_status`、`session_version`、密码/登录/停用/删除时间、隐私同意版本与时间。
- 规范化邮箱/学号，建立 `uk_user_email` 和 `uk_user_student_id`。
- 新增 `sys_audit_log`、`sys_stored_file`、`sys_mail_delivery`、`sys_scheduled_job_execution`。

真实 MySQL 历史测试已验证 V1 旧用户升级到 V2 后数据保留、规范化和默认状态；当前 V3 只增加审核队列索引。生产迁移前仍必须先查重，迁移后不得修改已执行的 V2/V3 SQL。

## 5. 新增或重要环境变量

生产必填：`MYSQL_ROOT_PASSWORD`、`MYSQL_APP_PASSWORD`、`REDIS_PASSWORD`、`JWT_SECRET`、`MAIL_USERNAME`、`MAIL_PASSWORD`、`PUBLIC_ORIGIN`、`SITE_ADDRESS`。

运营配置：`DB_MAX_POOL_SIZE`、`DB_MIN_IDLE`、`PRIVACY_POLICY_VERSION`、`PRIVACY_CONTACT_EMAIL`、`ACCOUNT_DELETION_RETENTION_DAYS`、`MAIL_MAX_ATTEMPTS`、`UPLOAD_MAX_FILE_SIZE_BYTES`、`UPLOAD_USER_QUOTA_BYTES`、`UPLOAD_SCHOOL_QUOTA_BYTES`、`UPLOAD_ORPHAN_GRACE_HOURS`、`UPLOAD_CLEANUP_BATCH_SIZE`、`SCHEDULED_JOB_LOCK_SECONDS`。

这些值只应由部署环境或秘密管理器注入。`NEXT_PUBLIC_*` 会进入浏览器 bundle，不能承载秘密。

## 6. 部署步骤

1. 修复/更换健康的 Docker 主机，在 staging 恢复一份脱敏数据库副本。
2. 运行 V1→V3 查重和迁移演练，确认 `flyway_schema_history` 到 3，并存在 `idx_resume_status_update`。
3. 执行后端全测、显式 Testcontainers、前端 lint/test/build。
4. 用受保护 env 文件执行生产 Compose config，确认只发布 Caddy 80/443。
5. 构建并扫描 backend/frontend 镜像，记录不可变 digest。
6. 启动 staging，执行 Playwright 登录、CSRF、权限、上传，以及备份恢复演练。
7. 通过 readiness、指标和日志检查后才切换流量；生产发布前再做一次备份。

## 7. 回滚

- 应用失败：切回上一组已验证镜像，保留 V2/V3 结构；V3 只是兼容索引，旧应用可忽略。
- V2 失败：停写、保留日志和 history，从备份恢复副本或发布新的向前修复迁移；不原地修改 V2。
- 上传回滚：保留上传卷和 `sys_stored_file`，旧应用必须能读取现有路径；对象存储切换需要双读/离线迁移。
- 邮件故障：停发送入口或降低重试，保留投递状态；不得通过删除状态表无限重发。
- 定时任务故障：停止调度并保留幂等记录，用新 key 或补偿任务恢复。

## 8. 剩余风险与发布门槛

1. 当前删除流程没有最终匿名化/删除执行器，需要管理员 Runbook、审批证据和可测试的完成动作后才能宣称完整删除能力。
2. 文件配额采用先汇总再写入，并发上传可能短暂超额；400 人规模可先监控，后续用数据库锁/原子配额计数修正。
3. 进程崩溃可能留下 `PENDING` 邮件，尚未实现数据库扫描补偿；不能无限重试验证码。
4. 本机 Docker VHD 发生文件系统 I/O 故障；最新镜像、Playwright、备份恢复和 Trivy 结果仍是发布阻断项。
5. Windows 上前端构建耗时明显波动；CI 应以 Linux runner 的稳定耗时和缓存命中为准。
6. Phase 3 仅有 SaaS ADR 和迁移方案，没有实施 tenant 隔离；当前仍是单学校系统。

Phase 3 设计见 `docs/adr/ADR-003-multi-school-saas.md`，本轮不实现支付或大规模多租户改造。

# Phase 2 验证记录

记录时间：2026-07-30（Asia/Shanghai）
最后按当前工作区复核：2026-08-26（Asia/Shanghai）

本记录对应当前未提交工作区，不把历史 Phase 1 镜像结果当作当前源码结果。所有命令使用隔离数据和运行时临时值，记录中不包含密码、Token、Cookie 或邮件凭据。

## 1. 实际执行结果

| 检查 | 结果 | 证据 |
| --- | --- | --- |
| `mvnw.cmd test` | 通过 | 2026-08-26：174 tests，0 failures，0 errors，1 skipped |
| 显式 Testcontainers | GitHub Actions 通过；本机待健康 Docker/staging 重跑 | 2026-08-26 的远端工作流 `#33006154587` 通过真实 MySQL、Redis 与 Flyway V1→V5；本机 Docker 仍不可用 |
| `npm run lint` | 通过 | ESLint exit code 0 |
| `npm run test` | 通过 | Vitest 6 files、12 tests |
| `npm ci` + `npm audit` | 通过 | 干净安装完成；完整与 production audit 均为 0 vulnerabilities |
| `npm run build` | 通过 | Next.js 16.3.3；页面生成数据 25/25 |
| 开发 Compose config | 通过 | 配置可解析 |
| 生产 Compose 缺变量 | 通过 | 缺少必填变量时按预期失败 |
| 生产 Compose 静态展开 | 通过 | 进程内随机值；只有 Caddy 发布端口；data 网络 internal |
| 当前 Docker 镜像构建 | 本机环境阻断；GitHub Actions 构建通过 | Docker 数据盘 I/O 错误，EXT4 journal aborted，dockerd SIGBUS；远端已完成前后端镜像构建 |
| 当前 Playwright | 本机环境阻断；GitHub Actions 通过 | Chromium 151 已安装；本机等待 Next.js dev server 120 秒超时；远端关键 E2E 已使用一次性 MySQL/Redis 和运行时 seed 验证 |
| 当前备份恢复复演 | 未完成 | Docker 数据盘故障后停止，保护已有卷 |
| Trivy / GitHub Actions 远端运行 | 首轮已定位失败；本提交待确认 | 首轮六个业务/配置/E2E job 通过；`dependency-review-action` 因私有仓库未启用 GHAS 失败，后端镜像 Trivy 检出可修复漏洞。当前提交移除不适用 Action 并升级相关依赖与 Alpine 包 |

默认后端全测跳过的 1 项就是 Testcontainers。历史记录曾在 V2 版本链上通过；当前测试已更新到 V5，并断言 V3 审核索引、V4 `sys_file_usage` 和 V5 贡献来源列，必须在健康 Docker/staging 重跑，不能把旧结果当作当前源码证据。

CI Redis 根因已复核：GitHub Actions 日志中 `test` profile 实际已经激活；空 `REDIS_HOST` 时，Redis 自动配置仍尝试创建连接工厂并失败。本 checkpoint 只在测试 profile 排除 Redis 自动配置，并在应用上下文测试中断言没有 `RedisConnectionFactory`。首轮远端工作流已通过后端单测、Testcontainers、前端检查、Compose 与 Playwright E2E；它还确认私有仓库未启用 GHAS 时不能使用 `dependency-review-action`，并从 Trivy 报告提取了有修复版本的镜像依赖。当前修复将 Spring Boot 提升至 3.5.14，并以 patch-level 覆盖 Spring Framework、Spring Data、Tomcat、Micrometer、Netty、Jackson、Commons IO，同时在 runner stage 执行 Alpine 包升级。没有新增 Flyway migration、生产配置或数据库影响。

Playwright 当前共有 5 个用例。缺少 `E2E_USERNAME/E2E_PASSWORD` 时，3 个认证用例会按设计跳过，但公开隐私页和未登录跳转仍应运行。本轮在测试开始前就因 Next.js dev server 120 秒未就绪而终止，所以不能把“理论上会跳过”写成“匿名用例已通过”。

## 2. 账号与会话

已由单元/控制器测试覆盖：

- 邮箱 lowercase + trim、学号 trim，V2 建立各自唯一索引。
- 修改密码、重置密码和吊销全部会话递增 `session_version`。
- JWT 缺少 `sessionVersion` 的旧格式不再被接受，避免绕过全会话吊销。
- 认证格式错误按未认证处理；数据库、Redis 等鉴权依赖故障返回 HTTP 500 和稳定错误外壳，不伪装为 401。
- 停用和删除申请会立即递增会话版本、清缓存并退出当前会话。

删除申请先进入 `DELETION_PENDING` 和保留期；`AccountAnonymizationTask` 到期后通过数据库幂等任务和批量事务自动匿名化符合条件的账号、简历和审计操作人。该能力不等于立即物理删除，备份副本仍按备份保留策略到期处理。

## 3. 数据、审计、上传、邮件与任务

- 个人数据导出使用 VO 白名单，不返回密码哈希、Token、验证码、`storageKey` 或完整审计详情。
- `AuditService` 拒绝密码、Token、Cookie、Authorization、验证码等敏感键，并覆盖登录安全、角色、导出、配置、删除、简历和竞赛授权事件。
- 上传校验扩展名、文件头和大小；V4 `sys_file_usage` 通过条件更新原子预占/释放个人与全站配额，再写入 `sys_stored_file` 元数据、SHA-256 和 provider；`FileStorage` 为未来对象存储保留边界。
- 邮件发送在独立执行器中有限重试，维护 `PENDING/SENDING/SENT/FAILED` 状态；`MailRecoveryTask` 扫描并认领超时的 `PENDING`/`SENDING` 记录继续有限重试，最终失败清理验证码和限流 key。
- 定时任务同时使用 Redis 锁和数据库唯一幂等键，测试覆盖锁冲突、已完成跳过和数据库兜底。

## 4. 数据库影响

`V2__production_operations.sql`：

- 扩展 `sys_user`：`account_status`、`session_version`、密码/登录/停用/删除时间、隐私同意版本与时间。
- 规范化邮箱/学号，建立 `uk_user_email` 和 `uk_user_student_id`。
- 新增 `sys_audit_log`、`sys_stored_file`、`sys_mail_delivery`、`sys_scheduled_job_execution`。

真实 MySQL 历史测试已验证 V1 旧用户升级到 V2 后数据保留、规范化和默认状态。当前版本链还包含 V3 审核队列索引、V4 账号匿名化/原子配额/邮件恢复/Git 同步结构和 V5 贡献来源/操作人字段；生产迁移前仍必须先查重，迁移后不得修改已执行的 V2-V5 SQL。

## 5. 新增或重要环境变量

生产必填：`MYSQL_ROOT_PASSWORD`、`MYSQL_APP_PASSWORD`、`REDIS_PASSWORD`、`JWT_SECRET`、`MAIL_USERNAME`、`MAIL_PASSWORD`、`PUBLIC_ORIGIN`、`SITE_ADDRESS`。

运营配置：`DB_MAX_POOL_SIZE`、`DB_MIN_IDLE`、`PRIVACY_POLICY_VERSION`、`PRIVACY_CONTACT_EMAIL`、`ACCOUNT_DELETION_RETENTION_DAYS`、`MAIL_MAX_ATTEMPTS`、`UPLOAD_MAX_FILE_SIZE_BYTES`、`UPLOAD_USER_QUOTA_BYTES`、`UPLOAD_SCHOOL_QUOTA_BYTES`、`UPLOAD_ORPHAN_GRACE_HOURS`、`UPLOAD_CLEANUP_BATCH_SIZE`、`SCHEDULED_JOB_LOCK_SECONDS`。

这些值只应由部署环境或秘密管理器注入。`NEXT_PUBLIC_*` 会进入浏览器 bundle，不能承载秘密。

## 6. 部署步骤

1. 修复/更换健康的 Docker 主机，在 staging 恢复一份脱敏数据库副本。
2. 运行 V1→V5 查重和迁移演练，确认 `flyway_schema_history` 到 5，并存在 `idx_resume_status_update`、`sys_file_usage`、`source` 和 `awarded_by`。
3. 执行后端全测、显式 Testcontainers、前端 lint/test/build。
4. 用受保护 env 文件执行生产 Compose config，确认只发布 Caddy 80/443。
5. 构建并扫描 backend/frontend 镜像，记录不可变 digest。
6. 启动 staging，执行 Playwright 登录、CSRF、权限、上传，以及备份恢复演练。
7. 通过 readiness、指标和日志检查后才切换流量；生产发布前再做一次备份。

## 7. 回滚

- 应用失败：切回上一组已验证镜像，保留 V2-V5 结构；旧应用通常可忽略新增表、列和索引，回滚前仍要核对其读写契约。
- V2 失败：停写、保留日志和 history，从备份恢复副本或发布新的向前修复迁移；不原地修改 V2。
- 上传回滚：保留上传卷和 `sys_stored_file`，旧应用必须能读取现有路径；对象存储切换需要双读/离线迁移。
- 邮件故障：停发送入口或降低重试，保留投递状态；不得通过删除状态表无限重发。
- 定时任务故障：停止调度并保留幂等记录，用新 key 或补偿任务恢复。
- CI checkpoint 回退：使用 Git revert 回退该提交，随后执行前端 `npm ci`；不需要执行数据库回退。

## 8. 剩余风险与发布门槛

1. 到期匿名化已有代码和单元测试，但当前 Docker/staging 尚未执行真实定时任务、失败恢复和备份保留演练；当前不做不可恢复的物理删除，运营审批/豁免流程仍需明确。
2. 原子上传配额依赖 V4 `sys_file_usage` 的初始回填和后续释放一致性；真实 MySQL 并发上传、孤儿清理和备份恢复仍需在 staging 验证并监控计数漂移。
3. 邮件补偿依赖 Redis 中有 TTL 的短期恢复载荷；停机超过 TTL、验证码过期或载荷不匹配会把投递标记失败，需要用户重新申请验证码，不能无限重试。
4. 本机 Docker VHD 发生文件系统 I/O 故障，当前源码的本地镜像、备份恢复和 Trivy 复验仍被阻断。首轮 GitHub Actions 已通过镜像构建和关键 Playwright E2E，但安全扫描修复后的新远端运行尚未完成；只有该运行全绿后，才可解除 CI 相关发布阻断项。
5. Windows 上前端构建耗时明显波动；CI 应以 Linux runner 的稳定耗时和缓存命中为准。
6. Phase 3 仅有 SaaS ADR 和迁移方案，没有实施 tenant 隔离；当前仍是单学校系统。

Phase 3 设计见 `docs/adr/ADR-003-multi-school-saas.md`，本轮不实现支付或大规模多租户改造。

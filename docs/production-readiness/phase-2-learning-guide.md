# Phase 2 运营能力学习与演练

这份文档对应 Phase 2 的代码、迁移、测试和 CI。目标不是记住几个类名，而是能证明账号生命周期、个人数据、审计、上传、邮件、定时任务和交付流水线在故障下仍然有边界。每次演练都记录：假设、输入、观察、结论、回滚。

## 1. 变更地图

| 能力 | 主要实现 | 数据库影响 | 回滚边界 |
| --- | --- | --- | --- |
| 账号生命周期 | `AccountService`、`AccountController`、`session_version` | V2 扩展 `sys_user` | 应用可回滚；V2 结构只能向前兼容 |
| 个人数据导出 | `PersonalDataExportService`、`PersonalDataExportVO`、`GET /api/account/export` | 读取简历、文件元数据、审计摘要 | 关闭入口或回滚前端，不删除用户数据 |
| 管理审计 | `AuditService`、`sys_audit_log` | 新增不可变审计表 | 审计写入失败按事件类型决定 best-effort；不记录秘密 |
| 上传元数据与配额 | `FileService`、`StoredFile`、`FileStorage`、清理任务 | 新增 `sys_stored_file` | 保留上传卷，应用回滚前确保旧版本能读当前路径 |
| 邮件状态与重试 | `MailService`、`AsyncMailSender`、`sys_mail_delivery` | 新增投递状态表 | 可停用发送或调低重试；验证码失败后允许重新申请 |
| 定时任务幂等 | `ScheduledJobService`、`sys_scheduled_job_execution` | 新增幂等键表 | 停止任务并保留执行记录，不重复补发 |
| 真实依赖测试 | Testcontainers MySQL/Redis/Flyway、Playwright | 隔离测试数据库 | 测试容器结束即清理，不触碰生产卷 |

## 2. 账号生命周期

### 学习目标

理解“改密码、吊销会话、停用、删除申请”不是前端按钮，而是带事务、缓存失效、JWT 吊销和审计的后端状态迁移。`session_version` 是全会话吊销的权威版本；Redis 中的单 Token 吊销只是补充。

### 观察点

- `V2__production_operations.sql` 为 `sys_user` 增加 `account_status`、`session_version`、密码和删除生命周期时间列。
- 邮箱和学号规范化后分别由 `uk_user_email`、`uk_user_student_id` 约束；MySQL 的唯一索引允许多个 `NULL`，所以“未填写”不等于重复。
- `AccountService` 的写操作有 `@Transactional`，成功后清理用户缓存并写审计。
- 修改密码和重置密码会递增 `session_version`，旧 JWT 即使尚未过期也不能继续使用。
- `JwtAuthenticationFilter` 要求 JWT 显式携带 `sessionVersion`；旧格式 Token 不会退化为版本 0，否则“吊销全部会话”可被绕过。

### 演练

```powershell
cd D:\CSA-Project\csa-official-backend
.\mvnw.cmd test -Dtest=AccountServiceTest,AccountControllerTest,AuthControllerTest
```

使用隔离账号完成：登录、改密、用旧 Cookie 请求、吊销全部会话、停用账号。每一步记录 HTTP status、`errorCode`、`X-Request-ID`，不要把 Cookie 或 Token 写进记录。

### 回滚

业务发布失败时切回上一份应用镜像，但保留已经落地的 V2 字段。不要直接删除 `session_version` 或把停用账号手工改回 ACTIVE；需要按审计记录和数据库备份走受控恢复。

### 删除能力边界

`POST /api/account/deletion-request` 先把账号迁移为 `DELETION_PENDING`、吊销会话并写审计。保留期结束后，`AccountAnonymizationTask` 通过 `ScheduledJobService` 的每日幂等键执行批量匿名化：替换账号标识和密码、清理个人字段、匿名化简历与审计操作人，并清除账号缓存。

该流程是自动匿名化，不是立即物理删除。备份副本按独立保留策略到期处理；生产上线前仍要演练任务失败恢复、运营审批/豁免和备份恢复，不能把“提交申请”描述成“数据已经立即删除”。

## 3. 隐私说明与个人数据导出

`GET /api/account/export` 只允许当前登录用户访问，返回显式白名单：账号资料、简历、上传文件元数据和安全事件摘要。它不返回密码哈希、JWT、CSRF Token、验证码、文件 `storageKey` 或审计 `detailsJson`。

### 演练

1. 用普通成员登录并请求 `/api/account/export`，确认 HTTP 200 和 `R<T>` 外壳。
2. 检查 JSON 字段白名单，特别是 `password`、`token`、`secret`、`storageKey` 不存在。
3. 用无 Cookie、错误 CSRF 或其它用户身份请求，确认分别被 401/403 拦截。
4. 确认导出事件只记录数量和 `scope=SELF`，不记录导出内容。

前端入口位于 `dashboard/settings`。浏览器只生成下载文件，不把导出内容写进 localStorage 或 URL。

### 回滚

若导出契约有问题，先关闭前端入口或限制接口权限，再修复 VO 白名单。不要为了回滚而删除审计和用户数据；修复后的响应应保持向后兼容，字段扩展优先于字段重命名。

## 4. 管理审计与秘密排除

审计覆盖登录成功/失败、注册、密码事件、角色变更、配置修改、资源删除、简历审核、竞赛授权、数据导出、文件访问和限流事件。`AuditService` 递归拒绝 `password`、`token`、`secret`、`cookie`、`authorization`、验证码等键名，并截断 User-Agent 和错误文本。

### 演练

```powershell
rg -n "password|token|secret|cookie|authorization|verificationcode" csa-official-backend/src/main/java/com/csa/official/modules/sys/service/AuditService.java
.\mvnw.cmd test -Dtest=AuditServiceTest,AuditLogControllerTest,PersonalDataExportServiceTest
```

故意向审计 details 传入敏感键，测试必须失败；正常事件必须带当前 trace/request ID。生产日志只能记录 action、target、result、数量和 trace，不输出凭据值。

### 回滚

审计表是追加型数据。若审计写入拖慢主流程，按事件重要性切换 `recordBestEffort` 或临时停用非关键事件，但保留登录安全、权限变化和删除事件；不要清空审计表来“修复性能”。

## 5. 上传元数据、配额与对象存储边界

上传先校验大小、扩展名和文件头，再写入 `FileStorage`，成功后写 `sys_stored_file` 元数据和 SHA-256。V4 新增 `sys_file_usage`，`FileAccountingService` 通过带配额条件的单条 `UPDATE` 原子预占个人和全站额度；元数据写入失败时事务回滚计数，删除文件时原子释放。当前实现是本地卷 `LocalFileStorage`，接口已经隔离了未来 S3-compatible provider。文件路径按用户分区，下载还会校验资源归属或发布状态。

关键配置：

```text
UPLOAD_MAX_FILE_SIZE_BYTES
UPLOAD_USER_QUOTA_BYTES
UPLOAD_SCHOOL_QUOTA_BYTES
UPLOAD_ORPHAN_GRACE_HOURS
UPLOAD_CLEANUP_BATCH_SIZE
```

### 演练

1. 上传合法签名 PNG，核对 `sys_stored_file.size_bytes`、`sha256`、`storage_provider`。
2. 用错误扩展名、错误文件头、超大文件和超过配额的文件测试 400/413。
3. 上传后让元数据与业务引用脱钩，等待超过 grace period，手动触发 `StoredFileCleanupTask`，确认物理文件删除后状态变为 deleted。
4. 运行备份恢复演练，比较上传文件 SHA-256 和数据库元数据。

### 回滚

应用回滚前必须确认旧版本能读取 `/files/{userId}/{uuid}` 路径和现有卷。元数据插入失败时实现会删除已写入的物理文件；若清理失败，保留 orphan 状态，交给定时任务处理。对象存储切换采用双读/双写或离线迁移，不能直接改 provider 字符串。

## 6. 邮件有限重试

验证码先写 Redis 和 `sys_mail_delivery`，再由独立 `mailTaskExecutor` 异步发送。失败最多重试 `MAIL_MAX_ATTEMPTS` 次，最终把状态设为 `FAILED` 并删除验证码和限流 key；收件人只保存哈希和脱敏地址，错误信息会截断并脱敏。

### 演练

运行 `AsyncMailSenderTest`，让 mock SMTP 前两次失败、第三次成功，再测试全部失败场景。观察 `PENDING -> SENDING -> SENT/FAILED` 和 `attempt_count`，确认 HTTP 请求线程没有同步等待 SMTP。

`MailRecoveryTask` 每分钟扫描超过阈值的 `PENDING` / `SENDING` 记录，先用数据库条件更新认领，再校验 Redis 中短期保存的收件人、验证码哈希和 key，最后从已有尝试次数继续有限重试。恢复载荷缺失、被替换或验证码过期时会标记失败，不会无限重发。

## 7. 定时任务幂等与分布式锁

`ScheduledJobService` 采用 Redis 锁减少并发执行，再用 `sys_scheduled_job_execution(job_name, idempotency_key)` 唯一键做数据库最终兜底。贡献结算以配置更新时间为幂等键，孤儿文件清理以日期为幂等键。

### 演练

1. 两个实例同时执行同一个 job/key，只有一个实例进入 work。
2. 重复执行已 SUCCESS 的 key，必须跳过。
3. 模拟 RUNNING 超时，确认下一次执行可以接管并保留失败/重试记录。
4. 删除 Redis 锁后再次触发，数据库唯一键仍不能造成重复业务写入。

### 回滚

任务逻辑出错时先停调度入口，保留 `sys_scheduled_job_execution` 记录；修复后使用新的 idempotency key 或明确的补偿任务。不要直接删除幂等表记录后重跑。

## 8. Testcontainers、Playwright 与 CI

本地快速检查：

```powershell
cd D:\CSA-Project\csa-official-backend
.\mvnw.cmd test
.\mvnw.cmd "-Dit.containers=true" "-Dtest=FlywayMySqlRedisIntegrationTest" test

cd D:\CSA-Project\csa-official-frontend
npm run lint
npm run test
npm run build
$env:E2E_USERNAME="member"
$env:E2E_PASSWORD="<runtime-only-password>"
$env:E2E_BASE_URL="https://localhost:8443"
$env:E2E_API_BASE_URL="https://localhost:8443"
$env:E2E_IGNORE_HTTPS_ERRORS="true" # 仅本地自签名证书演练
npm run test:e2e
```

真实 E2E 必须使用一次性 MySQL/Redis、运行时 seed 密码和隔离上传目录。Playwright 登录测试要先等待 `/api/auth/login` 的 200 响应，再断言 Dashboard URL；冷启动编译耗时不能和登录请求共用一个过短的断言窗口。

`.github/workflows/ci.yml` 的顺序是后端单测、前端 lint/build/test、Testcontainers、Compose fail-fast、Docker 构建与 Trivy、关键 Playwright E2E。任何失败都应保留脱敏日志和 trace，不能上传真实环境文件。

### 2026-08-26 实际结果

- 后端默认全测：2026-08-26 为 174 tests，0 failures，0 errors，1 skipped。
- 显式 Testcontainers：当前测试目标为 MySQL 8.0.36、Redis 7.2 和 Flyway V1→V5；GitHub Actions `#33010814757` 已在 `fcddab2` 上通过该真实依赖测试。本机 Docker 不可用时仍必须标记为待重跑，不能把远端 CI 结果冒充本机备份恢复或 staging 部署演练。
- 前端：干净 `npm ci` 后 Vitest **6 files、12 tests**、lint（0 errors、1 既有 warning）、Next.js 16.3.3 build 全部通过；build 页面生成数据为 **25/25**，完整与 production `npm audit` 为 0 vulnerabilities。
- CI Redis 修复：不要把“test profile 未激活”当作结论。实际日志已显示 profile 为 `test`；空 `REDIS_HOST` 下失败是测试上下文仍自动创建 Redis 连接工厂。测试 profile 排除 Redis 自动配置，并用 `CsaOfficialApplicationTests` 的 Bean 断言防止回归；生产 Redis 配置不受影响。
- Compose：缺必填变量按预期失败；临时值展开通过，只有 Caddy 发布端口。
- 当前 Docker 镜像重建时宿主 Docker VHD 出现 I/O/EXT4 journal 故障；本机 Playwright 等待 Next.js dev server 120 秒超时，备份恢复和本地镜像扫描仍不能完成。远端工作流 `#33009147368` 暴露了前端镜像两类问题：最终镜像全局 npm 自带的 `node-tar` 漏洞，以及可通过 `apk upgrade` 修复的 Alpine 包。应用 `node_modules` 的 production audit 为 0，不应把基础镜像问题误写成应用 lockfile 问题。修复在 runner stage 执行 Alpine 升级，移除构建后不需要的 npm/npx，并以 `node` 直接启动 Next.js；后续 `#33010814757` 已在 `fcddab2` 上全绿，包含前后端镜像 Trivy、真实 MySQL/Redis/Flyway 和关键 Playwright E2E。备份恢复仍需在健康 staging 演练。证据和恢复门槛见 [`phase-2-verification.md`](phase-2-verification.md)。

## 9. Phase 2 验收与迁移回滚清单

- [x] GitHub Actions `#33010814757` 已验证当前 V1→V5、`idx_resume_status_update`、`sys_file_usage` 和 V5 贡献来源列。
- [ ] 健康 staging 仍需使用脱敏数据库复演已有库升级、失败回滚与备份恢复；CI 通过不替代该部署演练。
- [x] 账号邮箱/学号约束、改密、会话吊销、停用和删除申请有测试。
- [x] 导出 JSON 是白名单，负向断言没有密码、Token 和 storage key。
- [x] 审计覆盖关键动作，敏感键会被剔除，日志可按 request ID 关联。
- [x] 上传元数据、原子配额预占/释放和孤儿清理有代码与单元测试。
- [ ] 当前源码的上传备份和恢复需要在健康 Docker/staging 环境复演。
- [x] 邮件异步、有限重试、失败状态和 `PENDING`/`SENDING` 崩溃补偿有单元测试。
- [x] 定时任务 Redis 锁 + 数据库幂等键有测试。
- [x] CI 依赖升级、干净 `npm ci`、完整/production `npm audit` 和本地前端 build 已复验；本 checkpoint 没有迁移或生产配置变化，回退使用 Git revert 后重新执行 `npm ci`。
- [x] GitHub Actions `#33010814757` 已完成当前源码的前后端镜像构建、Trivy、真实依赖与关键 Playwright E2E 验收；前端 runner/npm 移除与 Alpine 升级后的扫描结果为通过。
- [ ] 本机 Docker 仍不可用；上传文件和 MySQL 的备份恢复必须在健康 Docker/staging 做恢复演练，不能由 CI 代替。
- [x] 到期账号匿名化执行器、审计和单元测试已实现。
- [ ] 匿名化任务、运营审批/豁免和备份保留仍需在健康 staging 做恢复演练。

Phase 2 数据库回滚原则仍是“应用可回滚、数据库向前兼容”。如果 V2-V5 迁移已经落地，不原地删除列或改写已执行 SQL；先停写、备份、修复并发布新的向前迁移。完整操作入口见 [`runbook.md`](runbook.md)、[`flyway.md`](flyway.md) 和 [`backup-restore.md`](backup-restore.md)。

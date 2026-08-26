# Phase 1 验证记录

记录时间：2026-07-30（Asia/Shanghai）
最后按当前工作区复核：2026-08-26（Asia/Shanghai）

本记录区分两组证据：2026-07-29 完成的 Phase 1 隔离生产栈演练，以及 2026-08-26 对当前源码的重新验证。任何记录都不包含密码、Token、Cookie 或真实邮箱凭据。

## 1. 当前源码验证

- 后端 `mvnw.cmd test`（2026-08-26）：174 tests，0 failures，0 errors，1 skipped。跳过项是需要显式开关和 Docker 的 Testcontainers 用例。
- 显式 Testcontainers 用例：2026-08-26 未运行成功；当前机器 Docker 环境不可用，因此 V1-V5 的真实 MySQL、Redis、Flyway 验收仍待健康 staging 重跑。历史隔离栈曾在 V2 版本链上通过，不能当作当前 V5 证据。
- 真实依赖版本：MySQL 8.0.36、Redis 7.2-alpine；历史演练先把空库迁到 V1、插入旧版用户、再迁到 V2。当前源码还会执行 V3 审核队列索引、V4 账号匿名化/原子配额/邮件恢复/Git 同步结构和 V5 贡献审计结构，需在健康 Docker/staging 复验。
- 前端 `npm run test`：6 files、12 tests 通过；`npm run lint` 通过。
- 前端 `npm ci`：干净安装完成；`npm run test` 为 6 files、12 tests，`npm run lint` 为 0 errors、1 既有 warning，`npm run build`：Next.js 16.3.3 构建通过，页面生成数据 25/25；完整与 production `npm audit` 均为 0 vulnerabilities。
- CI Redis 回归：2026-08-26 在无 `.env`、空 `REDIS_HOST` 的等价 CI 配置下复验。`test` profile 原本已经激活，实际失败点是 Redis 自动配置仍创建连接工厂；`application-test.yml` 现只排除测试用 Redis 自动配置，`CsaOfficialApplicationTests` 断言没有 `RedisConnectionFactory`。生产 Redis 配置没有变化。
- Playwright：首次因缺少 Chromium 失败；安装 Playwright Chromium 151 后再次执行，等待配置的 Next.js dev server 120 秒超时，因此当前 E2E 仍未通过。该阻断与下述 Docker VHD 故障分别记录。
- 生产 Compose 缺少必填变量时 fail-fast；使用进程内临时随机值静默展开通过。静态断言确认只有 Caddy 发布宿主端口，`data` 网络为 internal。

## 2. 数据库演练

当前 Testcontainers 用例证明：

1. 空 MySQL 执行 `V1__initial_schema.sql` 后 `sys_user` 为空，生产迁移不创建共享密码账号。
2. V1 旧数据升级到 `V2__production_operations.sql` 后仍保留，邮箱被 trim + lowercase，学号被 trim。
3. 历史隔离演练中 `account_status=ACTIVE`、`session_version=0` 正确补齐，`flyway_schema_history` 当时最新成功版本为 2；当前源码版本链已到 V5，重跑后应为 5，并校验 V3 索引、V4 `sys_file_usage` 和 V5 贡献来源列。
4. 同一轮测试完成 Redis 写入和读取 round trip。

2026-07-29 的隔离栈还完成了已有库 baseline 和故意失败 migration 演练：MySQL 非事务 DDL 可能留下部分结构，恢复采用隔离库重建/备份恢复，没有把 `flyway repair` 当作数据回滚。

## 3. Phase 1 HTTPS E2E 证据

2026-07-29 使用 `csa-phase1-empty` 隔离 MySQL/Redis/backend/frontend/Caddy 栈完成：

- 首页通过同源 HTTPS 返回 200，并包含 HSTS。
- `/api/public/about` 返回 200，并包含 `X-Request-ID`。
- 错误登录返回 HTTP 401 和 `AUTHENTICATION_FAILED`。
- 未认证上传返回 401；有 Cookie 但缺 CSRF Header 返回 403 和 `CSRF_INVALID`。
- 合法 PNG 上传及 `/files` 下载完成 SHA-256 round trip。
- 普通会员调用高权限接口返回 403 和 `ACCESS_DENIED`。
- MySQL 和上传文件备份恢复后，checksum、数据值和 readiness 一致。

该栈镜像早于最后一轮 Phase 2/JWT 修改，因此只作为 Phase 1 历史证据，不能替代当前源码的镜像和 Playwright 验收。当前源码的 Phase 2 状态见 `phase-2-verification.md`。

本机 80 端口被 Windows HTTP.sys 占用，历史隔离验收将 Caddy HTTPS 映射到 `8443`；生产 Compose 仍发布 80/443。

## 4. 当前 Docker 环境阻断

2026-07-30 重新构建当前 backend/frontend 镜像时，Docker Desktop 数据盘发生宿主机 I/O 故障。日志显示 `/dev/sde` 写入错误、EXT4 journal aborted、只读重挂载和 dockerd SIGBUS；这不是 Maven、TypeScript 或应用测试失败。

为保护现有 Docker 卷，没有执行 factory reset、删除 VHD、`docker system prune --volumes` 或文件系统修复。当前源码镜像重建和当前备份恢复复演因此未完成；Playwright 则另外阻断在 Next.js dev server 120 秒未就绪。两条链路都不能标记为发布通过。Docker 数据盘应先由操作者备份并单独恢复，再在 staging 重跑本文件第 1-3 节。

## 5. 回滚

1. 应用发布失败：停止新容器，切回上一份已验证的 backend/frontend 镜像；保留数据库和上传卷。
2. Redis 序列化兼容故障：停止写流量，备份后清理受影响的应用缓存，再回滚镜像；Redis 不是数据库恢复源。
3. Flyway 失败：保存日志、history 和备份，在副本恢复或发布新的向前迁移；不要盲目 `repair`。
4. 数据或文件损坏：先制作当前快照，再按 `backup-restore.md` 在维护窗口恢复。
5. Docker 数据盘故障：先停止 Desktop 并复制 VHD/卷级备份，禁止在没有备份时 reset data 或格式化。
6. 本 CI checkpoint 回退：使用 Git revert 回退本提交并执行前端 `npm ci`；本轮没有 Flyway migration、数据库结构或生产环境变量变更。

## 6. 结论

Phase 1 代码、迁移、配置和历史隔离栈证据齐全；当前源码的单元测试、干净前端安装、Vitest/lint/build 和依赖审计通过。GitHub Actions 的远端结论需在此 checkpoint 推送后确认。V1-V5 真实 MySQL/Redis/Flyway、最新镜像和备份恢复仍受 Docker 数据盘故障阻断；Playwright 受 Next.js dev server 启动超时阻断。必须在健康的本机环境或 staging 分别重跑。

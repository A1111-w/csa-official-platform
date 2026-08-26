# Phase 1 验证记录

记录时间：2026-07-30（Asia/Shanghai）
最后按当前工作区复核：2026-08-26（Asia/Shanghai）

本记录区分两组证据：2026-07-29 完成的 Phase 1 隔离生产栈演练，以及 2026-07-30 对当前源码的重新验证。任何记录都不包含密码、Token、Cookie 或真实邮箱凭据。

## 1. 当前源码验证

- 后端 `mvnw.cmd test`（2026-08-26）：133 tests，0 failures，0 errors，1 skipped。跳过项是需要显式开关和 Docker 的 Testcontainers 用例。
- 显式 Testcontainers 用例：2026-08-26 未运行成功；当前机器 Docker 环境不可用，因此 V3/审核索引的真实 MySQL、Redis、Flyway 验收仍待健康 staging 重跑。历史隔离栈曾在 V2 版本链上通过，不能当作当前 V3 证据。
- 真实依赖版本：MySQL 8.0.36、Redis 7.2-alpine；历史演练先把空库迁到 V1、插入旧版用户、再迁到 V2。当前源码继续执行 V3 审核队列索引，需在健康 Docker/staging 复验。
- 前端 `npm run test`：3/3 通过；`npm run lint` 通过。
- 前端 `npm run build`：Next.js 16.2.12 构建通过，生成 21 个静态页面单元；本机耗时 123 秒。
- 生产 Compose 缺少必填变量时 fail-fast；使用进程内临时随机值静默展开通过。静态断言确认只有 Caddy 发布宿主端口，`data` 网络为 internal。

## 2. 数据库演练

当前 Testcontainers 用例证明：

1. 空 MySQL 执行 `V1__initial_schema.sql` 后 `sys_user` 为空，生产迁移不创建共享密码账号。
2. V1 旧数据升级到 `V2__production_operations.sql` 后仍保留，邮箱被 trim + lowercase，学号被 trim。
3. 历史隔离演练中 `account_status=ACTIVE`、`session_version=0` 正确补齐，`flyway_schema_history` 当时最新成功版本为 2；当前源码版本链已增加 V3，重跑后应为 3。
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

为保护现有 Docker 卷，没有执行 factory reset、删除 VHD、`docker system prune --volumes` 或文件系统修复。当前源码镜像重建、当前 Playwright 和当前备份恢复复演因此未完成，不能标记为发布通过。Docker 数据盘应先由操作者备份并单独恢复，再在 staging 重跑本文件第 1-3 节。

## 5. 回滚

1. 应用发布失败：停止新容器，切回上一份已验证的 backend/frontend 镜像；保留数据库和上传卷。
2. Redis 序列化兼容故障：停止写流量，备份后清理受影响的应用缓存，再回滚镜像；Redis 不是数据库恢复源。
3. Flyway 失败：保存日志、history 和备份，在副本恢复或发布新的向前迁移；不要盲目 `repair`。
4. 数据或文件损坏：先制作当前快照，再按 `backup-restore.md` 在维护窗口恢复。
5. Docker 数据盘故障：先停止 Desktop 并复制 VHD/卷级备份，禁止在没有备份时 reset data 或格式化。

## 6. 结论

Phase 1 代码、迁移、配置和历史隔离栈证据齐全；当前源码的单元测试通过。V3 真实 MySQL/Redis/Flyway、最新镜像、HTTPS E2E 和备份恢复仍因本机 Docker 数据盘故障未复验，必须在健康的 Docker/staging 环境重跑。

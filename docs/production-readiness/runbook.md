# CSA Official 生产部署 Runbook

## 适用范围

本 Runbook 面向单校、约 400 名学生的公网部署。架构仍是 Spring Boot + Next.js + MySQL + Redis 单体，Caddy 负责同源 HTTPS 入口：`/api` 和 `/files` 进入 backend，其余进入 frontend。

生产 backend 到 MySQL 使用 `sslMode=REQUIRED`，即使流量只走 Compose 内部网络也保持数据库连接加密；`allowPublicKeyRetrieval` 保持关闭。开发 Compose 才允许为了本地 MySQL 明文调试而使用 `sslMode=DISABLED`。

## 发布前检查

1. 准备 DNS、HTTPS 证书（Caddy 可自动申请）、备份存储和外部秘密管理。
2. 复制 `.env.production.example` 到部署机受保护目录，填入随机秘密；不要提交、粘贴到日志或写入 Compose 文件。
3. `docker compose -f compose.production.yml config` 在缺变量时必须失败；使用临时假值做静态展开检查。
4. 运行后端 `mvnw test`、前端 `npm run lint`、`npm run build` 和新增前端测试。
5. 在 staging 跑空库 Flyway、已有库 baseline、失败迁移恢复和备份恢复演练。

## 首次部署

```powershell
docker compose --env-file C:\secure\csa-production.env -f compose.production.yml config
docker compose --env-file C:\secure\csa-production.env -f compose.production.yml up -d --build
docker compose --env-file C:\secure\csa-production.env -f compose.production.yml ps
```

首次生产启动不会创建共享密码账号，也不会执行 `db/seed.sql`。管理员账号必须通过受控的一次性管理流程创建或导入已存在的合规密码哈希。

## 健康验收

从部署网络内部检查 liveness、readiness 和指标端点；公网只验证同源 API/页面，不发布 Actuator：

```powershell
docker compose --env-file C:\secure\csa-production.env -f compose.production.yml exec backend wget -qO- http://127.0.0.1:8080/actuator/health/liveness
docker compose --env-file C:\secure\csa-production.env -f compose.production.yml exec backend wget -qO- http://127.0.0.1:8080/actuator/health/readiness
docker compose --env-file C:\secure\csa-production.env -f compose.production.yml logs --tail=100 backend
```

再执行最小登录、CSRF、权限、上传和账号生命周期验收；Phase 1 基础演练见 `learning-guide.md`，Phase 2 运营演练见 `phase-2-learning-guide.md`。

## 发布与回滚

发布前先执行一次备份。应用镜像回滚使用上一份已验证镜像并保持数据库向前兼容；不要用旧镜像强行执行未知数据库结构。如果迁移失败，按 `flyway.md` 和 `backup-restore.md` 先恢复数据库，再决定是否回滚应用。

```powershell
docker compose --env-file C:\secure\csa-production.env -f compose.production.yml up -d --no-build
docker compose --env-file C:\secure\csa-production.env -f compose.production.yml ps
```

## 停机与回滚边界

- `docker compose down`：停止服务，保留命名卷。
- 不要执行 `down -v`，除非是已确认的隔离环境销毁。
- 删除容器不会删除数据库或上传卷；卷删除属于破坏性操作，必须先有异地加密备份。
- 旧前端或后端版本必须能读取当前数据库结构；破坏性迁移需要分阶段发布。

## 验证状态

2026-07-29 的隔离栈已完成空库 Flyway、已有库 baseline、失败迁移 fixture、备份恢复、backend/frontend 镜像和同源 HTTPS 最小 E2E；详细历史证据见 `phase-1-verification.md`。

2026-08-26 当前源码的后端 133 tests、前端 lint/test/build 已通过；显式 MySQL/Redis/Flyway 当前测试目标已更新到 V3，但本机 Docker 数据盘发生 I/O/EXT4 journal 故障，真实依赖、最新镜像构建、Playwright 和备份恢复必须在健康 staging 重跑；不得拿历史镜像结果替代。Phase 2 证据见 `phase-2-verification.md`。

本机 Windows 的 80 端口被 HTTP.sys 占用，历史 E2E 使用 `8443` 映射验证同一 Caddy 配置；生产部署仍按 80/443 运行。生产发布前必须在真实域名和 staging 证书环境再次执行健康检查、备份恢复、最小用户流程和当前镜像扫描。

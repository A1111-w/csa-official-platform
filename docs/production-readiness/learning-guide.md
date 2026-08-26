# 生产就绪改造学习与演练

这份文档把本轮改造拆成可以自己复现的学习任务。每项都要求记录“假设、命令、观察、结论、回滚”，不要只背配置名。

## 1. 统一错误契约

阅读 `ApiErrorCode`、`R<T>`、`GlobalExceptionHandler`、`TraceContext` 和 `RequestIdFilter`。用 MockMvc 验证：认证失败是 401，权限失败是 403，数据库异常是 500，三类响应都带稳定 `errorCode`、`traceId` 和 `X-Request-ID`。理解 HTTP status 与业务 `R.code` 为什么必须同时正确。

## 2. Flyway

先跑 `SchemaConsistencyTest`，再按 `flyway.md` 做空库和已有库实验。重点回答：为什么 V1 不包含 `CREATE DATABASE`/`USE`？为什么生产不加载 seed？为什么失败 migration 不能简单用 `repair` 当回滚？

## 3. Compose 和信任边界

对比 `docker-compose.yml` 与 `compose.production.yml`：开发基础设施只绑定回环地址，生产只有 Caddy 发布端口；生产后端通过 `data` 内部网络访问 MySQL/Redis。删除一个必填环境变量观察 fail-fast，再用临时假值运行 `config`，最后确认假值没有进入仓库和日志。

生产 MySQL URL 使用 `sslMode=REQUIRED&allowPublicKeyRetrieval=false`。如果把它误改成明文连接加关闭公钥回取，MySQL 8 的默认认证插件会在真实启动时失败；这个故障说明“配置能解析”不等于“依赖能连接”。

## 4. 备份恢复

运行 `deploy/backup.ps1`，检查 `database.sql.gz`、`uploads.tar.gz`、`metadata.json`、`SHA256SUMS`。修改归档的一个字节后运行 restore，确认校验失败；在隔离卷执行恢复，验证行数、文件内容、Flyway history 和应用登录。理解 SHA-256 是完整性校验，不是加密。

## 5. 可观测性

用不同的 `X-Request-ID` 请求一个成功和一个错误接口，按该值关联响应、JSON 日志和错误 envelope。查看 liveness/readiness 的差别，再读 `prometheus-alerts.yml`，说明每个阈值可能误报的场景。生产中 Actuator 只在内部网络可达。

## 6. 发布演练

按 `runbook.md` 做一次 staging 发布：备份、迁移、健康检查、最小登录/CSRF/权限/上传验证、回滚演练。把 Docker Hub 或数据库不可用当成真实故障记录，而不是删掉错误输出。

## 7. Redis 对象序列化故障

阅读 `RedisConfig` 和 `UserAccountCacheService`，理解 `auth_user` 缓存为什么能减少每次登录的数据库读取，也理解缓存格式本身是一个运行时契约。

复现步骤：

1. 启用 `CSA_CACHE_TYPE=redis`，让一个包含 `BigDecimal` 字段的 `User` 写入缓存。
2. 用受限的 `BasicPolymorphicTypeValidator` 读取同一条缓存，观察 `java.math.BigDecimal` 被拒绝时登录会变成 500。
3. 只允许必要的 `java.math`、`java.time`、`java.util` 和本项目包，运行 `RedisConfigSerializerTest`。
4. 清理隔离 Redis 后连续登录两次：第一次写缓存，第二次命中缓存；再跑 HTTP E2E，确认登录、CSRF 和上传都恢复。

这个实验的重点不是“把所有包都放行”。`LaissezFaireSubTypeValidator` 会扩大反序列化边界；生产配置应保持最小白名单，并为实体字段变更准备缓存失效或版本化策略。

## 8. Phase 1 收口

按 `phase-1-verification.md` 对照实际输出，分别记录代码测试、容器构建、Flyway、备份恢复、HTTP E2E 和回滚路径。不要把 Compose `config` 能展开误认为服务一定能启动，也不要把 HTTP 200 误认为业务 `errorCode` 正确。

## 面试表达

可以这样说：

```text
我把项目从“能启动”推进到可运维的单体部署：数据库结构由 Flyway 版本化，开发和生产 Compose 分离，生产只开放 Caddy 的 80/443，后端以非 Root 用户运行。请求带 X-Request-ID 并进入 MDC 和 JSON 日志，Actuator 提供内部 readiness/liveness/Prometheus。MySQL 和上传卷都有带 SHA-256 的备份恢复脚本，恢复前校验并停止写流量。对于 MySQL 非事务 DDL，我没有把 repair 当回滚，而是把失败迁移、备份恢复和 staging 演练写进 Runbook。
```

这段话只有在你真的跑过演练、能拿出输出和回滚记录时才算完成。

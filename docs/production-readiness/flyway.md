# Flyway 迁移与回滚

## 目标与版本链

生产数据库结构只由 `csa-official-backend/src/main/resources/db/migration/` 下的版本化 SQL 管理。当前版本链为：

| 版本 | 文件 | 作用 |
| --- | --- | --- |
| V1 | `V1__initial_schema.sql` | 12 张核心业务表，不创建数据库、账号或演示用户 |
| V2 | `V2__production_operations.sql` | 账号生命周期与唯一约束、审计、文件元数据、邮件状态、定时任务幂等表 |
| V3 | `V3__resume_review_queue_index.sql` | 为简历审核队列增加 `(status, update_time)` 联合索引 |

`db/schema.sql` 只是学习和人工核对快照，不是生产执行入口。`db/seed.sql` 只允许 dev/test 在显式注入运行时口令时加载。

生产默认启用 `clean-disabled=true`、`validate-on-migrate=true`、`out-of-order=false`。迁移失败时应用不得进入 readiness。

## 空数据库初始化

1. 在 MySQL 外部创建数据库和独立应用用户，授予目标库所需权限。
2. 设置 `FLYWAY_BASELINE_ON_MIGRATE=false`。
3. 启动 backend，让 Flyway 顺序执行 V1、V2、V3。
4. 查询：

```sql
SELECT installed_rank, version, description, success
FROM flyway_schema_history
ORDER BY installed_rank;
```

预期最新成功版本为 `3 / resume review queue index`，且 `sys_user` 没有演示账号。

## V1 数据升级到当前版本

发布前在隔离副本检查邮箱和学号规范化是否会造成唯一键冲突：

```sql
SELECT LOWER(TRIM(email)) normalized_email, COUNT(*) duplicate_count
FROM sys_user
WHERE NULLIF(TRIM(email), '') IS NOT NULL
GROUP BY LOWER(TRIM(email))
HAVING COUNT(*) > 1;

SELECT TRIM(student_id) normalized_student_id, COUNT(*) duplicate_count
FROM sys_user
WHERE NULLIF(TRIM(student_id), '') IS NOT NULL
GROUP BY TRIM(student_id)
HAVING COUNT(*) > 1;
```

存在重复时先完成业务确认和数据修复，不能让生产启动时才撞唯一索引。V2 会规范化邮箱/学号、补齐账号状态和会话版本，再创建唯一索引及运营表；V3 只增加简历审核队列索引，不修改业务数据。

## Flyway 之前的已有数据库

先确认旧库结构与 V1 一致。第一次启动仅临时设置：

```text
FLYWAY_BASELINE_ON_MIGRATE=true
FLYWAY_BASELINE_VERSION=1
```

Flyway 会把旧结构登记为 V1，然后继续执行 V2、V3。确认 history 最新成功版本为 3 后，立即恢复 `FLYWAY_BASELINE_ON_MIGRATE=false`。这个开关不能长期存在于生产环境。

如果旧库不等于 V1，先为实际差异编写新的兼容迁移并在副本验证；禁止用 `repair` 掩盖 SQL 或结构不一致。

## 失败迁移与回滚

MySQL 多数 DDL 不具备完整事务回滚能力。标准处理顺序：

1. 停止 backend 和写流量，保存日志、`flyway_schema_history` 和当前备份。
2. 记录已经落地的表、列和索引，不假设事务会自动撤销。
3. 在副本上从已验证备份恢复，或编写明确的向前修复 SQL。
4. 只有目标结构已经人工确认与 migration 一致时，才允许 `flyway repair` 清理失败记录。
5. 已发布 migration 永不原地改写；修复使用更高版本号。
6. staging 完成迁移、应用回滚和数据恢复演练后再恢复生产流量。

应用镜像可回滚，数据库只做向前兼容迁移。删除列、收紧约束等破坏性操作必须跨多个发布阶段完成。

仓库的 `deploy/drills/flyway-failure/` 只用于隔离数据库。故意失败的 V2 fixture 会留下部分 DDL，用于证明 `repair` 不是数据回滚；该目录不得加入生产 migration location。

## 验证命令

```powershell
cd D:\CSA-Project\csa-official-backend
.\mvnw.cmd test
.\mvnw.cmd "-Dit.containers=true" "-Dtest=FlywayMySqlRedisIntegrationTest" test
```

2026-07-30 的 Testcontainers 验证使用 MySQL 8.0.36 和 Redis 7.2：空库迁到 V1、写入旧数据、升级 V2、校验规范化和 Redis round trip，1/1 通过。2026-08-26 当前源码已把断言更新到 V3，并增加审核索引校验；由于本机 Docker 不可用，该新版真实依赖用例仍需在健康 staging 重跑。生产发布还必须先处理上面的重复数据查询。

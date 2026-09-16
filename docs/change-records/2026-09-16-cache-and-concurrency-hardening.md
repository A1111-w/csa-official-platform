# 2026-09-16 缓存与并发强化记录

- 日期：2026-09-16（项目开发记录日期）。
- 执行 agent：Codex。
- 关联需求/任务：`03-计算机协会项目完善清单-20260917.md`；
  `specs/001-cache-concurrency-hardening/tasks.md`。
- 状态：部分完成；代码、单元测试和文档已完成，真实 Docker/Testcontainers
  与 staging 验证待环境恢复。

## 背景与目标

补齐公开竞赛热点读链路的 Cache-Aside、空值缓存、热点回源互斥、TTL 抖动、
提交后失效和启动预热证据，同时收紧简历审核与提案投票的数据库并发边界。
不引入清单中暂不必要的布隆过滤器、RocketMQ 或 Redis Cluster。

## 实际变更

- 新增 `CacheKeys`、`CacheValueCodec`、`CacheTtlPolicy`、
  `CachedCompetitionPage` 和 `CacheWarmupRunner`。
- 公开竞赛列表和详情使用版本化 JSON 缓存；不存在/未发布详情使用短 TTL
  `__NULL__` 标记；热点详情使用随机 token、`SETNX`、二次检查和安全释放。
- 竞赛写入事务提交后递增公开列表 generation，并删除对应详情/空值 Key；
  缓存异常限频告警后回源数据库。
- 公开竞赛列表和详情增加 IP 限流；首页竞赛第一页在应用 ready 后可失败预热。
- 新增 Flyway V6，为 `biz_resume` 增加 `version` 和版本索引；提交/审核使用
  `status + version` 条件更新。
- 投票事务改为 `SELECT ... FOR UPDATE` 锁定提案，并将并发唯一键冲突转换为
  409 Conflict；提案最终状态更新使用条件更新。
- P2 的布隆过滤器、RocketMQ、Redis Cluster 和生产 QPS 声明未加入。

## 涉及文件

代码和配置：

- `csa-official-backend/src/main/java/com/csa/official/common/cache/`
- `csa-official-backend/src/main/java/com/csa/official/modules/biz/service/CompetitionService.java`
- `csa-official-backend/src/main/java/com/csa/official/modules/biz/controller/PublicCompetitionController.java`
- `csa-official-backend/src/main/java/com/csa/official/modules/resume/entity/Resume.java`
- `csa-official-backend/src/main/java/com/csa/official/modules/resume/service/ResumeService.java`
- `csa-official-backend/src/main/java/com/csa/official/modules/sys/mapper/ProposalMapper.java`
- `csa-official-backend/src/main/java/com/csa/official/modules/sys/service/VoteService.java`
- `csa-official-backend/src/main/resources/application.yml`
- `csa-official-backend/src/main/resources/db/migration/V6__resume_review_compare_and_set.sql`
- `db/schema.sql`

测试：

- `csa-official-backend/src/test/java/com/csa/official/common/cache/CacheTtlPolicyTest.java`
- `csa-official-backend/src/test/java/com/csa/official/modules/biz/service/CompetitionServiceTest.java`
- `csa-official-backend/src/test/java/com/csa/official/modules/sys/service/VoteServiceTest.java`

文档和 Spec Kit：

- `README.md`
- `docs/api-map.md`
- `docs/architecture.md`
- `docs/database.md`
- `docs/local-dev.md`
- `docs/module-completeness.md`
- `docs/project-status.md`
- `docs/production-readiness/flyway.md`
- `docs/study-and-demo-guide.md`
- `specs/001-cache-concurrency-hardening/`

## 兼容性、数据和配置

- 新增向前迁移 V6；不修改已执行的 V1-V5。
- `biz_resume.version` 默认从 0 开始，现有接口响应字段不变。
- 新增可选配置 `CACHE_WARMUP_ENABLED`，默认 `true`；设置为 `false` 可关闭预热。
- 公开竞赛接口继续使用原有 URL 和响应 VO；缓存只是内部实现。
- Redis 模式仍是多实例共享缓存，memory 模式仍只适用于本地单实例。

## 验证

执行目录：`csa-official-backend`

- `.\mvnw.cmd -q -DskipTests compile`：通过，使用 JDK 17.0.20.1。
- `.\mvnw.cmd -q "-Dtest=CompetitionServiceTest,CacheTtlPolicyTest,VoteServiceTest,ResumeServiceTest" test`：
  通过。
- `.\mvnw.cmd -q test`：通过；本轮命令退出码为 0。Testcontainers 因本机未发现
  可用 Docker 环境而跳过/未执行。
- `.\mvnw.cmd "-Dit.containers=true" "-Dtest=FlywayMySqlRedisIntegrationTest" test`：
  尚未执行；需要健康 Docker。
- `powershell -ExecutionPolicy Bypass -File tools/check-study-guide-drift.ps1`：
  通过，Study-guide drift check 共 52 项。
- `git diff --check`：通过。

## 风险与遗留

- 未在本机完成真实 MySQL、Redis、Flyway V1-V6 和并发回源实验；不能把单元测试
  当成生产吞吐或真实依赖证据。
- 公开详情在热点锁等待超时后返回 503；接口调用方应按正常重试策略处理。
- 竞赛公共缓存的 Redis 故障降级会增加数据库压力，仍依赖已有接口限流和连接池边界。
- 已有工作区的登录、Redis、前端和监控修改不属于本记录，未被本次提交归并。

## 回退

- 应用代码可通过后续 `git revert` 回退本功能提交。
- 如果 V6 已执行，数据库只允许保留向前兼容结构；不要删除 `version` 列或改写
  已执行 migration。旧应用回退前应确认不会读取新字段。
- 关闭预热可设置 `CACHE_WARMUP_ENABLED=false`；缓存故障时可临时切换到经确认的
  `CSA_CACHE_TYPE=memory`，但生产 profile 不得使用 memory。

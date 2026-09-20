# 2026-09-19 缓存并发收敛与验证口径修正记录

- 日期：2026-09-19（Asia/Shanghai）。
- 执行 agent：Codex。
- 关联需求/任务：`specs/001-cache-concurrency-hardening/tasks.md` 的 T015-T020；
  源码审计收敛 2026-09-18 发现的缓存适配、公共读降级、generation 失效和简历
  CAS 缺口。
- 状态：代码、测试、文档和记录已完成；真实 Docker/Testcontainers 和 staging
  仍待健康环境。

## 背景与目标

前一轮实现已经覆盖公开竞赛缓存、简历审核 CAS 和投票并发边界，但审计发现 Redis
适配器的 JSON 版本兼容、进程内降级复杂度、详情回填 generation 边界和 V6 真实依赖
断言还没有形成足够直接的证据。本轮补齐这些边界，并按用户要求减少与业务无关的
防御性代码，保持构建和维护路径简洁。

## 实际变更

- `RedisKeyValueStore` 公开缓存路径保留版本化 JSON，新增适配器读写回归测试；旧验证码等非公开缓存继续兼容历史反序列化。
- `PublicReadCache` 和 `PublicReadFallback` 覆盖正常命中、`__NULL__` 空值、热点 key 并发回源、提交后 generation 失效和回滚不失效。
- `PublicReadFallback` 简化为 `ConcurrentHashMap`、2 秒短 TTL 和 generation epoch；移除分片锁、Semaphore、容量统计和总字符限制。Redis 热点锁和 generation 仍保留，因为它们是跨请求一致性边界。
- `ResumeServiceTest` 增加保存版本冲突和已处于 `PENDING` 时禁止重复提交的测试。
- `FlywayMySqlRedisIntegrationTest` 更新最新迁移版本为 6，增加 `biz_resume.version` 默认值、`idx_resume_user_version`、CAS 更新、`uk_vote_proposal_voter` 和 Redis `SETNX` 断言。
- 修正当前状态和 Phase 1 验证文档，把旧 V1-V5/174-test 记录标为历史证据，明确当前工作树为 V1-V6，真实容器复验尚未完成。

## 涉及文件

代码和测试：

- `csa-official-backend/src/main/java/com/csa/official/common/cache/PublicReadFallback.java`
- `csa-official-backend/src/main/java/com/csa/official/common/cache/PublicReadCache.java`
- `csa-official-backend/src/main/java/com/csa/official/common/cache/RedisKeyValueStore.java`
- `csa-official-backend/src/main/java/com/csa/official/modules/resume/service/ResumeService.java`
- `csa-official-backend/src/test/java/com/csa/official/common/cache/RedisKeyValueStoreTest.java`
- `csa-official-backend/src/test/java/com/csa/official/common/cache/PublicReadCacheTest.java`
- `csa-official-backend/src/test/java/com/csa/official/modules/resume/service/ResumeServiceTest.java`
- `csa-official-backend/src/test/java/com/csa/official/integration/FlywayMySqlRedisIntegrationTest.java`

文档和任务：

- `docs/project-status.md`
- `docs/production-readiness/phase-1-verification.md`
- `docs/api-map.md`
- `docs/architecture.md`
- `docs/local-dev.md`
- `docs/production-readiness/flyway.md`
- `docs/study-and-demo-guide.md`
- `specs/001-cache-concurrency-hardening/tasks.md`
- `CHANGELOG.md`

## 文档同步

实际缓存 key、Redis 故障降级、V6 迁移边界和真实验证限制已同步到 API、架构、本地
开发、Flyway 和学习指南；当前状态、Phase 1 记录和 Spec Kit 任务已更新。没有把
历史 CI 或旧版本容器结果改写为当前工作树通过。

## 兼容性、数据和配置

- 公开接口 URL、响应 VO 和认证边界不变。
- V6 是新增向前 migration；不修改 V1-V5。`biz_resume.version` 默认值为 0，
  已执行 V6 后不能删除列或改写 SQL。
- 进程内 fallback 只适合单实例、短时间 Redis 故障降级，不能代替生产 Redis；
  多实例部署仍依赖 Redis 共享缓存和 generation。
- 没有新增生产秘密、账号或数据文件。

## 验证

执行目录：`C:\Users\Administrator\Documents\ChatGPT\计算机协会\csa-official-backend`

- `.\mvnw.cmd -q "-Dtest=RedisKeyValueStoreTest,PublicReadCacheTest,CacheTtlPolicyTest,CompetitionServiceTest,ResumeServiceTest" test`：通过。
- `.\mvnw.cmd -q -DskipTests test-compile`：通过。
- `.\mvnw.cmd -q test`：退出码 0；Surefire 汇总为 216 个用例、214 个实际执行通过、0 failures、0 errors、2 skipped。
- 跳过项：`FlywayMySqlRedisIntegrationTest` 因本机无健康 Docker；
  `LoginTrackingMapperTest` 因未配置 `CSA_LOCAL_MYSQL_TEST_URL`。这些不是失败，
  但也不构成真实 MySQL/Redis 通过证据。
- `.\mvnw.cmd "-Dit.containers=true" "-Dtest=FlywayMySqlRedisIntegrationTest" test`：
  本轮未执行，待健康 Docker/staging。
- `powershell -ExecutionPolicy Bypass -File tools/check-study-guide-drift.ps1`：通过，
  `Study-guide drift check passed: 52 checks.`。
- `git diff --check`：通过。

## 风险与遗留

- 真实 MySQL、Redis、Flyway V1-V6、跨实例缓存和 staging 备份恢复仍未在本机完成。
- 进程内 fallback 的短 TTL 只能降低短时故障压力，不能保证多实例之间的一致读；
  生产不可切换为 memory 模式作为长期方案。
- `T014` 保持未勾选，直到健康 Docker/Testcontainers 或 staging 完成真实迁移和并发
  验证。前端验证沿用既有独立记录，本轮没有将其声称为重新执行。

## 回退

应用和测试代码可通过后续针对本功能的 `git revert` 回退；文档同样随提交回退，
不 reset、clean 或覆盖工作区其他用户修改。若 V6 已落地，只能应用回退或发布新的
向前兼容 migration，不能删除 `version` 列、索引或改写已执行 migration。

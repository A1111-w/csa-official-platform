# Tasks: 协会 Agent、RAG 与评测实验室

**Input**: `specs/002-agent-rag-evaluation/` 内 spec、plan、research、data-model、contracts。

**Tests**: 规格 FR-012 要求风险适配的隔离验收；以下状态须由协调者依据实际结果更新。

## Phase 1: Setup

- [x] T001 完成规格与质量审查，在 `specs/002-agent-rag-evaluation/` 保存完整设计工件并在 `.specify/feature.json` 指向本功能。
- [x] T002 核对隔离开发分支与较新前端基线，在 `docs/project-status.md` 记录来源及当前工作区，保留原工作区修改。

## Phase 2: Foundational

- [x] T003 在 `csa-official-backend/src/main/java/com/csa/official/modules/ai/AiProperties.java` 和 `ModelGateway.java` 实现 offline/live 配置、兼容 HTTP 模型接口、请求超时和失败信息清洗。
- [x] T004 [P] 在 `csa-official-backend/src/main/java/com/csa/official/modules/ai/AiModels.java` 定义共享契约：`question` 非空且最多 2000 字符；`title` 非空且最多 200 字符；`content` 非空且最多 50000 字符；`source` 最多 500 字符；`minRoleLevel` 为 1–99。
- [x] T005 [P] 在 `csa-official-frontend/src/services/ai.ts` 封装 typed API，复用现有 Cookie/CSRF 客户端并校验评测输入。

## Phase 3: US1 知识录入与授权检索 (P1, MVP)

**Independent test**: 录入并重启后检索；普通会员无法获得高等级资料；删除后不再召回。

- [x] T006 [US1] 在 `csa-official-backend/src/main/resources/db/migration/` 新增 V7，在 `db/schema.sql` 同步文档/片段/运行表，不改已应用 migration。
- [x] T007 [US1] 在 `csa-official-backend/src/main/java/com/csa/official/modules/ai/KnowledgeService.java` 实现事务入库、切分、授权过滤、中文/英文 BM25、可选 embedding/RRF；minRoleLevel 不得超过录入者等级；演示容量最多 2000 块。
- [x] T008 [US1] 在 `csa-official-backend/src/main/java/com/csa/official/modules/ai/AiController.java` 提供知识列表、创建、删除与输入/角色校验。
- [x] T009 [P] [US1] 在 `csa-official-frontend/src/app/dashboard/ai/page.tsx` 增加知识列表、文档录入/删除、权限提示、请求失败与处理中状态。
- [x] T010 [US1] 在 `csa-official-backend/src/test/java/com/csa/official/modules/ai/` 验证分块边界、排序、向量融合、授权先于排序、数据库保存与删除，并记录真实测试结果。
- [x] T011 [US1] 同步 `docs/database.md`、`docs/production-readiness/flyway.md`、`docs/api-map.md`、`docs/rag-agent-roadmap.md`，在 `CHANGELOG.md` 加 US1 行为与验收记录。

## Phase 4: US2 有界助手与轨迹 (P1)

**Independent test**: 模拟上游执行工具循环；离线三类查询可用；未知工具、越权和预算均受控；记录不能跨用户读取。

- [x] T012 [US2] 在 `csa-official-backend/src/main/java/com/csa/official/modules/ai/AgentService.java` 实现三类只读工具、独立权限/参数校验、offline 抽取和 live 循环、调用预算、模式与 token/耗时轨迹。
- [x] T013 [US2] 在 `csa-official-backend/src/main/java/com/csa/official/modules/ai/AgentService.java` 持久化运行记录，仅 owner 可读，并对历史引用重新检查当前 ACL。
- [x] T014 [P] [US2] 在 `csa-official-frontend/src/app/dashboard/ai/page.tsx` 展示回答、模式、引用、轨迹、失败及耗时，并在 `csa-official-frontend/src/config/menu.ts` 加 Level 1 菜单。
- [x] T015 [US2] 在 `csa-official-backend/src/test/java/com/csa/official/modules/ai/` 验证未知工具、参数、循环预算、上游失败、只读边界和历史记录授权；前端在 `csa-official-frontend/src/services/ai.test.ts` 验证契约与错误输入。
- [x] T016 [US2] 同步 `docs/architecture.md`、`docs/security-design.md`、`docs/local-dev.md`、`docs/deployment.md`、后端环境示例与 `csa-official-frontend/README.md`，在 `CHANGELOG.md` 加 US2 记录，区分模拟上游与真实模型验证。

## Phase 5: US3 可复现规则评测 (P2)

**Independent test**: 固定用例两次评分一致；错误预期降分；N/A 不计均值；失败记零；报告只允许创建者读取。

- [x] T017 [US3] 在 `csa-official-backend/src/main/java/com/csa/official/modules/ai/EvaluationModels.java` 校验每批 1–5 条、三个数组必填、文档最多 20 项且每项 100 字符、工具最多 3 项且白名单、词最多 20 项且每项 200 字符、至少一项预期非空。
- [x] T018 [US3] 在 `csa-official-backend/src/main/java/com/csa/official/modules/ai/EvaluationMetrics.java` 和 `EvaluationService.java` 实现召回/MRR/工具召回/关键词覆盖、N/A 和失败评分、原数据集及 `rules-v1` 报告。
- [x] T019 [US3] 在 `csa-official-backend/src/main/java/com/csa/official/modules/ai/EvaluationController.java` 提供 Level 3 限流评测与 owner-only 回看，并在 `csa-official-backend/src/main/resources/db/migration/` 新增 V8 报告表。
- [x] T020 [P] [US3] 在 `csa-official-frontend/src/app/dashboard/ai/page.tsx` 展示数据集编辑、逐条/汇总报告、N/A 与指标局限，支持报告下载或回看。
- [x] T021 [US3] 在 `csa-official-backend/src/test/java/com/csa/official/modules/ai/` 验证评分公式、重复标注、失败、N/A、输入限制、权限与持久报告；在 `docs/rag-agent-roadmap.md` 和 `CHANGELOG.md` 记录结果与无语义 judge 的限制。

## Phase 6: Cross-cutting verification

- [ ] T022 依据 `specs/002-agent-rag-evaluation/quickstart.md` 完成当前工作区后端回归、前端测试/lint/build、文档漂移及页面演示检查，失败或未执行项明确记录。
- [x] T023 同步 `README.md`、`docs/module-completeness.md`、`docs/project-status.md`、`docs/study-and-demo-guide.md`，整合 `docs/change-records/` 与 `CHANGELOG.md` 并审查 diff，不能以历史 CI 或文档存在冒充本次通过。

T022 验证现状：后端全量回归与最终 AI 30 项测试、前端 28 项测试、lint、Webpack 生产构建及文档漂移已通过；自动审批在创建本地 Next 服务进程前返回 `blocked by policy`，浏览器交互脚本与截图未执行，故该项保持未勾选。详细命令与证据见 `docs/change-records/2026-10-31-agent-integration.md`。

## Dependencies & parallel examples

- [x] T024 根据用户补充的教学友好性要求，复查旧学习节奏，交付 `docs/agent-learning-plan.md` 的 4 节核心课和 2 节选修、每课成功信号与讲师提示，更新原指南、入口及修改记录；实际课堂效果仍需试教。

T001–T002 → T003–T005 → US1 → US2 → US3 → T022–T023。前后端按契约可以并行实现：US1 的 T007 与 T009、US2 的 T012 与 T014、US3 的 T018 与 T020 分属不同文件。两个前端页面任务必须由同一 owner 顺序整合；共享文档和 CHANGELOG 由协调者合并。

## Implementation strategy

先验收 US1 作为 MVP，再接助手与评测。每个故事的测试、文档和记录同批交付；不等最后阶段才补。初始 23 个任务：US1 6 个、US2 5 个、US3 5 个、基础与收尾 7 个；用户追加教学友好性要求后增加 T024，共 24 项。外部模型、真实 MySQL/Redis 与部署验证无环境时单列未验证，不阻塞诚实报告离线/隔离能力，也不宣称整个生产验收已通过。

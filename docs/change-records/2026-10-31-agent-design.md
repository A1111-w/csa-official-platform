# 20261031-agent-design | Agent、知识检索与评测设计

- 日期：2026-10-31（Asia/Shanghai）。
- 执行 agent：Codex research 子 agent；协调者整合共享 CHANGELOG。
- 关联需求：用户要求增加 Agent/RAG/评测技术点；`specs/002-agent-rag-evaluation/tasks.md` T001、T010、T022；用户追加“复查学习规划不仅要考虑教学内容正确性，还要考虑对于教学者是否友好”。
- 状态：需求与设计工件、隔离事务验收用例和教学安排已交付；真实 MySQL/Testcontainers 执行仍需 Docker。
- 变更：以较新分支代码及模型契约为基础，定义三个用户故事、权限与输入边界、规则指标局限、持久记录、验收步骤及 23 项任务；记录 Spring AI、Ragas、Langfuse 的参考来源与未集成边界。
- 审查与测试补充：新增 `KnowledgeTransactionTest`，使用轻量 Spring context、真实事务代理、H2 和 `DataSourceTransactionManager`；第二片段写入由测试约束故意拒绝，检查新文档及第一片段一起回滚、已提交资料保留。更新真实 MySQL/Redis Testcontainers 用例的 latest 版本为 8，并检查四张 AI 表、索引与文档/片段/运行/评测写入。规格路径已对齐实际 `/dashboard/ai`。
- 教学自审：旧 Day 5/7/14 把多条安全、文件和部署链挤入一天；roadmap 的八周对应表在仓库不存在；旧 RAG 图容易教成 Top-K 后才过滤权限；一串术语缺成功信号。新增 `docs/agent-learning-plan.md` 的 4 节核心课和 2 节选修，每节含前置/目标/步骤/成功结果/卡点/讲师提示/挑战，先离线再 live，时间标为环境准备完成后的估计。学习指南加入可拆分节奏与入口，roadmap 加教学口径校准，不改长期设计或实际业务实现。
- 文件：`specs/002-agent-rag-evaluation/`、`.specify/feature.json`、`docs/agent-learning-plan.md`、`docs/study-and-demo-guide.md`、`docs/rag-agent-roadmap.md`、`csa-official-backend/src/test/java/com/csa/official/modules/ai/KnowledgeTransactionTest.java`、`csa-official-backend/src/test/java/com/csa/official/integration/FlywayMySqlRedisIntegrationTest.java` 和本记录。
- 兼容性/数据/配置：仅设计文档、功能指针及隔离测试变化；不改生产业务数据、依赖、Git 分支或远程。
- 验证：在 `D:\CSA-Project-agent` 运行 Spec Kit 模板解析、`setup-plan.ps1 -Json` 与 `setup-tasks.ps1 -Json` 成功；需求质量清单 16 项通过。PowerShell 检查确认 23 项任务格式、16 项清单及所有本地 Markdown 链接有效；`git -C D:\CSA-Project-agent diff --check` 通过。应用测试由各实现 owner 记录，本子任务不以历史测试计数替代。
- 风险与遗留：真实模型效果、向量索引规模与语义忠实度未验证；规格不承诺这些能力。contracts 已明确 Recall@K/MRR 根据最终累积证据计算，不把多轮轨迹指标描述成单次 top-5 结果。
- 新增测试验证状态：本子任务未并行运行 Maven，避免覆盖后端进程。主 agent 本轮全量回归报告 232 个测试、231 通过、1 个 Docker 集成测试跳过，包含 `KnowledgeTransactionTest` 通过；真实 MySQL/Testcontainers 仍需健康 Docker 和显式 `-Dit.containers=true`，不能凭新增断言宣称已通过。
- 教学验证：`tools/check-study-guide-drift.ps1 -ProjectRoot D:\CSA-Project-agent` 52 项通过；脚本确认 6 课每课具备全部 7 个教学字段、所有本地文档链接存在；两份受影响旧指南 `git diff --check` 通过。新安排共 95 行，未扩写为第二份大型源码手册；尚无真实课堂试教反馈，估时和学习负担需根据讲师/学习者实际反馈继续调整。
- 回退：仅回退本次文档与当前功能指针变更，保留原有 `specs/001-*`、业务代码和用户工作；如已提交使用后续 revert。

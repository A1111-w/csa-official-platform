# 20261031-agent-integration | Agent 实验台、规则评测与集成验证

- 日期：2026-10-31（Asia/Shanghai）；执行者：Codex 主 agent，协作 research/backend/frontend。
- 需求：为 CSA 增加技术展示用 Agent、RAG、评测并使用 GitHub 新前端；规格为 `specs/002-agent-rag-evaluation/`。
- 状态：功能实现、学习安排与文档已完成；浏览器交互验收被本机自动审批审查阻断，未满足全部验收门槛，T022 保持未完成。
- 基线：`origin/codex/development` 的 `b3a7377`（2026-09-20），包含 `codex/resume-review-workflow` 的完整前端；独立目录 `D:/CSA-Project-agent`，分支 `codex/agent-rag-evaluation`。`D:/CSA-Project` 未提交用户文件保留；未提交或推送本次代码，未部署。
- 实际变更：主 agent 实现 EvaluationModels/Metrics/Service/Controller、V8 报告持久化与规则评测测试；协调 Agent/RAG 后端、前端实验台和独立审查，更新 API、架构、安全、数据库、运行与教学文档。
- 最终规格核对补齐 `RunResult.question` 的保存与回读，避免单次运行记录缺少原问题；该字段是新增响应字段，存于既有 JSON 列，无额外迁移。增加持久化回归断言并重跑 AI 模块全套定向测试。
- 数据/配置：V7/V8 只新增 AI 表，`db/schema.sql` 同步快照；默认离线。真实模型和 embedding 只从后端环境配置，不提交凭据。原 Cookie/CSRF/账号校验保留。
- 本地构建产物：根 `.gitignore` 增加 `logs/`，排除失败依赖复制留下的临时目录；前端依赖、截图与 Maven 输出仍由各自现有忽略规则管理，不纳入源码交付。
- 文档入口：`docs/agent-lab.md`、`docs/ai-backend.md`、`docs/ai-frontend.md`、`docs/ai-evaluation.md`；详细子项见同日 design/backend/frontend 记录。
- 教学复查：根据用户追加要求同时考虑知识正确性与教学友好性；新增 `docs/agent-learning-plan.md`，采用 4 节离线核心课 + 2 节模型/向量选修课。把前置环境与课堂目标分开，补齐操作、可观察结果、卡点、讲师提示及挑战；去掉“五分钟首次学完”的暗示，校准旧 Day 和无依据的八周表述，并把旧 RAG 图改为权限过滤先于 Top-K。已做源码对照，尚未真实试教；时间只是备课估计。

## 本轮验证

后端全量回归通过：232 个用例，231 通过、0 失败/错误、1 个 Docker 集成用例跳过。命令从 `csa-official-backend` 执行：设置 `JAVA_HOME=D:/java`，再运行 `.\mvnw.cmd '-DargLine=-javaagent:C:/Users/27719/.m2/repository/net/bytebuddy/byte-buddy-agent/1.17.8/byte-buddy-agent-1.17.8.jar' test`。显式 agent 用于解决本机 Windows/JDK21 动态附加停滞，源码编译目标仍为 Java 17；其他机器应替换为自身 Maven 缓存路径。没有修改 POM 来绕过测试。

全量结束后新增流式响应卡住的超时测试，以相同环境执行 `mvnw.cmd '-DargLine=-javaagent:C:/Users/27719/.m2/repository/net/bytebuddy/byte-buddy-agent/1.17.8/byte-buddy-agent-1.17.8.jar' '-Dtest=ModelGatewayTest' test`：6 项全部通过。最后补齐 question 持久化后，再运行同参数并设置 `-Dtest=AgentServiceTest,AiControllerAuthorizationTest,KnowledgeServiceTest,KnowledgeTransactionTest,EvaluationMetricsTest,EvaluationServiceTest,ModelGatewayTest`：AI 模块 30 项全部通过，0 跳过。后一次为最终 AI 源码的验证证据。

前端 `npm run test`：8 文件、28 项通过；`npm run lint`：0 错误、1 条原有 `src/lib/axios.ts:161` warning；`npm run build -- --webpack`：完整生产构建成功，27/27 页面，包含 `/dashboard/ai`。基线 Maven test-compile、52 项教学文档漂移检查、Spec Kit 前置检查及独立代码审查通过，最终 `git diff --check` 通过。

浏览器验证未运行，没有截图。自动审批审查在创建进程前拒绝从前端目录启动本地 Next 服务：`$env:NODE_OPTIONS='--max-old-space-size=1024'; $env:NEXT_PUBLIC_API_URL='http://localhost:8080'; npm.cmd run start -- --hostname 127.0.0.1 --port 3000`。工具只返回 `blocked by policy`，未提供更详细原因。没有换入口绕过拒绝，也没有把准备好的 `e2e/agent-lab.spec.ts` 当成已通过证据；没有遗留本轮前端服务进程。后续在允许启动本地服务的环境执行该浏览器脚本，并检查问答、权限界面、评测与导出。

测试全部使用隔离 H2、mock 或本地 HTTP fixture，不读取原工作目录生产 `.env`。真实模型/embedding 质量与生产 MySQL/Docker 环境不作为本轮已通过项。前端 registry 连接失败后复用本机已安装的匹配依赖，通过 worktree 内 node_modules junction 连接原依赖目录；原源码未修改，忽略的依赖缓存可能更新。这不等同成功的干净在线 npm ci。

## 风险、边界与回退

- MySQL JSON 向量穷举仅用于小型演示；不含专用 ANN 数据库、异步解析、PDF/OCR、长期记忆或 LLM-as-judge。
- 离线摘录和真实模型工具调用有明确区分；规则指标不证明语义正确性。指标为最终累积引用集合的文档召回与首次命中位置，非每个检索子调用的平均。
- 批量评测同步执行且会在 live 模式实际消耗额度；运行记录和报告没有自动过期清理。
- 生产部署、真实供应商调用和真实 MySQL 升级需单独验收。
- 本轮未动原工作目录；可继续使用原目录或基线分支。后续若提交则以独立 revert 回退应用，保留已执行 V7/V8 表和迁移历史，不自动删表或清数据。

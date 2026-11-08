# AI 后端：检索与工具执行

2026-10-31，记录编号 `20261031-ai-backend`。对应 `modules/ai`、Flyway V7。

## 运行模式

默认 `AI_ENABLED=false`：BM25 检索、带来源的原文摘录、按规则路由资源/竞赛工具。
这是无需模型凭据的离线演示，不是语义向量检索，也不是模型自主规划。

显式开启 `AI_ENABLED=true` 并配置 `AI_BASE_URL`（含 `/v1`）、`AI_API_KEY`、
`AI_MODEL` 后，通过 OpenAI-compatible `/chat/completions` 执行模型选工具循环。
仅当 `AI_EMBEDDING_MODEL` 非空时，导入和检索调用 `/embeddings`；实际模型费用由
配置的服务提供方产生。默认模型轮数 4，可配置 `AI_MAX_ITERATIONS`，代码强制 1–6。
`AI_TIMEOUT_SECONDS` 默认 25，单次 HTTP 请求强制 1–30 秒。

模型服务地址只由服务端环境配置，用户输入不能指定 URL。允许 HTTP 以支持本地模型；
远程服务应配置 HTTPS。不会自动下载 URL 文档或解析受控文件，正文从粘贴文本/Markdown 导入。

## 检索流水线

1. Level 3 以上导入文档，指定 Level 1 到自身等级的最低访问级别。
2. 归一化换行，按 700 字符切片、重叠 100 字符，避免拆断 UTF-16 代理对。
3. MySQL 保存文档元数据及分块；启用 embedding 时同时存储实际向量及模型标识。
4. 检索 SQL 在候选召回前筛选 `min_role_level <= currentRoleLevel`，随后计算词频和
   文档频率。英文/数字按词，汉字按双字切分；BM25 使用 k1=1.2、b=0.75。
5. 可选同模型向量余弦相似度（阈值 >0.15）与 BM25 以 RRF 合并，分数为排名融合分数，
   不是概率；默认返回 5 个分块，服务端 topK 限制 1–10。
6. 返回 `documentId/chunkId/title/source/text/score`，由模型指令要求引用 `[chunkId]`。

此版本为小型演示索引：全量内存评分、最多 2000 个分块，超过容量明确拒绝查询；文档
列表最多返回 200 条。没有 ANN 向量数据库、reranker、文件解析任务、增量更新和异步索引。
修改 embedding 模型后，旧向量不参加新模型评分；需删除并重新导入对应文档。
引用列表展示检索证据，不能自动证明模型答案每个陈述的忠实性。

## Agent 执行

每次运行先调用 `search_knowledge` 建立证据。在线模型随后可从以下白名单选择工具：

| 工具 | 实际范围 |
| --- | --- |
| `search_knowledge` | 当前角色可读的知识分块 |
| `search_resources` | 现有 ResourceService 返回的最近 100 条会员资源，关键词过滤后最多 5 条标题摘要 |
| `search_competitions` | 现有 CompetitionService 的公开列表最近 100 条，关键词过滤后最多 5 条标题摘要 |

工具参数仅接受 `query`，不允许模型传入 userId、roleLevel 或 SQL；每次服务调用重查会员
门槛。竞赛工具不调用后台全量列表，资源结果不包含上传者资料和下载地址。模型不能调用
发消息、提权、删除或任意 URL/代码执行工具。模型消息和原文仍可能含提示注入，所以安全
边界放在 Java 白名单、参数校验和业务服务，不依赖系统提示自律。

每次运行最多 8 次工具调用（含初始检索）、1–6 轮模型请求；每轮开始检查 90 秒预算，
已开始的 HTTP 请求最多再占单次超时预算。未得到最终回答时返回 `budget/stopped` 步骤。
单实例并发最多 4，满额返回 429。HTTP 响应在累计超过 2 MB 时取消读取；不跟随重定向；
调用失败统一返回不含 provider 原始响应/凭据的错误。

`steps` 记录工具名、成功/失败/停止状态、毫秒耗时和结果条数，不是模型隐藏思考过程。
`promptTokens/completionTokens` 为 provider 返回的聊天 usage 累计，不包含 embedding 花费。
未提供 usage 时为 0，应结合 provider 账单核对；离线模式为 0。
Micrometer 暴露 `csa.ai.runs`（mode/outcome）和 `csa.ai.run.duration`。

## API 与权限

所有接口保留既有 Cookie JWT、CSRF 和当前账号角色检查。

| 方法/路径 | 最低等级 | 输入/结果 |
| --- | ---: | --- |
| GET `/api/ai/status` | 1 | mode/model/embeddingMode，仅配置读取，不触发模型调用 |
| GET `/api/ai/documents` | 1 | 仅当前角色可读的文档元数据 |
| POST `/api/ai/documents` | 3 | title≤200、content≤50000、source≤500、minRoleLevel；返回文档元数据 |
| DELETE `/api/ai/documents/{id}` | 3 | 仅可读等级内文档；同步删除分块 |
| POST `/api/ai/runs` | 1 | question≤2000；返回答案、引用、步骤、模型/耗时/Token |
| GET `/api/ai/runs/{id}` | 1 | 仅创建者可读，其他用户得到 404 |

导入每 IP 5 次/分钟，运行每 IP 10 次/分钟，复用现有限流实现。运行读取重新校验每个引用
的当前访问权限；角色降低或知识源删除后，旧答案整条拒绝读取，避免历史 JSON 绕过权限。
执行中没有会话跨轮记忆，用户每次提问独立运行。执行记录包含问题、原文摘录和答案，部署方应
为数据库配置相应访问控制与保留周期；本次没有自动清理任务。

## 数据与验证

V7 新增 `ai_document`、`ai_chunk`、`ai_run`，与现有表均无结构修改。删除文档和创建
文档通过事务执行，迁移没有导入任何生产/演示知识。部署先正常执行 Flyway，再访问入口。

测试包括 H2 MySQL 模式执行真实 V7 SQL、分块重叠、中文检索、候选 ACL、向量模型身份、
删除清理、历史运行归属与降权、白名单拒绝、模型轮次预算、真实本地 HTTP stub 请求与
embedding 排序/异常响应、Spring Security 接口等级。结果见独立修改记录。

回退应用版本时保留 V7 三张表，不删除或修改已应用 migration。外部真实模型、真实 MySQL
和分布式规模/并发压测需要在具备对应环境时另行验证。

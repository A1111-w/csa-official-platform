# CSA Agent 实验台

Agent 后端最初基于 codex/development 的 b3a7377；React 完整快照已保留为 codex/agent-rag-evaluation（bf1415c）并推送GitHub。当前Vue迁移目录为 D:/CSA-Project-vue，分支 codex/vue3-frontend。原 main 未提交改动保持原样，未部署生产。

## 能展示什么

| 技术点 | 实现与演示位置 |
| --- | --- |
| 知识入库 | 文本/Markdown 输入、长度校验、700 字符分块与 100 字符重叠、事务持久化 |
| 中文检索 | 中文双字切分、BM25、Top-K；权限条件先进入 SQL 候选集 |
| 混合检索 | 配置 embedding 模型后：真实向量、余弦相似度、RRF 排名融合；向量保留模型标识 |
| Agent | 模型选择三个白名单工具：知识、会员资源、公开竞赛；严格参数、有限轮次与调用数 |
| 成本和可用性 | 请求限流、单实例并发槽、HTTP 超时、输出限制、明确的预算耗尽状态 |
| 可观察性 | 引用来源、工具/模型步骤、耗时、模型 usage token、Micrometer 运行计数和耗时 |
| 评测 | 数据集快照、规则版本、Recall@K、MRR、工具召回率、关键词覆盖率、逐条错误和运行 ID |
| 安全 | 复用 Cookie/CSRF、后端等级权限、运行与报告归属隔离、历史引用重新校验 |

### 环境准备完成后的演示提纲

下面是功能演示提纲，不是首次学习的五分钟承诺。环境启动、账号准备和真实模型配置需课前单独完成；学习者先只做一份文档的一次问答，再逐步进入评测和权限对照。分课目标、前置知识、讲师提示和验收问题见 [循序渐进的教学安排](agent-learning-plan.md)。

1. 按现有 [本地启动说明](local-dev.md)启动隔离开发数据库和服务。Flyway 顺序执行新增 V7/V8；不用生产数据库或生产秘密作演示环境。
2. 以部长及以上账号打开 `/dashboard/ai`，在知识库录入一个有明确答案的技术文档。示例标题为“RAG 技术演示”，正文可填：“RAG 先检索知识，再把证据交给模型生成回答。BM25 负责关键词检索，向量检索补充语义匹配，RRF 融合两个排名。” 访问等级设为 1。
3. 提问“RAG 如何组合检索与生成”，展开引用与 `search_knowledge` 步骤。默认页面显示离线摘录，没有调用大模型。
4. 在评测页填写问题、该文档 ID、预期工具 `search_knowledge`、关键词“检索”“生成”。执行后查看逐条分数并下载 JSON；再把预期关键词改成不存在的内容，验证报告会失败。
5. 用低等级账号验证不能写知识库/执行评测；可另建 Level 3 文档验证普通会员无法检索。启用真实模型后，再对比工具选择与耗时。

## 运行模式和配置

Vue 分支使用独立 npm ci，npm run dev 启动 Vite；React 分支的 Webpack/junction 限制仅为历史记录，不用于本分支。

默认 `AI_ENABLED=false`：BM25 检索和规则路由，回答只是明确标注的资料摘录。它适合无模型凭据的本地演示，不是语义生成，也不是自主模型规划。

真实模式配置见后端 `.env.example` 和 [后端说明](ai-backend.md)。只有启用并配置模型服务、模型名和服务端凭据后才会调用外部服务。可选 embedding 模型开启混合检索；离线期间录入的文档没有向量，需在启用 embedding 后重新导入。更换 embedding 模型也应重建对应文档。服务异常不会伪装成离线模型成功。

本轮使用已有 Java 17 / Spring Boot 和 Vue 3 架构，没有为展示额外拆微服务。向量保存在 MySQL JSON 文本字段中，对小型语料逐一计算相似度；这不是 Milvus/pgvector 级别的 ANN 向量索引。当前演示索引最多 2000 个分块，资源和竞赛工具只筛选最近 100 条记录。

## 当前边界

- 文本和 Markdown 入库已覆盖；PDF/DOCX、OCR、站点抓取、异步任务、重排序模型、长期会话记忆和写操作审批仍是后续方向。
- 引用是检索得到的真实来源；模型生成的每句话尚未经过独立事实核验。
- 规则评测不是 LLM-as-judge，不宣称语义忠实度或真实业务任务成功率。详细计算口径见 [评测说明](ai-evaluation.md)。
- 数据库保存知识、运行答案和评测问题；当前没有自动过期清理或跨实例全局并发配额。应按开发演示规模使用。
- 真实模型和 embedding 质量、外部服务网络、真实 MySQL 升级与生产部署需要独立环境验收，不能由离线测试替代。

## 开源参考

架构参考 [Spring AI 模块化 RAG](https://docs.spring.io/spring-ai/reference/api/retrieval-augmented-generation.html) 的检索与上下文增强拆分；评测参考 [Ragas Agent metrics](https://docs.ragas.io/en/stable/concepts/metrics/available_metrics/agents/) 和 [Langfuse evaluation](https://langfuse.com/docs/evaluation/overview)。本轮是与现有 Spring Boot 项目融合的独立实现，未安装这些框架或连接其云平台。

规格见 `specs/002-agent-rag-evaluation/`；本轮实际测试结果见 `docs/change-records/2026-10-31-agent-integration.md`。历史文档中的未来路线和旧测试数字不代替这份记录。

Vue 使用 D:/CSA-Project-vue，入口 src/views/admin/AgentView.vue；启动和分支对照见 [Vue 迁移](vue-migration.md)。未标注评测维度显示 N/A。

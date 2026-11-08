# Research: Agent、RAG 与评测

调研日期：2026-10-31。以下为设计参考与选择，不表示已经安装对应框架或验证真实模型效果。

## 检索模块

- **Decision**: 在现有 Java 服务中实现分块、权限过滤、BM25，以及可选 embedding 排序与 RRF 融合；模型上下文只接收已授权片段。
- **Rationale**: 无模型凭据时可演示真实关键词检索；可选向量路径展示召回融合。Spring AI 官方的模块化 RAG、动态过滤和上下文增强提供职责划分参考。[Spring AI RAG](https://docs.spring.io/spring-ai/reference/api/retrieval-augmented-generation.html)
- **Alternatives considered**: 直接引入 Spring AI 全套或单独 Python/向量数据库服务。首期复用现有技术栈，避免将升级框架和新增部署服务变成功能前置条件；不把本地 BM25 称为语义向量检索。

## 工具边界

- **Decision**: 仅 `search_knowledge`、`search_resources`、`search_competitions`，参数校验、身份检查和预算在服务端执行；保存可展示轨迹。
- **Rationale**: 现有资源业务 service 不独立执行 Controller 的 Level 1 限制，Agent 入口必须重新验证权限。知识内容和模型输出均不能授予权限。竞赛工具读取公开字段，不使用后台编辑权限投影。
- **Alternatives considered**: 自由 SQL、任意 HTTP 工具和写业务工具不在本期；它们不是扩展技术点的必要条件。

## 可重复规则评测

- **Decision**: 评测文档 Recall@K、MRR、成功工具名召回、答案关键词覆盖及耗时；未标注的维度是 N/A，失败样本已标注维度为 0；保存原用例与 `rules-v1`。
- **Rationale**: Ragas 区分工具选择/参数/顺序和目标达成，这说明工具名出现并不等于 Agent 完成目标。当前工具召回只是较窄的确定性指标。[Ragas agent metrics](https://docs.ragas.io/en/stable/concepts/metrics/available_metrics/agents/)
- **Alternatives considered**: Ragas 语义评测和 LLM-as-judge 后续可接入，但需要额外模型调用、标注集和校准。本期答案关键词命中不能冒称 faithfulness；后者检查回答中的论断能否由上下文支持。[Ragas faithfulness](https://docs.ragas.io/en/stable/concepts/metrics/available_metrics/faithfulness/)

## 观测与实验

- **Decision**: 持久化运行轨迹和评测报告，保留模型模式、来源、耗时、用量、评分器版本及原数据集；对创建者授权读取。
- **Rationale**: 借鉴 Langfuse 将生产轨迹、数据集、实验和评分分开的工程方式，为后续对比迭代留依据。[Langfuse evaluation](https://langfuse.com/docs/evaluation/overview)
- **Alternatives considered**: 本期不声称集成 Langfuse SDK、OpenTelemetry 导出、自动 CI 质量门禁或线上持续评测。数据库报告仅是本应用可复查记录。

## 已消除的不确定项

采用现有认证和 Level 1/3 权限；文本录入；默认 offline；live 显式配置；只读白名单；有界同步评测；不接入真实生产数据。模型供应商、实际费用和效果由部署者配置后单独验收。

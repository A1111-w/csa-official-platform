# Agent 评测

入口是 `/dashboard/ai` 的评测页。部长及以上（Level 3）提交带标注的 JSON 数据集，服务对每个问题真实调用与问答页相同的 `AgentService.run`。默认离线模式不会调用外部模型；启用 live 后评测会消耗模型额度，失败不会自动降级为离线成功。

每批 1–5 个用例，每条问题不超过 2000 字符；每条至少有一种预期。文档 ID 和答案关键词最多各 20 项，工具名最多 3 项。三个列表均须提供，可用空数组表示不评价该维度。

```json
{
  "cases": [
    {
      "question": "RAG 如何组合检索与生成？",
      "expectedDocumentIds": [],
      "expectedTools": ["search_knowledge"],
      "expectedAnswerTerms": ["检索", "生成"]
    }
  ]
}
```

先在知识库录入明确包含答案的测试文档，再将实际文档 ID 填入 `expectedDocumentIds`。不要填虚构 ID，也不要拿同一批开发用例当成独立测试集来报告模型质量。建议分开保留开发集和未参与调试的验收集。

## 指标定义

| 字段 | 计算方法 |
| --- | --- |
| `recallAtK` | 返回引用中命中的唯一预期文档数 / 唯一预期文档总数 |
| `reciprocalRank` | 返回引用按文档去重后，第一个相关文档的排名倒数；未命中为 0 |
| `toolRecall` | 实际成功执行的预期工具数 / 唯一预期工具总数 |
| `answerTermCoverage` | 答案中出现的唯一预期关键词数 / 唯一关键词总数；忽略大小写 |
| `latencyMs` | 单条实际执行墙钟耗时 |
| `passed` | 所有有标注的召回率、工具召回率、关键词覆盖率均为 1；MRR 用于诊断，不要求首位命中 |

未标注维度返回 `null`，聚合均值只计算有标注的用例；不能把没有标注当作满分。运行失败的有标注维度计 0，该条失败不阻止其余条执行。执行预算耗尽的用例强制失败，即使已调用预期工具。返回引用可能汇总多次知识检索，因此这里的 Recall@K/MRR 衡量**最终返回证据列表**，不是所有底层检索调用的逐次平均。

这些都是可重复的规则指标。`toolRecall` 不检查工具参数、顺序和多余调用，不等同 Ragas ToolCallAccuracy；关键词覆盖率不代表语义正确性、忠实度、事实正确性或任务成功率。离线摘录可能具有很高的关键词覆盖率，因此报告必须连同 `mode` 解读，不可用离线分数宣传模型效果。

## 接口与可追踪性

- `POST /api/ai/evaluations`：Level 3；现有限流器每 IP 每分钟 2 次；返回统一 `R<Report>`。
- `GET /api/ai/evaluations/{id}`：Level 3 且报告创建者；其他用户得到 404。
- 报告保存 `evaluatorVersion=rules-v1`、原始 `dataset`、逐条 `runId`、错误与分数。运行详情记录具体模型、引用及执行步骤。
- V8 新建 `ai_evaluation`，通过 `owner_id` 隔离；报告只保存问题、预期与评测结果，不复制答案和引用正文。原始问题仍属于用户数据，应纳入数据库访问与备份管理。
- 外部错误仅返回脱敏摘要，不把提供商响应体、SQL 或凭据写入报告。

本轮没有接入外部评测 SaaS、LLM-as-judge 或大规模异步任务系统。批量评测同步执行，长请求需要反向代理允许足够超时；页面下载报告便于保存和比较。若服务器仍在执行时浏览器超时，避免立即重复提交造成重复消耗。

## 参考实现与测试

设计参考 [Ragas Agent 指标](https://docs.ragas.io/en/stable/concepts/metrics/available_metrics/agents/)对工具行为和任务目标的区分，以及 [Langfuse evaluation](https://langfuse.com/docs/evaluation/overview) 的数据集、执行记录和实验关联方式。本项目是 Java 规则评测实现，没有安装或声称接入上述项目。

`EvaluationMetricsTest` 覆盖去重、MRR、无标注、无命中和严格通过条件；`EvaluationServiceTest` 使用 H2 验证报告持久化、数据集快照、用户隔离、失败隔离与服务层权限。完整验证结果见本轮修改记录。

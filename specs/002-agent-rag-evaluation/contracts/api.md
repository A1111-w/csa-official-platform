# Agent API contract

基路径 `/api/ai`，沿用 `R<T>` 成功外壳、现有错误状态和 traceId。浏览器使用 HttpOnly Cookie 与现有 axios/CSRF；接口不返回模型密钥。

| Method and path | Minimum role | Request / response |
| --- | --- | --- |
| `GET /status` | Level 1 | `{mode,model,embeddingMode}` |
| `GET /documents` | Level 1 | 可见文档数组，每项 `{id,title,source,minRoleLevel,chunkCount}` |
| `POST /documents` | Level 3 | `{title,content,source,minRoleLevel}` → 文档摘要；每 60 秒 5 次 |
| `DELETE /documents/{id}` | Level 3 + 可管理范围 | 删除知识及其可检索片段 |
| `POST /runs` | Level 1 | `{question}` → RunResult；每 60 秒 10 次 |
| `GET /runs/{id}` | Level 1 + owner | RunResult |
| `POST /evaluations` | Level 3 | `{cases:[...]}` → Report；限流每 60 秒 2 次 |
| `GET /evaluations/{id}` | Level 3 + owner | 保存的 Report |

RunResult：`id,question,answer,mode,model,citations,steps,durationMs,promptTokens,completionTokens`。引用含 `documentId,chunkId,title,source,text,score`；步骤含 `name,status,durationMs,detail`。问题与答案同存，便于回查；轨迹是工具执行记录，不是模型内部思维。

Case：`question,expectedDocumentIds,expectedTools,expectedAnswerTerms`。Report 包含 `id,mode,total,passed,passRate,meanReciprocalRank,meanRecallAtK,meanToolRecall,meanAnswerTermCoverage,averageLatencyMs,results,evaluatorVersion,dataset`，mode 为 `offline/live/unknown/mixed`。每条结果含 `question,runId,passed,recallAtK,reciprocalRank,toolRecall,answerTermCoverage,latencyMs,error`；失败时 runId 可为 null，各项未标注指标可为 null。

工具白名单为 `search_knowledge`、`search_resources`、`search_competitions`。输入上限见 [data-model.md](../data-model.md)。检索来源 score 是排序值，不是事实正确率；响应里的模式必须与实际运行一致。不存在、无权、非法输入和上游失败按现有错误协议区分。

## Rules-v1 scoring

- Recall@K：最终累积返回证据中的相关文档数 / 标注的相关文档数；文档 ID 去重。每次检索最多 5 个片段，但多次工具查询会累积证据，此指标不表示每次 top-5 的召回率。
- Reciprocal rank：第一个相关文档在最终证据首次出现顺序中的排名倒数；无命中为 0。MRR 为有文档标注样本的均值，不是各次检索 MRR 的均值。
- Tool recall：成功执行的预期工具名数 / 预期工具名数；不衡量参数正确、顺序或额外工具成本。
- Answer-term coverage：回答中出现的预期关键词数 / 预期关键词数；是字面覆盖，不是语义正确性或 faithfulness。
- 无对应标注的维度返回 null，聚合均值排除 N/A；执行失败时已标注维度为 0。
- 通过要求已标注的文档召回、工具召回和关键词覆盖均为 1，且没有执行错误；MRR 与耗时仅报告，不参与通过门槛。

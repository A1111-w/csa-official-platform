# Data model

设计与当前 `AiModels.java`、`EvaluationModels.java` 契约对齐；具体 SQL 类型、索引及生命周期以新 V7/V8 migration 和实现为准。

| Entity | Fields and relationship | Validation / lifecycle |
| --- | --- | --- |
| Knowledge document | id、title、content、source、minRoleLevel、创建者与时间；一对多 chunks | `title` 非空且最多 200 字符；`content` 非空且最多 50000 字符；`source` 最多 500 字符；`minRoleLevel` 为 1–99，并不得超过录入者等级 |
| Knowledge chunk | 文档 id、片段 id、顺序、文本、可选 embedding | 源文档与片段一并持久化；删除后不参与检索；授权从文档继承，不允许调用方自行降低等级 |
| Agent run | id、owner、question、answer、mode、model、citations、steps、durationMs、token 用量 | `question` 非空且最多 2000 字符；mode 为 offline/live；按创建者读取，轨迹不存储模型内部思维或配置密钥 |
| Evaluation case | question、expectedDocumentIds、expectedTools、expectedAnswerTerms | 每批 1–5 条；问题非空且最多 2000 字符；三个数组必填；文档最多 20 项，每项 100 字符；工具最多 3 项且属于白名单；词最多 20 项，每项 200 字符；至少一项预期非空 |
| Evaluation report | id、owner、mode、结果、均值、passRate、dataset、evaluatorVersion | 报告持久化；owner-only 且 Level 3；mode 为 offline/live/unknown/mixed，全失败为 unknown；评分器版本 rules-v1；未标注维度为 null |

文档创建是“校验 → 切分/可选向量 → 原子保存”；任何步骤失败不得暴露半份文档。原文以片段保存，没有独立正文回读接口。运行记录按创建者读取，且重新验证引用文档当前可见性；来源已删除或降权后不可见时，整条记录返回 404。评测逐条隔离失败，保存原数据集；不允许用他人的 run/report id 绕过身份限制。不承诺后台任务状态机或多轮会话。

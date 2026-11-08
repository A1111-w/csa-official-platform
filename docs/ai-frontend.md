# Agent 实验室前端

入口为 `/dashboard/ai`，侧栏名称为“Agent 实验室”。本分支使用 Vue 3 SFC、Vue Router、Pinia 与原生控件，继续复用 `src/lib/axios.ts` 和原有 AI API。React 实现保留在独立分支。

## 可演示流程

1. 使用会员及以上账号进入工作台。页面请求 `/api/ai/status` 和 `/api/ai/documents`，显示服务端返回的运行模式与当前身份可见的资料数量。
2. 部长及以上可切换到“知识库”，填写资料，或从本地 `.md` / `.txt` 文件填入正文。“填入演示资料”只填写表单，点击“加入知识库”才会提交。
3. 在“问答”中提问，等待 `/api/ai/runs` 返回。页面展示回答、原文引用、检索分数、步骤状态、耗时和 Token 用量；引用来源按普通文本展示，不自动执行或跳转模型输出。
4. 在“评测”中填写 1–5 个用例的 JSON 数组。先保存至少一份资料，“填入用例示例”才可点击，并使用第一份当前可见资料的 ID；使用者应根据具体测试目标修改问题和期望值。
5. 运行评测后展示逐条结果与汇总指标，并可导出包含实际提交数据集和报告的 JSON。没有标注的指标显示 `N/A`，不是 100%。答案关键词匹配不等于语义忠实度或答案正确性。

离线模式明确标注“离线摘录”；页面不模拟 LLM 输出，也不在客户端生成评分。每次问答是独立单轮，不把页面之前的回答自动作为记忆发送。

## 如何解释演示结果

先用同一个问题完成“无资料 → 加入资料 → 再次提问”的对照，观察回答和引用变化。然后在评测数据集中保留该文档 ID，添加答案中必须出现的关键词，例如 `Spring Boot`。再次运行时，查看检索是否命中、是否使用期望工具、答案是否覆盖标注词。

- 引用中的分数属于检索排序分数，不是“模型有多少把握”，不同检索配置的分数不应直接比较。
- Recall@K 反映期望文档在返回引用中的覆盖程度；MRR 反映第一份相关文档的位置。
- 工具召回率只统计成功执行的工具。工具被调用不代表答案内容正确，因此有内容目标的用例还应标注文档或关键词。
- 在同一版本、同一数据集和相同模式下比较报告。离线摘录的高关键词覆盖率不能用来证明在线模型生成质量。

## 权限和请求

| 行为 | 前端最低等级 | 接口 |
| --- | ---: | --- |
| 状态、可见资料、问答、运行详情 | 1 | `GET /api/ai/status`、`GET /api/ai/documents`、`POST /api/ai/runs`、`GET /api/ai/runs/{id}` |
| 添加、删除资料 | 3 | `POST /api/ai/documents`、`DELETE /api/ai/documents/{id}` |
| 批量评测、报告详情 | 3 | `POST /api/ai/evaluations`、`GET /api/ai/evaluations/{id}` |

浏览器复用 HttpOnly Cookie 与 CSRF 请求封装。菜单和按钮限制只影响展示，真实访问由后端判断。资料输入最多 30,000 字符；本地文本文件最多 150 KB。问答最长 2,000 字符。评测每个期望数组最多 20 项，每条用例至少标注一个维度。问答和建索引超时 180 秒，批量评测超时 600 秒，不自动重试 POST。请求超时不等于服务端已取消任务，重新提交可能再次调用模型。

## 代码与验证

- 页面：`src/router/index.ts`。
- 工作台：`src/views/admin/AgentView.vue`。
- 请求契约与数据集解析：`src/services/ai.ts`。
- Vitest：`src/services/ai.test.ts`，覆盖异常数据集、缺失标注、批次限制与不重试模型请求。
- Playwright：`e2e/agent-lab.spec.ts`，使用隔离的 API fixtures 检查会员权限展示、引用与轨迹、评测校验、N/A 指标和导出。此测试不证明真实模型或数据库联调成功。

在 `csa-official-frontend` 执行 `npm run test`、`npm run lint`、`npm run build`，以及 `npx playwright test e2e/agent-lab.spec.ts`。本次实际结果见 [Vue 集成记录](change-records/2026-11-08-vue-integration.md)。

Vue 分支独立安装依赖，开发使用 npm run dev，生产构建使用 npm run build，不使用 Webpack 参数或依赖 junction。

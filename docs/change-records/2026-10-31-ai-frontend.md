# 20261031-ai-frontend | Agent 实验室前端

- 日期：2026-10-31（Asia/Shanghai）。
- 执行 agent：Codex `/root/frontend`；主 agent 集成后端、评测契约与共享文档。
- 关联需求：为 CSA 项目加入 Agent、RAG、评测等技术展示能力；基于 `codex/development` 的隔离工作目录实现。
- 状态：实现、单测、lint、生产构建与文档已完成；浏览器验收被自动审批阻断，未发布。
- 实际变更：新增 `/dashboard/ai` 与侧栏入口；会员问答、来源折叠、执行步骤、模式与用量；部长知识资料维护、纯文本文件填入、评测数据集校验、报告指标和 JSON 导出。页面不伪造结果，不把未标注指标算作满分。
- 涉及代码：`csa-official-frontend/src/app/dashboard/ai/page.tsx`、`src/components/business/ai/AgentWorkspace.tsx`、`src/services/ai.ts`、`src/services/ai.test.ts`、`src/config/menu.ts`、`e2e/agent-lab.spec.ts`。
- 文档同步：`docs/ai-frontend.md` 与 `csa-official-frontend/README.md`；共享 `CHANGELOG.md` 条目由主 agent 集成本记录。
- 兼容性/数据/配置：没有新增前端依赖或修改 lockfile；复用 Cookie、CSRF、错误封装和用户状态。新增 API 来自同批后端实现，不兼容没有 AI API 的旧服务端。
- 验证执行目录：`D:/CSA-Project-agent/csa-official-frontend`。
- 初始环境情况：`npm ci --no-audit --no-fund` 访问 npm registry 时遇到 `ECONNREFUSED`；离线安装未正常完成。复制依赖因磁盘 I/O 过慢中止，最终通过本地 junction 复用已有 `node_modules`；未复制 `.env`，未修改原工作区源代码。32 个直接依赖以及全部 506 个已安装依赖的版本均与当前 lockfile 一致，104 个平台可选依赖未安装，没有必需依赖缺失；没有改变依赖声明。
- 验证：`npm run test` 通过，8 个文件、28 个用例（本功能新增 15 个）；`npm run lint` 通过，0 错误、1 条既有 `src/lib/axios.ts:161` 导航 warning。`npm run build -- --webpack` 完整通过，退出码 0，27/27 页面，包含 `/dashboard/ai`；编译、TypeScript、页面数据与构建追踪全部完成，构建器自动重试了一个慢的 `/_global-error` 页面。默认 Turbopack 构建因本地 junction 指向项目文件系统根目录之外而失败；没有为规避本地环境修改应用配置。
- 浏览器验证：未执行。为运行隔离的浏览器用例，尝试在本工作目录启动 `npm run start -- --hostname 127.0.0.1 --port 3000`，自动审批返回 `blocked by policy`，未提供更具体理由。未通过替代启动方式绕过该拒绝，没有留下测试服务或截图。`e2e/agent-lab.spec.ts` 为 API fixtures 测试，后续通过后也不作为真实模型或数据库联调证据。
- 风险与遗留：文本导入支持 Markdown/TXT；问答为单轮；网络超时不能取消服务端模型调用；评分关键词覆盖率不代表语义正确性。真实模型及真实后端 E2E 需配置对应服务。
- 回退：回退本次前端相关源码、菜单和文档；保留后端已应用的向前迁移，不覆盖原工作区用户修改。

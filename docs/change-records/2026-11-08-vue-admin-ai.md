# 20261108-vue-admin-ai — Vue 3 管理页面与 Agent 实验室迁移

- 日期：2026-11-08（Asia/Shanghai）。执行 agent：Codex `vue_admin_ai`。
- 关联需求：用户要求将前端迁移 Vue 3、保留 React 分支并上传 GitHub。
- 范围：`src/views/admin/` 七个 SFC、共享下载/错误 helper、Agent 组件测试，`e2e/agent-lab.spec.ts` 两项浏览器用例；使用说明 `docs/vue-admin-ai.md`。共享路由、axios、Pinia、依赖和 CHANGELOG 由主 agent 集成。
- 行为：保留原 Agent 问答/引用/工具轨迹、知识导入/删除、规则评测和报告导出；审计、贡献登记、名单导出、轮播、个人资料与账户设置沿用原 API、权限和交互闭环。
- 差异：审计详情改为原生展开区域；表单采用原生控件；Agent 标签页支持键盘切换；未标注评测维度为 N/A；公开介绍加载失败时禁止保存，防止空内容覆盖。
- 视觉复查：Agent 标题区、指标卡、标签页和引用边框使用全局主题变量，跟随 `data-theme=dark`；贡献选中状态使用同一强调色。浏览器截图前回到顶部并断言 scrollY 为 0，避免 sticky 导航出现在长截图中段。
- 兼容性：无后端 API/数据库配置变化。JWT 不落浏览器存储；由共享请求模块继续处理 Cookie/CSRF。无真实账号、生产或模型调用。
- 验证：新增 6 项 Agent 与 3 项设置组件测试；后者覆盖加载失败不覆盖公开介绍、删除取消不调用接口、富文本清洗。执行结果由下方补记。迁移 2 项 Playwright 用例，移除 localStorage 角色模拟，改由 `/api/sys/user/info` fixture 返回会话；增加问答/评测 POST 的 CSRF header 断言、无效 JSON 不发请求、N/A 与 JSON 下载内容核对。Vue 编译、浏览器与完整回归由主 agent 汇总，未运行的真实服务验证不标为通过。
- 风险：页面原生 HTML 实现改变视觉细节；需要在隔离环境确认真实上传、下载与账户生命周期。无线上部署。
- 回退：切换至保留的 React 分支 `codex/agent-rag-evaluation`，不回滚数据库迁移或删除数据。

## 实际验证

2026-11-08，在 `D:/CSA-Project-vue/csa-official-frontend` 执行：

```powershell
npm.cmd test -- src/views/admin --maxWorkers=1
```

Vitest 4.1.10：2 个测试文件、9 个测试全部通过，首次耗时 5.10 秒；主题适配修改后同一命令复跑仍为 9/9，通过耗时 2.67 秒。测试使用隔离 mock 与 jsdom，没有接触真实服务。Vue 类型检查、全量 ESLint、生产构建和 Playwright 的最终结果由主集成记录汇总；本记录不将定向组件测试代替这些检查。

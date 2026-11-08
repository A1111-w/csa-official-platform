# Vue 3 业务工作台

本分支将业务页面迁入 `csa-official-frontend/src/views/business/`。请求仍经 `src/services/` 和统一 axios 客户端发送，业务数据和权限由原 Spring Boot 接口管理。React 对照版本保留在 `codex/agent-rag-evaluation` 分支。

| 页面 | 路径 | 迁移后的操作 |
| --- | --- | --- |
| ResourcesView | `/resources`、`/dashboard/resources` | 会员查看分类、按每页 8 条分页、打开资源并统计下载；工作台部长可上传、新增、编辑、删除，删除有确认 |
| CompetitionsView | `/competitions`、`/dashboard/competitions` | 公开列表和详情；管理页发布、读取完整详情后编辑、封面上传、日期校验；按后端 `canEdit` / `canGrant` 展示编辑与协作者授权 |
| ResumeView | `/dashboard/resume` | 核心成员保存草稿、先保存再提交、查看审核结果、仓库同步与状态刷新；同步中每 2 秒轮询，离开页面取消后续轮询 |
| ResumeReviewsView | `/dashboard/resume-reviews` | 部长按状态筛选、每页 12 条、阅读完整简历与仓库信息、通过/驳回；驳回必填原因，审核失败后重读最新状态 |
| VoteView | `/dashboard/vote` | 部长创建三类提案、查看结果、赞成/反对并附说明；已结束及本次页面会话内投过票的提案不显示投票按钮 |
| DepartmentsView | `/dashboard/departments` | 会长选择部门、搜索已加载的成员、确认后任命；禁止任命会长/Root、其他部门在任部长、重复任命当前负责人 |

## 交互与维护

- 页面的异步加载、空状态、错误和重试分别呈现。保存中禁用重复操作；删除资源和人事任命使用浏览器确认框。
- 使用有标签的原生表单控件，键盘可操作；完整详情放在当前页内，避免自制弹窗遗漏焦点管理。
- 列表页仍使用后端 `records`、`pages`、`total`。删除最后一页的最后一项时重新计算末页并补发查询。
- 竞赛编辑必须调用详情接口，不用摘要覆盖正文；详情返回的 `canEdit` 和状态码再次核对，未知状态不允许编辑。草稿的 `status: 0` 会保持为未发布。
- 展示的比赛 HTML 经共享 `sanitizeHtml` 清洗；简历正文作为纯文本显示，不执行 HTML。
- 简历保存和提交均校验 HTTPS 仓库地址，链接不含用户信息、查询参数或 fragment，最长 255 字符，与后端一致。待审核和未知状态禁止编辑，仓库同步的权限和白名单由后端继续校验。
- 部长仅能编辑/删除自己上传的资源，会长及以上可维护全部资源；判断依赖当前会话的服务器资料。审核驳回反馈最长 500 字符。
- 列表中的成员检索沿用原接口上限：比赛授权最多载入 120 人，部门最多载入 150 人。部门页说明了搜索范围；这不是全库分页搜索。
- 页面角色判断仅是交互入口。后端权限、账号状态、会话版本、文件归属和业务并发校验继续执行。

## 验证

教学时建议先只打开资源页，对照一条完整链路：选择分类 → `loadResources(1)` → service 请求 → 后端分页结果 → `items` 更新 → 列表渲染。先确认这一条链路能跑通，再引入权限、上传和删除，不要求首次授课同时讲六个模块。

| React 对照概念 | 当前 Vue 写法 | 学员应观察的现象 |
| --- | --- | --- |
| `useState` | `ref`，模板自动解包 | 改分类后请求参数与列表同步变化 |
| `useMemo` | `computed` | 部门候选成员搜索时列表随输入过滤 |
| `useEffect` | `watch` 和组件生命周期 | 用户身份可用后才请求受限数据；离开简历页停止轮询 |
| JSX 事件 | `@click`、`@submit.prevent`、`v-model` | 表单保存后保留正确字段，重复提交按钮被禁用 |

老师可先提供正常、空列表、失败三种测试数据，让学员分别说明界面应该出现什么；若接口不可用，先运行下面的组件测试核对交互，不将接口故障归因于 Vue 响应式。

组件测试位置为 `src/views/business/business.test.ts`，覆盖受限资源不加载、分类分页、比赛全文编辑、审核锁定、HTTPS 提交校验、驳回原因、投票防重复及任命限制。命令：

```powershell
cd csa-official-frontend
npm test -- src/views/business/business.test.ts
npm run typecheck
```

实际执行状态和浏览器验证范围以 [迁移记录](change-records/2026-11-08-vue-business.md) 及主迁移记录为准。测试不连接生产接口。

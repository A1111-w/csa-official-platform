# 20261108-vue-business | 六个业务工作台迁移 Vue 3

- 日期：2026-11-08（Asia/Shanghai）。
- 执行 agent：Codex 子 agent `/root/vue_business`；主 agent 负责框架、服务适配、路由及 Git 集成。
- 关联需求：前端迁移 Vue 3，保留 React 分支并上传 GitHub；规格由主 agent 统一维护。
- 状态：源码、12 项定向组件测试和文档已完成；全局构建及浏览器验收由主集成记录汇总。
- 背景：完整迁移资源、比赛、简历提交/审核、提案投票、部门任命工作流。
- 变更：新增六个 Vue SFC，沿用原 service 类型和接口；保留角色显示门槛、服务器分页、异步状态及失败反馈。比赛读取全文后编辑；简历草稿和提交都校验 HTTPS；审核失败重读状态；角色任命显示前置限制。
- 文件：`csa-official-frontend/src/views/business/{Resources,Competitions,Resume,ResumeReviews,Vote,Departments}View.vue`；同目录 `business.test.ts`；`docs/vue-business.md`。
- 文档同步：业务页面/角色/接口工作流、成员列表上限和验证命令已记录于 `docs/vue-business.md`。共享 CHANGELOG 由主 agent 汇总此记录编号。
- 兼容性/数据/配置：不改后端、数据库或 service 方法签名。详情由 React 对话框改为页内阅读区。公开竞赛新增完整详情阅读。React 版本保存在独立分支。
- 验证：已逐项阅读原 React 业务逻辑、service API 与 Resource/Competition/Resume 后端校验。2026-11-08 在 `D:/CSA-Project-vue/csa-official-frontend` 执行 `npm test -- src/views/business/business.test.ts --maxWorkers=1`，1 个测试文件、12 个测试全部通过（exit 0）；覆盖权限、分类分页、全文编辑、审核中及未知状态锁定、HTTPS 校验、驳回原因、投票和任命限制。另通过 `@vue/compiler-sfc` 对 6 个 SFC 的 parse / compileScript / compileTemplate 检查。全局类型、lint、构建和浏览器结果以主集成记录为准；上述组件测试使用隔离 mock，不证明真实接口联调已通过。
- 风险与遗留：完整浏览器视觉/键盘验收、真实登录角色与接口联调需要集成环境；成员搜索仍只涵盖一次加载的 120/150 人。
- 回退：切换 React 保留分支；若在 Vue 分支回退，使用新 revert 提交，不覆盖 React 原目录或用户已有修改。

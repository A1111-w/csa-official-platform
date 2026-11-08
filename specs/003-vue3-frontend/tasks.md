# Tasks: Vue 3 frontend migration

## Setup

- [x] T001 保存 React bf1415c 提交与独立 Vue worktree，记录于 specs/003-vue3-frontend/spec.md。
- [x] T002 生成 specs/003-vue3-frontend/ 的需求、计划、契约与质量清单。

## Foundation

- [x] T003 替换 csa-official-frontend/package.json、package-lock.json 与 Vite/TypeScript 配置。
- [x] T004 实现 csa-official-frontend/src/store/useAuthStore.ts、src/router/、src/lib/axios.ts 的会话、权限和 CSRF 适配。
- [x] T005 实现 csa-official-frontend/src/App.vue、主布局、共享样式和通知。

## US1: Independent branch and public access

- [x] T006 [P] [US1] 迁移 csa-official-frontend/src/views/public/ 公开页面、加载/错误/重试和 HTML 净化。
- [x] T007 [P] [US1] 迁移 csa-official-frontend/src/views/auth/AuthView.vue，保留用户名 4–20、注册密码 6–20、重置密码 8–64、验证码 6 位约束及隐私版本。
- [x] T008 [US1] 验证认证/重定向/隐私版本并更新 docs/vue-public-auth.md 与 docs/change-records/2026-11-08-vue-public-auth.md。

## US2: Business workflows

- [x] T009 [P] [US2] 迁移 csa-official-frontend/src/views/business/ 的资源、竞赛、简历、审核、投票、部门。
- [x] T010 [P] [US2] 迁移 csa-official-frontend/src/views/admin/ 的审计、贡献、导出、轮播、资料和设置。
- [x] T011 [US2] 核对 src/router/ 路由与权限清单，并验证工作台业务操作。
- [x] T012 [US2] 更新 docs/vue-business.md、docs/vue-admin-ai.md 及对应 change-records。

## US3: Agent and teaching delivery

- [x] T013 [P] [US3] 迁移 csa-official-frontend/src/views/admin/AgentView.vue 的问答、知识库、评测和导出。
- [x] T014 [US3] 验证 AI 角色隔离、引用、空知识库与 N/A，记录 csa-official-frontend/ 测试结果。
- [x] T015 [US3] 更新 docs/agent-learning-plan.md、docs/ai-frontend.md 与 docs/study-and-demo-guide.md 的 Vue 路径及教学准备。

## Integration and delivery

- [x] T016 更新 csa-official-frontend/Dockerfile、docker-compose.yml、.github/workflows/ 与相关部署配置。
- [x] T017 更新 README.md、csa-official-frontend/README.md、docs/project-status.md、docs/local-dev.md、docs/deployment.md、docs/architecture.md、docs/security-design.md、docs/module-completeness.md、CHANGELOG.md 及主记录。
- [ ] T018 执行前端测试、类型检查、lint/build、tools/check-study-guide-drift.ps1 和 git diff --check，记录真实结果。
- [x] T019 执行隔离浏览器验收并记录截图/限制，不将失败或未执行写成通过。
- [ ] T020 审阅显式暂存 diff 后提交，将两个用户授权分支推送 GitHub 并核对远程提交。

## Dependencies and parallel execution

T001→T002→T003–T005；US1/US2/US3 页面可在共享服务契约确定后按不同目录并行。各故事文档与测试随实现完成；T016–T020 汇总验证，不能跳过未完成项。MVP 为 US1，完整交付仍须全部故事。

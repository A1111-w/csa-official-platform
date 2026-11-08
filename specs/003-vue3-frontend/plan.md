# Implementation Plan: Vue 3 前端迁移

**Branch**: `codex/vue3-frontend` | **Date**: 2026-11-08 | **Spec**: [spec.md](spec.md)

## Summary

基于保留 React 的 bf1415c 提交迁移整个前端。复用 TypeScript service 和后端，Vue SFC 重写页面，路由、会话、错误处理集中管理。

## Technical Context

- Language: TypeScript，Vue SFC Composition API。
- Dependencies: Vue 3.5.45、Vue Router 4.6.4、Pinia 3.0.4、Vite 8.3.6；精确版本由 package-lock.json 确定，Node >=22.12。
- UI: 原生 HTML 表单/表格、响应式 CSS，无 React 运行时。
- Storage: 身份事实由后端 Cookie 会话恢复；不存储 Token，无数据库改动。
- Testing: Vitest 服务/会话/权限/表单，vue-tsc 类型检查，生产 build，隔离浏览器 fixtures。
- Deployment: Vite 静态 dist，深层 URL fallback，/api 反向代理；既有后端不改动。
- Performance goal: 路由懒加载、按需加载工作台，不新增轮询和外部遥测。

## Constitution Check

- 依据现有 service、Controller 和 React 页面迁移，不虚构接口。
- 每个子任务包含说明和独立变更记录，由主 agent 整合 CHANGELOG。
- 独立工作区 D:/CSA-Project-vue；React 工作区保留，原 main 用户改动不接触。
- 用户明确授权 Vue 3，覆盖保持 Next.js 的旧默认规则；Cookie/CSRF/后端授权仍为硬性边界。
- 只提交源码及 npm lockfile，不提交 secrets/node_modules/dist/日志。
- 验证结果仅在执行后记为通过；GitHub 推送已获用户明确授权。

## Project Structure

- src/views/public: 首页、介绍、贡献者、隐私。
- src/views/auth: 登录、注册、忘记密码、重置密码。
- src/views/business: 资源、竞赛、简历、审核、提案、部门。
- src/views/admin: AI、审计、贡献、导出、轮播、资料、设置。
- src/router, src/store, src/lib: 路由权限、Pinia 会话、共享 HTTP、安全与通知。
- src/services, src/types: 既有 API 类型和服务，修正框架耦合。
- specs/003-vue3-frontend: spec/plan/tasks/contracts/research/quickstart。

## Delivery Order

1. 保留 React 提交，创建 Vue 分支；创建规格。
2. 基础框架与服务适配；页面按 public/business/admin 文件边界并行迁移。
3. 汇总路由清单和测试；更新 Docker/CI/文档/教学说明。
4. 类型检查、测试、构建、浏览器验收；审查后提交和推送两个分支。

## Complexity Tracking

无新增后端框架、数据库或服务。采用 Vite SPA；公开页面 SSR/SEO 行为与 Next 有区别，部署和回退文档需说明。

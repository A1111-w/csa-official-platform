# CSA Official · Vue 3 版

计算机协会官网与内部管理平台：公开官网、成员工作台、权限管理，以及带知识库检索、工具调用与规则评测的 Agent 实验室。后端沿用 Spring Boot，当前分支前端迁移为 Vue 3。

## 两个可独立运行的版本

| 版本 | Git 分支 | 本机工作目录 |
| --- | --- | --- |
| Vue 3 | `codex/vue3-frontend` | `D:/CSA-Project-vue` |
| React / Next.js | `codex/agent-rag-evaluation` | `D:/CSA-Project-agent` |

React 基线提交为 `bf1415c`，包含 Agent/RAG/规则评测完整源码。Vue 从这个提交继续开发；原 `D:/CSA-Project` 用户改动保持原样。两分支共享后端接口和 Flyway V1–V8，不需要为前端切换清库或修改业务数据。

## 功能

- 公开首页、协会介绍、贡献者、隐私说明、竞赛活动、会员资源入口。
- 登录、注册、邮箱验证码、找回密码、重置密码、账号安全与个人资料。
- 资源发布与下载、竞赛和协作者管理、简历编辑及审核、提案投票、部门任命。
- 审计查询、贡献记录、成员 Excel 导出、首页轮播与公开内容维护。
- Agent 问答、来源引用、工具轨迹、知识文档导入、角色隔离、规则评测和 JSON 导出。

菜单与路由都按角色展示；真实授权、Cookie JWT、CSRF、实时账号和文件权限由后端执行。前端不持久化 Token 或角色事实。

## 技术栈

| 层 | 技术 |
| --- | --- |
| 页面 | Vue 3.5.45，Composition API，单文件组件 |
| 路由 / 状态 | Vue Router 4.6.4，Pinia 3.0.4 |
| 工具 | TypeScript 5.9.3，Vite 8.3.6，npm lockfile |
| 请求 / 内容安全 | axios 1.21.0，DOMPurify 3.4.17 |
| UI | 响应式原生 CSS、语义化表单、浅色/深色主题 |
| 后端 | Java 17，Spring Boot 3.5.14，MyBatis-Plus，Spring Security |
| 数据 | MySQL，Flyway V1–V8，Redis / 内存缓存 |
| 检验 | Vitest，Vue Test Utils，vue-tsc，ESLint，Playwright |
| 部署 | Vue 静态 dist + Nginx，Caddy HTTPS 入口 |

Vue 版采用客户端 SPA，公开页不再由 Next.js 服务端渲染。静态宿主必须配置深层 URL 回退；生产默认同源 API。需要 SSR/SEO 服务端内容时，可以继续使用保留的 React 分支，或单独规划 Nuxt。

## 本地启动

要求 Node.js >=22.12、npm、JDK 17+，后端数据库使用隔离开发环境。

```powershell
cd D:/CSA-Project-vue/csa-official-frontend
npm ci
Copy-Item .env.example .env.local
npm run dev
```

浏览器访问 `http://127.0.0.1:3000`。`VITE_API_URL=/` 让浏览器访问同源路径，Vite 将 `/api`、`/files` 转到 `API_PROXY_TARGET=http://127.0.0.1:8080`。启动后端和数据库请按 [本地手册](docs/local-dev.md)。没有后端时仍能显示页面外壳和错误提示，但不能登录或演示真实业务。

```powershell
npm run lint
npm test
npm run build
npm run test:e2e
```

浏览器 fixture 用例无需生产账号；真实后端用例需要隔离测试账号，详见前端 README。

## 目录

```text
csa-official-frontend/
  src/views/public/      公开页面
  src/views/auth/        认证表单
  src/views/business/    资源、竞赛、简历、投票、部门
  src/views/admin/       AI、审计、贡献、导出、轮播、设置
  src/router/            路由与会话守卫
  src/store/             Pinia 会话
  src/services/          保留的后端 API 契约
  src/lib/               请求、安全、通知、格式化
  src/App.vue            导航和工作台布局
csa-official-backend/     现有 Java 服务
specs/003-vue3-frontend/  迁移规格与验收任务
docs/                    运维、接口、教学和变更记录
```

## 阅读入口

- [Vue 前端说明](csa-official-frontend/README.md) · [迁移与对照](docs/vue-migration.md)
- [学习与演示指南](docs/study-and-demo-guide.md) · [Agent 教学计划](docs/agent-learning-plan.md)
- [Agent 实验台](docs/agent-lab.md) · [评测指标定义](docs/ai-evaluation.md)
- [接口地图](docs/api-map.md) · [架构](docs/architecture.md) · [安全](docs/security-design.md)
- [部署](docs/deployment.md) · [当前验证状态](docs/project-status.md)
- [变更记录](CHANGELOG.md) · [开发约束](docs/development-constraints.md)

本轮真实验证结果、限制和 GitHub 交付信息见 [Vue 集成记录](docs/change-records/2026-11-08-vue-integration.md)。历史 CI、真实模型质量和生产部署不能由源码迁移推断为已通过。

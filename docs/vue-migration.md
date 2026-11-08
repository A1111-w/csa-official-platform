# Vue 3 迁移与双分支使用

日期：2026-11-08。

React：`codex/agent-rag-evaluation`，基线 `bf1415c`，目录 `D:/CSA-Project-agent`。
Vue：`codex/vue3-frontend`，目录 `D:/CSA-Project-vue`。

两个分支各自具有相同目录名 `csa-official-frontend`，切换分支后必须重新 `npm ci`，不能共用 React 的 node_modules。当前本机使用独立 worktree 避免来回覆盖。原 main 工作目录不受迁移影响。

## 概念对照

| React 版 | Vue 版 |
| --- | --- |
| Next App Router / page.tsx | src/router/index.ts + views/*.vue |
| useState | ref / reactive |
| useMemo | computed |
| useEffect | onMounted / watch / onUnmounted |
| Zustand | Pinia defineStore |
| Next Link / useRouter | RouterLink / useRouter |
| Radix / Tailwind 组件 | 原生语义控件 + src/style.css / scoped CSS |
| NEXT_PUBLIC_API_URL | VITE_API_URL（构建期） |
| next start | Nginx 静态托管，开发预览用 Vite |
| Next proxy Cookie 存在性检查 | Vue Router 先访问后端会话，再进入受保护页面 |
| isomorphic-dompurify | 浏览器 DOMPurify |

原 URL 和服务 API 保留；新增 /forbidden、/session-error 和404兜底页。公开页面改为客户端加载，SSR、页面源代码包含数据与 SEO 行为不同。Vue 版没有声称继续提供 Next.js 服务端渲染。

## 常见故障

| 现象 | 排查 |
| --- | --- |
| Vite 报 Node engine | 升级到 Node >=22.12 |
| 登录或会话检查连接失败 | 先检查8080后端；再检查 API_PROXY_TARGET；不要复制线上.env |
| 刷新 /dashboard/ai 返回服务器404 | 静态服务器缺少 fallback，按 nginx.conf 设置 |
| POST 403 | 检查 Cookie、CSRF header、真实角色；不要移除后端授权 |
| 页面可进但看不到管理按钮 | 对照角色表，AI 管理需3、轮播需4 |
| 评测指标显示 N/A | 该维度未标注，属于 N/A，不是100% |
| 切回 React 后启动失败 | 回到 React worktree并 npm ci，使用该分支 README |

## 教学安排

教师课前分别准备公开、会员和部长账号，先演示成功路径，再按课后挑战加入失败案例。学生只学习当前使用的一套前端。需要讲框架差异时，对照同一个 resourceService.list 请求；不同时讲生命周期、数据库和模型调用三个新主题。每节以可观察结果验收，时长由学员基础决定。

Vue 官方参考：[快速开始](https://vuejs.org/guide/quick-start.html)、[路由守卫](https://router.vuejs.org/guide/advanced/navigation-guards.html)、[Vite](https://vite.dev/guide/)。

## 回退

保留当前 Vue 工作区，在 React 工作目录运行其分支。无需改动数据库、清除上传卷或重写历史；两种前端对接同一后端契约。生产切换仍需独立环境验收，本轮 GitHub 推送不等于生产部署。

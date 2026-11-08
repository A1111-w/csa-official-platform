# CSA Vue 3 前端

当前分支 `codex/vue3-frontend` 使用 Vue 3 + Vite + TypeScript + Vue Router + Pinia。React 版保存在 `codex/agent-rag-evaluation`（`bf1415c`），不是在同一个应用里混装 React/Vue。

## 启动与验证

```powershell
npm ci
Copy-Item .env.example .env.local
npm run dev
```

Node >=22.12，推荐 Node 22 最新 LTS 补丁。默认地址 `http://127.0.0.1:3000`，API 同源，由 Vite 代理到 `http://127.0.0.1:8080`。`API_PROXY_TARGET` 只影响本地开发/预览；`VITE_API_URL` 构建期写入客户端，不得放密钥。镜像中 API 由 Nginx/Caddy 转发。

```powershell
npm run typecheck
npm run lint
npm test
npm run build
npm run start
npm run test:e2e
```

`start` 是 Vite 构建预览；生产使用 Docker/Nginx。Playwright 首次需 `npx playwright install chromium`。浏览器测试默认自己启动 Vite。真实后端测试仅在提供隔离环境 `E2E_USERNAME`、`E2E_PASSWORD` 时执行；其它 fixtures 不依赖外部模型或数据库。

如果本机 npm 旧代理不可达，先核对自己的代理配置。本轮安装使用仅当前命令的 `--proxy=null --https-proxy=null` 直连，没有更改用户或全局 npm 配置。

## 页面与权限

| 路径 | 最低角色 |
| --- | --- |
| /、/about、/contributors、/privacy、/competitions | 公开 |
| /resources | 页面公开，资源内容要求会员 1 |
| /login、/register、/forgot-password、/reset-password | 公开 |
| /dashboard、/dashboard/profile、/dashboard/settings | 已登录 0 |
| /dashboard/resources、/dashboard/ai | 会员 1 |
| /dashboard/resume | 核心成员 2 |
| /dashboard/resume-reviews、/dashboard/competitions、/dashboard/vote | 部长 3 |
| /dashboard/departments、/dashboard/audit、/dashboard/contributions、/dashboard/member-export、/dashboard/carousels | 会长 4 |

AI 知识写入和评测另外要求部长 3；公开介绍写入要求会长 4。直接输入受限 URL 同样经过 `sessionGuard`，菜单隐藏不代替后端权限。

## 会话与安全

- `src/store/useAuthStore.ts` 使用内存 Pinia 状态；不读旧 Zustand 缓存做授权。
- 受保护导航先调用 `/api/sys/user/info`；401 跳登录，角色不足进入403页面，网络故障显示重试页面。
- axios 保留 `withCredentials`、CSRF token 内存缓存、统一错误封装和401处理。
- 公开介绍与比赛 HTML 经 DOMPurify 允许列表清洗，答案和文档文本使用 Vue 插值。
- 生产静态资源不含内联执行脚本；Nginx 设置 CSP、禁止嵌入和其它安全响应头。

服务 URL、参数和接口返回类型保留在 `src/services/`，业务后端无需迁移。完整模块说明见 [公开与认证](../docs/vue-public-auth.md)、[业务](../docs/vue-business.md)、[管理和AI](../docs/vue-admin-ai.md)。

## 与 React 的区别

使用 Vue SFC、ref/computed/watch/onMounted 和 Pinia；页面由 Vue Router 懒加载。采用原生控件与响应式 CSS，视觉重新组织，业务操作继续调用原接口。无 SSR/Next proxy，认证守卫在浏览器执行，数据保护仍由 Spring Security 执行。Nginx `try_files` 保证刷新深层链接可用。

教学先按 [Vue 学习指南](../docs/study-and-demo-guide.md)跑通一个请求，再选择 [Agent 四节核心课](../docs/agent-learning-plan.md)。无需同时掌握两套前端。

若 npm10 修改依赖时报内部 edgesOut 错误，可使用 npm11 生成锁文件；正常使用已提交锁文件执行 npm ci。本轮完整依赖审计无high/critical、保留12项开发工具moderate；生产依赖审计为0。详见集成记录。

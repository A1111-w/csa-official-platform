# CSA Official Frontend

计协官网前端，基于 Next.js App Router。页面分为公开官网和登录后的工作台两部分。

## 技术栈

- Next.js 16.2.12 + React 19.2.4
- TypeScript
- Tailwind CSS
- shadcn/ui 风格组件
- Zustand 登录态
- axios 1.18.1 前后端通信
- `src/proxy.ts` 入口保护和安全响应头
- isomorphic-dompurify 富文本展示清洗

## 页面入口

| 路径 | 说明 |
| --- | --- |
| `/` | 官网首页 |
| `/about` | 协会介绍 |
| `/competitions` | 公开竞赛 |
| `/contributors` | 贡献者墙 |
| `/resources` | 资源入口 |
| `/login` | 登录 |
| `/register` | 注册 |
| `/dashboard` | 工作台概览 |
| `/dashboard/profile` | 个人资料 |
| `/dashboard/resources` | 资源库 |
| `/dashboard/competitions` | 竞赛管理 |
| `/dashboard/resume` | 我的简历 |
| `/dashboard/departments` | 部门人事 |
| `/dashboard/vote` | 提案中心 |
| `/dashboard/settings` | 公开设置 |

`/dashboard/**` 会先经过 `src/proxy.ts`。没有 `CSA_AUTH_TOKEN` Cookie 时会跳转到 `/login?redirect=...`，同时 proxy 会设置 CSP nonce、CSP Header，生产环境还会加 HSTS。

## 本地开发

```powershell
cd D:\CSA-Project\csa-official-frontend
npm install
npm run dev
```

本地联调后端时，设置 `.env.local`：

```properties
NEXT_PUBLIC_API_URL=http://localhost:8080
```

## 构建验证

```powershell
npm run build
npm run test
npm run test:e2e
```

## 前后端契约

- API 基础地址来自 `NEXT_PUBLIC_API_URL`。
- `src/lib/axios.ts` 统一处理 `withCredentials`、CSRF Token 自动获取/注入、401 跳转和错误包装。
- 浏览器前端不保存、不读取、不拼接 Bearer Token；认证 JWT 由后端写入 `CSA_AUTH_TOKEN` HttpOnly Cookie。
- 登录接口响应体只返回 `csrfToken`、`username`、`roleLevel`，`src/store/useAuthStore.ts` 只保存页面渲染需要的用户信息。
- 业务接口集中在 `src/services/*.ts`。
- 菜单权限配置在 `src/config/menu.ts`，真实权限仍以后端为准。

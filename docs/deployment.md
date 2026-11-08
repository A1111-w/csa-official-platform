# Vue 版部署说明

本分支前端为 Vue 3 + Vite SPA，后端与数据库沿用原方案。React/Next.js 部署资料在 `codex/agent-rag-evaluation` 分支；不要把 next start 或 .next 产物用于本分支。

## 服务与构建

| 服务 | 实现 | 端口 / 状态 |
| --- | --- | --- |
| mysql | MySQL 8，Flyway V1–V8 | 3306，生产仅data网络 |
| redis | Redis 7 | 6379，生产仅data网络 |
| backend | Spring Boot / Java17 | 8080 |
| frontend | Vite build → Nginx 静态 dist | 3000 |
| caddy（生产） | HTTPS入口、同源反向代理 | 80/443 |

前端 Dockerfile 用 Node22构建，最终使用 unprivileged Nginx。生产不需要 Node运行时、React、Next服务或 npm。`nginx.conf` 设置 history fallback、静态文件服务、CSP与安全头。

## 本地容器

```powershell
docker compose config
docker compose up --build
```

默认不导入演示数据；只在隔离开发环境按现有 seed 文档启用 DEMO_SEED_ENABLED 并设置临时口令。不能通过重置卷解决应用升级问题。MySQL/Redis健康后启动后端，再提供前端页面。

浏览器访问 http://localhost:3000。开发容器的 Nginx 将 /api 和 /files 转发到 backend:8080；本地 Vite开发服务则使用 API_PROXY_TARGET。

## 生产

使用 `compose.production.yml` 与 `.env.production.example`，按 [生产就绪资料](production-readiness/runbook.md) 准备隔离staging、密钥、邮件、数据库、备份和恢复验证。Compose对缺少的生产变量会快速失败。

Caddy按以下路径路由：

```text
/api/*、/files/* → backend:8080
其它路径        → frontend:3000 → index.html / assets
```

生产 TLS、HSTS 由 Caddy提供；静态 Nginx提供 CSP、frame-ancestors none、X-Frame-Options DENY等。Spring Security仍负责 API授权、HttpOnly Cookie、CSRF、实时账号和文件访问，不依赖浏览器路由保护数据。

AI评测最多多次执行，Nginx开发同源API代理的读取超时为620秒，与前端600秒评测请求上限匹配；单次Agent后端仍有自己的调用预算。

## 构建期变量

`VITE_API_URL=/` 是默认和推荐容器配置，构建时内联进浏览器bundle。运行期改变同名变量不会改变已生成页面。镜像构建参数：

```powershell
docker build --build-arg VITE_API_URL=/ -t csa-frontend:vue ./csa-official-frontend
```

默认 CSP connect-src 只允许同源。若另行设计跨域部署，必须同时调整构建API地址、Nginx/Caddy CSP和后端CORS；不能只填外部地址就认为部署完成。不要把AI key、数据库密码等放进任何VITE_变量。

`API_PROXY_TARGET` 只被Vite开发/预览使用，不是生产Nginx配置；生产由Compose服务名和Caddy路由决定。

## 验收与回退

1. 构建与单元测试通过后，先在staging检查公开页、登录、角色隔离、Cookie/CSRF。
2. 直接访问并刷新 /dashboard/ai、/privacy，验证history fallback。
3. 验证资源上传/访问、简历审核、轮播保存与公开回读、AI评测。
4. 验证健康探针、日志、备份恢复与生产CSP；不要以本地fixture替代真实集成。
5. 回退时切换已保留的React前端镜像/分支；保留已应用V7/V8和业务数据，不删卷或重写migration。

本轮未执行生产部署。Docker镜像和真实环境验证是否执行，以 [Vue集成记录](change-records/2026-11-08-vue-integration.md) 为准。

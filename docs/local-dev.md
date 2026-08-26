# 本地启动和联调手册

这份文档的目标是：你拿到一台新电脑，能够知道要装什么、配什么、先启动谁、怎么验证、哪里报错该怎么查。

项目分为两个服务：

```text
前端：D:\CSA-Project\csa-official-frontend   Next.js，默认端口 3000
后端：D:\CSA-Project\csa-official-backend    Spring Boot，默认端口 8080
```

## 1. 准备环境

### 1.1 必备工具

| 工具 | 建议版本 | 检查命令 | 说明 |
| --- | --- | --- | --- |
| JDK | 17+ | `java -version` | 后端编译运行。当前机器用 Java 21 也能跑 |
| Node.js | 20+ | `node -v` | 前端开发和构建 |
| npm | 随 Node 安装 | `npm -v` | 前端依赖管理 |
| MySQL | 8.x 或兼容云库 | 用数据库工具连接 | 后端业务数据 |
| Redis | 可选 | `redis-cli ping` | 本地可以不用，改用内存缓存 |
| Git | 可选 | `git status` | 版本管理 |

### 1.2 为什么 Maven 不要求单独安装

后端目录自带 Maven Wrapper：

```text
csa-official-backend\mvnw.cmd
```

所以可以直接运行：

```powershell
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

这样能避免“别人电脑 Maven 版本不一致”的问题。

## 2. 后端配置

进入后端目录：

```powershell
cd D:\CSA-Project\csa-official-backend
```

复制环境变量模板：

```powershell
Copy-Item .env.example .env
```

必须做这一步。`application.yml` 的缓存默认值是 `redis`，而 `.env.example` 为本地学习默认写了 `CSA_CACHE_TYPE=memory`。如果不复制 `.env`，本地可能会按 Redis 配置启动，Redis 没配好时就容易在验证码、限流、单 Token 吊销等地方报错；全会话吊销仍由数据库中的 `session_version` 保证。

打开 `.env`，至少填写：

```properties
DB_URL=jdbc:mysql://localhost:3306/csa_db?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai
DB_USERNAME=root
DB_PASSWORD=your-password
JWT_SECRET=replace-with-at-least-32-characters-secret
CSA_CACHE_TYPE=memory
```

### 2.1 最小本地配置

如果你只想先把后端跑起来，不想折腾 Redis，可以用：

```properties
CSA_CACHE_TYPE=memory
```

这会让验证码、限流、单 Token 黑名单等依赖 `KeyValueStore` 的功能使用内存实现；全会话吊销不依赖黑名单，而是比较账号的 `session_version`。

优点：

- 本地启动简单。
- 不需要装 Redis。
- 适合学习代码和跑测试。

缺点：

- 重启后缓存数据清空。
- 多实例部署时不共享。
- 不适合生产环境。

### 2.2 接近线上配置

如果要模拟线上环境，把缓存切到 Redis：

```properties
CSA_CACHE_TYPE=redis
REDIS_HOST=127.0.0.1
REDIS_PORT=6379
REDIS_PASSWORD=
REDIS_SSL=false
```

如果用云 Redis，如 Upstash，要按服务商要求配置 host、password、ssl。

### 2.3 变量说明

| 变量 | 是否必填 | 示例 | 作用 | 配错表现 |
| --- | --- | --- | --- | --- |
| `SERVER_PORT` | 否 | `8080` | 后端端口 | 前端连不上后端 |
| `DB_URL` | 是 | `jdbc:mysql://localhost:3306/csa_db...` | 数据库地址 | 启动失败或接口 500 |
| `DB_USERNAME` | 是 | `root` | 数据库用户名 | 数据源连接失败 |
| `DB_PASSWORD` | 是 | `<local-db-password>` | 数据库密码 | 数据源连接失败 |
| `DB_MAX_POOL_SIZE` | 否 | `4` | 数据库连接池最大连接数 | 太大可能耗尽云数据库连接 |
| `CSA_CACHE_TYPE` | 否 | `memory` / `redis` | 缓存实现选择 | Redis 配错时建议切 memory |
| `REDIS_HOST` | Redis 模式需要 | `127.0.0.1` | Redis 地址 | 验证码、限流、吊销失败 |
| `REDIS_PORT` | Redis 模式需要 | `6379` | Redis 端口 | Redis 连接失败 |
| `REDIS_SSL` | Redis 模式视情况 | `false` | 是否启用 TLS | 云 Redis 常需要 true |
| `JWT_SECRET` | 是 | 至少 32 字符 | JWT 签名密钥 | 启动失败或 Token 无效 |
| `JWT_EXPIRATION` | 否 | `604800000` | Token 过期毫秒数 | 登录态过期时间异常 |
| `MAIL_USERNAME` | 发验证码需要 | `<smtp-account>` | SMTP 账号 | 发送验证码失败 |
| `MAIL_PASSWORD` | 发验证码需要 | QQ 授权码 | SMTP 密码/授权码 | 发送验证码失败 |
| `UPLOAD_PATH` | 否 | `D:/csa-upload/` | 文件保存目录 | 上传或下载失败 |
| `MAX_FILE_SIZE` | 否 | `50MB` | 单文件上传限制 | 大文件 413 |
| `UPLOAD_MAX_FILE_SIZE_BYTES` | 否 | `52428800` | 服务层大小限制 | 大文件被拒 |
| `CORS_ALLOWED_ORIGIN_PATTERNS` | 否 | `http://localhost:3000` | 允许跨域来源 | 浏览器 CORS 错误 |
| `SWAGGER_ENABLED` | 否 | `false` / `true` | 是否打开 Knife4j/OpenAPI | `/doc.html` 打不开 |
| `AUTH_COOKIE_NAME` | 否 | `CSA_AUTH_TOKEN` | 登录 Cookie 名 | 前后端 Cookie 对不上 |
| `AUTH_COOKIE_SECURE` | 否 | `false` | Cookie 是否仅 HTTPS | 本地 http 下 Cookie 不生效 |
| `AUTH_COOKIE_SAME_SITE` | 否 | `Lax` | Cookie 跨站策略 | 跨域登录异常 |
| `CSRF_ENABLED` | 否 | `true` | 是否校验 CSRF | POST 可能 403 |
| `CSRF_HEADER_NAME` | 否 | `X-CSRF-Token` | CSRF Header 名 | Header 不一致时 403 |

## 3. 数据库准备

当前项目依赖 MySQL。先创建空数据库，表结构由后端启动时的 Flyway migration 管理：

```sql
CREATE DATABASE csa_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

启动后会按顺序执行 `V1__initial_schema.sql`、`V2__production_operations.sql` 和 `V3__resume_review_queue_index.sql`。不要在本地开发流程里手工维护一份与 migration 并行的 DDL。当前核心业务表包括：

```text
user / sys_user 类用户表
dept 部门表
resource 资源表
competition 竞赛表
resume 简历表
proposal 提案表
vote_record 投票记录表
contribution_log 贡献记录表
sys_config 系统配置表
carousel 轮播图表
 invite_code 邀请码表
```

V2 还会增加 `sys_audit_log`、`sys_stored_file`、`sys_mail_delivery` 和
`sys_scheduled_job_execution` 四张运营表；它们由 Flyway 迁移创建，不由本地手工 DDL 维护。

如果启动时看到类似：

```text
Table 'csa_db.xxx' doesn't exist
```

说明数据库连接、Flyway 路径或 migration 状态有问题。先检查 `flyway_schema_history` 和后端启动日志；已有旧库需要按 [`production-readiness/flyway.md`](production-readiness/flyway.md) 做一次性 baseline 或补充向前迁移。

## 4. 启动后端

运行：

```powershell
cd D:\CSA-Project\csa-official-backend
.\mvnw.cmd spring-boot:run
```

看到类似内容说明启动成功：

```text
Started CsaOfficialApplication
Tomcat started on port 8080
```

访问：

```text
http://localhost:8080/doc.html
```

注意：Knife4j/OpenAPI 默认关闭，`application.yml` 里 `SWAGGER_ENABLED` 默认是 `false`。如果你要看接口文档，需要在 `.env` 里显式加入：

```properties
SWAGGER_ENABLED=true
```

如果只是验证后端是否启动，优先访问：

```text
http://localhost:8080/actuator/health
```

返回 UP 或浏览器能访问，说明后端 Web 服务起来了。

## 5. 验证后端

运行测试：

```powershell
cd D:\CSA-Project\csa-official-backend
.\mvnw.cmd test
```

本地运行成功时会看到类似：

```text
Failures: 0, Errors: 0
BUILD SUCCESS
```

测试覆盖的重点包括：

- 文件访问权限。
- JWT 密钥和过期时间。
- 生产 Cookie 安全配置启动校验。
- 竞赛编辑授权。
- JGit 相关逻辑。
- 登录、注册、验证码限流。
- 统一错误响应。
- 401/403 安全响应。
- 部门任命业务。
- 文件上传校验。
- 投票阈值逻辑。

## 6. 前端配置

进入前端目录：

```powershell
cd D:\CSA-Project\csa-official-frontend
```

本地联调时 `.env.local` 应该写：

```properties
NEXT_PUBLIC_API_URL=http://localhost:8080
```

如果这个变量指向线上后端，例如：

```properties
NEXT_PUBLIC_API_URL=https://csa-backend-mi58.onrender.com
```

那你本地页面请求的就是线上后端，不会打到本地 Java 服务。

## 7. 启动前端

安装依赖：

```powershell
npm install
```

启动开发服务：

```powershell
npm run dev
```

默认访问：

```text
http://localhost:3000
```

构建验证：

```powershell
npm run build
```

当前已验证：Next.js 生产构建通过。

## 8. 推荐启动顺序

本地联调建议：

1. 先启动 MySQL。
2. 如果用 Redis，启动 Redis；如果不用，`.env` 设置 `CSA_CACHE_TYPE=memory`。
3. 启动后端 `.\mvnw.cmd spring-boot:run`。
4. 打开 `http://localhost:8080/actuator/health` 确认后端可访问；如果要看 Knife4j，先设置 `SWAGGER_ENABLED=true`。
5. 设置前端 `.env.local` 指向 `http://localhost:8080`。
6. 启动前端 `npm run dev`。
7. 打开 `http://localhost:3000`。
8. 注册、登录、进入 `/dashboard`。

## 9. 手动接口检查

### 9.1 检查公开接口

不用登录：

```powershell
Invoke-WebRequest -UseBasicParsing http://localhost:8080/api/public/about
```

预期：

```json
{
  "code": 200,
  "message": "Success",
  "data": "..."
}
```

### 9.2 获取 CSRF

```powershell
Invoke-WebRequest -UseBasicParsing http://localhost:8080/api/auth/csrf
```

会返回 `csrfToken`，并设置 `CSA_CSRF_TOKEN` Cookie。

### 9.3 登录

用 Postman 或 Apifox 更方便，因为要保存 Cookie。

请求：

```http
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "username": "your-username",
  "password": "your-password"
}
```

成功后会：

- 响应体返回 `csrfToken`、`username`、`roleLevel`，不返回 JWT token。
- 写入 `CSA_AUTH_TOKEN` HttpOnly Cookie。
- 写入 `CSA_CSRF_TOKEN` Cookie。

### 9.4 调用需要登录的 GET 接口

```http
GET http://localhost:8080/api/sys/user/info
Cookie: CSA_AUTH_TOKEN=...
```

或：

```http
GET http://localhost:8080/api/sys/user/info
Authorization: Bearer ...
```

浏览器前端默认使用 Cookie，不会保存、读取或拼接 Bearer Token。`Authorization: Bearer ...` 只是后端兼容的 API 调试/非浏览器客户端方式。

### 9.5 调用需要 CSRF 的 POST 接口

如果使用 Cookie 登录，POST/PUT/DELETE 这类非安全方法需要：

```http
Cookie: CSA_AUTH_TOKEN=...
Cookie: CSA_CSRF_TOKEN=...
X-CSRF-Token: 和 Cookie 中相同的 csrf 值
```

否则会返回 403。

## 10. 常见错误和排查

### 10.1 后端启动失败：找不到 `DB_URL`

表现：

```text
Could not resolve placeholder 'DB_URL'
```

原因：

- 没有复制 `.env.example`。
- `.env` 不在 `csa-official-backend` 目录。
- 变量名写错。

处理：

```powershell
cd D:\CSA-Project\csa-official-backend
Copy-Item .env.example .env
```

然后填写数据库连接。

### 10.2 后端启动失败：JWT secret 问题

表现：

- Token 生成失败。
- 启动校验失败。
- 登录后请求一直 401。

处理：

确保：

```properties
JWT_SECRET=一串至少32字符的随机字符串
```

不要使用短值、固定值或多人共享值。

### 10.3 前端登录后仍然跳回登录页

可能原因：

1. `NEXT_PUBLIC_API_URL` 指向错了。
2. 后端 CORS 没允许当前前端地址。
3. Cookie 没写入。
4. Cookie SameSite/Secure 配错。
5. 后端 `/api/sys/user/info` 返回 401。

排查顺序：

1. 浏览器 DevTools 看 Network 里 login 请求是否 200。
2. 看 Response Headers 是否有 `Set-Cookie`。
3. 看 Application/Cookies 是否有 `CSA_AUTH_TOKEN`。
4. 看 `/api/sys/user/info` 请求是否带 Cookie。
5. 看后端日志是否 Token 解析失败。

### 10.4 POST 接口 403

如果 GET 正常、POST 403，优先怀疑 CSRF。

排查：

- 请求是否是 Cookie 登录。
- 是否有 `CSA_CSRF_TOKEN` Cookie。
- 是否有 `X-CSRF-Token` Header。
- Header 值是否和 Cookie 值匹配。
- 接口是否在 CSRF 豁免列表。

前端正常走 `src/lib/axios.ts` 一般会自动处理。

### 10.5 浏览器 CORS 错误

表现：

```text
Access to XMLHttpRequest at ... has been blocked by CORS policy
```

处理：

后端 `.env` 加入当前前端地址：

```properties
CORS_ALLOWED_ORIGIN_PATTERNS=http://localhost:3000,http://127.0.0.1:3000
```

然后重启后端。

### 10.6 Redis 连接失败

如果本地只是学习：

```properties
CSA_CACHE_TYPE=memory
```

如果必须使用 Redis，检查：

- host 是否正确。
- port 是否正确。
- password 是否正确。
- `REDIS_SSL` 是否符合服务商要求。

### 10.7 文件上传失败

排查：

1. 文件类型是否在 `csa.allow-types` 中。
2. 文件大小是否超过 `MAX_FILE_SIZE` 或 `UPLOAD_MAX_FILE_SIZE_BYTES`。
3. `UPLOAD_PATH` 目录是否存在。
4. Java 进程是否有写权限。
5. 下载时当前用户是否有权限访问该文件。

### 10.8 端口被占用

后端：

```properties
SERVER_PORT=8081
```

前端：

```powershell
npm run dev -- -p 3001
```

## 11. 生产环境注意事项

生产环境至少要做到：

1. `JWT_SECRET` 使用强随机密钥。
2. 不提交 `.env`。
3. `AUTH_COOKIE_SECURE=true`。
4. 如果 `AUTH_COOKIE_SAME_SITE=None`，必须同时 `AUTH_COOKIE_SECURE=true`。
5. `CORS_ALLOWED_ORIGIN_PATTERNS` 写具体域名，不要随便放 `*`。
6. 使用 Redis，而不是内存缓存。
7. 上传目录放在持久化磁盘。
8. 关闭或严格限制 `/api/test/**` 调试接口。
9. 数据库账号使用最小权限。
10. 邮箱授权码、数据库密码、Redis 密码都放环境变量。

项目里的 `SecurityStartupValidator` 已经做了一部分保护：

- `SameSite=None` 时必须 `Secure=true`。
- prod/production profile 下认证 Cookie 必须 `Secure=true`。

# Docker 一键部署手册

> **生产部署入口（2026-07）**：本页保留本地 Docker 学习说明。真实公网部署必须使用
> [`production-readiness/runbook.md`](production-readiness/runbook.md) 和
> [`compose.production.yml`](../compose.production.yml)，不要把本页早期的
> `db/schema.sql` 首次启动方式当作生产迁移方案。生产数据库由 Flyway 管理，演示 seed 不会加载。
> 备份恢复见 [`production-readiness/backup-restore.md`](production-readiness/backup-restore.md)。

这份文档的目标是：你拿到一台装了 Docker 的机器，能够用一条命令把整套系统（数据库、缓存、后端、前端）跑起来，并且知道每个服务在做什么、数据放在哪、出错了怎么查。

如果你要的是不使用 Docker、手动逐个启动服务的本地开发流程，请看 [`local-dev.md`](./local-dev.md)。本文最后一节会说明两者的对应关系。

项目一共编排四个服务：

```text
mysql      MySQL 8      业务数据库，端口 3306
redis      Redis 7      验证码 / 限流 / Token 黑名单，端口 6379
backend    Spring Boot  Java 后端，端口 8080
frontend   Next.js      前端页面，端口 3000
```

## 1. 准备环境

### 1.1 必备工具

| 工具 | 建议版本 | 检查命令 | 说明 |
| --- | --- | --- | --- |
| Docker Engine | 24+ | `docker --version` | 容器运行时 |
| Docker Compose | v2 | `docker compose version` | 编排插件，注意是 `docker compose` 不是 `docker-compose` |

只要装了 Docker Desktop（Windows / macOS）或 Linux 上的 Docker Engine + compose 插件即可，不需要单独装 JDK、Node、MySQL、Redis。

### 1.2 Flyway 与演示数据

MySQL 容器只负责创建空数据库和独立应用用户。后端启动后由 Flyway 按顺序执行：

```text
csa-official-backend/src/main/resources/db/migration/V1__initial_schema.sql
csa-official-backend/src/main/resources/db/migration/V2__production_operations.sql
csa-official-backend/src/main/resources/db/migration/V3__resume_review_queue_index.sql
```

`db/seed.sql` 只用于 dev/test 演示。开发 Compose 默认不加载 seed；确实需要演示账号时，运行前临时设置 `DEMO_SEED_ENABLED=true` 和随机生成的 `DEMO_SEED_PASSWORD`。固定口令不得写入 Compose、文档、命令脚本或 Git。

## 2. 一键启动

在项目根目录（即 `docker-compose.yml` 所在目录）执行：

```bash
docker compose up --build
```

这条命令会：

1. 构建 `backend` 和 `frontend` 两个镜像（首次较慢，后续有缓存会快很多）。
2. 拉起 `mysql`、`redis` 并等它们健康。
3. 等 `mysql` 和 `redis` 都 `healthy` 后再启动 `backend`（`depends_on` 用了 `condition: service_healthy`）。
4. 后端执行所有未落地的 Flyway migration；失败时 readiness 不会通过。
5. 启动 `frontend`。

想在后台运行，加 `-d`：

```bash
docker compose up --build -d
```

启动完成后访问：

```text
前端：http://localhost:3000
后端健康检查：http://localhost:8080/actuator/health
```

停止：

```bash
docker compose down          # 停止并移除容器，保留数据卷（数据库、上传文件都还在）
docker compose down -v       # 连数据卷一起删除（彻底重置，见第 5 节）
```

查看日志：

```bash
docker compose logs -f backend     # 跟踪后端日志
docker compose logs -f             # 跟踪全部
```

## 3. 每个服务在做什么

| 服务 | 镜像 / 来源 | 端口映射 | 数据卷 | 作用 |
| --- | --- | --- | --- | --- |
| `mysql` | `mysql:8.0` | `127.0.0.1:3306:3306` | `mysql-data` | 业务数据库；结构由后端 Flyway 管理 |
| `redis` | `redis:7-alpine` | `6379:6379` | `redis-data` | 验证码、限流、Token 黑名单等 `KeyValueStore` 实现 |
| `backend` | `csa-official-backend/Dockerfile` | `8080:8080` | `backend-uploads` | Spring Boot 后端，挂载上传目录到卷保证重启不丢文件 |
| `frontend` | `csa-official-frontend/Dockerfile` | `3000:3000` | 无 | Next.js 生产服务（`next start`） |

### 3.1 后端环境变量

`docker-compose.yml` 里 `backend` 服务的环境变量与 `csa-official-backend/src/main/resources/application.yml` 严格一一对应。关键几项：

| 变量 | 编排里的值 | 说明 |
| --- | --- | --- |
| `DB_URL` | `jdbc:mysql://mysql:3306/csa_db?...&sslMode=DISABLED&allowPublicKeyRetrieval=true` | 主机名用服务名 `mysql`，容器网络内可解析 |
| `DB_USERNAME` / `DB_PASSWORD` | `root` / `csa_root_pwd` | 与 MySQL 容器的 root 账号一致 |
| `CSA_CACHE_TYPE` | `redis` | 缓存实现，见 [第 6 节](#6-切换缓存类型) |
| `REDIS_HOST` / `REDIS_PORT` | `redis` / `6379` | 指向 redis 服务 |
| `REDIS_SSL` | `false` | **必须显式为 false**，原因见 [第 4 节](#4-为什么必须-redis_sslfalse) |
| `JWT_SECRET` | 至少 32 字节的字符串 | 见 [第 7 节](#7-常见故障排查) |
| `CORS_ALLOWED_ORIGIN_PATTERNS` | `http://localhost:3000,http://127.0.0.1:3000` | 允许前端跨域来源 |
| `UPLOAD_PATH` | `/app/uploads/` | 容器内上传目录，挂到 `backend-uploads` 卷 |

本地 Compose 提供开发默认值，可以通过项目根目录的 `.env` 文件或宿主机环境变量覆盖。秘密值应在当前 shell 或不入库的 `.env` 中生成，不要粘进文档。例如：

```bash
JWT_SECRET=$(openssl rand -hex 32) docker compose up --build
```

## 4. 为什么必须 REDIS_SSL=false

这是最容易踩的坑。看 `application.yml`：

```yaml
spring:
  data:
    redis:
      ssl:
        enabled: ${REDIS_SSL:true}
```

`REDIS_SSL` 的**默认值是 `true`**。这个默认值是为线上云 Redis（如 Upstash，强制 TLS）准备的。但本编排里的 `redis:7-alpine` 是一个**纯明文、不带 TLS** 的本地容器。如果后端带着 `ssl.enabled=true` 去连一个明文 Redis，TLS 握手会直接失败，表现为启动或首次用到缓存时报连接 / 握手错误。

所以 `docker-compose.yml` 的 `backend` 服务里**显式写死了 `REDIS_SSL: "false"`**，不要删掉这一行。

## 5. 重置本地开发数据

数据库的数据保存在名为 `mysql-data` 的命名卷里。Flyway history 也在数据库中；正常升级只需新增 migration 并重启后端，不需要删卷。只有要丢弃本地开发数据、重新从 V1 初始化时才执行：

```bash
# 方式一：删除全部卷（数据库 + redis + 上传文件都清空）
docker compose down -v
docker compose up --build

# 方式二：只删数据库卷，保留其它
docker compose down
docker volume rm csa-project_mysql-data
docker compose up --build
```

> 卷名前缀 `csa-project_` 来自 compose 的项目名（默认取目录名）。可用 `docker volume ls` 确认实际名称。
>
> 生产禁止用 `down -v` 当回滚。生产迁移失败按 `production-readiness/flyway.md` 和备份恢复流程处理。

## 6. 切换缓存类型

后端支持两种 `KeyValueStore` 实现，通过 `CSA_CACHE_TYPE` 切换：

| 值 | 含义 | 适用场景 |
| --- | --- | --- |
| `redis`（默认） | 用 Redis 存验证码、限流计数、Token 黑名单 | 贴近生产；本编排默认值 |
| `memory` | 用进程内内存实现 | 想省事、不关心 Redis 时 |

切成 `memory` 有两种做法：

```bash
# 临时覆盖
CSA_CACHE_TYPE=memory docker compose up --build
```

或在项目根目录建 `.env` 写：

```properties
CSA_CACHE_TYPE=memory
```

用 `memory` 时，Redis 容器仍会启动（后端健康检查里也会连它），但缓存逻辑不再走 Redis。注意 `memory` 模式下缓存在后端重启后清空、且不跨实例共享，不适合生产。

## 7. NEXT_PUBLIC_ 的构建期陷阱（重要）

前端代码里用到的后端地址来自 `NEXT_PUBLIC_API_URL`（见 `src/lib/axios.ts`、`src/proxy.ts`）。Next.js 有一个必须理解的规则：

> **所有 `NEXT_PUBLIC_*` 变量会在 `next build` 时被静态内联进浏览器端 bundle，是构建期固化的，不是运行期读取的。**

也就是说，构建完成后再改容器的 `NEXT_PUBLIC_API_URL` 环境变量，对已经打好的前端页面**不会生效**。因此：

- `csa-official-frontend/Dockerfile` 把 `NEXT_PUBLIC_API_URL` 声明为 **build ARG**，在 `npm run build` 之前注入。
- `docker-compose.yml` 的 `frontend.build.args` 里传入这个值：

```yaml
frontend:
  build:
    args:
      NEXT_PUBLIC_API_URL: ${NEXT_PUBLIC_API_URL:-http://localhost:8080}
```

改这个地址后必须**重新构建**才生效：

```bash
NEXT_PUBLIC_API_URL=http://localhost:8080 docker compose up --build
```

### 7.1 为什么是 localhost 而不是 backend

值是 `http://localhost:8080`，而不是容器网络里的 `http://backend:8080`。因为真正发请求的是**用户浏览器**，浏览器跑在宿主机上，解析不到 Docker 内部的服务名 `backend`，只能访问宿主机映射出来的 `localhost:8080`。服务名 `backend` 只在容器之间互相调用时才可用。

编排里同时也把 `NEXT_PUBLIC_API_URL` 作为运行期环境变量传给了 `frontend`，那是给 `proxy.ts`（服务端中间件，用它拼 CSP 的 `connect-src`）读取的，与构建期内联互不冲突。

### 7.2 关于 Next.js 产物形态

`csa-official-frontend/next.config.ts` **没有开启** `output: 'standalone'`。所以前端镜像没有走 standalone 精简产物，而是采用常规的 `next start` 方式：运行镜像里带上生产依赖（`npm ci --omit=dev`）和 `.next` 构建产物，用 `npm run start` 启动。如果以后在 `next.config.ts` 里加了 `output: 'standalone'`，可以把 Dockerfile 改成只拷贝 `.next/standalone` 和 `.next/static` 来进一步瘦身。

## 8. 常见故障排查

### 8.1 端口冲突

表现：`docker compose up` 报 `port is already allocated` 或 `bind: address already in use`。

原因：宿主机上已经有程序占用了 3000 / 3306 / 6379 / 8080。

处理：改用其它宿主机端口，编排里所有端口都可通过环境变量覆盖：

```bash
FRONTEND_PORT=3001 BACKEND_PORT=8081 MYSQL_PORT=3307 REDIS_PORT=6380 docker compose up --build
```

注意：如果改了 `BACKEND_PORT`，浏览器访问后端的地址也变了，要同步改 `NEXT_PUBLIC_API_URL` 并重新构建前端（见第 7 节）。

### 8.2 后端起来了但连不上数据库 / MySQL 没准备好

表现：后端日志报 `Communications link failure`、`Unknown database`、或表不存在。

排查：

1. 编排已用 `depends_on: condition: service_healthy` 保证 MySQL 健康后才启动后端，一般不会连不上。若仍失败，先看 `docker compose logs mysql` 确认它是否 `healthy`。
2. 首次启动 MySQL 和执行 Flyway 较慢，`backend` 的健康检查设了 `start_period: 60s` 容错。若机器较慢，先看后端日志中的 Flyway 版本和失败 SQL。
3. 报“表不存在”时检查 `flyway_schema_history`、migration classpath 和 `FLYWAY_LOCATIONS`。不要绕过 Flyway 手工导入 `db/schema.sql` 后继续发布。

### 8.3 JWT_SECRET 长度不足导致后端启动失败

表现：后端启动直接抛异常并退出，日志里有类似：

```text
csa.jwt.secret 必须配置且长度不少于 32 字节 (HS256 要求 256bit)
```

原因：后端用 HS256 签名 JWT，密钥至少要 **256 bit = 32 字节**。这个校验写在 `common/util/JwtUtils.java` 的 `@PostConstruct validateSecret()` 里（`MIN_SECRET_BYTES = 32`）。密钥不足 32 字节，应用启动即失败。

> 补充：`config/SecurityStartupValidator.java` 是另一处启动校验，但它只管 Cookie 的 `Secure` / `SameSite`（`SameSite=None` 必须 `Secure=true`；prod 环境 Cookie 必须 `Secure=true`），**不校验** JWT 密钥长度。密钥长度的约束只来自 `JwtUtils`。

处理：本地编排有明确标记为非生产的默认值；生产 Compose 则缺少 `JWT_SECRET` 就立即失败。自行配置时务必给足长度：

```bash
JWT_SECRET=$(openssl rand -hex 32) docker compose up --build   # hex 32 字节 = 64 字符，足够
```

不要使用短值、固定值或多人共享值。

### 8.4 Redis 连接 / 握手失败

见第 4 节。九成是漏了 `REDIS_SSL=false`，或者把 `REDIS_SSL` 手动改回了 `true` 去连本地明文 Redis。

### 8.5 前端能打开但接口全部失败 / CORS 报错

排查顺序：

1. 前端构建时的 `NEXT_PUBLIC_API_URL` 是否正确指向浏览器可达的后端地址（默认 `http://localhost:8080`）。改了要重新 `--build`。
2. 后端 `CORS_ALLOWED_ORIGIN_PATTERNS` 是否包含当前前端来源（默认含 `http://localhost:3000`）。
3. 用 `docker compose logs backend` 看后端是否已经 `Started` 且健康。

### 8.6 上传的文件重启后消失

编排把后端 `UPLOAD_PATH=/app/uploads/` 挂到了命名卷 `backend-uploads`，正常情况下 `docker compose down` 再 `up` 文件仍在。只有执行 `docker compose down -v` 才会连上传文件一起删掉。

## 9. 与手动本地开发流程的对应关系

`docs/local-dev.md` 讲的是不使用 Docker、在宿主机上逐个手动启动服务的流程。两者做的事情是一样的，只是环境提供方式不同：

| 事项 | 手动流程（local-dev.md） | Docker 编排（本文） |
| --- | --- | --- |
| 数据库 | 自己装 MySQL 8，手动建空库后由后端 Flyway 迁移 | `mysql` 容器提供空库，后端 Flyway 管理结构；seed 仅 dev/test 显式启用 |
| 缓存 | 本地装 Redis 或用 `CSA_CACHE_TYPE=memory` | `redis` 容器，`CSA_CACHE_TYPE` 默认 `redis` |
| 后端配置 | 复制 `.env.example` 为 `.env` 再填值 | 环境变量直接写在 `docker-compose.yml`，可用根目录 `.env` 覆盖 |
| 启动后端 | `.\mvnw.cmd spring-boot:run` | `backend` 容器由 `Dockerfile` 构建运行 |
| 前端配置 | 写 `.env.local` 的 `NEXT_PUBLIC_API_URL` | 通过 build ARG 传入并 `next build` |
| 启动前端 | `npm run dev`（开发模式） | `next start`（生产模式，镜像内构建） |
| 启动顺序 | 手动记住：先 MySQL/Redis，再后端，再前端 | 编排用 `depends_on` + 健康检查自动编排 |

一个关键差异：`local-dev.md` 里 `.env.example` 默认 `CSA_CACHE_TYPE=memory`（方便学习，不装 Redis），而本编排既然已经拉起了 Redis 容器，默认就用 `redis`，更贴近生产。想省事随时可切回 `memory`（第 6 节）。

另外，手动开发用的是 `npm run dev` 热更新，改代码即时生效；Docker 编排是生产构建，改了前端代码需要重新 `docker compose up --build`。因此日常写代码建议用 `local-dev.md` 的流程，而验证整体部署、给别人演示、或需要一条命令拉起全套时用本文的 Docker 流程。

# CSA Official 计协官网与协会管理平台

CSA Official 是一个面向计算机协会的官网与内部管理平台。它不是单纯的展示页，而是一个“公开官网 + 登录工作台 + 管理后台 + 后续 AI 能力扩展入口”的完整项目。项目当前由 Spring Boot 后端和 Next.js 前端组成，已经覆盖登录注册、角色权限、资源库、竞赛管理、简历投递、部门人事、提案投票、贡献墙、公开内容维护、文件上传和统一异常处理等核心能力。

这份 README 按 `D:\java train` 的 Java 后端训练思路整理：先讲项目是什么，再讲项目怎么跑、请求怎么流转、每个模块做什么、权限怎么保护、怎么验证，最后讲如何把它继续扩展成带 RAG 知识库和 Agent 工具调用的“协会智能管理平台”。

## 项目定位

这个项目有三层价值：

1. 对用户来说，它是计协官网。游客能看协会介绍、贡献者、比赛活动和公开内容。
2. 对协会内部来说，它是管理平台。不同身份的成员能进入工作台，管理资源、简历、竞赛、提案和部门。
3. 对学习和面试来说，它是一个 Java 后端练手项目。可以用来讲 Spring Boot 请求链路、JWT 鉴权、Redis 降级、前后端联调、统一异常、权限控制和后续 RAG/Agent 扩展。

最终目标不是“AI 生成了一堆页面”，而是把项目整理到你能解释、能排错、能继续开发、能面试讲清楚的状态。

## 当前完成度

| 方向 | 当前状态 |
| --- | --- |
| 公开官网 | 已有首页、关于、竞赛、贡献者、资源入口等页面 |
| 后台工作台 | 已有概览、资源、竞赛、简历、简历审核、部门、投票、设置等页面 |
| 登录注册 | 已有登录、注册、验证码、邀请码、退出登录、修改/找回密码和全会话吊销 |
| 安全 | 已有 Spring Security、JWT、HttpOnly Cookie、CSRF、CORS、稳定 HTTP status / `errorCode` / `traceId` |
| 权限 | 已有 `roleLevel` 等级体系，前端菜单隐藏 + 后端 `@PreAuthorize` |
| 数据访问 | 使用 MyBatis-Plus + MySQL |
| 数据库迁移 | 生产使用 Flyway V1-V5；`db/schema.sql` 只是当前结构快照，`db/seed.sql` 仅供 dev/test 演示，`SchemaConsistencyTest` 防止实体与 migration 漂移 |
| 缓存/限流 | 支持 Redis，也支持本地内存缓存降级 |
| 文件 | 支持上传、类型限制、大小限制、受控访问 |
| 简历审核 | 已完成“成员提交 → 部长队列 → 详情 → 通过/驳回 → 状态回写”前后端闭环；接口和竞态规则见 `docs/api-map.md` |
| 一键启动 | 已提供 `docker-compose.yml`，一条命令拉起 MySQL、Redis、后端、前端 |
| 文档 | 已补充本 README、启动文档、接口地图、架构说明、数据库说明、部署手册、学习路线 |
| 测试 | 后端全量测试、前端测试、lint 和生产构建均纳入验收，实际结果见生产就绪验证记录 |
| AI 扩展 | 已规划 RAG / Agent 接入路线，尚未真正落库实现 |

## 技术栈

| 层 | 技术 | 在项目中的作用 |
| --- | --- | --- |
| 前端框架 | Next.js 16.3.3、React 19.2.4 | 页面路由、客户端组件、生产构建 |
| 前端语言 | TypeScript | 约束接口类型和组件 props |
| 样式 | Tailwind CSS、shadcn/ui 风格组件 | 页面布局、表单、按钮、卡片、弹窗 |
| 状态 | Zustand | 保存当前登录用户和会话状态 |
| 请求 | axios 1.18.1 | 统一封装 API、Cookie、CSRF、错误处理 |
| 后端框架 | Spring Boot 3.5.14 | Web 接口、依赖注入、配置管理；关键传递依赖使用受控补丁级覆盖 |
| 安全 | Spring Security、JJWT 0.12.6 | 登录认证、接口授权、Token 校验 |
| 数据访问 | MyBatis-Plus 3.5.5 | Mapper、分页、条件查询、逻辑删除 |
| 数据库 | MySQL | 用户、资源、竞赛、简历、部门、投票等业务数据 |
| 缓存 | Redis / 内存缓存 | 验证码、限流、Token 黑名单 |
| 文档 | Knife4j 4.3.0 / OpenAPI | 接口文档入口 |
| 文件/导出 | Multipart、EasyExcel 3.3.4 | 文件上传、成员数据导出 |
| Git 集成 | JGit 6.8.0.202311291450-r | 竞赛/资源相关 Git 能力 |
| HTML 清洗 | 后端 Jsoup、前端 isomorphic-dompurify | 富文本入库和展示双层 XSS 防护 |
| 测试 | JUnit、Spring Boot Test、Mockito | Controller、Service、安全和文件访问测试 |
| 容器化 | Docker、Docker Compose | 本地一条命令拉起数据库、缓存、后端、前端 |

## 目录结构

```text
D:\CSA-Project
├─ .github                GitHub 工作流/仓库配置
├─ .vscode                编辑器配置
├─ csa-official-backend
│  ├─ .mvn
│  ├─ .vscode
│  ├─ target                 Maven 构建输出，本地生成
│  ├─ Dockerfile
│  ├─ HELP.md
│  ├─ mvnw
│  ├─ mvnw.cmd
│  ├─ .env                  本地私密配置，不提交
│  ├─ .env.example
│  ├─ .gitattributes
│  ├─ .gitignore
│  ├─ .dockerignore         构建镜像时排除 target、.git 等
│  ├─ pom.xml
│  └─ src
│     ├─ main
│     │  ├─ java\com\csa\official
│     │  │  ├─ CsaOfficialApplication.java
│     │  │  ├─ common
│     │  │  │  ├─ annotation      自定义注解，如限流、贡献记录
│     │  │  │  ├─ aspect          AOP 切面，如限流切面、贡献切面
│     │  │  │  ├─ cache           KeyValueStore 抽象、Redis/内存实现
│     │  │  │  ├─ constant        角色等级等常量
│     │  │  │  ├─ controller      通用文件上传/访问接口
│     │  │  │  ├─ exception       业务异常、全局异常处理
│     │  │  │  ├─ result          统一返回 R<T>
│     │  │  │  ├─ security        CSRF、JWT 吊销、统一安全响应
│     │  │  │  └─ util            JWT、Security、分页收敛（PageUtils）工具
│     │  │  ├─ config             Security、CORS、CSRF、Redis/内存缓存、异步线程池、MyBatis、Swagger 等配置
│     │  │  └─ modules
│     │  │     ├─ sys             用户、部门、资源、投票、配置、贡献、导出、DTO/VO/任务
│     │  │     ├─ biz             竞赛、竞赛编辑者、VO、Git 相关能力
│     │  │     └─ resume          简历投递和审核，含 entity/enums/mapper/service
│     │  └─ resources
│     │     └─ application.yml
│     └─ test                    后端测试
├─ csa-official-frontend
│  ├─ .next                  Next.js 构建输出，本地生成
│  ├─ node_modules           前端依赖，本地安装生成
│  ├─ public                 静态资源
│  ├─ .env.local
│  ├─ .gitignore
│  ├─ .dockerignore         构建镜像时排除 node_modules、.next 等
│  ├─ Dockerfile            前端多阶段构建镜像
│  ├─ components.json        shadcn/ui 风格组件配置
│  ├─ eslint.config.mjs
│  ├─ next-env.d.ts
│  ├─ next.config.ts
│  ├─ package.json
│  ├─ package-lock.json
│  ├─ postcss.config.mjs
│  ├─ README.md
│  ├─ tsconfig.json
│  ├─ tsconfig.tsbuildinfo   TypeScript 增量构建缓存
│  └─ src
│     ├─ app                    Next.js App Router 页面
│     ├─ components             布局、业务组件、UI 组件
│     ├─ config                 菜单和权限入口配置
│     ├─ hooks                  客户端渲染相关 hook
│     ├─ lib                    axios、权限、格式化、导航、HTML 清洗
│     ├─ services               auth/resource/competition 等 API 封装
│     ├─ store                  Zustand 登录态
│     ├─ types                  API 和用户类型
│     └─ proxy.ts               Dashboard 入口保护、CSP nonce、安全响应头
├─ db
│  ├─ schema.sql               当前结构快照，供学习和人工核对，不是生产迁移入口
│  └─ seed.sql                 仅 dev/test 演示种子，不用于生产账号初始化
├─ docker-compose.yml          MySQL/Redis/后端/前端一键启动编排
├─ docs
│  ├─ local-dev.md              本地启动和联调
│  ├─ api-map.md                接口地图和请求示例
│  ├─ architecture.md           架构、模块、数据流说明
│  ├─ database.md               表清单、ER 关系、索引取舍、逻辑删除约定
│  ├─ deployment.md             Docker Compose 用法、环境变量、排错
│  ├─ security-design.md        登录、JWT、CSRF、权限和限流说明
│  ├─ rag-agent-roadmap.md      RAG/Agent 后续接入方案
│  └─ study-and-demo-guide.md   学习路线、演示脚本、面试表达
├─ logs                         本地日志
├─ security-war                 安全/部署相关产物
├─ packed-project.xml           项目打包快照
├─ repomix.config.json          代码打包配置
└─ README.md
```

## 功能总览

### 1. 公开官网

面向未登录用户和普通访问者，主要页面包括：

| 页面 | 路径 | 内容 |
| --- | --- | --- |
| 首页 | `/` | 协会定位、入口导航、公开内容聚合 |
| 关于 | `/about` | 协会介绍，内容来自后端配置 |
| 比赛活动 | `/competitions` | 公开竞赛分页 |
| 贡献者 | `/contributors` | 核心成员/贡献者展示 |
| 资源入口 | `/resources` | 引导进入登录后的资源库 |

公开接口统一在 `/api/public/**` 下，大部分不需要登录。注意：公开页面能展示内容，不代表可以做后台写操作，写操作仍然需要角色权限。

### 2. 登录注册

登录注册模块包括：

- 用户名密码登录。
- 邮箱验证码注册。
- 邀请码升级为会员。
- 未校验的 `merchantNo` 不再允许提权。
- 登录成功后写入 `CSA_AUTH_TOKEN` HttpOnly Cookie。
- 同时设置 `CSA_CSRF_TOKEN` Cookie，响应体只返回 `csrfToken`、`username`、`roleLevel`，不再返回 JWT token。
- 退出登录时吊销当前 Token，并清理 Cookie。

登录链路：

```text
LoginForm
→ authService.login
→ POST /api/auth/login
→ AuthenticationManager 校验密码
→ 查询 User
→ JwtUtils.generateToken
→ CsrfTokenService.generateToken
→ Set-Cookie: CSA_AUTH_TOKEN
→ Set-Cookie: CSA_CSRF_TOKEN
→ Response body: csrfToken + username + roleLevel
→ 前端保存 user，记住 csrfToken
```

### 3. 工作台

工作台入口为 `/dashboard`。`src/proxy.ts` 会先根据 `CSA_AUTH_TOKEN` Cookie 做入口跳转，没有 Cookie 时重定向到 `/login?redirect=...`，同时设置 CSP nonce、CSP Header 和生产环境 HSTS。进入页面后，`DashboardGuard` 会再请求 `/api/sys/user/info` 校验当前会话，避免 Cookie 已过期但前端仍以为登录。

工作台菜单根据角色等级展示：

| 菜单 | 路径 | 最低等级 |
| --- | --- | --- |
| 概览 | `/dashboard` | 0 |
| 个人资料 | `/dashboard/profile` | 0 |
| 资源库 | `/dashboard/resources` | 1 |
| 我的简历 | `/dashboard/resume` | 2 |
| 比赛看板 | `/dashboard/competitions` | 3 |
| 提案中心 | `/dashboard/vote` | 3 |
| 部门人事 | `/dashboard/departments` | 4 |
| 公开设置 | `/dashboard/settings` | 4 |

前端菜单隐藏只是体验优化。真实权限以后端接口为准。

### 4. 资源库

资源库负责协会资料、模板、工具和文件入口。

能力：

- 资源分页列表。
- 按分类筛选。
- 获取分类列表。
- 发布/编辑资源。
- 删除资源。
- 下载计数。
- 上传文件。
- 受控访问上传后的文件。

典型链路：

```text
/dashboard/resources
→ ResourceLibrary
→ resourceService.list
→ GET /api/sys/resource/list
→ JwtAuthenticationFilter / SecurityContext / @PreAuthorize
→ ResourceController.list
→ ResourceService.listResources
→ PageUtils.of + ResourceMapper.selectPage
→ Page<Resource> → ResourceVO
→ R<Page<ResourceVO>>
→ Axios response interceptor → 前端状态
```

文件链路：

```text
前端选择文件
→ POST /api/common/file/upload
→ FileService 校验大小和类型
→ 保存到 UPLOAD_PATH
→ 返回可访问 URL
→ GET /files/{ownerId}/{fileName}
→ StoredFileController 校验访问权限
```

### 5. 竞赛管理

竞赛模块负责比赛/活动信息。

能力：

- 公开竞赛分页。
- 后台竞赛列表。
- 新增或编辑竞赛。
- 授权竞赛编辑者。
- 对非管理员编辑行为做权限校验。

为什么单独有 `PublicCompetitionController`：

- 公开列表只返回适合公开展示的字段。
- 后台列表可以服务于管理场景。
- 公开接口和管理接口分离，能降低越权和字段泄露风险。

### 6. 简历投递

简历模块负责成员简历草稿、提交和审核。

典型状态：

- 草稿：用户可以保存但不进入审核。
- 待审核：成员提交后进入部长审核队列。
- 审核通过或驳回：由管理端处理。

典型链路：

```text
/dashboard/resume
→ resumeService.getMyResume
→ GET /api/resume/my
→ ResumeController
→ ResumeService
→ ResumeMapper
```

审核闭环：

```text
/dashboard/resume
→ POST /api/resume/save
→ POST /api/resume/submit
→ /dashboard/resume-reviews（LEVEL_3+）
→ GET /api/resume/reviews
→ GET /api/resume/reviews/{id}
→ POST /api/resume/audit
→ 原子更新状态 + 审计记录
```

审核队列是分页查询，申请人和部门按当前页批量加载；详情接口隐藏草稿，审核接口只允许 `PENDING` 状态，驳回必须填写原因。部长不需要提前知道 `resumeId`，可以从工作台完成整个流程。

### 7. 部门人事

部门人事模块负责查看部门和任命部长。

任命逻辑里有一个重要业务点：任命新部长时，原部长会被自动降级为成员，避免一个部门出现多个部长状态混乱。

这类业务逻辑应该放在 Service 层，而不是写在前端，也不是堆在 Controller 里。原因是：

- Service 可以加事务。
- Service 更容易测试。
- 多个 Controller 或后续 Agent 工具都可以复用同一套业务规则。

### 8. 提案投票

投票模块负责组织内部治理。

能力：

- 创建提案。
- 查看提案列表。
- 投票。
- 按权重统计同意/反对。
- 达到阈值后改变提案状态。

投票模块特别适合面试时讲“业务规则不能只写 CRUD”：

- 谁能发起提案？
- 谁能投票？
- 一个人能不能重复投？
- 会长和部长的票权是否一样？
- 通过阈值怎么算？
- 提案通过后是否自动执行高风险动作？

当前项目里对 Root 提权类提案保守处理：达到通过阈值也不自动提权，避免 AI 或业务误操作直接改高权限。

### 9. 公开设置

公开设置用于维护协会介绍内容。

关键点：

- 后端使用 Jsoup 白名单清洗 HTML。
- 前端展示和设置预览通过 `isomorphic-dompurify` 再清洗一层。
- 允许基础富文本、图片、标题、引用、代码块等。
- 不直接相信前端传来的 HTML。

这可以作为 Web 安全面试点：富文本内容如果不清洗，可能产生 XSS 风险。

## 角色和权限模型

项目使用数字等级 `roleLevel` 表示用户身份，再映射为 Spring Security 角色。

| roleLevel | 常量 | 角色 | 说明 |
| --- | --- | --- | --- |
| 0 | `GUEST` | 游客 | 登录用户的最低等级 |
| 1 | `MEMBER` | 会员 | 可访问资源库 |
| 2 | `CORE_MEMBER` | 核心成员 | 可使用简历等内部协作能力 |
| 3 | `MINISTER` | 部长/副部长 | 可发布资源、管理竞赛、参与提案 |
| 4 | `PRESIDENT` | 会长 | 可做部门人事、公开设置等管理 |
| 99 | `ROOT` | 超级管理员 | 最高权限，偏运维/兜底 |

权限控制分两层：

1. 前端：`src/config/menu.ts` 根据 `minLevel` 控制菜单是否显示。
2. 后端：Controller 上使用 `@PreAuthorize("hasRole('LEVEL_3')")` 等注解做真实保护。

后端权限才是安全边界。原因很简单：前端代码可以被用户修改，请求也可以绕过浏览器直接发。

## 统一返回和异常处理

后端统一返回 `R<T>`：

```json
{
  "code": 200,
  "message": "Success",
  "data": {}
}
```

错误响应保留相同外壳，同时返回真实 HTTP status、稳定 `errorCode` 和可关联日志的 `traceId`：

```json
{
  "code": 401,
  "message": "用户名或密码错误",
  "data": null,
  "errorCode": "AUTHENTICATION_FAILED",
  "traceId": "request-id"
}
```

响应头 `X-Request-ID` 与错误体 `traceId` 对应。认证失败才转换为 401；数据库和系统异常保持 5xx，不能伪装成“用户名或密码错误”。

常见错误：

| code | HTTP 语义 | 场景 |
| --- | --- | --- |
| 400 | 参数错误 | 缺字段、类型错误、JSON 格式错误 |
| 401 | 未认证 | 未登录、Token 无效 |
| 403 | 无权限 | 登录了但角色不够，或 CSRF 错误 |
| 404 | 不存在 | 资源或业务对象不存在 |
| 405 | 方法不支持 | HTTP method 不匹配 |
| 409 | 冲突 | 用户名重复、邀请码耗尽 |
| 413 | 文件过大 | 上传超过限制 |
| 429 | 限流 | 登录、注册、验证码请求太频繁 |
| 500 | 服务端错误 | 未处理异常或系统繁忙 |
| 502/503 | 依赖不可用 | 上游或邮件等依赖暂时不可用 |

前端 Axios 拦截器会做几件事：

- 自动带上 Cookie。
- 浏览器前端不读取、不保存、不拼接 Bearer Token；后端仍兼容 `Authorization: Bearer`，主要给 API 调试或非浏览器客户端使用。
- 需要 CSRF 的请求自动获取并带 `X-CSRF-Token`。
- 同时检查 HTTP status 和业务 `code`；成功时只返回 `data`。
- 401 时跳转登录页。
- 失败时包装成 `ApiError`。

## 配置和环境变量

后端核心配置在：

```text
csa-official-backend/src/main/resources/application.yml
```

本地变量从 `.env` 读取。示例文件：

```text
csa-official-backend/.env.example
```

核心变量：

| 变量 | 必填 | 作用 |
| --- | --- | --- |
| `DB_URL` | 是 | MySQL JDBC 地址 |
| `DB_USERNAME` | 是 | 数据库用户名 |
| `DB_PASSWORD` | 是 | 数据库密码 |
| `JWT_SECRET` | 是 | JWT 签名密钥 |
| `CSA_CACHE_TYPE` | 否 | `redis` 或 `memory` |
| `REDIS_HOST` | Redis 模式必填 | Redis 地址 |
| `REDIS_PASSWORD` | Redis 模式视情况 | Redis 密码 |
| `MAIL_USERNAME` | 发验证码时需要 | 邮箱账号 |
| `MAIL_PASSWORD` | 发验证码时需要 | 邮箱授权码 |
| `UPLOAD_PATH` | 否 | 文件上传目录 |
| `CORS_ALLOWED_ORIGIN_PATTERNS` | 否 | 允许跨域的前端地址 |
| `SWAGGER_ENABLED` | 否 | 是否打开 Knife4j/OpenAPI |
| `AUTH_COOKIE_SECURE` | 否 | Cookie 是否要求 HTTPS |
| `AUTH_COOKIE_SAME_SITE` | 否 | Cookie SameSite 策略 |
| `CSRF_ENABLED` | 否 | 是否启用 CSRF |

注意：`application.yml` 的缓存默认值是 `redis`，但 `.env.example` 为本地开发默认写了 `CSA_CACHE_TYPE=memory`。本地启动时必须复制 `.env.example` 为 `.env`，或者自己显式配置缓存类型。

前端核心变量：

```text
csa-official-frontend/.env.local
```

```properties
NEXT_PUBLIC_API_URL=http://localhost:8080
```

## 一键启动（Docker Compose）

如果本机装了 Docker，想最快看到效果，直接在仓库根目录执行：

```bash
docker compose up --build
```

这会依次拉起 MySQL 8、Redis 7、后端和前端。开发 backend 使用 Flyway 创建结构，并在显式启用的 dev 配置下加载演示 seed；生产 Compose 不执行 seed，也不自动创建共享密码账号。启动完成后：

| 服务 | 地址 |
| --- | --- |
| 前端 | http://localhost:3000 |
| 后端 | http://localhost:8080 |

开发演示账号仅用于本地 dev/test，覆盖各角色等级；密码和 Token 不记录在文档中，生产账号必须通过受控流程创建或导入：

| 用户名 | 角色 | roleLevel |
| --- | --- | --- |
| `root` | 超级管理员 | 99 |
| `president` | 会长 | 4 |
| `minister` | 部长 | 3 |
| `core` | 核心成员 | 2 |
| `member` | 会员 | 1 |
| `guest` | 游客 | 0 |

停止并保留数据用 `docker compose down`，想连数据卷一起清空（重置数据库）用 `docker compose down -v`。环境变量说明和排错见 [docs/deployment.md](docs/deployment.md)。

## 本地启动

详细步骤见 [docs/local-dev.md](docs/local-dev.md)。

后端：

```powershell
cd D:\CSA-Project\csa-official-backend
Copy-Item .env.example .env
# 编辑 .env
.\mvnw.cmd spring-boot:run
```

前端：

```powershell
cd D:\CSA-Project\csa-official-frontend
npm install
npm run dev
```

默认地址：

| 服务 | 地址 |
| --- | --- |
| 前端 | http://localhost:3000 |
| 后端 | http://localhost:8080 |
| Knife4j | http://localhost:8080/doc.html，需要 `SWAGGER_ENABLED=true` |

## 验证命令

后端测试：

```powershell
cd D:\CSA-Project\csa-official-backend
.\mvnw.cmd test
```

前端测试、lint 和生产构建：

```powershell
cd D:\CSA-Project\csa-official-frontend
npm ci
npm run test
npm run lint
npm run build
npm audit --audit-level=high
```

真实依赖迁移测试（需要 Docker）：

```powershell
cd D:\CSA-Project\csa-official-backend
.\mvnw.cmd "-Dit.containers=true" "-Dtest=FlywayMySqlRedisIntegrationTest" test
```

当前验证结果与生产演练证据见 [`docs/production-readiness/phase-1-verification.md`](docs/production-readiness/phase-1-verification.md) 和 [`docs/production-readiness/phase-2-verification.md`](docs/production-readiness/phase-2-verification.md)。

## 推荐学习顺序

如果你是为了“Java 康复训练”和面试准备，建议按这个顺序吃透项目：

1. 先读 `pom.xml` 和 `application.yml`，知道项目依赖和配置。
2. 再读 `CsaOfficialApplication.java`，理解 Spring Boot 扫描范围。
3. 读 `R.java` 和 `GlobalExceptionHandler.java`，理解统一返回和统一异常。
4. 读 `SecurityConfig.java`、`JwtAuthenticationFilter.java`、`AuthController.java`，理解登录鉴权。
5. 读 `src/proxy.ts`、`src/lib/axios.ts`、`src/store/useAuthStore.ts`，理解前端如何靠 HttpOnly Cookie、CSRF 和会话校验工作。
6. 挑一个模块完整追踪，比如资源库，从页面追到 Controller、Mapper。
7. 跑后端测试，理解测试覆盖了哪些安全和业务风险。
8. 看 docs 里的架构、安全、RAG/Agent 路线，把项目讲成自己的作品。

## 文档入口

- [本地启动和环境变量](docs/local-dev.md)
- [接口地图和请求示例](docs/api-map.md)
- [架构和模块说明](docs/architecture.md)
- [安全设计说明](docs/security-design.md)
- [RAG / Agent 扩展路线](docs/rag-agent-roadmap.md)
- [项目讲解、演示和学习路线](docs/study-and-demo-guide.md)
- [模块完成度审计](docs/module-completeness.md)
- [生产就绪改造学习与演练](docs/production-readiness/learning-guide.md)
- [Phase 2 运营能力学习与演练](docs/production-readiness/phase-2-learning-guide.md)
- [Phase 1 验证记录](docs/production-readiness/phase-1-verification.md)
- [Phase 2 验证记录](docs/production-readiness/phase-2-verification.md)
- [Phase 3 多学校 SaaS ADR](docs/adr/ADR-003-multi-school-saas.md)
- [生产部署 Runbook](docs/production-readiness/runbook.md)
- [Flyway 迁移说明](docs/production-readiness/flyway.md)
- [备份恢复与演练](docs/production-readiness/backup-restore.md)

## 面试时可以强调的亮点

1. 不是只有 CRUD：包含登录、权限、投票、文件、贡献、竞赛编辑授权等业务规则。
2. 安全边界清楚：JWT、HttpOnly Cookie、CSRF、CORS、单 Token/全会话吊销，以及统一 4xx/5xx 错误契约。
3. 本地开发友好：Redis 可以降级到内存缓存，`.env.example` 明确列出配置。
4. 前后端契约清楚：统一返回、Axios 拦截器、接口地图、类型定义。
5. 工程化有验证：后端测试、前端构建、启动文档和排错手册。
6. AI 扩展路线明确：资源库可以自然演进为 RAG 知识库，竞赛和资源查询可以封装成 Agent 工具。

## 当前生产就绪状态与后续优先级

本轮已经落地：

- 生产数据库改为 Flyway V1-V5 版本链；`db/schema.sql` 保留为学习快照，`db/seed.sql` 只允许 dev/test 显式注入运行时口令后加载。
- 开发与生产 Compose 分离，生产只发布 Caddy 80/443；后端使用独立 MySQL 用户、Redis、Secure Cookie、可信代理和非 Root 容器。
- 统一 HTTP status、`errorCode`、`traceId` 和 `X-Request-ID`，补齐 JSON 日志、指标、readiness/liveness、优雅停机、备份恢复和回滚 Runbook。
- Phase 2 已加入账号生命周期、到期匿名化、密码找回/修改、会话吊销、审计、个人数据导出、原子上传配额、邮件有限重试/崩溃补偿和定时任务幂等。
- Phase 3 只完成多学校 SaaS ADR 与迁移方案，当前仍是单学校系统，没有直接实现 tenant 隔离或支付。
- 2026-08-26 GitHub Actions `#33010814757` 在 `fcddab2` 上全绿：后端单测、MySQL/Redis/Flyway Testcontainers、前端 lint/build/test/audit、Compose fail-fast、依赖/配置扫描、前后端镜像构建与 Trivy、关键 Playwright E2E 全部通过。此前 `#33009147368` 的前端镜像 Trivy 失败来自最终 `node:20-alpine` 镜像中运行时不需要的全局 npm `node-tar` 依赖；runner 现用 `node` 直接启动 Next.js、移除 npm/npx 并执行 Alpine 升级，已由本次远端扫描验证。

发布前仍需完成：

1. GitHub Actions 已对当前源码完成镜像构建、Trivy 和 Playwright 登录/CSRF/权限等关键路径验收；本机 Docker 数据盘仍不可用。上线前仍必须在健康 staging 使用真实部署环境完成备份恢复演练，并记录镜像 digest 和恢复证据。
2. 在 staging 演练到期匿名化、备份保留和恢复；当前已实现保留期后的自动匿名化，不做不可恢复的物理删除，审批/豁免流程仍需按运营制度补齐。
3. 按 ADR 设计 tenant、membership、租户角色和行级隔离；本轮不直接大改业务表。

详细证据、环境变量、部署步骤和回滚边界分别见 `docs/production-readiness/phase-2-verification.md`、`runbook.md`、`flyway.md` 和 `backup-restore.md`。

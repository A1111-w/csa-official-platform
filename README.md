# CSA Official Platform

广州华立学院计算机协会（CSA）的官网与会员协作平台。项目采用前后端分离架构：前端负责公开展示、会员登录和管理面板，后端负责认证、角色权限、业务数据、文件和贡献记录。

## 项目能力

- **官网展示**：协会介绍、核心成员/贡献者、公开轮播和基础公共信息。
- **会员体系**：邮箱验证码注册、登录、邀请码/会员信息、JWT 无状态认证和分级角色权限。
- **资源中心**：会员资源列表、分类筛选、上传/编辑/删除、下载计数和文件上传。
- **竞赛管理**：发布和分页查看比赛，支持比赛编辑授权；保存内容时进行基础 HTML/XSS 清理。
- **简历审核**：在线编辑 Markdown 简历、绑定 Git 仓库地址、提交审核、部长审核、驳回原因和状态流转。
- **协会治理**：提案创建、成员投票、部门/成员管理，以及会长级协会介绍维护。
- **贡献度记录**：记录官网建设、资源、比赛和运维贡献，并在公开贡献墙中聚合展示。
- **运营工具**：成员 Excel 导出、邮件验证码、Redis 限流、Git 仓库同步服务和 OpenAPI/Knife4j 接口描述。

## 技术栈

### 后端 `csa-official-backend`

- Java 17
- Spring Boot 3.5.8、Spring Web、Spring Security、Validation、AOP、Actuator
- MyBatis-Plus 3.5.5
- MySQL、Redis（缓存和限流）
- JJWT、JGit、EasyExcel、Jsoup、Knife4j

### 前端 `csa-official-frontend`

- Next.js 16、React 19、TypeScript
- Tailwind CSS 4、Radix UI、Lucide React
- Axios、Zustand、React Hook Form、Zod

## 目录结构

```text
.
├─ csa-official-backend/          # Spring Boot API
│  ├─ src/main/java/com/csa/official/
│  │  ├─ common/                  # 统一响应、异常、安全、限流和工具
│  │  ├─ config/                  # Security、Redis、CORS、MyBatis、OpenAPI
│  │  └─ modules/
│  │     ├─ biz/                  # 比赛和 Git 仓库相关业务
│  │     ├─ resume/               # 简历保存与审核
│  │     └─ sys/                  # 用户、部门、资源、投票、贡献和配置
│  └─ src/main/resources/application.yml
└─ csa-official-frontend/         # Next.js Web 前端
   └─ src/
      ├─ app/                     # 首页、登录、注册和管理面板路由
      ├─ components/              # 业务组件、布局组件和 UI 组件
      ├─ services/                # 认证、公共信息、简历等 API 封装
      ├─ store/                   # Zustand 客户端状态
      └─ config/                  # 菜单和角色可见性配置
```

## 本地运行

### 1. 后端

环境要求：JDK 17、Maven（或仓库自带 Maven Wrapper）、MySQL、Redis。

后端默认监听 `8080`，配置从环境变量读取：

```text
SERVER_PORT=8080
DB_URL=jdbc:mysql://localhost:3306/csa?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
DB_USERNAME=your_user
DB_PASSWORD=your_password
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=your_redis_password
JWT_SECRET=replace-with-a-long-random-secret
JWT_EXPIRATION=604800000
MAIL_USERNAME=your_mail_account
MAIL_PASSWORD=your_mail_smtp_password
UPLOAD_PATH=/absolute/path/to/csa-upload
```

启动：

```bash
cd csa-official-backend

# Windows
.\mvnw.cmd spring-boot:run

# Linux/macOS
./mvnw spring-boot:run
```

`application.yml` 当前默认开启 Redis SSL；如果本地 Redis 没有 TLS，需要在本地配置中关闭对应 SSL 设置。仓库当前没有根级数据库初始化 SQL，首次部署前需要准备与实体/Mapper 对应的数据库表结构。

### 2. 前端

前端默认监听 `3000`。在 `csa-official-frontend` 下创建 `.env.local`，指定后端地址：

```dotenv
NEXT_PUBLIC_API_URL=http://localhost:8080
```

安装并启动：

```bash
cd csa-official-frontend
npm ci
npm run dev
```

浏览器访问 `http://localhost:3000`。

## 角色等级

后端使用方法级权限控制，前端菜单也按最低等级隐藏：

| 等级 | 角色 | 典型权限 |
| ---: | --- | --- |
| 0 | 路人 | 访问公开内容、基础面板 |
| 1 | 会员 | 查看和下载资源 |
| 2 | 核心成员 | 编辑和提交个人简历 |
| 3 | 部长 | 管理比赛、投票、部门和审核简历 |
| 4 | 会长 | 修改协会配置、导出成员等管理操作 |
| 99 | 超级管理员 | 系统级管理 |

## API 与安全边界

- `/api/auth/**` 和 `/api/public/**` 提供登录注册及公开内容接口。
- 其他业务接口默认要求 `Authorization: Bearer <JWT>`。
- 登录、注册、发送验证码等接口使用 Redis 计数限流。
- 密码使用 BCrypt 保存；竞赛内容写入前经过 Jsoup 基础白名单清理。
- 邮件、数据库、Redis、JWT 和上传目录都应通过部署环境变量注入，不要提交真实密钥。

## 开发状态

项目目前以协会官网和内部协作平台为目标，后端和前端功能正在持续完善。提交新功能时，建议同时补充对应的实体/Mapper、接口权限、前端 API 封装和页面状态处理。

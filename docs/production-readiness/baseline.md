# Production Readiness Baseline

记录时间：2026-07-29 22:40（Asia/Shanghai）

## Git 基线

- 分支：`main`，跟踪 `origin/main`
- HEAD：`43889f95ea36f1d5cc865a835e4a05ee3878d3a7`
- 最近提交：`2026-01-14 22:41:58 +0800 添加 Dockerfile 配置`
- 根目录没有实体 `AGENTS.md`；本轮以操作者在任务消息中提供的完整 AGENTS 指令为准。
- 工作区在本轮开始前已经包含大量未提交修改：62 个已跟踪文件被修改，并有大量未跟踪源码、测试、数据库、部署和文档文件。
- 这些初始改动一律视为已有工作，不执行 reset、checkout、stash、覆盖或删除。本轮修改将记录在本目录的阶段交付文档和 `CHANGELOG.md` 中。

### `git status --short --branch` 摘要

```text
## main...origin/main
62 tracked files modified
Untracked groups:
- CHANGELOG.md, README.md, docker-compose.yml
- csa-official-backend: Docker/config, cache/security/service/VO additions and tests
- csa-official-frontend: Docker/config, pages/components/services/types/proxy additions
- db/, docs/, logs/, security-war/
```

初始已跟踪修改文件由下列命令确定：

```powershell
git diff --name-only
```

```text
csa-official-backend/.gitignore
csa-official-backend/pom.xml
csa-official-backend/src/main/java/com/csa/official/common/annotation/RateLimit.java
csa-official-backend/src/main/java/com/csa/official/common/aspect/ContributionAspect.java
csa-official-backend/src/main/java/com/csa/official/common/aspect/RateLimitAspect.java
csa-official-backend/src/main/java/com/csa/official/common/exception/GlobalExceptionHandler.java
csa-official-backend/src/main/java/com/csa/official/common/security/JwtAuthenticationEntryPoint.java
csa-official-backend/src/main/java/com/csa/official/common/util/JwtUtils.java
csa-official-backend/src/main/java/com/csa/official/config/CorsConfig.java
csa-official-backend/src/main/java/com/csa/official/config/JwtAuthenticationFilter.java
csa-official-backend/src/main/java/com/csa/official/config/RedisConfig.java
csa-official-backend/src/main/java/com/csa/official/config/SecurityConfig.java
csa-official-backend/src/main/java/com/csa/official/config/WebMvcConfig.java
csa-official-backend/src/main/java/com/csa/official/modules/biz/controller/CompetitionController.java
csa-official-backend/src/main/java/com/csa/official/modules/biz/service/CompetitionService.java
csa-official-backend/src/main/java/com/csa/official/modules/biz/service/GitService.java
csa-official-backend/src/main/java/com/csa/official/modules/resume/controller/ResumeController.java
csa-official-backend/src/main/java/com/csa/official/modules/resume/service/ResumeService.java
csa-official-backend/src/main/java/com/csa/official/modules/sys/controller/AuthController.java
csa-official-backend/src/main/java/com/csa/official/modules/sys/controller/CarouselController.java
csa-official-backend/src/main/java/com/csa/official/modules/sys/controller/ContributionController.java
csa-official-backend/src/main/java/com/csa/official/modules/sys/controller/DeptController.java
csa-official-backend/src/main/java/com/csa/official/modules/sys/controller/PublicController.java
csa-official-backend/src/main/java/com/csa/official/modules/sys/controller/ResourceController.java
csa-official-backend/src/main/java/com/csa/official/modules/sys/controller/SysUserController.java
csa-official-backend/src/main/java/com/csa/official/modules/sys/controller/TestController.java
csa-official-backend/src/main/java/com/csa/official/modules/sys/controller/VoteController.java
csa-official-backend/src/main/java/com/csa/official/modules/sys/mapper/ContributionLogMapper.java
csa-official-backend/src/main/java/com/csa/official/modules/sys/mapper/UserMapper.java
csa-official-backend/src/main/java/com/csa/official/modules/sys/mapper/VoteRecordMapper.java
csa-official-backend/src/main/java/com/csa/official/modules/sys/service/CsaSecurityService.java
csa-official-backend/src/main/java/com/csa/official/modules/sys/service/DeptService.java
csa-official-backend/src/main/java/com/csa/official/modules/sys/service/FileService.java
csa-official-backend/src/main/java/com/csa/official/modules/sys/service/MailService.java
csa-official-backend/src/main/java/com/csa/official/modules/sys/service/UserDetailsServiceImpl.java
csa-official-backend/src/main/java/com/csa/official/modules/sys/service/VoteService.java
csa-official-backend/src/main/java/com/csa/official/modules/sys/task/ContributionTask.java
csa-official-backend/src/main/resources/application.yml
csa-official-frontend/README.md
csa-official-frontend/next.config.ts
csa-official-frontend/package-lock.json
csa-official-frontend/package.json
csa-official-frontend/src/app/(auth)/login/page.tsx
csa-official-frontend/src/app/(auth)/register/page.tsx
csa-official-frontend/src/app/dashboard/layout.tsx
csa-official-frontend/src/app/dashboard/page.tsx
csa-official-frontend/src/app/dashboard/resume/page.tsx
csa-official-frontend/src/app/globals.css
csa-official-frontend/src/app/layout.tsx
csa-official-frontend/src/app/page.tsx
csa-official-frontend/src/components/business/auth/LoginForm.tsx
csa-official-frontend/src/components/business/auth/RegisterForm.tsx
csa-official-frontend/src/components/layout/DashboardSidebar.tsx
csa-official-frontend/src/components/layout/Footer.tsx
csa-official-frontend/src/components/layout/Navbar.tsx
csa-official-frontend/src/config/menu.ts
csa-official-frontend/src/lib/axios.ts
csa-official-frontend/src/services/auth.ts
csa-official-frontend/src/services/public.ts
csa-official-frontend/src/services/resume.ts
csa-official-frontend/src/store/useAuthStore.ts
packed-project.xml
```

## 工具链基线

| 工具 | 版本 |
| --- | --- |
| Java | 21.0.8 LTS |
| Node.js | 22.17.0 |
| npm | 10.9.2 |
| Docker | 29.0.1 |
| Docker Compose | 2.40.3 |

## 验证基线

| 检查 | 结果 | 关键证据 |
| --- | --- | --- |
| `csa-official-backend\\mvnw.cmd test` | 通过 | 87 tests，0 failures，0 errors，0 skipped；17.787 秒 |
| `npm run lint` | 通过 | ESLint exit code 0；86.1 秒 |
| `npm run build` | 通过 | Next.js 16.2.10，18 个静态页面单元，16 个业务路由；137.7 秒 |
| 前端测试 | 缺失 | `package.json` 没有 `test` 脚本 |
| `docker compose config` | 通过但不满足生产要求 | 当前单文件 Compose 可解析，但包含默认秘密、root 数据库用户，并公开 MySQL/Redis 端口 |

## 已知基线风险

1. 当前数据库初始化依赖 `db/schema.sql` 和 `db/seed.sql` 的首次容器启动机制，不是版本化迁移。
2. 当前 Compose 混合开发和生产用途，并存在固定默认秘密、共享数据库高权限账号与基础设施端口暴露。
3. 当前 Actuator 只公开 health/info，没有 Prometheus 指标、细分探针或生产 JSON 日志验证。
4. 当前没有前端单元测试、真实 MySQL/Redis 集成测试或 Playwright E2E。
5. 本文件建立后才开始本轮代码修改；后续新增或再次修改的文件以阶段交付清单为准。

# Changelog

本文件记录 CSA Official 的行为变化、配置变化、数据库变化和验证结果，供维护者与后续 AI 在修改项目前快速建立上下文。

## 维护规则

1. 修改项目前先阅读本文件，再结合 `git status`、`git diff` 和相关源码确认现状。
2. 新修改先写入 `[Unreleased]`；提交或发布时再移动到带日期的版本区。
3. 记录“改了什么、为什么改、影响哪里、是否需要迁移、怎么验证”，不要只列文件名。
4. 涉及接口、环境变量、数据库结构、权限或部署方式时必须明确写出兼容性影响。
5. 不得记录密码、Token、Cookie 值、邮箱授权码或其他私密环境变量值。
6. 不删除历史条目；发现旧记录不准确时追加更正说明。

## [Unreleased]

### Added

- 完成此前模块审计列出的产品缺口：到期账号匿名化、贡献排行、邮件崩溃补偿、上传配额原子计数、简历 Git 同步、首页轮播、审计日志页面和成员导出页面。
- 新增 `/dashboard/audit` 与 `/dashboard/member-export`，并在工作台按 Level 4 权限显示入口。
- 新增 `docs/feature-completion-learning-guide.md`，记录调用链、测试方法、独立 commit 和非破坏性回退方式。
- 引入 Flyway V1 结构迁移、生产/开发 Compose 分离、Caddy 同源反向代理、非 Root 后端镜像和生产环境变量示例。
- 增加请求 ID/MDC、`X-Request-ID`、Prometheus 指标、liveness/readiness 探针、graceful shutdown、生产 JSON 日志与基础告警规则示例。
- 增加 `deploy/backup.ps1` 与 `deploy/restore.ps1`，覆盖 MySQL、上传文件、SHA-256 校验、保留策略和恢复确认。
- 增加生产就绪 Runbook、Flyway 迁移说明、备份恢复说明和对应学习演练文档。
- 增加 `phase-1-verification.md`，记录隔离数据库、镜像、备份恢复和同源 HTTPS E2E 的实际结果及回滚路径；历史镜像证据与当前源码复验边界已在文档中标明。
- 增加 `phase-2-verification.md`，记录账号生命周期、审计、个人数据导出、上传元数据、邮件重试、定时任务幂等和真实依赖测试的实际结果。
- 完成简历审核前后端闭环：成员提交后，部长可在 `/dashboard/resume-reviews` 分页查看队列、打开详情并通过/驳回；新增审核接口说明、学习链路和模块完成度审计文档。
- 完成人工贡献记录前后端闭环：Level 4 可在 `/dashboard/contributions` 搜索成员、记录贡献并分页查询 `AUTO`、`MANUAL`、`LEGACY` 流水；写入操作人和管理审计事件。
- 完成首页轮播后台管理闭环：Level 4 可在 `/dashboard/carousels` 查看启用/停用项，上传或填写图片地址，并执行预览、新增、编辑、排序、启停和删除。
- 新增 `docs/carousel-management-learning-guide.md`，记录轮播公开展示与后台维护的调用链、DTO/VO 边界、文件授权、缓存、测试和非破坏性回退方法。

### Changed

- 2026-08-26：前端 runner 镜像在构建完成后执行 `apk upgrade --no-cache`，并移除运行时不需要的全局 `npm`/`npx`，改由 `node node_modules/next/dist/bin/next start` 启动 Next.js。该改动针对 GitHub Actions `#33009147368` 中前端镜像全局 npm 自带的 `node-tar` 漏洞；不修改应用 `package-lock.json`、数据库或业务行为。回退时仅需 revert 本提交并重新构建镜像。
- 2026-08-26 GitHub Actions 安全门禁修复：Spring Boot 从 `3.5.12` 升级到 `3.5.14`，并以受控 patch-level 覆盖 Spring Framework `6.2.19`、Spring Data `2025.0.12`、Tomcat `10.1.55`、Micrometer `1.15.12`、Netty `4.1.136.Final`、Jackson BOM `2.21.4` 和 Commons IO `2.20.0`。后端 runner 镜像在创建运行用户前执行 `apk upgrade --no-cache`，以取得基础镜像可用的安全补丁；不引入迁移或运行时行为变更。
- CI 移除仅在启用 GitHub Advanced Security 的私有仓库可用的 `actions/dependency-review-action`；保留并继续强制 Trivy 文件系统/镜像扫描、`npm audit`、后端测试、Testcontainers、Compose fail-fast 和 Playwright E2E。
- 2026-08-26 CI 修复 checkpoint 将 Spring Boot 从 `3.5.8` 升级到 `3.5.12`，将 Next.js 与 `eslint-config-next` 升级到 `16.3.3`；前端 lockfile 同步更新受影响的传递依赖，完整与 production `npm audit` 均为 0 vulnerabilities。
- 前端 Axios 响应断言改用 TypeScript `satisfies Partial<ApiError>`，适配 Next.js 16.3.3 的类型检查，不改变运行时请求或错误处理行为。
- 开发 Compose 仅将基础设施绑定到回环地址，并使用独立应用数据库用户；生产 Compose 不发布 MySQL、Redis、backend、frontend 端口。
- 生产部署强制 `production` profile、HTTPS origin、Secure Cookie、Redis、CSRF 和可信代理配置；Caddy 增加 HSTS。
- 测试配置改为 `test` profile + H2，不再以同名 `application.yml` 遮蔽主配置；Surefire 自动激活 test profile。
- 同步根 README、项目学习指南、部署手册、接口地图和安全说明：生产结构以 Flyway V1-V5 为准，`db/schema.sql` 只作学习快照，补齐 Phase 2 账号匿名化、审计、导出、原子配额、邮件补偿和任务口径，并标明当前 Docker 故障。
- 简历审核列表使用轻量 `ResumeReviewListVO`，详情使用白名单 `ResumeReviewDetailVO`；申请人和部门按页批量加载，避免审核队列 N+1 查询。
- 贡献管理历史使用 `ContributionAwardVO` 和批量成员/部门加载，人工记录不再直接绑定完整实体；贡献类型、分值、说明和目标账号状态均在后端校验。
- 轮播公开读取与后台维护使用独立契约：匿名接口只返回启用项和渲染字段，管理接口返回启用/停用状态、排序和时间字段；保存使用 `CarouselSaveRequest` 白名单 DTO。

### Fixed

- 修复 Linux CI 中测试上下文在已激活 `test` profile 后仍创建 `RedisConnectionFactory`、进而因空 `REDIS_HOST` 连锁失败的问题；测试 profile 现显式排除 Redis 自动配置，并新增“没有 `RedisConnectionFactory` Bean”的回归断言。生产 Redis 配置和运行时行为未改变；当前工作区复验为后端 174 tests、前端 6 files / 12 tests，lint 与 production build 通过。
- 修复 GitHub Actions 引用不存在的 `aquasecurity/trivy-action@0.28.0` 导致扫描任务在下载 Action 阶段失败的问题；改为固定到官方 `v0.36.0` 对应的不可变 commit SHA。
- 修复 Flyway 引入后 Spring Boot 测试上下文因测试资源同名遮蔽主配置而缺失 DataSource 的回归。
- 修复生产 MySQL JDBC URL 使用明文连接且关闭公钥回取导致 MySQL 8 默认认证插件无法登录的问题；生产改为强制 TLS。
- 修复 Redis 缓存反序列化 `User.balance` 的 `BigDecimal` 被安全类型白名单拒绝而导致登录 500 的问题，并增加序列化回归测试。
- 修复简历审核此前只有按 ID 手工调用接口、没有队列和网页入口的问题；补上草稿隐私、审核中禁止编辑、驳回原因校验和并发审核冲突处理。
- 修复人工贡献接口缺少事务、来源和操作人追踪的问题；旧贡献流水不猜测来源，统一标记为 `LEGACY`。
- 修复轮播此前只有首页展示和后端保存/删除接口、管理员仍需手工调用 API 的问题；后台现可完整维护停用项、排序、跳转和发布状态。
- 修复轮播编辑时清空跳转地址可能被 ORM 忽略的问题；管理字段改用显式更新 SQL，空跳转会写入数据库 `NULL`。
- 修正文档、公开隐私说明与账户设置页面仍把已实现的到期匿名化、原子上传配额和邮件崩溃补偿描述为“未实现”的问题；同时把集成测试的迁移验收从 V3 更新到 V5。

### Security

- 生产 Compose 的数据库、Redis、JWT、邮件和公网地址变量使用 `${VAR:?required}`；仓库新增根级忽略规则，备份产物和本地秘密不进入 Git。
- 生产后端镜像以 UID/GID 10001 运行，根文件系统只读并使用受限 tmpfs。
- 轮播保存限制图片和跳转为安全站内路径或 HTTP(S)，站内上传只允许发布者本人拥有的有效 JPG/PNG/GIF；启用轮播图片可匿名读取，停用项只允许 Level 4 预览，其他私人文件权限不变。

### Database / Config

- 本 CI checkpoint 不新增 Flyway migration、不修改生产环境变量、不改变数据库结构；回退应用提交后执行 `npm ci` 即可恢复对应的前端依赖树。
- 2026-08-27 轮播管理不新增 Flyway migration、不修改数据库结构、不新增环境变量，也不修改 Maven/npm 依赖或 lockfile；功能提交为 `f295e24`，回退使用 `git revert f295e24`。
- 空数据库执行 `db/migration/V1__initial_schema.sql`；已有数据库 baseline/升级和失败迁移处理见 `docs/production-readiness/flyway.md`。
- 生产不执行 `db/seed.sql`；演示 seed 仅允许 `dev`/`test` profile 且显式开启。
- 新增 Flyway `V3__resume_review_queue_index.sql`，为 `biz_resume(status, update_time)` 增加审核队列索引；回滚应用时保留该向前兼容索引，不原地修改已执行 migration。
- 新增 Flyway `V4__account_storage_and_git_sync.sql`，为账号匿名化、上传配额计数、邮件恢复索引和简历 Git 同步补齐结构；已执行后回滚应用代码时保留结构。
- 新增 Flyway `V5__contribution_award_audit.sql`，为 `sys_contribution_log` 增加 `source`、`awarded_by` 和管理历史索引；已有记录标记为 `LEGACY`，回退应用代码前不得删除已执行的 V5，结构回退需另写补偿 migration。

### Verification

- 2026-08-26：GitHub Actions `#33010814757` 在 `fcddab2` 上全绿：后端单测、MySQL/Redis/Flyway Testcontainers、前端 lint/build/test/audit、Compose fail-fast、依赖/配置扫描、前后端镜像构建与 Trivy、关键 Playwright E2E 均通过。前端 runner 移除运行时不需要的全局 npm/npx 并升级 Alpine 包后，前端镜像 Trivy 已通过；本机 Docker 数据盘故障不影响这份远端当前源码证据，备份恢复 staging 演练仍需单独完成。
- 更正：此前“GitHub Actions 远端结果待本 checkpoint 推送后确认”的历史记录已由 `#33010814757` 完成确认；该表述仅保留当时的验证时间点，不代表当前状态。
- 2026-08-26：GitHub Actions `#33009147368` 已通过后端单测、MySQL/Redis/Flyway Testcontainers、前端 lint/build/test/audit、Compose fail-fast 和关键 Playwright E2E；Docker job 仅在前端镜像 Trivy 扫描失败。报告显示 `node:20-alpine` 最终镜像中的全局 npm `node-tar` 有 19 个高危/严重项，另有 4 个 Alpine 高危项；该问题已由后续 `#33010814757` 修复并验证通过。
- 2026-08-26：GitHub Actions `#33006154587` 在 `5b5457a` 上通过后端单测、MySQL/Redis/Flyway Testcontainers、前端 lint/build/test/audit、Compose fail-fast 和关键 Playwright E2E。失败的两项已定位：私有仓库未启用 GitHub Advanced Security，导致 `dependency-review-action` 不可运行；后端镜像 Trivy 检出均有修复版本的 Alpine 与 Java 依赖漏洞。当前提交应用相应修复；其远端扫描结果以新运行记录为准。
- 2026-08-26：本提交在本地通过 `csa-official-backend\\mvnw.cmd test`（174 tests，0 failures，0 errors，1 个 Docker Testcontainers skipped）、`npm run test`（6 files / 12 tests）、`npm run lint`（0 errors，1 条既有 warning）、`npm run build`（25/25）和 `npm audit --omit=dev --audit-level=high`（0 vulnerabilities）；开发 Compose 可解析，生产 Compose 对缺少必填秘密按预期 fail-fast，临时占位值可展开。
- 2026-08-26：在无 `.env`、空 `REDIS_HOST` 的等价 CI 配置下，`mvnw.cmd test` 为 174 tests、0 failures、0 errors、1 Docker Testcontainers skipped；`npm ci`、`npm run test`（6 files / 12 tests）、`npm run lint`（0 errors、1 既有 warning）、`npm run build`（Next.js 16.3.3，25/25）及完整/production `npm audit` 均通过。GitHub Actions 远端结果待本 checkpoint 推送后确认。
- 2026-08-26：后端全量 163 个测试通过、0 失败、0 错误，1 个 Testcontainers 测试因 Docker 不可用跳过；前端 5 个测试文件、10 个测试通过，`npm run lint` 和 `npm run build` 通过；Playwright 公开边界 2 个通过，3 个真实账号场景因未配置 E2E 凭据跳过。
- `csa-official-backend\\mvnw.cmd test`：本轮 Redis 修复后的全量测试通过，最终计数见 `phase-1-verification.md`。
- 前端 `npm run test`：3/3 通过；`npm run lint` 和 `npm run build` 通过。
- 开发 Compose config 通过；生产 Compose 缺少必需秘密时 exit 1，使用临时假值展开通过，只有 Caddy 发布 80/443。
- 2026-07-29 的 Docker 隔离栈已完成空库、已有库、失败迁移、备份恢复和 HTTPS E2E；本机 80 端口冲突使 HTTPS 验收使用 `8443` 映射，生产定义仍为 80/443。2026-07-30 当前源码的单测、Testcontainers、前端和 Compose 静态检查通过，但 Docker VHD 发生 I/O/EXT4 journal 故障，当前镜像、Playwright、备份恢复和 Trivy 复验仍是发布阻断项。
- V2 迁移新增账号状态/会话版本、邮箱和学号唯一约束、审计、文件元数据、邮件投递状态和定时任务幂等表；旧库升级必须先查规范化后的重复值。
- 修正隐私说明：删除申请先进入保留期，符合条件的账号由计划任务自动匿名化；该能力不等同于物理删除，备份副本仍按备份保留策略到期处理。
- 2026-08-26：本轮定向简历测试 8/8 通过；后端全量测试 133 个通过、0 失败、1 个因 Docker 不可用跳过；前端测试 6/6、`npm run lint` 和 `npm run build` 通过。浏览器级简历审核 E2E 仍需在具备真实数据库、Redis 和部长测试账号的环境补跑。
- 文档更正：V4/V5 已加入当前 Flyway 版本链，旧文档中“最新版本为 V2/V3”和旧测试计数均保留为历史证据或已改为当前验证口径；当前 V1-V5 真实 MySQL/Testcontainers 结果仍待健康 Docker/staging 重跑。
- 2026-08-26：人工贡献专项后端 `11/11` 通过；后端全量共 `174` 个测试，0 失败、0 错误、1 个 Docker Testcontainers 测试跳过；前端 Vitest `6` 个文件/`12` 个测试通过，`npm run lint` 和 `npm run build` 通过。
- 2026-08-27：轮播与文件访问专项后端 `16/16` 通过；后端全量 `185` 个测试通过、0 失败、0 错误，1 个 Docker/Testcontainers 测试因本机 Docker 不可用跳过；前端 Vitest `7` 个文件/`13` 个测试通过，ESLint 0 错误（保留 `src/lib/axios.ts` 的 1 条既有 warning），production build 通过并包含 `/dashboard/carousels`；Playwright 3 个可运行场景通过，3 个需要真实账号的场景按配置跳过。

## [2026-07-27] Opus 5 优化基线（尚未提交）

> 重要：本节根据 2026-07-27 当前工作区与 Git 基线的差异反向整理。仓库没有对应这批优化的 Git commit，因此它是“当前工作区基线”，不是已经发布的正式版本。

### Added

- 新增根目录项目说明及架构、接口、安全、数据库、部署、本地开发、学习演示和 RAG/Agent 路线文档，集中放在 `README.md` 与 `docs/`。
- 新增 MySQL 初始化结构与演示数据：`db/schema.sql`、`db/seed.sql`，覆盖 12 张业务表。
- 新增根目录 `docker-compose.yml`，可编排 MySQL 8、Redis 7、Spring Boot 后端和 Next.js 前端；补充前后端 Docker 构建文件及 `.dockerignore`。
- 新增 `KeyValueStore` 缓存抽象及 Redis、进程内存两种实现。验证码、限流、JWT 吊销和部分业务锁不再直接绑定某一种缓存。
- 新增 CSRF 双提交校验、JWT 吊销服务、统一安全响应写入器、生产启动安全校验和独立异步线程池。
- 新增资源业务 `ResourceService`、受控文件读取 `StoredFileController`、统一分页参数工具 `PageUtils`。
- 新增资源、比赛、简历、部门、成员目录、提案、轮播图等 VO，收敛实体直接暴露和字段泄漏。
- 新增公开比赛接口 `/api/public/competitions` 及详情接口，使公开展示与后台管理接口分离。
- 前端新增关于、比赛、贡献者、资源页，以及资源、比赛、个人资料、部门、提案、公开设置等工作台模块。
- 前端新增统一 API 类型、业务 service、权限工具、安全跳转、HTML 清洗、登录态启动校验和 Dashboard 守卫。
- 新增后端 Controller、Service、安全、文件、数据库结构与种子密码等测试。

### Changed

- 浏览器登录态由前端保存 JWT 改为后端写入 `CSA_AUTH_TOKEN` HttpOnly Cookie；Zustand 只持久化用户名和角色等级，不再保存 Token。
- Axios 统一启用 `withCredentials`，自动获取并附加 CSRF Header；响应拦截器把后端 `R<T>` 解包为 `data`，并统一包装接口错误和处理 401 跳转。
- 后端保留 `Authorization: Bearer` 兼容能力，供 API 调试或非浏览器客户端使用。
- Controller/Service 职责进一步拆分：资源规则、比赛权限、部门任命、简历状态和投票规则集中到 Service，Controller 负责参数与 HTTP 边界。
- 列表/详情接口逐步改为 VO 输出；竞赛和简历状态对外返回数字 code，匹配前端 TypeScript 契约。
- 比赛公开列表只展示已发布内容；编辑比赛时前端先拉取详情，避免用列表摘要覆盖完整正文。
- 贡献记录与邮件发送改为独立异步执行，降低主请求被 I/O 阻塞的风险。
- 资源下载计数改为数据库原子自增；分页大小统一由 `PageUtils` 限制。
- JJWT 从 `0.11.5` 升级到 `0.12.6`，EasyExcel 从 `3.3.3` 升级到 `3.3.4`。
- 前端 Next.js 从 `16.0.7` 升级到 `16.2.10`，React/React DOM 从 `19.2.0` 升级到 `19.2.4`，Axios 固定为 `1.18.1`，并加入 `isomorphic-dompurify`。
- 前端采用 App Router 页面入口 + 业务组件 + services + Axios 的分层方式；`src/proxy.ts` 负责 Dashboard Cookie 入口保护和 CSP nonce。

### Fixed

- 修复未校验 `merchantNo` 可能影响注册角色的问题；普通注册默认保持游客等级，邀请码才可升级会员。
- 修复 JWT 中旧角色声明可能绕过数据库最新角色的问题，鉴权以数据库当前用户状态为准。
- 修复注销后 Token 仍可继续使用的问题，Token 在剩余有效期内进入吊销集合。
- 修复比赛协作者授权仅靠 Controller 判断的问题，Service 层再次校验发布者、会长和 Root 权限。
- 修复公开比赛可能泄露未发布内容，以及比赛列表摘要可能在编辑时截断正文的问题。
- 修复简历状态枚举与前端数字状态不匹配、审核中仍可编辑、并发提交锁不可靠等问题。
- 修复部门任命后旧部长未正确降级、会长或 Root 被错误任命为部长等业务边界。
- 修复投票重复提交、票数统计和高风险 `ROOT_APPLY` 自动提权问题；该提案即使通过也不会自动提升权限。
- 修复资源越权编辑/删除、超大分页、并发下载计数丢失和实体内部字段外泄问题。
- 修复文件名路径穿越、扩展名与文件签名不匹配、超限文件写盘和上传目录直接静态暴露等问题。
- 修复 Git 仓库地址可指向非允许主机、回环地址、带凭据 URL 或非 HTTPS 协议的问题。
- 修复富文本直接展示带来的 XSS 风险，后端使用 Jsoup、前端使用 DOMPurify 双层清洗。
- 修复开放重定向风险，登录后的 `redirect` 参数只接受站内安全路径。

### Security

- Cookie 认证的非安全方法启用 CSRF 双提交校验；登录、注册、验证码和 CSRF 获取接口按设计豁免。
- 统一输出 JSON 格式的 401/403，与普通业务错误保持相同 `R<T>` 外形。
- 收紧 CORS 来源并允许凭据；仅信任显式配置的代理来源提供的转发 IP Header。
- 登录、注册、验证码等接口按用户标识和可信客户端 IP 组合限流；Redis 实现使用原子 Lua 操作。
- 后端增加 CSP、HSTS、`X-Frame-Options`、`Referrer-Policy`、`Permissions-Policy` 等安全响应头。
- 前端增加 nonce CSP、生产 HSTS 和 Dashboard 服务端入口保护。
- 生产环境启动时校验 Cookie `Secure` / `SameSite` 组合；JWT 密钥至少要求 32 字节。
- Swagger/OpenAPI 默认关闭，Actuator 只公开 `health` 与 `info`。

### Database / Config

- `application.yml` 支持从后端目录的 `.env` 可选导入配置。
- 新增 HikariCP 连接池大小、空闲连接和超时参数。
- 缓存通过 `CSA_CACHE_TYPE=redis|memory` 切换；本地 `.env.example` 默认使用 `memory`，Docker Compose 默认使用 `redis`。
- Redis 配置补充端口、用户名、密码、database 和 SSL 开关；本地明文 Redis 必须设置 `REDIS_SSL=false`。
- 新增 Cookie、CSRF、CORS、可信代理、上传大小、Git 允许主机、Swagger 和代理转发等环境变量。
- 数据库使用逻辑关联而非物理外键；唯一索引防止重复用户名、邀请码、竞赛协作者和投票记录。
- Docker Compose 首次创建 MySQL 数据卷时自动执行 schema 与 seed；修改 SQL 后旧数据卷不会自动重放初始化脚本。

### Documentation

- 文档已经覆盖项目结构、请求链路、角色权限、配置启动、接口地图、数据库、Docker 部署、安全设计和学习路线。
- `packed-project.xml` 是历史打包快照，不能替代当前源码和本日志；阅读项目时以真实工作区文件为准。

### Verification

- 2026-07-27：`csa-official-backend\\mvnw.cmd test` 通过，共 87 个测试，0 Failures、0 Errors、0 Skipped。
- 2026-07-27：`npm run lint` 通过。
- 2026-07-27：`npm run build` 通过，Next.js 生成 16 个应用路由并启用 Proxy Middleware。
- 本轮未执行完整 Docker Compose 启动和真实 MySQL/Redis/QQ 邮箱端到端联调。

### Known Limitations

- 这批优化仍在未提交工作区中；后续修改前必须先看 `git status`，不要把现有文件误判成可删除的临时内容。
- 根 `README.md` 和 `docs/study-and-demo-guide.md` 曾写“后端 81 个测试通过”，实际当时为 87 个；该历史文档漂移已在后续记录中修正，当前 Phase 2 口径为默认 125 个，另有显式 Testcontainers 1 个。
- `docs/local-dev.md` 的数据库准备章节仍保留“后续补初始化 SQL”的旧描述，但 `db/schema.sql`、`db/seed.sql` 和 `docs/database.md` 已经存在，需要删除这段过期说明。
- RAG/Agent 目前只有路线文档，尚未实现向量库、检索链路或工具调用运行时。
- 邮件验证码依赖有效 QQ SMTP 账号和授权码；未配置时相关真实发送链路不可用。
- `memory` 缓存只适合本地单实例开发：进程重启会清空，多实例之间也不共享。

## [2026-01-14] 初始 Git 基线

- `514190d`：初始化项目。
- `43889f9`：添加 Dockerfile 配置。
- 此日期之后、2026-07-27 之前没有其它可用 Git commit；该期间的具体修改只能依据当前工作区差异追溯。

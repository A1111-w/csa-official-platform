# 功能闭环、测试与 Git 回退学习指南

更新日期：2026-08-27

本文对应账号匿名化、贡献排行、邮件补偿、上传配额、简历 Git 同步、审计日志、成员导出、人工贡献记录和轮播后台管理改造。目标是解释“为什么这样设计、请求怎么流转、测试怎么证明、Git 怎么回退”。

## 1. 版本地图

| Commit | 模块 | 回退命令 |
| --- | --- | --- |
| `b6fb812` | 账号生命周期、存储配额、邮件和 Git 同步数据库结构 | `git revert b6fb812` |
| `2f12e3a` | 到期账号匿名化、贡献排行 SQL | `git revert 2f12e3a` |
| `dfb415f` | 邮件 `PENDING` / `SENDING` 崩溃补偿 | `git revert dfb415f` |
| `ae0e198` | 上传配额原子预占和释放 | `git revert ae0e198` |
| `82a675e` | 简历 Git 同步后端状态机 | `git revert 82a675e` |
| `c5d3c45` | 简历 Git 同步前端轮询和审核展示 | `git revert c5d3c45` |
| `c517eda` | 首页轮播和贡献排行 | `git revert c517eda` |
| `4850231` | 审计日志管理页 | `git revert 4850231` |
| `2fe19b1` | 成员筛选、列选择和 Excel 导出页 | `git revert 2fe19b1` |
| `77b944f` | 人工贡献记录后端、V5 迁移和后端测试 | `git revert 77b944f`（数据库需另行向前迁移） |
| `26de897` | 人工贡献记录前端管理页、服务和前端测试 | `git revert 26de897` |
| `f295e24` | 首页轮播后台管理、上传图片公开授权和测试 | `git revert f295e24` |

回退应用代码优先使用 `git revert`，不要用 `reset --hard`。Flyway migration 一旦在数据库执行，不能通过回退 Git commit 自动撤销；需要新增更高版本的补偿 migration，并先备份和演练。

## 2. 简历 Git 同步

### 2.1 为什么不是在 HTTP 请求里直接 clone

Git clone 是不可控时长的网络和磁盘 I/O。如果 Controller 同步执行，远程仓库慢、仓库大或 DNS 异常都会长期占用 Tomcat 请求线程。因此请求只负责 claim 任务，实际 clone 交给独立 `gitSyncTaskExecutor`。

```text
POST /api/resume/git-sync
→ ResumeGitSyncService.startMySync
→ ResumeMapper.claimGitSync
→ 事务提交
→ ResumeGitSyncWorker.sync
→ GitService.syncRepository
→ completeGitSync / failGitSync
```

### 2.2 `runId` 解决什么问题

旧任务可能比新任务更晚结束。每次 claim 都写入新的 `runId`，完成和失败更新必须同时匹配 `user_id + SYNCING + runId`。旧任务即使最后返回，也不能覆盖新任务状态。

### 2.3 安全边界

- 只允许 HTTPS。
- 拒绝 URL 凭据、query、fragment 和非 443 自定义端口。
- 主机必须命中 GitHub、Gitee、GitLab 或配置白名单。
- shallow clone，设置网络超时和仓库大小上限。
- 仓库目录由服务端按 user ID 生成，不接受用户路径。
- 每个用户在单 JVM 内串行同步，数据库 claim 负责跨请求竞态。

### 2.4 前端状态机

`/dashboard/resume` 启动任务后保存 `SYNCING`，每 2 秒查询一次状态；状态离开 `SYNCING` 后停止轮询。审核页读取同一组 branch、commit、size 和 errorCode，避免审核人只看到一个未经验证的 URL。

## 3. 贡献排行和首页轮播

贡献排行不是把整张日志表读到 Java 再聚合，而是由 MySQL 完成 `SUM + COUNT + GROUP BY + ORDER BY + LIMIT`。服务按 limit 缓存，新增贡献时清空缓存。

首页使用 `Promise.allSettled` 并行读取介绍、成员、轮播和排行。某一个公开接口失败时，只降级对应区域，不把整个首页清空。轮播跳转只接受站内路径或 HTTP(S) URL，图片使用 `next/image`。

这里要区分两个提交：

- `c517eda` 完成的是首页读取和展示轮播、贡献排行的公开链路。
- `f295e24` 完成的是 `/dashboard/carousels` 可视化管理、停用项管理、上传图片发布权限和回归测试。

因此“首页能展示轮播”和“管理员能在网页维护轮播”不是同一个完成标准。详细调用链、安全边界和回退演练见 `docs/carousel-management-learning-guide.md`。

## 4. 邮件崩溃补偿

异步 SMTP 有一个典型窗口：数据库已经写入 `PENDING`，进程却在执行器真正发送前崩溃。补偿任务会扫描超时的 `PENDING` / `SENDING` 记录，按有限重试规则重新投递；数据库幂等状态和分布式任务锁保证多实例不会重复处理同一批任务。

## 5. 上传配额原子性

“先 SUM 已用空间，再 INSERT 文件”无法抵抗并发。现在先对 usage 行执行条件更新，只有 `used + requested <= quota` 才能预占成功；文件或元数据写入失败时释放预占。这样两个并发上传不能同时越过同一额度。

## 6. 管理审计

`/dashboard/audit` 只读调用 `/api/sys/audit/list`，支持 action、result、requestId 和分页。详情展示 IP、User Agent、目标对象、Request ID 和清洗后的 JSON。审计写入层会拒绝密码、Token、Cookie、Authorization 和验证码等敏感键，前端不提供修改或删除日志的能力。

## 7. 成员导出

`/dashboard/member-export` 把筛选条件和列白名单发送给 `/api/sys/export/members`。后端重新校验列名，不信任前端；响应是 Excel Blob，不走普通 `R<T>` JSON。浏览器创建临时 Object URL 下载后立即释放。

导出默认选择姓名、学号、学院、班级、手机号和角色。支付单号等字段不默认选择。每次导出记录操作人、列集合和是否使用筛选，但不记录导出的具体个人数据。

## 8. 人工贡献记录

### 8.1 业务边界

人工贡献记录用于补录系统自动切面无法捕获的线下或人工确认贡献。它不是普通成员自助加分入口，只向 `LEVEL_4` 开放。记录创建后不提供前端修改或删除按钮，后续纠正应追加一条有说明的记录，并保留原记录和审计证据。

### 8.2 请求链路

```text
POST /api/sys/contribution/award
→ ContributionController
→ ContributionService.award
→ 校验成员状态、类型、分值和说明
→ 事务写入 sys_contribution_log(source=MANUAL, awarded_by=当前操作人)
→ 清理贡献排行缓存
→ 写入 CONTRIBUTION_MANUAL_AWARD 审计事件
```

历史查询使用：

```text
GET /api/sys/contribution/awards
→ ContributionService.listAwards
→ PageUtils.of 收敛分页参数
→ 先分页流水，再批量加载成员和部门
→ 返回 ContributionAwardVO
```

`keyword` 只查用户名、姓名和学号；列表不会把密码、邮箱等不必要的用户字段带到页面。`source` 用于区分 `AUTO`、`MANUAL` 和 `LEGACY`，V5 不对旧数据来源做猜测。

### 8.3 前端使用

`/dashboard/contributions` 包含成员搜索、贡献类型选择、分值、说明、二次确认和历史筛选。页面默认显示人工记录，也可以切换查看系统自动和迁移前历史记录。成员搜索复用受权限保护的用户目录并限制单次结果数，避免随着成员数量增长一次加载全表。

### 8.4 测试重点

- Level 3 账号不能调用人工补录接口，Level 4 可以调用。
- 参数校验失败返回 400，Service 不写库。
- 不存在、已删除、已停用或已匿名化账号不能成为补录目标。
- 成功写入必须带 `source=MANUAL` 和 `awarded_by`，并触发排行缓存清理和审计。
- 历史接口分页后批量加载用户，不按每条流水重复查库。
- V5 migration 将旧行标为 `LEGACY`，不伪造历史操作人。

### 8.5 V5 升级与回退

升级前先备份 MySQL。部署包含 `77b944f` 的后端后，由 Flyway 执行 `V5__contribution_award_audit.sql`，再核对：

```sql
SELECT version, description, success
FROM flyway_schema_history
WHERE version = '5';

SHOW COLUMNS FROM sys_contribution_log LIKE 'source';
SHOW COLUMNS FROM sys_contribution_log LIKE 'awarded_by';
```

若 V5 尚未执行，可直接 `git revert 26de897 77b944f`。若 V5 已执行，应用代码仍可回退，但必须保留 V5 和新增列；旧代码会忽略额外列，新写记录将使用数据库默认值 `LEGACY`。不要删除 `flyway_schema_history` 记录，也不要修改已经执行的 V5 文件。

只有确认旧版本稳定、已备份且确实需要删除结构时，才新增更高版本补偿 migration 删除索引和两列。该操作会永久丢失来源与操作人数据，不能作为常规应用回退手段。

## 9. 验证命令

```powershell
cd D:\CSA-Project\csa-official-backend
.\mvnw.cmd test

cd D:\CSA-Project\csa-official-frontend
npm run test
npm run lint
npm run build
```

Git 同步专项测试：

```powershell
.\mvnw.cmd "-Dtest=GitServiceTest,ResumeServiceTest,ResumeGitSyncServiceTest,ResumeGitSyncWorkerTest,ResumeControllerAuthorizationTest" test
```

轮播管理专项测试：

```powershell
cd D:\CSA-Project\csa-official-backend
.\mvnw.cmd "-Dtest=CarouselControllerAuthorizationTest,StoredFileControllerTest" test

cd D:\CSA-Project\csa-official-frontend
npm run test -- src/services/carousel.test.ts src/services/public.test.ts
npm run test:e2e -- e2e/critical-workflows.spec.ts
```

2026-08-26 实际结果：

- 后端专项：38/38 通过。
- 本轮贡献专项：后端 `11/11` 通过（管理接口 5、Service 6）。
- 后端全量：174 个测试，0 失败，0 错误，1 个测试因 Docker 不可用跳过。
- 前端 Vitest：6 个测试文件、12 个测试通过。
- 前端 ESLint 和生产 build：通过，`/dashboard/contributions` 已进入构建路由表。
- Playwright：公开隐私页与未登录重定向 2 个通过；登录、CSRF/权限和上传 3 个用例因未提供 E2E 账号而跳过。

2026-08-27 轮播管理实际结果：

- 后端轮播与文件访问专项：16/16 通过。
- 后端全量：185 个测试，0 失败，0 错误，1 个 Testcontainers 测试因本机 Docker 不可用跳过。
- 前端 Vitest：7 个测试文件、13 个测试通过。
- 前端 ESLint：0 错误，保留 `src/lib/axios.ts` 的 1 条既有 warning。
- 前端 production build：通过，路由表包含 `/dashboard/carousels`。
- Playwright：公开页、未登录重定向、模拟 Level 4 轮播管理页面 3 个通过；3 个需要真实账号的场景按配置跳过。
- `git diff --check` 对本轮文件通过；用户已有 `packed-project.xml` 未纳入功能提交。

## 9. 尚需发布环境验证

本机 Docker VHD/EXT4 故障不影响源码单测和前端静态构建。当前源码的镜像构建、Trivy、真实 MySQL/Redis/Flyway 和关键 Playwright E2E 已由 GitHub Actions `#33010814757` 在 `fcddab2` 上验收通过；备份恢复仍必须在健康 staging 环境重新执行后，才能把“代码已完成”提升为“生产发布已完成”。

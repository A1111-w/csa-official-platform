# 功能闭环、测试与 Git 回退学习指南

更新日期：2026-08-26

本文对应账号匿名化、贡献排行、邮件补偿、上传配额、简历 Git 同步、审计日志和成员导出改造。目标是解释“为什么这样设计、请求怎么流转、测试怎么证明、Git 怎么回退”。

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

## 4. 邮件崩溃补偿

异步 SMTP 有一个典型窗口：数据库已经写入 `PENDING`，进程却在执行器真正发送前崩溃。补偿任务会扫描超时的 `PENDING` / `SENDING` 记录，按有限重试规则重新投递；数据库幂等状态和分布式任务锁保证多实例不会重复处理同一批任务。

## 5. 上传配额原子性

“先 SUM 已用空间，再 INSERT 文件”无法抵抗并发。现在先对 usage 行执行条件更新，只有 `used + requested <= quota` 才能预占成功；文件或元数据写入失败时释放预占。这样两个并发上传不能同时越过同一额度。

## 6. 管理审计

`/dashboard/audit` 只读调用 `/api/sys/audit/list`，支持 action、result、requestId 和分页。详情展示 IP、User Agent、目标对象、Request ID 和清洗后的 JSON。审计写入层会拒绝密码、Token、Cookie、Authorization 和验证码等敏感键，前端不提供修改或删除日志的能力。

## 7. 成员导出

`/dashboard/member-export` 把筛选条件和列白名单发送给 `/api/sys/export/members`。后端重新校验列名，不信任前端；响应是 Excel Blob，不走普通 `R<T>` JSON。浏览器创建临时 Object URL 下载后立即释放。

导出默认选择姓名、学号、学院、班级、手机号和角色。支付单号等字段不默认选择。每次导出记录操作人、列集合和是否使用筛选，但不记录导出的具体个人数据。

## 8. 验证命令

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

2026-08-26 实际结果：

- 后端专项：38/38 通过。
- 后端全量：163 个通过，0 失败，0 错误，1 个 Testcontainers 测试因 Docker 不可用跳过。
- 前端 Vitest：5 个测试文件、10 个测试通过。
- 前端 ESLint 和生产 build：通过，共生成 24 个应用路由。
- Playwright：公开隐私页与未登录重定向 2 个通过；登录、CSRF/权限和上传 3 个用例因未提供 E2E 账号而跳过。

## 9. 尚需发布环境验证

本机 Docker VHD/EXT4 故障不影响源码单测和前端静态构建，但当前源码镜像、Playwright、备份恢复和 Trivy 必须在健康 staging 环境重新执行后，才能把“代码已完成”提升为“发布已验收”。

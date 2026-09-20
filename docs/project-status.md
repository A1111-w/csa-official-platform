# 当前项目状态

核对日期：2026-09-20（Asia/Shanghai）

## 项目身份

- 项目：CSA Official 计算机协会官网与协会管理平台。
- 当前路径：`C:\Users\Administrator\Documents\ChatGPT\计算机协会`。
- Git 根目录：当前路径。
- Git 元数据：当前路径下独立的 `.git`。
- GitHub 远程：`origin` 指向 `https://github.com/A1111-w/csa-official-platform.git`；2026-09-20 已核对远程保留迁移前提交 `fb1a2f9`。
- 当前分支：`codex/development`。
- 迁入基线：`fb1a2f9`（轮播管理工作流完成后的 CSA 历史）；后续提交以 Git log 为准。
- 主体代码：`csa-official-backend/`（Spring Boot 3.5.14 / Java 17）和
  `csa-official-frontend/`（Next.js 16.3.3 / React 19.2.4）。

实际项目此前位于旧 CSA Git worktree。迁入时保留了原文件内容、历史引用和
未提交工作；迁移前后对 407 个文件做了 SHA-256 对照，原工作区的 43 项修改
没有被覆盖。旧 worktree 不再是本项目的开发入口，迁移证据位于被忽略的
`.migration-backup/`，其中可能包含 Git bundle 或路径清单，禁止提交。

## 已实现范围

当前代码已经包含：

- 公开官网、协会介绍、竞赛、贡献者和资源入口。
- 登录、注册、验证码、密码找回/修改、Cookie JWT、CSRF、会话版本和账号生命周期。
- 资源、竞赛、简历提交与审核、部门、提案投票、贡献记录、审计和成员导出。
- 首页轮播管理、文件上传与受控访问、简历 Git 同步、邮件投递补偿。
- MySQL + Flyway V1-V6、Redis/内存缓存、Prometheus 指标、健康探针和 Docker Compose。
- 公开竞赛缓存已补齐版本化 JSON Cache-Aside、空值缓存、TTL 抖动、热点回源锁、失效 generation 和 ready 后可失败预热；简历审核和提案投票补齐数据库并发边界。
- RAG/Agent、多学校 SaaS 和支付仍是规划或 ADR，不是已实现产品能力。

## 当前未提交工作

迁入时已有的代码和测试修改属于用户/历史工作，不由本次文档治理记录代为拆分或
重写。当前工作区仍有登录时间投影、Redis 连接校验、登录页面动效、个人资料
会话复用、监控告警以及相应测试和运行记录等改动。后续 agent 必须先查看
`git status --short --branch`，不能把这些已有修改误归入自己的功能。

## 当前验证证据

本次迁移后直接执行：

| 检查 | 结果 |
| --- | --- |
| 前端 Vitest | 9 个测试文件、25 个测试通过 |
| 前端 ESLint | 0 个错误，1 条既有 `window.location.href` warning |
| 前端 Next.js production build | 通过，生成 26 个页面 |
| 后端 Maven test（2026-09-19，本轮） | 216 个用例；214 个实际执行通过，0 failures，0 errors，2 skipped |
| 后端 skipped | `FlywayMySqlRedisIntegrationTest` 因本机无可用 Docker；`LoginTrackingMapperTest` 因未配置 `CSA_LOCAL_MYSQL_TEST_URL` |
| Spec Kit integration | OK，Codex integration 完整 |
| 生产/真实依赖 | 本轮未验证；V1-V6、真实 MySQL/Redis、备份恢复和 staging 仍需健康环境 |

2026-09-19 还通过了以下后端验证：定向缓存、竞赛和简历测试；`-DskipTests test-compile`；
以及 `git diff --check`。V6 集成测试代码已经包含最新版本、简历
版本列和索引、CAS 更新、投票唯一约束及 Redis `SETNX` 断言，但因 Docker 不可用
没有把这些断言写成真实依赖已通过。前端 9 个测试文件/25 个测试、lint 和生产构建
仍沿用迁移阶段的独立记录，不冒充本轮前端重跑结果。

历史 CI、远程 staging 和生产恢复记录保留在
`docs/production-readiness/`，不能替代当前工作区验证。

2026-09-20 的分支提交范围与独立快照验证见
[GitHub 交付记录](change-records/2026-09-20-github-delivery.md)。上述 216 个用例来自
2026-09-19 整个工作区，包含未随公共缓存功能提交的历史修改。
2026-09-20 仅包含本次提交内容的快照测试为 203 个用例、202 通过、1 个 Docker
集成测试跳过，0 失败/错误；没有用未暂存改动补齐提交依赖。

## 后续开发入口

1. 先阅读根目录 `AGENTS.md`、`agent.md`、`docs/development-constraints.md`、
   本文件和 `CHANGELOG.md`。
2. 修改业务功能时同步对应 API、架构、安全、数据库、部署或前端文档。
3. 在 `CHANGELOG.md` 的 `[Unreleased]` 下追加独立记录，并写清验证、风险和回退。
4. 使用项目本地 `.agents/skills/speckit-*` 推进新需求，不修改全局技能。

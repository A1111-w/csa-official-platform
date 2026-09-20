# 20260920-github-delivery | GitHub 提交整理

- 日期：2026-09-20（Asia/Shanghai）；执行 agent：Codex。
- 关联需求：用户要求登录 GitHub、提交项目并写清更新内容；承接 `specs/001-cache-concurrency-hardening/tasks.md`。
- 状态：提交内容与独立快照验证已完成；登录成功，目标仓库已核对，远程交付以 `codex/development` 分支为准。

## 范围与实际变更

本次整理公共竞赛、协会介绍和轮播共用的版本化 JSON 缓存、公开 GET 故障限流、
事务提交后 generation 失效、简历保存/提交/审核的条件更新，以及对应回归测试。
代码范围为后端 `common/annotation/RateLimit.java`、`common/aspect/RateLimitAspect.java`、
相关 `common/cache/` 文件、竞赛公开 Controller/Service、轮播与协会介绍的 Controller/Service、
`AuditService` 缓存失效审计和 `ResumeService` / `ResumeMapper`。
测试范围为公共缓存、Redis 适配器、竞赛、简历和 V6 集成断言。

本轮还修正 API 地图、架构、本地开发、轮播学习指南、项目状态和前一轮记录中的
命令拼写；新增本记录及 CHANGELOG 条目。认证、前端会话、登录动效、监控告警及
生产恢复记录等原有未提交修改不在此次提交范围内。

## Git 与兼容性

- GitHub 账号：`A1111-w`；目标：`A1111-w/csa-official-platform`。
- 对应依据：远程 `codex/resume-review-workflow` 指向本项目迁入基线 `fb1a2f9`。
- 目标分支：`codex/development`；不强推或改写远程 main。
- OAuth 登录由 GitHub CLI 完成，不保存凭据到仓库，不修改全局 Git 配置。
- 该交付整理不新增数据库迁移；V6 为既有提交，部署仍需保留其向前兼容结构。
- 接口响应结构和权限保留；公开竞赛详情限流按 IP 聚合，不再按 ID 分桶。

## 验证与剩余事项

提交前将选定暂存内容导出到本项目 `.git/delivery-check/` 下的独立快照，使用 Java 17
在快照的 `csa-official-backend/` 中运行 `.\mvnw.cmd -q test`：退出码 0，
48 个测试类、203 个用例、202 通过、0 failures、0 errors、1 skipped。
跳过项为 `FlywayMySqlRedisIntegrationTest`，报告原因是 Docker 不可用。
快照不含未暂存修改、秘密、迁移备份和前端构建产物。

- `powershell -ExecutionPolicy Bypass -File tools/check-study-guide-drift.ps1`：52 项通过。
- `git -C <project-root> diff --cached --check`：通过。
- 显式暂存的 35 个文件中没有秘密配置、构建产物或迁移备份路径；常见凭据模式扫描无匹配。
- 测试后的变更仅为交付记录和状态文档补录，不改变已验证的源码和测试。

2026-09-19 工作区全量测试 216 个、214 通过、2 跳过仅作为历史证据。
本次没有前端修改，不重跑前端构建。V1-V6 真实 Docker/MySQL/Redis、当前镜像、
staging 和备份恢复仍未验收；单机短缓存不保证多实例故障期间一致性。

## 回退

对已交付提交使用后续 revert，保留数据库与文件数据，不删除 V6，不强推、不覆盖
工作区中其他修改。独立测试快照仅为本地验证证据，不进入 Git 提交。

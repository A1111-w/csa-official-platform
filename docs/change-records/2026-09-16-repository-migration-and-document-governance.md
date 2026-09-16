# 2026-09-16 项目迁移与文档治理记录

- 执行 agent：Codex。
- 关联需求：将实际 CSA 项目迁入当前项目目录；按真实代码修正文档；
  建立 `AGENTS.md`、`agent.md`、开发约束和每个功能必须留记录的规则。
- 状态：已完成；已有业务代码修改不属于本次实现范围。

## 变更内容

- 从原 CSA worktree 迁入完整后端、前端、部署、数据库和文档文件。
- 将 CSA 历史导入当前路径的独立 `.git`，当前开发分支为 `codex/development`，
  不添加远程，不修改原 main worktree。
- 保留迁移前 407 个文件的内容和 43 项工作区修改；迁移证据放在忽略的
  `.migration-backup/`。
- 新增 `docs/project-status.md`，记录当前代码范围、Git 身份、已知限制和验证证据。
- 新增 `docs/development-constraints.md`、`agent.md` 和
  `docs/templates/change-record.md`，要求每个 agent 完成功能时同步实际文档、
  验证结果和独立修改记录。
- 将 README、前端 README、本地开发和 Docker 文档中的旧路径改为
  `<project-root>`，补充 V4/V5 migration。
- 更新 Spec Kit 章程为项目治理规则，保持生成模板和技能不变。

## 代码事实对账

- `UserDetailsServiceImpl` 当前每次从 `UserMapper` 读取账号状态、密码、角色和
  session version；不存在文档曾写的 `UserAccountCacheService` 登录缓存链路。
- 登录成功先持久化带 `deferredLastLogin` 标记的审计事件，独立的
  `LoginTrackingService` 后续投影 `last_login_at`；登录不会在 Controller 里直接
  更新最后登录时间。
- 前端真实页面包括审计、轮播、贡献、简历审核、成员导出、忘记密码和重置密码，
  已纳入当前状态和文档入口说明。

## 验证

- 前端：`node node_modules/vitest/vitest.mjs run`，9 个文件 / 25 个测试通过。
- 前端：`node node_modules/eslint/bin/eslint.js .`，0 错误，保留 1 条既有 warning。
- 前端：`node node_modules/next/dist/bin/next build`，production build 通过，26 个页面生成。
- 后端：Java 17 + Maven 3.9.11 离线测试，198 tests，0 failures，0 errors，2 skipped。
- Spec Kit：`specify integration status` 为 OK。
- 迁移：407 个文件哈希一致，43 项源工作区状态保持不变，Git bundle 校验通过。

## 风险与遗留

- 本机没有可用 Docker，因此真实 MySQL/Redis/Testcontainers、镜像、备份恢复和
  staging Playwright 不属于本次本机通过项。
- ESLint 的 1 条 warning 位于 `csa-official-frontend/src/lib/axios.ts`，
  不影响本次文档交付，但后续可单独处理。
- 原有业务修改仍未拆分提交；后续 agent 必须先识别既有工作，再按功能追加记录。

## 回退

- 文档治理回退：按本记录的文件清单逐项 `git revert` 后重新运行文档检查；
  不使用 reset、clean 或覆盖未提交业务修改。
- 迁移回退：保留 `.migration-backup/20260917/csa-history.bundle` 和源 worktree，
  需要人工确认后恢复，不在默认开发流程自动删除或覆盖。

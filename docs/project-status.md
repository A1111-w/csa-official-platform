# 当前项目状态

核对日期：2026-11-08（Asia/Shanghai）。

## 分支和工作区

- 当前Vue分支：codex/vue3-frontend，工作区 D:/CSA-Project-vue。
- React保留分支：codex/agent-rag-evaluation，提交 bf1415c，工作区 D:/CSA-Project-agent。
- 两个 worktree 的 Git common dir 均为本项目 D:/CSA-Project/.git。
- GitHub：https://github.com/A1111-w/csa-official-platform 。
- 原 D:/CSA-Project main 的用户修改保持原样。
- 两个版本均已按用户要求上传GitHub：React保留提交 bf1415c，Vue迁移提交 e4cc74a；已核对远程分支提交。最终验证及交付事实见集成记录。

## 当前实现

前端为 Vue3.5.45 / Vue Router4.6.4 / Pinia3.0.4 / Vite8.3.6 / TypeScript，后端为 Spring Boot3.5.14 / Java17。保留公开官网、认证、安全、全部工作台和Agent实验室业务契约。Flyway V1–V8，不新增数据库迁移。

迁移内容和API角色清单见 [前端说明](../csa-official-frontend/README.md)、[Vue迁移](vue-migration.md)。默认离线Agent提供真实检索和规则评测，在线模型需单独配置和验收。

## 验证事实

本轮Vue测试、类型检查、lint/build、浏览器fixtures和文档检查的实际结果统一记录于 [20261108-vue-integration](change-records/2026-11-08-vue-integration.md)。

React之前的后端全量回归、AI30项、前端28项及Webpack构建是历史证据，详见 [20261031-agent-integration](change-records/2026-10-31-agent-integration.md)，不能冒充Vue运行结果。当时React浏览器验收因自动审批拒绝启动服务而未执行。

生产服务、真实模型质量、数据库容器迁移验收需独立环境。本轮GitHub推送不等于部署。

## 开发入口

先读AGENTS.md、开发约束、章程和CHANGELOG；按 specs/003-vue3-frontend 的验收任务继续。教学从 [Vue学习指南](study-and-demo-guide.md) 开始；React历史长讲义保留为参考，不混用前端路径。

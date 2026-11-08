# Implementation Plan: 协会 Agent、RAG 与评测实验室

**Branch**: `codex/agent-rag-evaluation` | **Date**: 2026-10-31 | **Spec**: [spec.md](spec.md)

**Input**: `specs/002-agent-rag-evaluation/spec.md`

## Summary

基于远程 `codex/development` 完整前后端，在 `modules/ai` 增加知识持久化、分块、带权限的 BM25 检索、可选 embedding 与 RRF 融合、有界工具循环及规则评测；前端增加 Agent 实验室。保留 Java/Next.js 架构和 Cookie/CSRF。使用现有 HTTP/JSON 能力对接兼容模型，不引入独立 Python 服务。开源参考与未集成项见 [research.md](research.md)。

## Technical Context

**Language/Version**: Java 17、TypeScript；版本取当前 manifests。

**Primary Dependencies**: Spring Boot 3.5.14、MyBatis-Plus、Spring Security、Jackson、Next.js 16.3.3、React 19.2.4、现有 axios。

**Storage**: MySQL 新增知识文档、片段、运行记录和评测报告表；V7/V8 向前 Flyway migration，H2 隔离测试。运行记录和报告按创建者隔离，报告保存原始用例与评分器版本。

**Testing**: JUnit/Mockito/Spring、Vitest、ESLint、生产构建；重点验收权限、分块、排序、融合、循环预算、失败路径和评分公式。

**Target Platform**: 现有 Windows 开发、Linux/Docker 部署。

**Project Type**: 前后端分离 Web 应用。

**Performance Goals**: 输入、结果、文档、候选规模、模型超时、工具轮数和评测批次均有界；不宣称已测量吞吐。

**Constraints**: 保留 Cookie/CSRF、实时账号状态与后端角色权限；不接入生产凭据/数据；不抓取任意 URL；模型只能调用只读白名单；授权过滤先于上下文。

**Scale/Scope**: 协会文本知识库、三类只读工具、单轮助手、小批量同步评测；规模化索引、多轮记忆和 LLM judge 后续单独验收。

## Constitution Check

| Gate | Design decision | Status |
| --- | --- | --- |
| 基于源码描述 | 核对基线与安全链路；本文件是计划，实际结果以记录为准 | Pass |
| 每故事文档与记录 | US1/US2/US3 tasks 各含验证、文档、独立记录 | Pass |
| 原修改保留 | 主 agent 创建隔离工作区；本计划不改原工作区、不提交或推送 | Pass |
| 安全边界 | 现有认证；工具独立授权；维护/评测 Level 3；无业务写工具 | Pass |
| 数据库演进 | 新增 V7/V8，不改 V1-V6；同步 schema 与运维说明 | Pass |
| 验证诚实 | H2/模拟上游与真实模型/MySQL 分开，无凭据不宣称实测 | Pass |

Phase 0 与 Phase 1 后复核：设计无须豁免的架构、安全、文档要求。历史路径说明与本次隔离工作区差异由协调者记录，子任务不执行 Git 写操作。

## Project Structure

### Documentation (this feature)

```text
specs/002-agent-rag-evaluation/
├── spec.md
├── checklists/requirements.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/api.md
└── tasks.md
```

### Source Code (repository root)

```text
csa-official-backend/
  src/main/java/com/csa/official/modules/ai/
  src/main/resources/db/migration/
  src/test/java/com/csa/official/modules/ai/
csa-official-frontend/
  src/app/dashboard/ai/
  src/services/ai.ts
  src/config/menu.ts
docs/
```

**Structure Decision**: 增加 `ai` 模块，复用安全和业务读模型。分离知识检索、模型连接、工具编排与评测，避免引入第二套登录和独立服务。

## Delivery and verification

US1 完成知识与授权检索；US2 接只读工具、模型与轨迹；US3 完成规则评测。前后端按合同并行。每故事同步 API/安全/架构或使用文档及记录。最后执行回归、构建、文档漂移和 diff 检查；勾选只依据当前工作区证据。

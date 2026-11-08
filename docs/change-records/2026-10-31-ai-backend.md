# 20261031-ai-backend

- 日期：2026-10-31（Asia/Shanghai）。执行者：Codex `/root/backend`。
- 关联需求：技术展示型 Agent、RAG、工具执行轨迹；Spec Kit 记录由主 agent 整合。
- 范围：`modules/ai/{AiModels,AiProperties,KnowledgeService,ModelGateway,AgentService,AiController}`；
  Flyway V7；`application.yml` 与后端 `.env.example`；四个 AI 测试类；`docs/ai-backend.md`。
- 行为：持久化文本知识与重叠分块、角色过滤 BM25、可选真实 embedding+RRF；默认离线摘录，
  显式开启在线模型工具循环；三个只读工具、请求/调用预算、运行归属保护与历史引用重新授权、
  Micrometer 指标。保留原 Cookie/CSRF、实时用户和角色校验。
- 兼容性：新增 `/api/ai`，现有接口无改动；新环境变量默认不调用收费服务；V7 新增三张表。
- 验证：后端目录设置 `JAVA_HOME=D:/java` 后执行
  `mvnw.cmd -q -DargLine=-javaagent:C:/Users/27719/.m2/repository/net/bytebuddy/byte-buddy-agent/1.17.8/byte-buddy-agent-1.17.8.jar -Dtest=KnowledgeServiceTest,AgentServiceTest,AiControllerAuthorizationTest,ModelGatewayTest test`，
  19 个定向测试通过（0 failures/errors/skipped）。本地首次运行因 Windows Byte Buddy 动态附加
  停滞，已终止该测试专属进程；显式 agent 避免该环境问题，不修改项目构建依赖。
  此后增加通用业务查询测试、HTTP 独立超时等待与每工具预算检查。主 agent 全量验证 232 项，231 通过、1 项 Docker 集成测试跳过、0 失败/错误；之后增加流式响应卡住的超时测试，`-Dtest=ModelGatewayTest` 单独执行 6 项全部通过。详情见 [集成记录](2026-10-31-agent-integration.md)。
- 待验证：真实 provider、真实 MySQL、完整浏览器与分布式负载；不能用 stub/H2 替代这些结果。
- 局限：最近 100 条业务检索、2000 分块演示索引、无 ANN/文件解析/跨轮记忆；引用为检索证据，
  不表示已做 LLM 忠实性评判；报告和运行记录无自动保留期清理。
- 回退：回退新增入口与模块、关闭 `AI_ENABLED`；保留已应用 V7 和数据，不原地改 migration。

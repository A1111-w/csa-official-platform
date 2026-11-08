# Validation quickstart

这是验收步骤，不是已通过结果。使用现有 [本地开发指南](../../docs/local-dev.md) 建立隔离 MySQL/H2 与测试用户，禁止复制生产 `.env` 或自动连接线上。实际配置名以交付后的 `AiProperties`、`application.yml` 和环境示例为准。

## Automated checks

从项目根目录切换到后端执行 `.\mvnw.cmd -q test`；前端执行 `npm ci`、`npm run test`、`npm run lint`、`npm run build`。根目录执行 `pwsh -File tools/check-study-guide-drift.ps1`。记录当前工作区结果，真实 MySQL/Testcontainers 与真实模型未执行时分别注明。

## US1: Knowledge

1. 本地模型模式保持 offline，以部长账号打开 `/dashboard/ai`。
2. 新建标题“CSA Java 学习”、正文“Java 学习路线：先掌握集合、并发，再学习 Spring Boot。”、来源“本地演示资料”、可见等级 1 的文档，记录返回的 id。
3. 新建另一份仅部长可见的资料；用会员账号查询，确认回答和引用不包含受限正文。
4. 重启后仍能查询第一份文档；删除后重新查询，结果不应再包含它。

## US2: Agent

1. 分别问“Java 学习路线是什么”“推荐协会学习资源”“有哪些竞赛”，检查模式、工具轨迹、来源和耗时。
2. 使用不同账号回看同一个 run id，必须拒绝读取他人记录。
3. 使用模拟上游验证未知工具、非法参数、重复工具调用、超时与无上下文回答；检查预算与失败提示。
4. 真实模型由部署者显式配置后再验收；不以 offline 或模拟上游测试替代 live 效果结论。

## US3: Evaluation

在评测编辑器输入最多 5 条用例。将以下占位文档标识替换为 US1 实际返回的 id：

```json
[
  {
    "question": "Java 学习路线是什么？",
    "expectedDocumentIds": ["replace-with-created-document-id"],
    "expectedTools": ["search_knowledge"],
    "expectedAnswerTerms": ["集合", "Spring Boot"]
  }
]
```

运行两次，除标识和耗时外分数应一致。修改一个预期文档/工具/词观察相应指标下降；留空某类预期应显示 N/A；所有预期全空应拒绝。报告可按 id 回看，仅创建者可读；报告保存原数据集和 `rules-v1`。查看指标解释，确认未声称 faithfulness。

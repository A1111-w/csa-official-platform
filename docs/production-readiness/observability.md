# 可观测性与基础告警

## 端点

backend 提供：

- `/actuator/health/liveness`：进程是否还能工作，不检查外部依赖。
- `/actuator/health/readiness`：是否可以接收流量，包含数据库和生产 Redis。
- `/actuator/prometheus`：供内部 Prometheus 抓取，不经 Caddy 对公网发布。

每个请求都会生成或复用合法的 `X-Request-ID`，响应、MDC 和错误 envelope 的 `traceId` 使用同一值。生产日志使用 Logstash JSON 格式，禁止记录密码、Token、Cookie 和完整个人信息。

## Prometheus 抓取

示例目标：

```yaml
scrape_configs:
  - job_name: csa-backend
    metrics_path: /actuator/prometheus
    static_configs:
      - targets: [backend:8080]
```

Prometheus 必须与 Compose 的内部网络连通，或通过受控的管理网络访问；不要给 Actuator 增加公网端口。

## 基础告警

`deploy/monitoring/prometheus-alerts.yml` 给出可复制的规则：backend 不可抓取、5xx 比例、P95 延迟、Hikari 连接池逼近上限和 readiness 失败。阈值是起点，应根据 400 名学生的真实基线调整。

告警处理顺序：先看 readiness 和最近发布，再按 `X-Request-ID` 查 JSON 日志，确认数据库/Redis/邮件/上传依赖，必要时切换流量到维护页并执行恢复 runbook。告警通知中不得包含请求体、Authorization、Cookie 或邮件验证码。

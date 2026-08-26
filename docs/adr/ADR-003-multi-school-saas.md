# ADR-003：多学校 SaaS 的租户隔离演进方案

- 状态：Proposed
- 日期：2026-07-30
- 范围：Phase 3 方案设计；本轮不实现多租户代码或迁移

## 1. 背景

当前系统是单校单体，目标用户约 400 名学生。短期重点是稳定、可回滚和低运维成本；未来可能为多所学校提供独立域名、品牌和管理员。现在直接把所有表改成多租户会扩大风险，因此先冻结边界、列出迁移顺序和越权测试。

## 2. 方案比较

| 方案 | 优点 | 代价与风险 | 当前结论 |
| --- | --- | --- | --- |
| 共享库 + 每行 `tenant_id` | 成本低、部署简单、适合 400 人到中等规模、跨租户运营报表容易 | 查询漏条件会越权；需要统一数据访问规范、索引和测试 | 采用，作为当前演进路径 |
| 每租户独立数据库 | 隔离强、单租户备份恢复清晰、租户级扩展容易 | 连接池、迁移、监控、备份和版本管理数量随租户增长；跨租户运营复杂 | 未来高合规或大租户再评估 |

选择共享库不等于依赖“开发人员记得加条件”。所有租户业务查询必须通过带租户上下文的 Service/Mapper 入口，并以跨租户负向测试作为发布门槛。数据库暂不依赖 MySQL RLS，隔离责任先放在应用查询、唯一索引和集成测试。

## 3. 目标领域模型

### 3.1 `tenant`

建议字段：`id`、`slug`、`display_name`、`status`、`plan_code`、`created_at`、`suspended_at`、`archived_at`。`status` 至少包含 `PROVISIONING`、`ACTIVE`、`SUSPENDED`、`ARCHIVED`。`slug` 全局唯一，用于初始路由和日志关联。

### 3.2 `tenant_membership`

连接全局用户身份和学校：`tenant_id`、`user_id`、`tenant_role`、`status`、`joined_at`、`deactivated_at`。唯一键为 `(tenant_id, user_id)`；角色至少包含 `TENANT_OWNER`、`TENANT_ADMIN`、`TENANT_EDITOR`、`TENANT_MEMBER`。平台 Root 不应被伪装成学校成员，平台角色和租户角色分开校验。

### 3.3 域名、品牌与生命周期

- `tenant_domain`：`hostname` 全局唯一，记录验证状态、主域名标记和证书/解析状态；域名解析到租户后才建立请求上下文。
- `tenant_brand_config`：租户名称、Logo、主题色、联系信息和版本号；富文本仍走后端与前端双层清洗。
- 生命周期操作必须审计：创建、域名验证、品牌修改、暂停、恢复、归档和成员邀请。
- `plan`、`quota`、`feature_flag` 只做边界模型，不在本轮引入支付、账单、Webhook 或订阅扣款。

## 4. 需要 `tenant_id` 的表

现有全局身份表 `sys_user` 暂保为平台身份；学校范围的数据表增加非空 `tenant_id`。具体清单：

| 表 | 租户化要求 |
| --- | --- |
| `sys_dept` | 部门属于租户；`leader_id` 只能指向同租户 membership |
| `sys_carousel` | 首页内容按租户隔离 |
| `sys_invite_code` | 邀请码按租户隔离 |
| `sys_proposal`、`sys_vote_record` | 提案、投票和投票人都带租户上下文，投票记录保留冗余 `tenant_id` 便于防漏条件 |
| `sys_resource`、`sys_contribution_log` | 资源和贡献属于租户 |
| `sys_config` | 配置键改为 `(tenant_id, config_key)` |
| `biz_competition`、`biz_comp_editor` | 比赛和授权编辑者属于租户，编辑者 membership 必须同租户 |
| `biz_resume` | 允许同一全局用户在不同租户有不同简历，唯一键改为 `(tenant_id, user_id)` |
| `sys_audit_log` | 增加 `tenant_id`；平台事件允许为 NULL，租户事件不可 NULL |
| `sys_stored_file` | 增加 `tenant_id`，路径按租户和用户分区 |
| `sys_mail_delivery` | 增加 `tenant_id`，收件人哈希不能替代租户隔离 |
| `sys_scheduled_job_execution` | 租户任务用 `(tenant_id, job_name, idempotency_key)`；平台任务使用保留的 platform scope |

`sys_user` 的全局用户名/邮箱策略需要在迁移前定稿。推荐登录身份继续全局唯一，学校内的学号放到租户成员资料并按租户唯一；这样不会因为同一学生加入多校而复制账号。

## 5. 各层隔离规则

### JWT 与请求上下文

JWT 只携带不可变的用户身份和当前 membership/tenant 上下文，例如 `user_id`、`tenant_id`、`membership_version`。角色仍以数据库当前 membership 为准，不能信任旧 JWT 中的角色。域名、显式租户选择和 membership 必须相互校验，禁止用户仅修改 URL 或 JWT payload 切换学校。

### Redis、缓存和限流

所有租户数据 key 使用 `tenant:{tenantId}:...` 前缀；用户全局会话 key 使用 `user:{userId}:...` 并明确不能当成租户业务缓存。验证码、限流、业务锁、缓存和定时任务 key 都要在测试中断言租户前缀，跨租户不能命中或删除对方 key。

### 文件路径

统一规划为 `/files/tenant/{tenantId}/user/{userId}/{uuid}`。下载先查 metadata 的 `tenant_id` 和业务引用，再解析物理路径；不能从客户端传入的路径推断租户，也不能只校验 user ID。

### 唯一索引

租户内唯一的业务键都把 `tenant_id` 放在索引前缀：配置键、邀请码、比赛编辑者、投票记录、简历、文件存储键、定时任务幂等键。全局唯一的域名、平台用户名和平台审计 request id 维持全局约束。每次改唯一索引必须先查重复数据，再分阶段清洗和切换。

### 审计与任务

审计记录必须带 `tenant_id`、actor user、membership、action、target 和 request ID；平台运维事件明确标记 `PLATFORM`。定时任务锁的粒度按租户决定：租户任务允许并行但不能重复，平台任务只能有一个执行者。

## 6. 分阶段迁移方案

1. **建模阶段**：新增 `tenant`、`tenant_membership`、`tenant_domain`、品牌和生命周期表，创建默认学校租户；不改变现有请求行为。
2. **兼容阶段**：为上述业务表增加可空 `tenant_id`，应用对新写入做双写；从现有单校数据回填默认租户，检查 NULL 和重复。
3. **读隔离阶段**：所有 Service/Mapper 接受 tenant context，列表、详情、写入、导出、审计、缓存和文件访问都强制条件；加入跨租户负向测试。
4. **约束阶段**：把 `tenant_id` 改为 NOT NULL，替换租户内唯一索引，删除旧的全局业务唯一索引；保留兼容视图或双读一个发布周期。
5. **域名与品牌阶段**：启用 hostname -> tenant 解析、品牌缓存和生命周期校验；域名未验证不能接管请求。

所有迁移使用新的 Flyway 版本，不原地修改已执行 SQL。每阶段先在副本做备份、校验和、行数和重复检查；回滚应用不等于回滚数据库，破坏性约束变更只能向前修复。

## 7. 跨租户越权测试设计

测试夹具至少创建 Tenant A、Tenant B、同名业务对象和各自成员。必须验证：

- A 的列表、详情、搜索、导出看不到 B 的对象，即使对象 ID 连续或被猜到。
- A 成员不能读取、修改、删除、审核或授权 B 的资源、比赛、简历、部门、配置和文件。
- A 的投票不能写入 B 的提案；重复投票唯一键包含正确 tenant。
- JWT 换 `tenant_id`、改 host、复用另一租户缓存 key、篡改文件路径都必须失败。
- A 的审计和定时任务查询不能返回 B；平台管理员的跨租户操作必须有显式 scope 和审计。
- 任何 Mapper 漏写 tenant 条件时，集成测试必须失败，而不是只靠 code review 发现。

建议加入 SQL 审计/测试规则：所有租户表的查询必须出现 tenant predicate，特殊的 platform query 必须显式标注并单独测试。

## 8. Plan、Quota、Feature Flag 边界

- `plan_code` 只描述能力档位；`tenant_quota` 描述文件容量、成员数、月度邮件等硬限制；usage 由可重建计数或账本维护。
- `feature_flag` 以租户为范围，默认关闭高风险能力；读取必须经过统一 FeatureGate，不允许页面自己猜权限。
- 超过 quota 返回稳定业务错误码并记录审计，不通过前端隐藏按钮代替后端校验。
- 本 ADR 不设计支付价格、订单、扣款、续费、发票、支付回调或订阅状态机。

## 9. 决策与剩余风险

当前采用共享库行级隔离，原因是规模、运维和成本更匹配单体架构。最大风险是遗漏 tenant 条件造成跨租户数据泄露；因此“租户上下文 + 统一数据访问 + 负向集成测试”是不可拆的整体。达到更高合规等级、单租户规模显著增长或需要独立恢复窗口时，再重新评估独立数据库，并沿用相同的领域模型、审计和文件路径边界。

# Frontend state model

后端实体及迁移保持不变。

| 状态 | 字段/约束 | 生命周期 |
| --- | --- | --- |
| Session | user 或 null、服务端 profile、加载状态；不含 Token | 未知→恢复中→已登录/未登录；401 清理 |
| Auth form | username、email、password、code、可选资料、隐私同意 | 页面切换清理密码与错误；提交中禁用 |
| Privacy version | 服务器读取的非空 policyVersion | 注册页面加载；失败禁止注册 |
| Query view | items、loading、error、page/filter | 参数变化请求；失败不能伪装成功 |
| AI run/report | 沿用 services/ai.ts | 真实 API 结果；未标注维度 null→N/A |

注册用户名 4–20 位字母数字下划线，密码 6–20 位字母数字且两者皆有；重置密码 8–64 位且两者皆有；验证码恰 6 位数字。保留当前服务端约束差异，不擅自统一。

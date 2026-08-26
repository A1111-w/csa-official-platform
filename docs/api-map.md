# 接口地图和调用说明

这份文档不只列接口路径，还说明每个接口属于哪个模块、谁能调用、常见参数是什么、前端大概在哪里使用，以及调用时要注意什么。

## 1. 全局约定

### 1.1 统一返回

大部分 JSON 接口返回：

```json
{
  "code": 200,
  "message": "Success",
  "data": {}
}
```

错误仍保留 `R<T>` 兼容外壳，但 HTTP status 不再固定为 200，并增加稳定错误码和请求追踪 ID：

```json
{
  "code": 401,
  "message": "用户名或密码错误",
  "data": null,
  "errorCode": "AUTHENTICATION_FAILED",
  "traceId": "request-id"
}
```

响应头 `X-Request-ID` 与错误体 `traceId` 对应。前端 Axios 同时检查真实 HTTP status 和业务 `code`；认证失败才按 401 处理，数据库或系统异常保持 5xx。

前端 `src/lib/axios.ts` 会自动拆包：

```text
后端 R<T>
→ Axios response interceptor
→ code == 200 返回 data
→ code != 200 抛 ApiError
```

所以在前端 service 里看到：

```ts
api.get<UserInfo, UserInfo>("/api/sys/user/info")
```

拿到的已经是 `data`，不是完整的 `{ code, message, data }`。

### 1.2 常见状态码

| code | 含义 | 常见原因 |
| --- | --- | --- |
| 200 | 成功 | 请求正常 |
| 400 | 参数错误 | 缺参数、JSON 错、校验失败 |
| 401 | 未认证 | 没登录、Token 无效、Token 过期 |
| 403 | 无权限 | 角色等级不够、CSRF 错误、文件无权访问 |
| 404 | 不存在 | 资源或业务对象不存在 |
| 405 | 方法不支持 | HTTP method 与接口不匹配 |
| 409 | 冲突 | 用户名重复、邀请码次数耗尽 |
| 413 | 文件过大 | 上传超过限制 |
| 429 | 请求过于频繁 | 登录、注册、验证码触发限流 |
| 500 | 服务端错误 | 未处理异常或系统繁忙 |
| 502/503 | 上游或服务不可用 | 邮件、缓存或其它依赖暂时不可用 |

### 1.3 鉴权方式

后端支持两种 Token 来源：

1. `Authorization: Bearer <token>`
2. `CSA_AUTH_TOKEN` Cookie

浏览器前端默认使用 Cookie，不保存、不读取、不拼接 Bearer Token。Bearer 兼容主要用于 API 调试或非浏览器客户端。

### 1.4 CSRF 规则

如果使用 Cookie 登录，非安全方法需要 CSRF：

```text
POST / PUT / PATCH / DELETE
```

需要带：

```http
Cookie: CSA_CSRF_TOKEN=xxx
X-CSRF-Token: xxx
```

豁免接口：

```text
/api/auth/login
/api/auth/register
/api/auth/send-code
/api/auth/csrf
/api/public/**
```

前端 Axios 会自动获取并注入 CSRF Header。

### 1.5 分页与列表条数上限

所有分页和列表接口的条数都由 `common/util/PageUtils` 统一收敛，不再各写各的 `Math.min`：

| 常量 | 值 | 作用 |
| --- | --- | --- |
| `DEFAULT_PAGE_SIZE` | 10 | 分页接口 `size` 缺省值 |
| `MAX_PAGE_SIZE` | 100 | 分页接口 `size` 上限 |
| `MAX_LIST_LIMIT` | 200 | 不分页列表接口单次返回条数上限 |

规则：

- 分页接口（`/api/sys/resource/list`、`/api/biz/comp/list`、`/api/public/competitions`）的 `size` 会被收敛到 1-100。
- 不分页列表接口（`/api/sys/vote/list`、`/api/sys/user/list`、`/api/public/contribution/wall` 等）的 `size` / `limit` 会被收敛到 1-200。
- `page` 小于 1 或为空时按第 1 页处理。
- 这些参数现在都是可选参数（不再写死 `defaultValue`），省略时使用上表的默认值。

目的：防止 `?size=1000000` 这类请求让数据库一次性把整表拉出来、把堆打满。以前每个 Controller 各自 `Math.min`，上限还不一致（有的 200、有的干脆没有），现在统一收敛到这里。

## 2. 公开接口

公开接口主要给官网页面使用，不需要登录。

### 2.1 获取协会介绍

```http
GET /api/public/about
```

返回：

```json
{
  "code": 200,
  "message": "Success",
  "data": "<p>协会介绍 HTML</p>"
}
```

后端来源：

```text
PublicController.getAbout
→ SysConfigMapper
→ configKey = CSA_INTRO
```

前端使用：

```text
/about
/dashboard/settings 预览或编辑前读取
```

注意：内容是富文本 HTML。后台更新时会用 Jsoup 白名单清洗。

### 2.2 获取贡献者

```http
GET /api/public/contributors
```

逻辑：

```text
查询 roleLevel >= CORE_MEMBER 的用户（只 select 需要的列，最多 200 条）
→ 批量查部门
→ 组装 ContributorVo
→ 按 roleLevel 降序、id 升序
```

这是未登录可访问的公开接口，所以加了 `LIMIT 200`（`MAX_LIST_LIMIT`）兜底，并且只 `select` 用得到的列，避免协会规模变大后把整张用户表拉出来。

返回字段大致包括：

```json
{
  "id": 1,
  "realName": "张三",
  "avatar": "...",
  "deptName": "技术部",
  "title": "部长",
  "roleLevel": 3
}
```

### 2.3 获取公开竞赛分页

```http
GET /api/public/competitions?page=1&size=10
```

参数：

| 参数 | 默认 | 说明 |
| --- | --- | --- |
| `page` | 1 | 当前页，小于 1 或为空时按第 1 页处理 |
| `size` | 10 | 每页数量，收敛到 1-100（详见 1.5） |

返回 MyBatis-Plus `Page<CompetitionListVO>`，常见字段：

```json
{
  "records": [],
  "total": 0,
  "size": 10,
  "current": 1,
  "pages": 0
}
```

为什么用 VO：

- 公开竞赛不应该返回所有数据库字段。
- VO 可以控制标题、摘要、状态、时间等展示字段。
- 后续接 RAG/Agent 时也可以复用这个轻量结构。

注意：`CompetitionListVO` 不再返回完整的 `content` 正文，只返回 `summary` 摘要——后端用 Jsoup 把富文本标签剥掉、截断到 200 字（超出补 `…`）。列表页每条只渲染 120 字左右，旧响应却把整段富文本 HTML 塞进每一行，一页 10 条就可能白白多传几百 KB。需要完整正文时走下面的详情接口。

还要注意 `status` 现在是**数字** code（`0` 未发布 / `1` 进行中 / `2` 已结束），不是枚举名 `"ONGOING"`。
以前返回的是枚举名（Jackson 序列化枚举的默认行为），列表展示因为有兼容函数看不出问题，
但后台编辑弹窗回填状态时用 `Number.parseInt(String(status)) || 1` 还原，
遇到 `"FINISHED"` 会得到 `NaN` 再回退成 `1`——表现为「编辑一个已结束的比赛，保存后变成进行中」。
现在由 `CompetitionListVO` / `CompetitionDetailVO` 显式返回 code 修正。

### 2.4 公开竞赛详情

```http
GET /api/public/competitions/{id}
```

权限：公开，无需登录。

返回：`CompetitionDetailVO`，相比列表项多了完整的 `content` 正文（入库前已经过 Jsoup 白名单清洗）。

注意：

- 状态为 `UNPUBLISHED` 的竞赛在这里一律返回 404，避免未登录用户通过 id 探测到还没公开的活动。
- `canEdit`、`canGrant` 在公开入口恒为 `false`。

失败：

| 场景 | 返回 |
| --- | --- |
| id 不存在 | 404 |
| 竞赛未发布（UNPUBLISHED） | 404 |

### 2.5 轮播图

```http
GET /api/public/carousel/list
```

逻辑：

```text
只查 status = 1 的轮播图
→ sortOrder 升序
→ createTime 降序
→ LIMIT 200（MAX_LIST_LIMIT）兜底，防止后台误配大量轮播图拖垮首页
```

返回：`R<List<CarouselVO>>`，只对外暴露渲染用的四个字段：`id`、`imgUrl`、`targetUrl`、`title`。`sortOrder`、`status`、`deleted` 属于后台维护信息，不再下发给未登录用户。

适合首页展示。

### 2.6 贡献墙

```http
GET /api/public/contribution/wall?limit=100
```

逻辑：

```text
limit 默认 100
→ Controller 限制到 1-200
→ ContributionLogMapper.selectWall(limit)
→ SQL 聚合贡献数据
→ 返回 List<ContributionWallVO>
```

贡献类型：

| 类型 | 含义 | 当前统计方式 |
| --- | --- | --- |
| `DEV` | 官网建设 | 累加 score |
| `RES` | 资源贡献 | 统计条数 |
| `COMP` | 发布比赛 | 统计条数 |
| `OPS` | 首页维护 | 统计条数 |

### 2.7 贡献排行

```http
GET /api/public/contribution/rank?limit=10
```

当前实现为总排行：

- `limit` 默认 10，统一限制到 1-200。
- 按贡献 `score` 总和降序，再按贡献记录数降序、用户 ID 升序。
- 排除已删除或已匿名化账号。
- 按 `limit` 缓存 10 分钟；新增贡献时清空排行缓存。
- 首页调用 `limit=5` 展示前五名。

返回字段：`userId`、`username`、`realName`、`avatar`、`deptName`、`score`、`contributionCount`。

## 3. 登录注册接口

### 3.1 获取 CSRF Token

```http
GET /api/auth/csrf
```

用途：

- 浏览器使用 Cookie 登录时，非 GET 请求要带 CSRF。
- 前端 Axios 在需要时会自动请求这个接口。

返回：

```json
{
  "code": 200,
  "message": "Success",
  "data": {
    "csrfToken": "..."
  }
}
```

同时写入 Cookie：

```text
CSA_CSRF_TOKEN=...
```

### 3.2 登录

```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "alice",
  "password": "<runtime-test-password>"
}
```

限流：

```text
key = login
time = 300 秒
count = 10
identifiers = username
```

响应体：

```json
{
  "code": 200,
  "message": "Success",
  "data": {
    "csrfToken": "...",
    "username": "alice",
    "roleLevel": "1"
  }
}
```

JWT 不再出现在响应体里，避免被前端 JavaScript 或 XSS 读取。

同时设置：

```text
CSA_AUTH_TOKEN=...; HttpOnly
CSA_CSRF_TOKEN=...
```

失败：

| 场景 | 返回 |
| --- | --- |
| 用户名或密码错 | 401 |
| 请求太频繁 | 429 |
| 参数缺失 | 400 |

### 3.3 退出登录

```http
POST /api/auth/logout
```

逻辑：

```text
解析当前 Token
→ JwtRevocationService.revoke
→ 清理 CSA_AUTH_TOKEN Cookie
→ 清理 CSA_CSRF_TOKEN Cookie
```

注意：如果使用 Cookie 登录，这个 POST 也需要 CSRF。

### 3.4 注册

```http
POST /api/auth/register
Content-Type: application/json

{
  "username": "alice",
  "password": "<runtime-test-password>",
  "email": "alice@example.com",
  "code": "<one-time-code>",
  "realName": "Alice",
  "studentId": "20260001",
  "college": "计算机学院",
  "className": "软件 1 班",
  "inviteCode": "CSA2026"
}
```

限流：

```text
key = register
time = 60 秒
count = 2
identifiers = username, email
```

逻辑：

1. 校验邮箱验证码。
2. 检查用户名是否重复。
3. 创建用户。
4. 如果邀请码有效，设置为会员。
5. 如果没有邀请码，设置为游客。
6. 忽略未校验的 `merchantNo`，防止注册提权。

常见失败：

| 场景 | code |
| --- | --- |
| 验证码错误或过期 | 400 / `BAD_REQUEST` |
| 用户名已存在 | 409 |
| 邀请码不存在 | 400 |
| 邀请码次数耗尽 | 409 |
| 注册太频繁 | 429 |

### 3.5 发送验证码

```http
POST /api/auth/send-code?email=alice@example.com
```

限流：

```text
key = send_code
time = 60 秒
count = 1
identifiers = email
```

注意：

- 邮箱必须包含 `@`。
- 需要后端配置 SMTP 账号和授权码。
- 本地不想发邮件时，可以在测试里 Mock `MailService`。

验证码发送先同步写入缓存和 `sys_mail_delivery`，SMTP 由独立执行器有限重试。HTTP 返回“发送成功”表示请求已接收，不等于 SMTP 一定已经投递；最终状态为 `SENT` 或 `FAILED`。

### 3.6 发起密码找回

```http
POST /api/auth/forgot-password
Content-Type: application/json

{
  "email": "alice@example.com"
}
```

接口会规范化邮箱，并且无论账号是否存在都返回同一条接受提示，避免通过响应枚举注册邮箱。只有 ACTIVE 账号会创建密码重置验证码，限流为同一邮箱每 60 秒最多 2 次。

### 3.7 重置密码

```http
POST /api/auth/reset-password
Content-Type: application/json

{
  "email": "alice@example.com",
  "code": "<one-time-code>",
  "newPassword": "<runtime-test-password>"
}
```

验证码必须为 6 位数字；新密码必须包含字母和数字，长度 8-64 位。成功后密码哈希被更新、`session_version` 递增，所有旧 JWT 失效。验证码错误/过期返回 400，账号状态不允许重置返回 409。

## 4. 当前用户和成员目录

### 4.1 当前用户信息

```http
GET /api/sys/user/info
```

权限：登录。

逻辑：

```text
SecurityUtils.getCurrentUser
→ BeanUtil.copyProperties
→ UserInfoVO
```

前端用途：

- 工作台会话校验。
- 个人资料页。
- 刷新 roleLevel。

### 4.2 用户列表

```http
GET /api/sys/user/list?keyword=alice&departmentId=1&minRoleLevel=1&size=100
```

权限：`LEVEL_3`。

参数：

| 参数 | 必填 | 说明 |
| --- | --- | --- |
| `keyword` | 否 | 匹配 username / realName / email |
| `departmentId` | 否 | 部门筛选 |
| `minRoleLevel` | 否 | 最低角色等级 |
| `size` | 否 | 最多返回数量，后端限制 1-200 |

返回：`UserDirectoryVO[]`，包含部门名。

### 4.3 权限测试接口

```http
GET /api/sys/user/member-test
GET /api/sys/user/admin-test
```

用途：开发阶段验证权限。

生产建议：删除或只允许管理员访问。

### 4.4 账号生命周期与个人数据导出

权限：以下接口都要求登录；Cookie 认证的 POST 必须带有效 CSRF。

```http
POST /api/account/change-password
POST /api/account/revoke-sessions
POST /api/account/deactivate
POST /api/account/deletion-request
GET  /api/account/export
```

修改密码请求体：

```json
{
  "currentPassword": "<runtime-current-password>",
  "newPassword": "<runtime-new-password>"
}
```

改密、重置密码和吊销全部会话都会递增 `session_version`。停用与删除申请还会修改 `account_status`，并立即清缓存、吊销当前 Token、清理认证 Cookie。

`GET /api/account/export` 返回当前用户的账号资料、简历、上传文件元数据和安全事件摘要白名单，不包含密码哈希、Token、验证码、文件 `storageKey` 或完整审计详情。

注意：`deletion-request` 只进入 `DELETION_PENDING` 和保留/审核流程。当前尚未实现最终匿名化/物理删除执行器，接口成功不代表数据已经完成删除。

## 5. 部门接口

### 5.1 部门列表

```http
GET /api/sys/dept/list
```

当前代码注释写“公开接口”，但受全局 Security 影响，除非在 SecurityConfig 放行，否则仍需要登录。

返回：`R<List<DeptVO>>`。用 `DeptVO` 而非 `Dept` 实体，屏蔽 `deleted` 等持久层字段。

### 5.2 任命部长

```http
POST /api/sys/dept/appoint
Content-Type: application/json

{
  "deptId": 1,
  "userId": 1001
}
```

权限：

```text
LEVEL_4 或 ADMIN
```

逻辑：

```text
校验 deptId/userId
→ DeptService.appointLeader
→ 原部长降级
→ 新用户任命为部长
→ 事务提交
```

失败：

- 参数缺失：400。
- 部门不存在：业务异常。
- 用户不存在：业务异常。

## 6. 资源库接口

### 6.1 资源列表

```http
GET /api/sys/resource/list?page=1&size=10&category=Java
```

权限：`LEVEL_1`。

参数：

| 参数 | 默认 | 说明 |
| --- | --- | --- |
| `page` | 1 | 页码，小于 1 或为空时按第 1 页处理 |
| `size` | 10 | 每页数量，收敛到 1-100（详见 1.5） |
| `category` | 无 | 分类筛选 |

返回：`R<Page<ResourceVO>>`。字段：`id`、`title`、`summary`、`fileUrl`、`category`、`uploaderId`、`downloadCount`、`createTime`；不再暴露实体上的 `deleted` 逻辑删除标记。

后端：

```text
ResourceController.list
→ ResourceService.listResources
→ PageUtils.of 收敛 page/size
→ category 可选
→ createTime 降序
→ selectPage
→ 映射为 ResourceVO
```

### 6.2 分类列表

```http
GET /api/sys/resource/categories
```

权限：`LEVEL_1`。

逻辑：

```text
select DISTINCT category
→ 排除 null 和空字符串
→ orderByAsc
```

### 6.3 新增/编辑资源

```http
POST /api/sys/resource/save
Content-Type: application/json

{
  "title": "Java 后端学习路线",
  "summary": "适合协会新人学习 Spring Boot",
  "fileUrl": "/files/42/demo.pdf",
  "category": "Java"
}
```

编辑时传 `id`：

```json
{
  "id": 12,
  "title": "更新后的标题",
  "fileUrl": "/files/42/demo.pdf",
  "category": "Java"
}
```

权限：`LEVEL_3`。

校验：现在走 Bean Validation（`@Valid` + `@NotBlank` / `@Size`），不再手写判断。

| 字段 | 规则 |
| --- | --- |
| `title` | 必填，且不超过 200 字符 |
| `fileUrl` | 必填，且不超过 500 字符 |
| `summary` | 可选，不超过 1000 字符 |
| `category` | 可选，不超过 64 字符 |

校验不通过由 `GlobalExceptionHandler` 统一返回 400，消息形如 `参数错误: <字段>: <提示>`。

另外，编辑时如果 `id` 不存在，Service 返回 404。

贡献：

保存资源会触发：

```text
@LogContribution(type = RES, detail = "上传资源")
```

### 6.4 删除资源

```http
POST /api/sys/resource/delete?id=12
```

权限：`LEVEL_3`。

不存在返回 404。

### 6.5 下载计数

```http
POST /api/sys/resource/download?id=12
```

权限：`LEVEL_1`。

逻辑：

```text
download_count = COALESCE(download_count, 0) + 1
```

## 7. 文件接口

### 7.1 上传文件

```http
POST /api/common/file/upload
Content-Type: multipart/form-data

file=<binary>
```

权限：登录。

逻辑：

```text
SecurityUtils.getUserId
→ FileService.upload
→ 校验大小
→ 校验扩展名
→ 生成存储名
→ 保存到 UPLOAD_PATH/{userId}/
→ 返回 /files/{ownerId}/{fileName}
```

### 7.2 下载/访问文件

```http
GET /files/{ownerId}/{fileName}
```

权限：`LEVEL_1`。

访问规则：

1. 文件属于当前用户：允许。
2. 文件 URL 已发布为资源：允许。
3. 否则：403。

返回头：

- `Content-Disposition: attachment`
- `X-Content-Type-Options: nosniff`
- `Content-Length`

## 8. 竞赛接口

### 8.1 后台竞赛列表

```http
GET /api/biz/comp/list?page=1&size=10
```

权限：登录。

参数：`size` 收敛到 1-100，`page` 小于 1 或为空时按第 1 页处理（详见 1.5）。

返回：`R<Page<CompetitionListVO>>`。列表项只带 `summary` 摘要，不含完整 `content` 正文；每条还带上针对当前用户的 `canEdit`、`canGrant` 标记。需要完整正文时走下面的详情接口。

### 8.2 竞赛详情

```http
GET /api/biz/comp/{id}
```

权限：登录即可。该接口在 `/api/biz/**` 下，未在 SecurityConfig 放行，因此走全局 `authenticated()`；方法上没有额外的 `@PreAuthorize`，任何登录用户都能查看（包括未发布竞赛）。

返回：`CompetitionDetailVO`，相比列表项多了完整的 `content` 正文，并带上当前用户的 `canEdit`、`canGrant` 标记（会长及以上，或发布者/被授权编辑者为 `true`）。

失败：

| 场景 | 返回 |
| --- | --- |
| id 不存在 | 404 |

### 8.3 新增/编辑竞赛

```http
POST /api/biz/comp/save
Content-Type: application/json

{
  "title": "蓝桥杯训练赛",
  "content": "协会内部训练活动",
  "coverImg": "https://example.com/cover.png",
  "startTime": "2026-07-17T10:00:00",
  "endTime": "2026-07-18T18:00:00",
  "status": "ACTIVE"
}
```

权限表达式：

```text
(#dto.id == null and @csaSec.canCreateCompetition())
or
(#dto.id != null and @csaSec.canEditCompetition(#dto.id))
```

含义：

- 请求体使用 `SaveCompetitionDto`，`title` 和 `content` 有 `@NotBlank` 校验。
- 新建竞赛：`dto.id == null`，需要具备创建权限。
- 编辑竞赛：`dto.id != null`，需要是会长、发布者或该竞赛编辑者。

贡献：

```text
@LogContribution(type = COMP, detail = "publish or update competition")
```

### 8.4 授权竞赛编辑者

```http
POST /api/biz/comp/grant
Content-Type: application/json

{
  "compId": 1,
  "targetUserId": 1001
}
```

权限：

```text
@csaSec.canGrantCompetitionEditor(compId)
```

参数缺失返回 400。

## 9. 简历接口

### 9.1 我的简历

```http
GET /api/resume/my
```

权限：`LEVEL_2`。

返回当前用户简历，类型为 `R<ResumeVO>`。用户还没建过简历时 `data` 为 `null`，前端按草稿处理。

```json
{
  "code": 200,
  "message": "Success",
  "data": {
    "id": 1,
    "content": "# 我的简历\n...",
    "gitRepoUrl": "https://github.com/xxx/yyy",
    "status": 2,
    "rejectReason": null,
    "auditBy": 3,
    "auditTime": "2026-07-20 10:00:00",
    "createTime": "2026-07-01 09:00:00",
    "updateTime": "2026-07-20 10:00:00"
  }
}
```

注意两点：

- 不再返回 `Resume` 实体，`userId` 和 `deleted` 不会出现在响应里。
- `status` 是**数字** code（0草稿/1待审核/2已通过/3已驳回），不是枚举名。
  这一点以前是反的：Jackson 默认把枚举序列化成名字（`"APPROVED"`），
  而前端按 `number` 比较，导致状态标签一直显示成草稿。现在由 `ResumeVO` 显式返回 code 修正。

### 9.2 保存简历草稿

```http
POST /api/resume/save
Content-Type: application/json

{
  "content": "# 我的简历\n...",
  "gitRepoUrl": "https://github.com/xxx/yyy"
}
```

权限：`LEVEL_2`。

### 9.3 提交审核

```http
POST /api/resume/submit
```

权限：`LEVEL_2`。

### 9.4 审核队列

```http
GET /api/resume/reviews?page=1&size=20&status=1
```

权限：`LEVEL_3`（部长及以上）。

参数：

| 参数 | 默认 | 说明 |
| --- | --- | --- |
| `page` | 1 | 页码，小于 1 时按 1 处理 |
| `size` | 10 | 每页数量，统一收敛到 1-100 |
| `status` | 1 | 只能是 1 待审核、2 已通过、3 已驳回；草稿 0 不允许进入管理队列 |

返回：`R<Page<ResumeReviewListVO>>`。列表只返回申请人、部门、状态、内容摘要、仓库地址和时间字段；完整简历正文走详情接口，不在分页查询中重复传输。

后端装配方式：

```text
按 status 分页查询 biz_resume
→ 批量查询申请人
→ 批量查询部门
→ 组装 ResumeReviewListVO
```

因此不会对当前页的每一条简历分别查询用户和部门。传入 0 或其它不支持的状态会返回 400。

### 9.5 审核详情

```http
GET /api/resume/reviews/{id}
```

权限：`LEVEL_3`。

返回：`R<ResumeReviewDetailVO>`，包含申请人的审核所需身份字段、学院/班级/学号、完整文字内容、Git 仓库地址、状态、驳回原因和审核人信息。密码、Token、支付字段等持久层敏感字段不会出现在 VO 中。

安全规则：

- 草稿或不存在的简历统一返回 404，避免未提交内容被部长通过 ID 探测。
- 只有 `PENDING`、`APPROVED`、`REJECTED` 三种状态可以查看审核详情。

前端入口：`/dashboard/resume-reviews`。部长从队列点击“查看审核”后加载此接口。

### 9.6 审核简历

```http
POST /api/resume/audit
Content-Type: application/json

{
  "resumeId": 1,
  "pass": true,
  "reason": "内容完整"
}
```

权限：`LEVEL_3`。

参数：

| 参数 | 必填 | 说明 |
| --- | --- | --- |
| `resumeId` | 是 | 简历 ID |
| `pass` | 是 | 是否通过 |
| `reason` | 驳回时必填 | 驳回原因，最多 500 个字符；通过时可省略 |

状态规则：

- 只有当前状态为 `PENDING` 的简历可以审核。
- 后端使用带 `status = PENDING` 条件的原子更新；两个部长同时操作时只有一个请求成功，另一个返回 409。
- 审核成功会写入 `audit_by`、`audit_time`，并记录不含密码/Token 的管理审计事件。
- 审核中的简历不能被申请人再次保存；审核通过或驳回后，申请人修改内容会回到草稿状态并需要重新提交。

失败场景：

| 场景 | 返回 |
| --- | --- |
| 不是部长及以上 | 403 |
| 简历不存在 | 404 |
| 驳回但没有原因 | 400 |
| 简历已经被其他人审核 | 409 |

### 9.7 查询我的 Git 同步状态

```http
GET /api/resume/git-sync
```

权限：`LEVEL_2`。

返回 `configured`、`status`、`startedAt`、`completedAt`、`errorCode`、`branch`、`commit` 和 `sizeBytes`。状态为 `NOT_SYNCED`、`SYNCING`、`SUCCEEDED` 或 `FAILED`。

### 9.8 启动 Git 仓库同步

```http
POST /api/resume/git-sync
```

权限：`LEVEL_2`，需要 CSRF。

流程：

```text
校验简历中的 HTTPS 仓库地址和允许主机
→ 数据库原子 claim 为 SYNCING，并写入 runId
→ 事务提交后投入独立 gitSyncTaskExecutor
→ shallow clone / pull
→ 校验仓库大小、读取 branch 和 HEAD commit
→ 按 runId 原子写入 SUCCEEDED 或 FAILED
```

重复同步返回 409；仓库超限返回 413；远程仓库异常记录为 `UPSTREAM_ERROR`。前端 `/dashboard/resume` 在同步期间每 2 秒轮询，部长审核详情也会展示同步结果。

## 10. 投票接口

### 10.1 提案列表

```http
GET /api/sys/vote/list?size=100
```

权限：`LEVEL_3`。

当前按 `createTime desc` 返回 `R<List<ProposalVO>>`（用 VO 而非 `Proposal` 实体，屏蔽 `deleted` 等持久层字段）。`size` 默认 100，并被 `PageUtils.clampLimit` 收敛到 1-200，避免一次拉太多数据。

### 10.2 创建提案

```http
POST /api/sys/vote/create
Content-Type: application/json

{
  "type": "NORMAL",
  "title": "举办 Java 分享会",
  "reason": "帮助新成员入门后端"
}
```

权限：`LEVEL_3`。

返回：`R<ProposalVO>`（不再直接返回 `Proposal` 实体）。

校验：走 Bean Validation（`@Valid`）。

| 字段 | 规则 |
| --- | --- |
| `type` | 必填 |
| `title` | 必填，且不超过 200 字符 |
| `reason` | 可选，不超过 2000 字符 |

校验不通过由 `GlobalExceptionHandler` 统一返回 400，消息形如 `参数错误: <字段>: <提示>`。

安全规则：

```text
type = ROOT_APPLY 会被拒绝，需要人工处理
```

原因：Root 提权这类高风险操作不能靠普通提案自动执行。

### 10.3 提交投票

```http
POST /api/sys/vote/submit
Content-Type: application/json

{
  "proposalId": 1,
  "agree": true,
  "comment": "支持"
}
```

权限：`LEVEL_3`。

逻辑：

```text
VoteService.vote
→ 检查提案
→ 检查重复投票
→ 根据角色计算权重
→ 写 VoteRecord
→ VoteRecordMapper.selectTally 返回 VoteTallyVO
→ 判断是否达到阈值
```

## 11. 公开内容和轮播图管理

### 11.1 更新协会介绍

```http
POST /api/sys/config/update-about
Content-Type: application/json

{
  "content": "<h2>关于计协</h2><p>...</p>"
}
```

权限：`LEVEL_4`。

安全：

```text
Jsoup.clean(content, ABOUT_CONTENT_SAFELIST)
```

允许基础富文本，过滤危险脚本。

### 11.2 保存轮播图

```http
POST /api/sys/carousel/save
Content-Type: application/json

{
  "title": "协会招新",
  "imgUrl": "https://example.com/banner.png",
  "targetUrl": "https://example.com",
  "sortOrder": 1,
  "status": 1
}
```

权限：`LEVEL_4`。

校验：

- `title` 不能为空。
- `imgUrl` 不能为空。
- 编辑不存在的 ID 返回 404。

贡献：

```text
@LogContribution(type = OPS, detail = "更新轮播图")
```

### 11.3 删除轮播图

```http
POST /api/sys/carousel/delete?id=1
```

权限：`LEVEL_4`。

## 12. 贡献和导出接口

### 12.1 人工记录贡献

```http
POST /api/sys/contribution/award
Content-Type: application/json

{
  "userId": 1001,
  "type": "DEV",
  "score": 20,
  "reason": "完成官网页面开发"
}
```

权限：`LEVEL_4`。

请求校验：

- `userId` 必须对应仍存在且处于 `ACTIVE` 状态的成员；已删除或已匿名化账号不能新增记录。
- `type` 只能是 `DEV`、`RES`、`COMP`、`OPS`，分别表示官网建设、资源贡献、发布比赛和首页维护。
- `score` 必须大于 0，最多 8 位整数和 2 位小数；`reason` 必填且最多 500 个字符。
- 成功后写入 `source=MANUAL`、`awardedBy=当前操作人`，并记录 `CONTRIBUTION_MANUAL_AWARD` 管理审计事件。
- 贡献墙和总排行会通过 SQL 聚合这条记录。

### 12.2 查询贡献记录

```http
GET /api/sys/contribution/awards?page=1&size=20&keyword=张三&type=DEV&source=MANUAL
```

权限：`LEVEL_4`。分页 `size` 统一收敛到 1-100。筛选参数：

- `keyword`：按成员用户名、姓名或学号匹配，最多 64 个字符。
- `type`：`DEV`、`RES`、`COMP`、`OPS`。
- `source`：`AUTO`（系统自动）、`MANUAL`（人工补录）或 `LEGACY`（V5 以前的历史记录）。

返回 `R<Page<ContributionAwardVO>>`，包含成员、部门、类型、分值、说明、来源、操作人和创建时间。查询先分页贡献流水，再批量加载成员和部门信息，不按记录逐条查库。

前端入口：`/dashboard/contributions`。Level 4 管理员可以搜索成员、选择类型、输入分值和说明，确认后提交；页面默认展示人工记录，并可切换查看自动和历史流水。

### 12.3 导出成员

```http
POST /api/sys/export/members
Content-Type: application/json

{
  "columns": ["realName", "studentId", "college", "className"],
  "startTime": "2026-08-01 00:00:00",
  "endTime": "2026-08-31 23:59:59",
  "roleLevel": 2
}
```

权限：

```text
LEVEL_4 或 ADMIN
```

返回：Excel 文件流。

这个接口不是 `R<T>`，而是直接写 `HttpServletResponse`。

前端入口：`/dashboard/member-export`。支持日期、学院、班级、姓名、学号、角色、邀请码筛选和列选择；导出动作写入审计日志。

### 12.4 查询审计日志

```http
GET /api/sys/audit/list?page=1&size=20&action=LOGIN_FAILURE&result=FAILURE&requestId=request-id
```

权限：`LEVEL_4`。分页 `size` 收敛到 1-100，可按 action、result 和 request ID 精确筛选。审计写入前会递归剔除密码、Token、Cookie、Authorization 和验证码等敏感键；查询接口用于通过 request ID 关联安全事件与服务日志。

前端入口：`/dashboard/audit`，可查看执行人、目标、IP、User Agent、Request ID 和结构化详情。

## 13. 调试接口

```http
GET /api/test/users
GET /api/test/password
```

这些接口用于开发调试。生产环境建议：

1. 删除。
2. 或只允许最高管理员。
3. 或按 profile 条件加载。

## 14. 前端 service 对照表

| 前端文件 | 后端模块 | 说明 |
| --- | --- | --- |
| `src/services/auth.ts` | `/api/auth/**` | 登录、注册、验证码、退出、找回/重置密码 |
| `src/services/account.ts` | `/api/account/**` | 改密、会话吊销、停用、删除申请、个人数据导出 |
| `src/services/user.ts` | `/api/sys/user/**`、`/api/sys/export/members` | 当前用户、成员目录、成员导出 |
| `src/services/resource.ts` | `/api/sys/resource/**` | 资源列表、分类、保存、删除、下载计数 |
| `src/services/file.ts` | `/api/common/file/upload` | 文件上传 |
| `src/services/competition.ts` | `/api/biz/comp/**`、`/api/public/competitions` | 竞赛 |
| `src/services/resume.ts` | `/api/resume/**` | 简历、审核和 Git 同步 |
| `src/services/dept.ts` | `/api/sys/dept/**` | 部门 |
| `src/services/vote.ts` | `/api/sys/vote/**` | 提案投票 |
| `src/services/public.ts` | `/api/public/**`、`/api/sys/config/update-about` | 公开内容 |
| `src/services/audit.ts` | `/api/sys/audit/**` | 管理审计查询 |

## 15. 推荐调试顺序

如果一个接口失败，按这个顺序查：

1. 前端 Network：请求 URL 是否正确。
2. 前端 Request Headers：浏览器默认看 Cookie 和 CSRF；只有 API 调试/非浏览器客户端才看 Authorization。
3. 后端 Security：是否被 401/403 拦截。
4. Controller：参数是否命中。
5. Service：业务规则是否抛异常。
6. Mapper：SQL 是否正确、表是否存在。
7. GlobalExceptionHandler：最终返回什么错误结构。

这个顺序比直接盯着前端页面更快。

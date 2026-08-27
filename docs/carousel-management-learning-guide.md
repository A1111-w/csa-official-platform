# 首页轮播管理学习与回退指南

更新日期：2026-08-27

本文解释首页轮播从“只能展示和调用 API”补齐为“可在网页完整维护”的实现。功能提交为 `f295e24`，改动前标签为 `before-carousel-management-2026-08-27`，实际指向提交 `45a07c2`。

## 1. 原来有什么，缺什么

原来已经存在：

- 首页调用 `GET /api/public/carousel/list`，只展示启用轮播。
- 后端有保存和删除接口。
- `sys_carousel` 表、公开缓存和首页轮播组件已经存在。

原来缺少：

- 能看到停用项的后台列表接口。
- 前端轮播 service、页面、菜单和工作台入口。
- 上传、预览、新增、编辑、排序、启停、删除的网页流程。
- 站内上传图片被首页匿名读取时的对象级授权规则。

所以 `c517eda` 解决“首页展示”，`f295e24` 解决“后台维护闭环”。

## 2. 页面和请求链路

管理列表：

```text
/dashboard/carousels
→ CarouselManagementWorkspace
→ carouselService.list
→ GET /api/sys/carousel/list
→ @PreAuthorize(LEVEL_4)
→ CarouselController.getAdminList
→ CarouselMapper.selectList
→ CarouselAdminVO[]
```

新增或编辑：

```text
填写标题、图片、跳转、排序和启用状态
→ carouselService.save
→ POST /api/sys/carousel/save + CSRF
→ CarouselSaveRequest 白名单校验
→ 图片归属/类型与 URL 校验
→ insert 或 updateManagedFields
→ 清 public_carousel 缓存
→ CAROUSEL_SAVE 审计 + OPS 自动贡献
→ 页面重新加载管理列表
```

上传图片：

```text
选择 JPG / PNG / GIF
→ POST /api/common/file/upload
→ FileService 校验大小、扩展名和文件签名
→ 写物理文件和 sys_stored_file 元数据
→ 返回 /files/{ownerId}/{fileName}
→ 保存轮播时再次校验当前用户是 owner
```

## 3. 为什么需要两个 VO/DTO

`CarouselSaveRequest` 是输入白名单，只允许：

- `id`
- `title`
- `imgUrl`
- `targetUrl`
- `sortOrder`
- `status`

它不会接收 `deleted`、`createTime`、`updateTime`。这样即使客户端额外发送实体字段，也不能修改逻辑删除和审计时间。

公开接口继续使用 `CarouselVO`，只返回首页渲染需要的四个字段。后台列表使用 `CarouselAdminVO`，额外返回排序、状态和时间。公开契约和管理契约分开后，数据库实体变化不会自动扩大匿名接口响应。

## 4. URL 和文件安全边界

图片地址只允许：

- 有有效元数据的站内 `/files/...` 图片；
- 带合法 host 的 HTTP(S) URL。

跳转地址只允许：

- `/competitions` 这类站内绝对路径；
- HTTP(S) URL。

后端拒绝 `javascript:`、URL 凭据、协议相对 URL、反斜杠伪装和非法 URI。前端预览校验只是用户体验，真正安全边界仍在后端。

`/files/**` 在 SecurityFilterChain 中允许请求进入 Controller，但不等于目录公开。Controller 继续按对象判断：

| 文件状态 | 匿名 | 普通登录用户 | Level 4 |
| --- | --- | --- | --- |
| 启用轮播引用 | 可读，`inline` | 可读 | 可读 |
| 停用轮播引用 | 401 | 非 owner 为 403 | 可预览 |
| 已发布资源引用 | 401 | 可读 | 可读 |
| 私人文件 | 401 | 仅 owner 可读 | 非 owner 仍不可读 |

这条规则避免为了首页图片把所有上传目录变成公开静态目录。

## 5. 为什么更新不用完整实体

新增时构造新的 `Carousel`，只设置白名单字段。编辑时调用 `CarouselMapper.updateManagedFields`，SQL 只更新标题、图片、跳转、排序和状态。

显式 SQL 还有一个实际作用：管理员把跳转地址清空时，`target_url = NULL` 会真正执行。若直接依赖默认 `updateById`，MyBatis-Plus 可能忽略 `null`，页面显示“保存成功”但旧链接仍留在数据库。

## 6. 缓存、审计和删除

- 公开列表缓存名为 `public_carousel`。
- 保存和删除都使用 `@CacheEvict(allEntries = true)`，下一次首页读取会重新查库。
- 保存记录 `CAROUSEL_SAVE`，删除记录 `CAROUSEL_DELETE`。
- 删除使用既有 MyBatis-Plus 逻辑删除，不物理删除 `sys_carousel` 行。
- 孤儿文件清理 SQL会检查未删除轮播引用；轮播删除或换图后，旧上传文件经过宽限期才有资格被清理。

## 7. 数据库、依赖和配置影响

本功能：

- 不新增 Flyway migration。
- 不修改 `sys_carousel` 或 `sys_stored_file` 结构。
- 不新增环境变量。
- 不修改 Maven、npm 依赖或 lockfile。

部署时只需部署对应前后端代码。已有数据库不需要额外 SQL。

## 8. 测试覆盖

后端测试覆盖：

- 公开接口不泄露状态和排序字段。
- Level 3 不能读取管理列表，Level 4 可以。
- 保存只接受白名单字段并规范化输入。
- 非法跳转、反斜杠伪装和他人私人上传被拒绝。
- 编辑时可以把已有跳转地址清空。
- 启用轮播图片可匿名读取，停用图片只允许 Level 4 预览，私人文件仍受保护。

前端测试覆盖 service 端点和请求参数。Playwright 使用模拟 Level 4 会话和 API 响应，验证页面能显示轮播、进入编辑状态并回填标题和跳转地址。

实际结果：后端专项 16/16；后端全量 185 个测试通过、1 个 Docker/Testcontainers 跳过；前端 7 个文件/13 个测试通过；lint 0 错误；production build 通过；Playwright 3 个可运行用例通过、3 个真实账号用例跳过。

## 9. 回退方法

推荐非破坏性回退：

```powershell
cd D:\CSA-Project
git revert f295e24
```

回退前对照点：

```powershell
git show before-carousel-management-2026-08-27
git diff before-carousel-management-2026-08-27..f295e24
```

不要使用 `reset --hard`。本功能没有 migration，回退应用代码不需要回退数据库结构。已保存的 `sys_carousel` 数据可以保留；旧代码仍能读取启用项，也保留原有保存/删除接口。

## 10. 手工验收

1. 使用 Level 3 账号访问 `/dashboard/carousels`，确认页面无管理权限，直接请求管理接口返回 403。
2. 使用 Level 4 新建一条停用轮播，确认它出现在后台但不出现在首页。
3. 启用该轮播，刷新匿名首页，确认图片和标题出现。
4. 清空跳转地址并保存，确认首页不再显示“查看详情”按钮。
5. 停用轮播，确认匿名图片 URL 不再可读，而 Level 4 后台仍能预览。
6. 删除轮播，确认管理列表和首页都不再显示，审计日志存在 `CAROUSEL_DELETE`。

真实登录、上传和写操作仍应在健康 staging 用受控 Level 4 测试账号再跑一次，不能只把模拟浏览器测试当成生产发布证据。

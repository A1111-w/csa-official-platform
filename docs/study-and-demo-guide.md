# Vue 版学习与演示指南

本分支使用 Vue 3；完整 React 历史讲义保留为 [React 参考讲义](study-and-demo-guide-react.md)。不要混用两个分支的命令与文件路径。后端原理仍可参考旧讲义的对应章节，当前前端事实以本文件和源码为准。

### 0.1 中断后如何继续

先确认 `git branch --show-current` 为 `codex/vue3-frontend`，查看最近一次学习记录，记录“已能独立完成什么、在哪一步卡住、下一次只做什么”。源码根使用 `<project-root>`，本机为 `D:/CSA-Project-vue`。老师先问学员当前能观察到什么，不要求背完整项目。

### 0.2 课前准备

老师：准备 Node >=22.12、隔离数据库/后端、会员与部长账号；确认 `npm ci`、`npm run dev` 可用。不要共享密码或使用生产数据。学员：会基本 HTML、JavaScript 对象/数组和 async/await 即可先学公开页；TypeScript 可在阅读接口类型时逐步补。

每节分为“演示一次 → 学员复现 → 改一个参数 → 解释观察”。先走核心路径，再做挑战。以下是主题顺序，不要求一天学完一节。

### 0.3 历史漂移记录

React 旧版为 Next.js 16.3.3 / React 19.2.4，工作目录 `D:/CSA-Project-agent`。Vue 版替换了前端框架和启动工具，保留服务契约与后端。旧讲义中的 page.tsx、useState、Zustand、next start 仅适用于 React 分支。历史 CI 和测试数字不能用于证明 Vue 版通过。

## 1. 从页面到一个请求

**先修**：能启动前端，知道“浏览器和后端是两个进程”。

**目标**：用自己的话解释一次资源列表请求。

**教师演示**：打开 /dashboard/resources，在浏览器网络面板定位 /api/sys/resource/list。

**学员步骤**：

1. 打开 src/router/index.ts，找到资源路由对应的 ResourcesView.vue。
2. 打开 src/views/business/ResourcesView.vue，定位 loadResources 和 pageSize = 8。
3. 跟进 src/services/resource.ts 的 resourceService.list。
4. 看 src/lib/axios.ts：取 response.data 为协议 payload，再在成功时返回 payload.data。
5. 后端 ResourceController.list → ResourceService.listResources → resourceMapper.selectPage。
6. 回到页面，观察 records、pages、total 如何写入 items.value、pages.value、total.value。

**可观察结果**：切换分类会重置到第一页；下一页时网络参数变化；空列表有明确提示。

**常见阻塞**：401 需登录、403 需会员角色、连接失败检查8080。老师先让学生读状态码，再提供下一层提示。

**挑战**：解释为什么当前一页只有8条，却仍可能显示 total=30；不要让学生先改数据库或模型配置。

## 2. 理解 Vue 的状态更新

**先修**：完成上节请求追踪。

**目标**：理解 ref 的 .value 与模板自动解包。

**练习**：找出 ResourcesView 的 items/page/loading，说明哪个值控制列表、分页、加载态；在本地测试 fixture 改 total，观察界面。

**验收**：能说明 computed 用于派生状态，本页 watch(..., { immediate: true }) 在首次和身份变化时加载；其它页面如 AgentView 用 onMounted 首次加载；知道 Vue 插值会转义文本。

**教师提示**：先画“接口数据→状态→模板”三个框，不要求同时理解编译器。

**挑战**：检查快速切换过滤器时旧请求为什么不能覆盖新结果。

## 3. 会话、角色和 CSRF

**先修**：会查看网络请求。

**目标**：区分身份认证、角色授权和 CSRF。

**练习**：会员打开 AI 页面，再直接输入 /dashboard/audit；比较 /api/sys/user/info 的 roleLevel 与菜单；退出后重新打开深层链接。

**验收**：路由守卫先验证后端会话，会员可问答但不能管理评测；没有 JWT 出现在 localStorage；POST 携带 X-CSRF-Token。

**教师提示**：先解释“是谁、能做什么、请求是否来自可信页面”三个独立问题。前端隐藏按钮只是体验，真正权限由后端 @PreAuthorize 决定。

**常见阻塞**：不要把网络故障当成登录过期；/session-error 给出重试入口。

**后端关联**：校验失败会产生 MethodArgumentNotValidException；401 由 JwtAuthenticationEntryPoint 输出，403 由 JwtAccessDeniedHandler 输出；GlobalExceptionHandler 包装业务错误。

**挑战**：用隔离 fixture 将 roleLevel 从4改1，观察下一次受保护导航被拒绝。

## 4. 完成一个业务闭环

**先修**：前三节；老师准备对应角色。

**目标**：从“能看列表”进阶到“能解释写入与回读”。

**核心练习**：部长新增一份资源→刷新列表→修改简介→重新打开确认；会长练习轮播预览与保存。

**验收**：失败时表单保留输入；按钮在提交中禁用；成功后列表重新获取；删除要二次确认。

**教师提示**：一次只选一个模块，先画操作顺序再看 service；简历审核、投票和部门任命作为不同难度选题。

**挑战**：模拟保存409或403，解释前端怎样显示错误，不允许把后端规则移到浏览器替代校验。

## 5. Agent / RAG / 评测

完成 [Agent 教学计划](agent-learning-plan.md) 的四节核心课即可展示完整离线链路；在线模型和向量混合检索是两节选修。Vue 入口为 src/views/admin/AgentView.vue，service 仍是 src/services/ai.ts，后端仍是 modules/ai。

先做问答和引用，再看工具轨迹，最后解释规则评测中的 Recall、MRR、工具召回和关键词覆盖。未标注的维度显示 N/A；规则通过不代表语义事实正确。教师避免在同一节同时引入 Vue 生命周期和 RAG 排序公式。

## 6. 构建、部署与排错

**先修**：能运行一个业务闭环。

**练习**：运行 npm test、npm run lint、npm run build；观察 dist；npm run start 预览。

**验收**：知道 Vite 是构建工具，生产由 Nginx 托管 dist；/dashboard/ai 刷新需要 fallback；/api 与 /files 保持同源代理。

**教师提示**：先本地预览，再讲 Docker；不要让初学者从部署故障开始学 Vue。

**挑战**：解释 VITE_API_URL 为什么必须在构建前配置，API_PROXY_TARGET 为什么不进入浏览器 bundle。

## 当前版本契约

以下版本来自源码，不代表外部服务已运行：

- Vue 3.5.45；Vue Router 4.6.4；Pinia 3.0.4；Vite 8.3.6。
- axios 1.21.0；dompurify 3.4.17；TypeScript 5.9.3。
- Spring Boot 3.5.14；Java 17；MyBatis-Plus 3.5.5；JJWT 0.12.6。
- EasyExcel 3.3.4；JGit 6.8.0.202311291450-r；Knife4j 4.3.0。

运行 `tools/check-study-guide-drift.ps1` 检查部分文档与源码契约。它不代替业务测试、浏览器验收或真实模型质量评估。最新验证在 [集成记录](change-records/2026-11-08-vue-integration.md)。

## 给教师的复查清单

- 内容正确：字段、权限、路径、请求行为均能在当前分支定位。
- 难度合适：每节只有一个主目标，先修和成功表现具体。
- 遇错可继续：有常见阻塞、逐层提示、离线替代和重试方式。
- 学员能独立表达：用实际观察解释结果，不能只复述术语。
- 不制造虚假进度：完成课内操作不等于掌握所有边界；挑战题可留到下次。
- 当前安排经过文档和源码复查，尚无真实课堂试教记录。

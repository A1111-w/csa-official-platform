# 项目学习、演示和面试讲解指南

这份文档的目标是把 CSA Official 从“AI 帮我写过的项目”变成“我能讲清楚、能排错、能继续开发、能写进简历”的项目。

它对应当前项目和学习记录仓库里的训练思路。项目源码根目录是 `D:\CSA-Project`，学习记录仓库是
`C:\Users\27719\Documents\java康复训练`。文档里的路径、版本和回答必须以当前源码为准，不能沿用旧截图或旧线程答案。

```text
先看结构
再追请求
再理解鉴权
再做模块
再补测试和文档
再让别人也能跑起来（数据库脚本 + 一键启动）
最后扩展 RAG / Agent
```

## 0. 文档与源码防漂移规则

这份指南不是脱离源码的背诵稿。每次项目重构、依赖升级或接口变更后，必须按下面顺序重新校准：

```text
1. 先读当前源码和配置（不要先相信旧学习日志）
2. 用 rg 搜索类名、注解、路径和字段，确认真实位置
3. 用一次可重复的请求/测试验证运行结果
4. 再修改本指南和当天学习日志
5. 重新读取修改后的文档，检查同一概念是否还出现旧说法
```

学习结论必须标注证据等级：

| 标记 | 含义 |
| --- | --- |
| 独立找到 | 能指出文件、类/方法/字段，并解释其作用 |
| 经提示掌握 | 在提问或纠错后能复述，但还没有独立完成验证 |
| 只记住结果 | 只能说出数字或名称，不能解释源码链路 |
| 尚未掌握 | 目前无法从源码说明，不能写成面试中的确定性结论 |

如果文档和源码冲突，先把旧说法标成“过时”，再以源码修正文档；不要为了保持旧答案一致而修改正确源码。

### 0.1 对话被压缩后的恢复流程

长线程会被摘要压缩，摘要只能帮助定位，不能替代原始问答和当前源码。继续修改这份指南前必须执行：

```text
1. 重新读取当前任务目标和 `docs/study-and-demo-guide.md`，不能把压缩摘要当成已经完成的证明
2. 能读取任务历史时，分页读回本任务的原始用户回答和批注；不能读取时，必须使用用户保存的导出文本/附件，并明确记录缺失范围
3. 重新读取持久化学习记录：
   C:\Users\27719\Documents\java康复训练\learning-log\day-01-project-overview.md
   C:\Users\27719\Documents\java康复训练\learning-log\day-02-request-chain.md
   C:\Users\27719\Documents\java康复训练\learning-log\day-03-unified-response-and-errors.md
4. 如果旧线程有导出文本或附件，完整读取原文，不能只靠压缩摘要转述
5. 查看 `D:\CSA-Project` 当前分支、commit、`git status` 和相关 diff，避免覆盖用户尚未提交的修改
6. 对本次涉及的类、方法、接口、版本重新从当前源码取证；历史日志只能证明“当时答过什么”，不能证明现在代码仍然如此
7. 把每个结论拆成“用户原回答 / 纠正后的理解 / 当前源码证据 / 当前掌握等级”，不能由标准答案倒推用户已经掌握
8. 修改后搜索整份文档中的旧关键词，再完整回读被改章节，并至少运行一项与改动内容对应的测试或可重复检查
```

事实来源优先级固定为：

```text
当前源码与运行/测试结果
> 当前配置和依赖清单
> 当前学习指南
> 持久化学习日志
> 旧对话和旧截图
```

旧对话的价值是恢复“你答过什么、哪里曾经混淆”，不是证明现在的代码仍然如此。

如果原始对话暂时无法读取，处理规则不是“相信摘要继续写”，而是：先完成源码可证明的部分，把涉及用户掌握程度的结论标成“待原文复核”，等拿到原文后再更新。缺少原文时不能擅自把“未回答”补成“已掌握”。

### 0.2 验收出题规则

最近几天的问答暴露出两类出题问题：使用了不存在的占位类（例如泛化的 `SaveDto`），以及继续沿用重构前答案。以后出题必须遵守：

1. 题目先给出当前源码中的真实文件、类和方法；抽象示例必须明确标注“非项目源码”。
2. 题目涉及路径、返回类型、权限、异常或版本时，出题前先用源码搜索确认。
3. 一次只验收一个清晰链路，不把“类、异常、处理方法、错误码”混成一个模糊问题。
4. 评价答案时区分：结论对、术语不准、链路缺失、源码已变化，不能只给“对/错”。
5. 没在阅读材料里覆盖的问题先补材料，不直接用它否定当天核心目标。
6. 你明确说“没说的默认不会”时，未回答项必须进入“尚未掌握”，不能由文档替你补成“已学会”。
7. 每个 Day 的完成标准必须同时包含“能从哪里找到”和“能解释为什么”，不能只背结果。

### 0.3 本轮恢复证据和已知历史陷阱

2026-08-26 这次更新没有只依赖压缩摘要，已重新读取以下持久化材料，并再次对照当前工作区源码：

| 材料 | 覆盖内容 | 使用方式 |
| --- | --- | --- |
| `C:\Users\27719\.codex\attachments\369db1ee-4576-4a77-b7e4-13d5707e6b30\pasted-text.txt` | 7 月文档对账、Day 1 初次验收和八个补强问题 | 恢复用户当时的真实回答和“不在材料里”的异议 |
| `learning-log/day-01-project-overview.md` | Day 1 阅读范围、模块理解和当时掌握状态 | 作为历史学习证据，不直接覆盖当前版本和接口 |
| `learning-log/day-02-request-chain.md` | 重构后的资源列表主链路 | 与当前 `ResourceLibrary`、`ResourceService`、Axios 源码逐箭头复核 |
| `learning-log/day-03-unified-response-and-errors.md` | 统一返回、MVC 异常、Validation、401/403 和逐题掌握状态 | 压缩后恢复 Day 3 真实验收进度，不把标准答案当作已掌握 |
| 当前任务中的批注和逐题回答 | Day 1 后半段、Day 2 请求链路、Day 3 异常链路 | 用于区分独立找到、经提示掌握和术语仍混淆的部分 |
| `D:\CSA-Project` 当前工作区 | 最终事实来源 | 每次以 `git status --short --branch` 和 `git rev-parse HEAD` 重新取证；不把本文件中的旧分支或 commit 当作当前事实 |

历史材料里已经确认存在下列旧口径。它们保留的是学习过程，不能复制回当前答案：

| 历史说法 | 当前口径 | 当前证据 |
| --- | --- | --- |
| Next.js `16.2.10` | Next.js `16.3.3` | `csa-official-frontend/package.json` 的 `dependencies.next` |
| 资源列表返回 `R<Page<Resource>>` | Controller 返回 `R<Page<ResourceVO>>` | `ResourceController.list()` |
| 资源接口没有 Service，Controller 直接操作 Mapper | 已有 `ResourceService`；Controller 只接参、鉴权和包装结果 | `ResourceController`、`ResourceService` |
| Mapper “没有执行操作，只是继承 BaseMapper” | 真正查询调用是 `resourceMapper.selectPage(pageParam, query)` | `ResourceService.listResources()` |
| 页码超过总页数会被 `PageUtils` 改成 1 | `PageUtils` 不知道数据库总页数；前端收到总页数后重新请求最后一页 | `PageUtils.of()`、`ResourceLibrary.loadResources()` |
| `JwtAccessDeniedHandler` 或 `AuthCookieService` 负责从请求取 JWT | `JwtAuthenticationFilter` 取出并校验 JWT；前两者分别处理 403 和 Cookie 生命周期 | 三个类的当前实现 |
| 认证结果“保存到 User” | 当前请求的 `Authentication` 写入 `SecurityContext`；不是永久写入 `User` 实体 | `JwtAuthenticationFilter.doFilterInternal()` |
| `SaveResourceDto` 是发现空标题的校验注解 | DTO 只承载数据；`title` 上的 `@NotBlank` 定义规则，参数上的 `@Valid` 启动校验 | `ResourceController.SaveResourceDto`、`save()` |
| Validation 失败直接说“抛 BindException” | 实际是 `MethodArgumentNotValidException`，因继承 `BindException` 被 `handleBindingException` 接住 | `GlobalExceptionHandler` |
| 无 JWT 最终由 Filter 直接写 401 | Filter 未建立认证后继续；Spring Security 最终调用 `JwtAuthenticationEntryPoint` 写 401 JSON | `SecurityConfig`、EntryPoint |
| `sys` 只是用户信息模块 | `sys` 还包含部门、资源、投票、贡献、轮播图、公开配置、文件元数据和账号治理 | `modules/sys` 当前目录 |

这张表是恢复检查表，不是新的背诵题。以后只要其中任一相关源码发生变化，就重新取证并更新“当前口径”，不能继续在表后面叠加第三套说法。

学习路线一共 14 天：Day 1-12 是「读懂这个项目」，Day 13-14 是「能把它交付给别人」。
后两天是补的，因为项目早期只有手工表和结构快照，换台机器无法稳定复现；现在生产结构已经进入 Flyway 版本链，
同时保留结构快照帮助学习和人工核对。

## 1. 你要形成的最终表达

你不能只说：

```text
这是我用 AI 写的计协官网。
```

你要能说：

```text
这是一个面向计算机协会的官网和内部管理平台。前端用 Next.js，后端用 Spring Boot 3。它包含公开官网、登录注册、资源库、竞赛管理、简历投递、部门人事、提案投票和贡献墙。安全上用了 Spring Security + JWT + HttpOnly Cookie + CSRF，权限通过 roleLevel 映射到 Spring Security 角色，后端接口用 @PreAuthorize 做真实校验。数据层用 MyBatis-Plus 和 MySQL，缓存层支持 Redis，也可以在本地切到内存缓存。项目还补了统一返回、全局异常、限流、文件访问控制、后端测试、前端构建和详细启动文档。后续我准备把资源库扩展为 RAG 知识库，再把资源查询和竞赛查询封装成 Agent 工具。
```

这段话要练到不用看稿也能说。

## 2. 1 分钟项目介绍

适合面试开场：

```text
CSA Official 是我做的一个计算机协会官网和内部管理平台。前端使用 Next.js 和 TypeScript，后端使用 Spring Boot 3、Spring Security、MyBatis-Plus、MySQL 和 Redis。系统分为公开官网和登录后的工作台：公开部分展示协会介绍、贡献者和比赛信息；后台部分根据用户角色开放资源库、简历、竞赛、提案投票、部门人事和公开内容维护。项目重点不只是页面展示，我还补了 JWT 登录、HttpOnly Cookie、CSRF 防护、角色权限、统一异常、限流、文件访问控制和测试。后续计划把资源库升级成 RAG 知识库，并封装 Agent 工具，让 AI 能查询资源和竞赛、生成通知草稿。
```

## 3. 3 分钟项目介绍

适合面试官说“展开讲讲你的项目”：

```text
这个项目的业务背景是计算机协会需要一个官网和内部管理系统。游客可以看协会介绍、贡献者、比赛活动；登录用户进入工作台后，会根据 roleLevel 看到不同功能。比如会员能访问资源库，核心成员能维护简历，部长可以发布资源和管理竞赛，会长可以做部门任命和公开内容维护。

技术上，前端用 Next.js App Router，页面、业务组件、service、store 分层比较清楚。所有接口调用都集中在 src/services，底层由 src/lib/axios.ts 统一处理 API 地址、withCredentials、CSRF Token 自动获取和附加、401 跳转和错误包装，不再由前端读取或拼接 Bearer Token。登录响应体只返回 csrfToken、username、roleLevel，认证 JWT 只通过后端 Set-Cookie 写入 HttpOnly Cookie。登录态放在 Zustand，只保存页面渲染需要的 user 信息；src/proxy.ts 会在进入 /dashboard 前做 Cookie 存在性拦截，并设置 CSP nonce、CSP Header 和生产环境 HSTS。

后端用 Spring Boot 3，目录分 common、config、modules。common 放统一返回、异常、安全、缓存、文件等通用能力；config 放 Security、CORS、CSRF、Redis/内存缓存、异步线程池、启动安全校验、MyBatis、Swagger 等配置；modules 里有 sys、biz、resume 三个业务模块。请求进来后先过 Spring Security，JwtAuthenticationFilter 会从 Authorization 或 Cookie 解析 JWT，写入 SecurityContext，然后 Controller 通过 @PreAuthorize 做权限控制，Service 处理业务规则，Mapper 访问 MySQL，最后用 R<T> 统一返回。

安全上，我重点做了几件事：登录成功后使用 HttpOnly Cookie 保存 JWT，降低 XSS 读取 Token 的风险；因为 Cookie 会被浏览器自动携带，所以对 Cookie 登录的非 GET 请求增加 CSRF Token 校验；退出登录时把 JWT 放入吊销缓存；Redis 不可用时本地可以切到内存缓存；文件访问不是简单静态目录，而是校验 owner 或资源发布状态；富文本内容用后端 Jsoup 和前端 DOMPurify 双层清洗，减少 XSS 风险。

工程化上，项目有 .env.example、本地启动文档、接口地图、架构说明、安全说明、后端测试和前端构建验证。数据库结构由 Flyway V1-V5 migration 版本化，db/seed.sql 只做 dev/test 演示；开发 Compose 会起 MySQL、Redis、前后端，生产 Compose 则由 Caddy 提供同源入口。我还写了一个一致性测试，把实体字段和全部 Flyway migration 做双向比对，改了实体忘改 DDL 会直接构建失败，而不是等到换环境时才在运行时报 Unknown column。

我也做过几轮针对性优化：分页接口原来 size 没有上限，一个 ?size=1000000 就能把数据库和堆打满，现在统一收敛；竞赛列表原来把整段富文本正文都发给前端，而列表只显示 120 字摘要，现在列表只返回服务端截断的摘要，正文改由详情接口按需取；资源模块的业务逻辑从 Controller 抽到了 Service，这样才能加事务、才能脱离 Web 环境做单元测试。

后续扩展方向是 RAG 和 Agent：先把资源文档解析、切片、向量化，做带引用的问答；再把查询资源、查询竞赛、生成通知草稿封装成工具，让 AI 调用后端真实能力，同时保留权限校验和工具调用审计。
```

## 4. 10 分钟深度讲解结构

如果你有 10 分钟，可以按这个顺序：

1. 业务背景：协会为什么需要官网和管理后台。
2. 用户角色：游客、会员、核心成员、部长、会长、Root。
3. 前端结构：页面、组件、service、store、axios。
4. 后端结构：common、config、modules。
5. 登录链路：用户名密码、AuthenticationManager、JWT、Cookie、CSRF。
6. 权限链路：roleLevel、GrantedAuthority、@PreAuthorize。
7. 业务模块：资源、竞赛、简历、部门、投票、贡献。
8. 工程化：统一返回、全局异常、限流、测试、文档。
9. 安全细节：CSRF、Token 吊销、文件访问、HTML 清洗。
10. 后续扩展：RAG 知识库、Agent 工具、审计日志。

## 5. 学习路线

### 当前学习进度基线（2026-08-26）

| 阶段 | 状态 | 已证明的能力 | 仍需补证据 |
| --- | --- | --- | --- |
| Day 1 项目全景 | 核心完成，补强中 | 能从依赖文件查版本、找到页面和模块、说明总体架构 | Maven Wrapper、环境占位符、缓存用途、JJWT 与密码编码的区别等补强题需独立复述 |
| Day 2 请求链路 | 核心完成，细节复测中 | 能追资源列表的页面 → Controller → Service → Mapper → VO → 前端状态 | Hook 触发、SecurityContext 生命周期、Axios 泛型和运行时拆包需脱稿且指出源码位置 |
| Day 3 统一返回和异常 | 验收中 | 已实际观察 400/401/403 JSON，能判断大部分失败阶段和错误码 | `@NotBlank` / `@Valid` / `MethodArgumentNotValidException` / `BindException` 的层次还会混淆，需要完成七题复测 |
| Day 4 及以后 | 尚未按计划系统验收 | 部分代码曾经阅读过 | 不能因为读过零散 Controller 就标记完成 |

状态更新规则：只有“自己能定位源码 + 能解释链路 + 至少一次请求/测试证据”三项同时满足，才从“验收中”改成“完成”。

### Day 1：项目全景

目标：知道项目有哪些部分。

读这些文件：

```text
README.md
csa-official-backend/pom.xml
csa-official-backend/src/main/resources/application.yml
csa-official-backend/src/main/java/com/csa/official/CsaOfficialApplication.java
csa-official-frontend/package.json
csa-official-frontend/src/app
```

当前版本要对得上源码：

```text
Spring Boot 3.5.12
Java 17
MyBatis-Plus 3.5.5
JJWT 0.12.6
EasyExcel 3.3.4
JGit 6.8.0.202311291450-r
Knife4j 4.3.0
Next.js 16.3.3
React 19.2.4
axios 1.18.1
isomorphic-dompurify 3.18.0
```

你要能回答：

1. Spring Boot 版本在哪里查？
   `csa-official-backend/pom.xml` 的 `<parent><version>`，当前是 `3.5.12`。
2. Java 版本在哪里查？
   同一个 `pom.xml` 的 `<properties><java.version>`，当前是 `17`。
3. Next.js、React、React DOM、TypeScript 版本在哪里查？
   `csa-official-frontend/package.json`：`dependencies.next`、`dependencies.react`、
   `dependencies.react-dom` 和 `devDependencies.typescript`。当前分别是 `16.3.3`、
   `19.2.4`、`19.2.4` 和 `^5`。
4. 后端依赖如何从“名字”追到“用途”？
   先从 `pom.xml` 列出依赖，再用源码搜索使用位置，最后按调用范围判断是否核心：

   | 依赖 | 作用 | 源码证据 |
   | --- | --- | --- |
   | `spring-boot-starter-web` | MVC Controller、JSON 请求/响应 | `modules/*/controller` |
   | `spring-boot-starter-security` | 认证、授权、过滤器链 | `SecurityConfig`、`JwtAuthenticationFilter` |
   | `spring-boot-starter-validation` | `@Valid` 触发 DTO 字段校验 | `LoginDto`、`RegisterDto`、`ResourceController.SaveResourceDto` |
   | MyBatis-Plus `3.5.5` | Mapper、CRUD、分页查询 | `*Mapper extends BaseMapper`、`ResourceService` |
   | JJWT `0.12.6` | JWT 生成、签名、解析和校验 | `JwtUtils` |
   | Redis starter | Redis 缓存实现 | `RedisKeyValueStore`、缓存配置 |
   | EasyExcel `3.3.4` | Excel 导出 | `UserExportService` |
   | JGit `6.8.0.202311291450-r` | 读取/分析 Git 仓库 | `GitService` |
   | Spring Mail | 邮件验证码和异步投递 | `MailService`、`AsyncMailSender` |
   | AOP starter | 限流、贡献记录等切面 | `RateLimitAspect`、`ContributionAspect` |
   | Jsoup | 后端 HTML 清洗、摘要生成 | `CompetitionService`、`PublicController` |
   | Flyway | 数据库向前迁移 | `db/migration/V*.sql` |
   | MySQL Connector | JDBC 运行时驱动 | `pom.xml` 的 `runtime` 依赖 |

   “Validation 是问 AI 得到的”不能作为最终学习结果。最低要求是能指出一组完整关系：
   `RegisterDto.email` 上的 `@Email` → `AuthController.register(@RequestBody @Valid RegisterDto)` →
   校验失败 → `GlobalExceptionHandler` 返回 400。
5. `common`、`config`、`modules` 分别放什么？
   `common` 放跨业务复用的返回、异常、安全、缓存、工具和注解；`config` 放全局 Bean 和基础设施配置；
   `modules` 放具体业务域。
6. `sys`、`biz`、`resume` 分别是什么？
   `sys` 是当前较大的系统/组织治理域，包含用户、部门、资源、投票、贡献、轮播图、公开配置和导出；
   `biz` 是竞赛、竞赛编辑授权和 Git 能力；`resume` 是简历保存、提交、审核和状态流转。
7. `CsaOfficialApplication.java` 中三个启动注解各自做什么？
   `@SpringBootApplication` 组合启动配置、自动配置和组件扫描；
   `@MapperScan("com.csa.official.modules.*.mapper")` 扫描 `sys`、`biz`、`resume` 三个模块的 Mapper；
   `@EnableScheduling` 开启 `@Scheduled` 定时任务，例如 `ContributionTask.settleIntroContribution()`。

产出：

```text
项目结构图
核心依赖表   Spring Boot + Spring Security + MyBatis-Plus + JJWT + Redis + Next.js + React + Axios
启动配置表
Next.js 前端 + Spring Boot 后端 + MySQL 数据库
+ Redis 缓存 + QQ 邮箱 + 本地文件存储
+ JWT/Spring Security 安全体系
```

### Day 1 当前验收状态

| 内容 | 当前状态 | 下一步 |
| --- | --- | --- |
| 从 `pom.xml` / `package.json` 查版本 | 独立掌握 | 继续保持“文件 + 字段”表达 |
| 找前端页面和模块目录 | 独立掌握 | 进入请求链路 |
| 解释 Validation、JWT、Mapper 的用途 | 经提示掌握 | 每个依赖都补一条源码证据 |
| 解释启动注解 | 经提示掌握 | 不再只说“这是 Spring Boot 注释” |
| 解释 `sys` 的真实边界 | 经纠正掌握 | 不要把 `sys` 简化成“用户模块” |

#### Day 1 补强材料：旧验收中新增、但原阅读清单没有覆盖的内容

下面这些问题曾被拿来追加验收，但它们不在最初 Day 1 的六个文件里。以后不能先问一个学习材料未覆盖的问题，再据此判定 Day 1 不合格。处理方式是：Day 1 核心目标保持“建立项目全景”，下面内容作为补强，并明确给出阅读入口。

```text
csa-official-backend/mvnw.cmd
csa-official-backend/.mvn/wrapper/maven-wrapper.properties
csa-official-backend/.env.example
csa-official-backend/src/main/resources/application.yml
csa-official-backend/src/main/java/com/csa/official/common/cache/KeyValueStore.java
csa-official-backend/src/main/java/com/csa/official/common/cache/RedisKeyValueStore.java
csa-official-backend/src/main/java/com/csa/official/common/cache/MemoryKeyValueStore.java
csa-official-backend/src/main/java/com/csa/official/config/SecurityConfig.java
csa-official-backend/src/main/java/com/csa/official/config/JwtAuthenticationFilter.java
任选一个 modules/*/mapper/*Mapper.java
```

补强后要能回答：

1. **Maven Wrapper 是什么？**
   `mvnw` / `mvnw.cmd` 会读取 `.mvn/wrapper/maven-wrapper.properties` 中锁定的 Maven 发行版；本机没有全局 Maven 时也能下载并运行项目指定版本。仍然需要安装符合要求的 JDK。
2. **`pom.xml` 的 Spring Boot parent 不只是“写版本号”还做什么？**
   它提供 Spring Boot 的依赖版本管理、插件默认配置和常用构建约定，因此很多 starter 不需要逐个写版本。
3. **`${DB_URL}` 和 `${SERVER_PORT:8080}` 是什么？**
   前者要求从环境变量/`.env` 提供；后者表示优先读 `SERVER_PORT`，未提供时使用 `8080`。这样代码和秘密、不同环境配置分离。
4. **为什么 `.env.example` 是 `CSA_CACHE_TYPE=memory`，`application.yml` 默认是 `redis`？**
   `.env.example` 是本地开发建议，复制成 `.env` 后覆盖默认值，让本地不必安装 Redis；如果没有 `.env` 或显式环境变量，`application.yml` 会走 Redis。生产 profile 禁止使用 memory，因为多实例限流、验证码和吊销状态必须共享。
5. **Spring Security、JJWT、CSRF 各负责什么？**
   Spring Security 组织认证/授权和过滤器链；JJWT 负责 Token 的签名、解析和校验，不是“解密密码”；CSRF 防护校验 Cookie 认证写请求是否来自持有 CSRF Token 的合法页面。
6. **MyBatis-Plus、MySQL、Mapper 的关系是什么？**
   MySQL 保存数据；MyBatis-Plus 是 Java 数据访问框架；Mapper 是 Java 层的数据访问接口，继承 `BaseMapper<T>` 后获得 CRUD，Service 调用 Mapper 最终执行 SQL。
7. **Redis/内存缓存主要存什么？**
   至少包括验证码、限流计数、JWT 吊销标记；项目还用 Spring Cache 保存 `auth_user`、部门、公开介绍、贡献墙和轮播图等读缓存，并用缓存实现定时任务锁。
8. **Spring Security 为什么在 Controller 前？**
   请求先经过 Security Filter Chain；未认证或不满足 URL 规则时可在进入 MVC 前结束。方法级 `@PreAuthorize` 则在 Controller 方法调用前由 Spring AOP 做授权，失败时方法体也不会执行。核心不是单纯“节省资源”，而是建立统一的安全边界。

这些补强项中，Mapper 会在 Day 2 深入，Security/JWT/缓存会在 Day 4-6 深入。Day 1 只需能说明它们在全局架构中的位置，不要求提前背完内部实现。

### Day 2：请求链路

目标：追一个接口从前端到数据库。

建议追资源列表：

```text
/dashboard/resources
→ csa-official-frontend/src/app/dashboard/resources/page.tsx
→ DashboardResourcesPage
→ csa-official-frontend/src/components/business/resources/ResourceLibrary.tsx
→ useEffect
→ resourceService.list({ page, size: 8, category })
→ src/lib/axios.ts request interceptor
→ GET /api/sys/resource/list?page=x&size=8&category=...
→ SecurityConfig filter chain
→ JwtAuthenticationFilter
→ SecurityContextHolder
→ @PreAuthorize("hasRole('LEVEL_1')")
→ ResourceController.list
→ ResourceService.listResources
→ PageUtils.of(page, size)
→ ResourceMapper.selectPage
→ MySQL
→ Page<Resource>
→ ResourceVO.from
→ Page<ResourceVO>
→ R.ok
→ Axios response interceptor
→ setItems / setPages / setTotal
→ ResourceLibrary 重新渲染
```

你要能回答：

1. `/dashboard/resources` 对应哪个文件？渲染哪个组件？
   `src/app/dashboard/resources/page.tsx` 只返回 `ResourceLibrary variant="dashboard"`；真正的请求和页面状态在
   `src/components/business/resources/ResourceLibrary.tsx`。
2. 哪个 service 封装资源接口？
   `src/services/resource.ts`：

   | 方法 | HTTP | 地址 | 用途 |
   | --- | --- | --- | --- |
   | `list` | GET | `/api/sys/resource/list` | 分页、分类查询 |
   | `listCategories` | GET | `/api/sys/resource/categories` | 获取分类 |
   | `save` | POST | `/api/sys/resource/save` | 新增/更新资源 |
   | `remove` | POST | `/api/sys/resource/delete?id=...` | 删除资源 |
   | `trackDownload` | POST | `/api/sys/resource/download?id=...` | 下载计数 |

3. 后端方法签名是什么？
   `ResourceController.list()`，路径是 `GET /api/sys/resource/list`，返回 `R<Page<ResourceVO>>`。
4. Controller、Service、Mapper 各做什么？
   Controller 接收参数并做 `@PreAuthorize`；Service 调用 `PageUtils`、组装查询条件、调用 Mapper 并把 Entity 转成 VO；
   Mapper 继承 `BaseMapper<Resource>`，由 `selectPage(pageParam, query)` 执行数据库分页查询。
5. 分页、分类和排序分别在哪里设置？
   `ResourceService.listResources()` 先调用 `PageUtils.of(page, size)`；有分类时执行
   `query.eq(Resource::getCategory, category.trim())`；无论是否分类，都执行
   `query.orderByDesc(Resource::getCreateTime)`；最终由 `resourceMapper.selectPage(pageParam, query)` 发出 SQL。
6. `PageUtils.of()` 收敛什么？
   `page == null` 或小于 1 时取 1；`size == null` 时取 10；`size` 最终限制在 1 到 100。
   它不知道真实总页数，因此前端发现请求页超出结果页数时，会重新请求最后一页。
   注意参数名必须是 `size`：请求写成 `/api/sys/resource/list?page=2&siz=10&category=java` 时，
   未知的 `siz` 不会绑定到 `size`，后端实际按 `size=null` 处理并使用默认 10；`page=2` 和 `category=java` 仍会生效。
7. 为什么返回 `ResourceVO` 而不是 `Resource`？
   Mapper 先返回 `Page<Resource>`；Service 用 `ResourceVO.from` 只复制接口需要的字段，避免把持久层字段（例如逻辑删除标记）直接暴露为 API 契约。
8. `api.get<PageResult<ResourceItem>, PageResult<ResourceItem>>` 两个泛型是什么？
   按 Axios 类型签名，第一个泛型描述 `AxiosResponse<T>.data` 的编译期类型，第二个泛型描述
   `await api.get(...)` 最终返回的类型。当前项目把第一个也写成 `PageResult<ResourceItem>`，
   是因为响应拦截器把后端 `R<Page<ResourceVO>>` 的业务数据适配成了页面数据；这是一个类型约定，
   并不代表网络上真的直接返回了 `PageResult`。
   第二个泛型让 `resourceService.list()` 的调用方直接得到 `PageResult<ResourceItem>`，而不是完整 `AxiosResponse`。
   两个泛型都只给 TypeScript 编译器看，不会在运行时执行拆包。
9. Axios 运行时到底取了哪两层 `data`？
   `response.data` 是 AxiosResponse 的传输层字段，里面装着后端的 `R<T>`；随后拦截器读取 `payload.data`，取出 `R<T>` 的业务数据并返回。
   因此可以把它理解成“去掉 Axios 外壳，再去掉 R 外壳”，但不是泛型在拆包。

产出：

```text
资源列表请求链路图（必须能解释每一箭头）
页面 → 组件 → useEffect → service → Axios → Security Filter Chain → JWT/权限 → Controller
→ Service → PageUtils/查询条件 → Mapper.selectPage → MySQL → Entity Page
→ VO Page → R<T> → Axios response interceptor → React State → 页面
```

### Day 2 当前验收状态

已经能追通主链路；还要独立补齐三点：

1. `useEffect` 是依赖 `page`、`activeCategory`、`canViewResources` 等状态变化触发 `loadResources`，不是 `useMemo` 发请求。
2. `JwtAuthenticationFilter` 只负责解析/校验 Token 并在当前请求的 `SecurityContext` 写入 `Authentication`；无 Token 时最终由 `JwtAuthenticationEntryPoint` 返回 401，已登录但权限不足时由 `JwtAccessDeniedHandler` 返回 403。
3. `setItems`、`setPages`、`setTotal` 更新 React State，React 随后重新渲染；`items` 驱动资源卡片，`pages` 驱动分页控件，`total` 显示总数。

### Day 3：统一返回和异常

目标：不只记住“返回 400/401/403”，而是能从一次请求判断：请求在哪一层失败、抛了什么异常、哪个组件负责写响应，以及 Controller 方法体是否执行。

读：

```text
common/result/R.java
common/exception/GlobalExceptionHandler.java
common/exception/CsaException.java
common/exception/ApiErrorCode.java
common/security/SecurityResponseWriter.java
common/security/JwtAuthenticationEntryPoint.java
common/security/JwtAccessDeniedHandler.java
config/JwtAuthenticationFilter.java
config/SecurityConfig.java
modules/sys/controller/ResourceController.java
modules/sys/controller/AuthController.java
```

#### 3.1 `R<T>` 的字段和成功响应

`common/result/R.java` 当前有五个字段：

| 字段 | 类型 | 含义 |
| --- | --- | --- |
| `code` | `Integer` | HTTP/业务结果码，成功为 200 |
| `message` | `String` | 给调用方看的结果说明 |
| `data` | `T` | 成功数据或错误详情；错误响应通常为 `null` |
| `errorCode` | `String` | 稳定的机器可读错误分类 |
| `traceId` | `String` | 关联请求日志的请求 ID |

成功路径由 `R.ok(data)` 创建：

```java
code = 200;
message = "Success";
data = data;
```

成功响应不需要 `errorCode`；`traceId` 由请求链路生成，`R.ok` 本身不主动填它。错误响应通过 `R.fail(...)` 填充 `code`、`message`、`errorCode` 和当前 `traceId`。

#### 3.2 异常分类和真实处理方法

| 触发场景 | 实际异常 | `GlobalExceptionHandler` 方法 | HTTP/code | `errorCode` |
| --- | --- | --- | --- | --- |
| `@RequestParam String email` 缺失 | `MissingServletRequestParameterException` | `handleMissingServletRequestParameterException` | 400 | `MISSING_PARAMETER` |
| `@RequestParam Integer page` 收到 `abc` | `MethodArgumentTypeMismatchException` | `handleMethodArgumentTypeMismatchException` | 400 | `TYPE_MISMATCH` |
| JSON 少括号、请求体无法读取 | `HttpMessageNotReadableException` | `handleHttpMessageNotReadableException` | 400 | `MALFORMED_REQUEST` |
| `@RequestBody @Valid` 字段不符合约束 | `MethodArgumentNotValidException`（是 `BindException` 的子类） | `handleBindingException` | 400 | `VALIDATION_FAILED` |
| Service 主动拒绝业务规则 | `CsaException` | `handleCsaException` | 由异常决定 | 异常携带的值 |
| 未处理的运行时错误 | `RuntimeException` | `handleRuntimeException` | 500 | `INTERNAL_ERROR` |

`ApiErrorCode` 只是错误码枚举和 HTTP 状态映射，不是异常处理器。真正统一接住 MVC 异常的是 `GlobalExceptionHandler`（`@RestControllerAdvice`）。

#### 3.3 `CsaException` 怎么用

最简单的构造方式：

```java
throw new CsaException("邀请码无效");
```

默认得到 `400 + BUSINESS_RULE_VIOLATION`。需要指定状态或错误分类时，使用带 `HttpStatus`/`ApiErrorCode` 的构造器，例如资源不存在可以抛 404。异常冒泡到 `handleCsaException`，Controller 不需要自己 try-catch。

#### 3.4 参数绑定、JSON 解析和字段校验的时间顺序

这三类错误不能混为一谈：

```text
请求进入 DispatcherServlet
→ 先绑定 URL/query/path 参数或读取 JSON body
→ 绑定失败：Controller 方法体不会执行
→ 绑定成功后，如果参数有 @Valid，再执行 Bean Validation
→ 校验失败：Controller 方法体仍不会执行
→ 全部通过后，才进入 Controller 方法正文
```

真实例子：

```java
// AuthController.java
public R<String> sendCode(@RequestParam String email)

// ResourceController.java
public R<String> save(@RequestBody @Valid SaveResourceDto dto)
```

- `/api/auth/send-code` 不带 `email`：不是 401，而是 `MissingServletRequestParameterException`。
- `/api/public/competitions?page=abc`：public 接口允许匿名访问，先发生 `MethodArgumentTypeMismatchException`，不会进入方法体。
- `POST /api/auth/login` 发送 `{"username":"alice",`：这是当前测试中的公开认证入口，JSON 语法不完整，发生 `HttpMessageNotReadableException`；不要用需要登录/CSRF 的资源保存接口做这个最小实验。
- 发送合法 JSON `{"title":"","fileUrl":"https://example.com/a.pdf"}`：JSON 能解析，但 `title` 上的 `@NotBlank` 失败，产生 `MethodArgumentNotValidException`，由 `handleBindingException` 接住。

注意：`SaveResourceDto` 是 DTO，不是发现错误的东西；真正定义规则的是字段上的 `@NotBlank`、`@Size` 等注解，`@Valid` 负责启动校验。

#### 3.5 401、403 的过滤器链路

```text
没有 JWT / JWT 无效
→ JwtAuthenticationFilter 尝试解析，失败时清空 SecurityContext 并继续链路
→ Spring Security 发现受保护接口没有 Authentication
→ JwtAuthenticationEntryPoint.commence()
→ SecurityResponseWriter
→ 401 + AUTHENTICATION_REQUIRED
```

```text
JWT 有效，Filter 已将 Authentication 写入 SecurityContext
→ @PreAuthorize 或 URL 权限检查发现角色不足
→ 方法体不执行
→ JwtAccessDeniedHandler.handle()
→ SecurityResponseWriter
→ 403 + ACCESS_DENIED
```

`JwtAuthenticationFilter` 负责取 Token、校验 Token 和写入当前请求的 `SecurityContext`，不是最终统一写 401 JSON 的组件。`JwtAuthenticationEntryPoint` 处理“未认证”，`JwtAccessDeniedHandler` 处理“已认证但无权”。

当前源码里的权限测试接口是：

```text
GET /api/sys/user/admin-test  → SysUserController.onlyForRoot() → LEVEL_99
GET /api/sys/user/member-test → SysUserController.onlyForMember() → LEVEL_1
```

不要把它写成 `ADMIN`；这是最近验收中发现的题目口径漂移。

#### 3.6 为什么不在每个 Controller 里 try-catch

统一异常处理有四个实际收益：

1. Controller 保持“接参 → 调 Service → 返回”的主线，不重复写格式化代码。
2. 同一类错误的 HTTP 状态、`errorCode`、message 结构保持一致。
3. 日志和 `traceId` 可以集中处理，排错时能从响应追到服务端日志。
4. 测试可以针对异常类型和 JSON 契约写一次，而不是给每个 Controller 重复测试。

只有在当前方法能恢复、降级或把底层异常转换成更有业务意义的异常时，才适合局部捕获；不能为了“所有代码都 try-catch”而捕获后吞掉异常。

#### 3.7 当前验收题（先作答，再核对判断点）

| 题目 | 必须说出的判断点 |
| --- | --- |
| `GET /api/public/competitions?page=abc` | public 放行；`MethodArgumentTypeMismatchException`；方法体不执行；`TYPE_MISMATCH` / 400 |
| `POST /api/auth/send-code` 不带 `email` | `MissingServletRequestParameterException`；方法体不执行；`MISSING_PARAMETER` / 400 |
| `POST /api/auth/login` 发送不完整 JSON | `HttpMessageNotReadableException`；方法体不执行；`MALFORMED_REQUEST` / 400 |
| 已通过认证、授权和 CSRF 后，`POST /api/sys/resource/save` 发送合法 JSON 但 `title=""` | `@NotBlank` 失败；`MethodArgumentNotValidException` 被 `handleBindingException` 接住；方法体不执行；`VALIDATION_FAILED` / 400 |
| Service 执行 `throw new CsaException("邀请码无效")` | Controller 不捕获；`GlobalExceptionHandler.handleCsaException` 处理；默认 `BUSINESS_RULE_VIOLATION` / 400 |
| 受保护接口无 JWT | Filter 不建立认证；`JwtAuthenticationEntryPoint` 写 JSON；401 / `AUTHENTICATION_REQUIRED` |
| JWT 有效但缺少所需角色 | 认证成功但授权失败；方法体不执行；`JwtAccessDeniedHandler` 写 JSON；403 / `ACCESS_DENIED` |

一个容易机械背错的细节：`new CsaException(400, "邮箱格式不正确")` 会根据 HTTP 状态得到
`errorCode=BAD_REQUEST`；只有 `new CsaException("...")` 这个构造方式默认是
`BUSINESS_RULE_VIOLATION`。所以看到 `CsaException` 后必须继续看它调用了哪个构造器。

资源保存校验题默认使用有效 JWT、有效 CSRF Token，并满足 `LEVEL_3`。没有有效认证时，Filter Chain 会先返回 401；CSRF 校验失败时，自定义 Filter 会先返回 403，这两种情况都到不了 MVC 参数解析。要注意：`@PreAuthorize` 是方法级鉴权，MVC 会先解析参数并执行 `@Valid`，再尝试调用代理后的 Controller 方法。因此“已登录但角色不足 + DTO 也无效”时不能凭印象断言一定先返回 403；出题和测试应一次只改变一个条件。

练习：

```text
1. POST /api/auth/send-code，不带 email：确认 400 / MISSING_PARAMETER。
2. GET /api/public/competitions?page=abc：确认 400 / TYPE_MISMATCH，方法体不执行。
3. POST /api/auth/login，发送缺少右括号的 JSON：确认 400 / MALFORMED_REQUEST。
4. POST /api/auth/register，发送合法 JSON 但 username 为空：确认 400 / VALIDATION_FAILED。
5. 不带 JWT 访问受保护接口：确认 401 / AUTHENTICATION_REQUIRED。
6. 带有效 JWT 但角色不足访问 `/api/sys/user/admin-test`：确认 403 / ACCESS_DENIED。
7. 对资源列表故意把 `size` 写成 `siz`：确认请求不会报类型错，但每页条数会回到默认 10。
```

每个练习都要记录五项：请求、实际异常、处理方法、HTTP/code/errorCode、Controller 方法体是否执行。只写“返回 400”不算完成。

#### 最近手工验收留下的真实证据

这些结果来自 2026-08-22 的 Thunder Client/浏览器请求，保留在计划里，防止以后只剩“我记得返回过错误”：

```text
GET http://localhost:8080/api/sys/user/admin-test
→ HTTP 403
→ {"code":403,"message":"无权访问","data":null,"errorCode":"ACCESS_DENIED","traceId":"..."}

GET http://localhost:8080/api/public/competitions?page=abc
→ HTTP 400
→ {"code":400,"message":"参数类型不正确: page","data":null,"errorCode":"TYPE_MISMATCH","traceId":"..."}

POST http://localhost:8080/api/auth/send-code（不带 email）
→ HTTP 400
→ {"code":400,"message":"缺少必填参数: email","data":null,"errorCode":"MISSING_PARAMETER","traceId":"..."}
```

截图中的 `traceId` 每次请求都可能不同；它不是业务结果的一部分，而是用来关联服务端日志的请求标识。

### Day 3 当前验收状态（2026-08-26）

最近问答已经覆盖了统一返回、参数绑定、Bean Validation、业务异常和 401/403。当前结论分层如下：

- **已能复述**：`R<T>` 五个字段、成功 `R.ok`、常见错误码、参数错误和业务错误不是一回事。
- **经纠正掌握**：`ApiErrorCode` 不是处理器；没有 JWT 最终是 `JwtAuthenticationEntryPoint` 写 401；低权限是 `JwtAccessDeniedHandler` 写 403；`@RequestBody` JSON 解析失败和字段校验失败是两条不同路径。
- **必须再独立验证**：能否不看提示，准确说出异常类、处理方法、错误码以及 Controller 是否执行。

最近一次“合法 JSON，但 `SaveResourceDto.title` 为空”的作答记录：

| 判断点 | 本次回答 | 判定 |
| --- | --- | --- |
| JSON 能否解析 | 可以 | 正确；JSON 语法合法，问题发生在后续字段校验 |
| `save()` 方法体是否执行 | 不会 | 正确；参数解析后、方法调用前的 Bean Validation 已失败 |
| 哪个校验注解发现问题 | `SaveResourceDto` | 错误；`SaveResourceDto` 是承载参数的 DTO，字段上的 `@NotBlank` 定义规则，参数上的 `@Valid` 启动校验 |
| 抛出什么异常、由哪个方法处理 | `BindException` | 部分正确；实际抛出 `MethodArgumentNotValidException`，因为它继承 `BindException`，所以被 `@ExceptionHandler(BindException.class)` 对应的 `handleBindingException` 接住 |
| `errorCode` | `VALIDATION_FAILED` | 正确 |

这次作答证明已经能判断失败阶段和最终错误码，但“规则注解 → 实际异常 → handler 方法”仍未完全分开，因此 Day 3 继续保持“验收中”。

Day 3 只有在上面 7 个练习能连续答对，且每题能指出源码文件/方法，才标记为完成。

### Day 4–14 的自学使用规则

Day 1–3 是已学习背景。从 Day 4 开始，本指南不再把一串类名当作教材，而是严格按“概念 → 文件 → 调用关系 → 运行验证 → 自测”编排。当天自测中出现的名词、注解、类名和方法名，必须已经在当天材料里解释过。

每个名称首次出现时至少要回答四件事：

| 要说明的内容 | 具体含义 |
| --- | --- |
| 它是什么 | 先分清它是文件、类、接口、注解、方法、字段、配置项还是框架概念 |
| 它负责什么 | 只说它自己的职责，不把整条链路都算到一个类头上 |
| 谁会触发或调用它 | 说明入口来自浏览器、Spring Filter Chain、Controller、Service 还是框架代理 |
| 它接着调用谁 | 说明下一跳，并明确它不负责的边界 |

每天都按下面顺序学习：

1. 先读“今日目标”和“前置名词”，不知道名词时不要直接钻进方法体。
2. 按“文件和类职责表”的顺序打开文件，不要只在项目中随机搜索类名。
3. 按“精确阅读顺序”追同一条链路，每走一步都记下调用者、被调用者和数据形态。
4. 完成“动手验证”，保留浏览器 Network、响应 JSON、日志或测试汇总作为证据。
5. 最后再做自测。自测不是背标准答案，而是要求能重新定位到源码。

完成某一天至少要同时满足：

- 能定位：不看答案也能找到文件、类和关键方法。
- 能解释：能说清每一跳为什么存在，而不只是复述类名。
- 能区分：能分清容易混淆的两个概念，例如认证与授权、Entity 与 VO、前端隐藏与后端鉴权。
- 能验证：至少亲手跑过一个请求、页面操作或测试，并记录实际结果。

下面所有路径都相对于项目根目录 `D:\CSA-Project`。

### Day 4：登录、会话恢复和退出链路

#### 今日目标

学完后，要能独立说明一次登录如何从 React 表单进入 Spring Security，密码在哪里比较，JWT 在哪里生成和保存，刷新页面后如何确认会话仍有效，以及退出时为什么既要清 Cookie 又要吊销 Token。

#### 前置名词

| 名词 | 解释 |
| --- | --- |
| 认证 Authentication | 判断“当前用户是谁”。登录时校验用户名密码，后续请求通过 JWT 恢复当前用户身份。 |
| 授权 Authorization | 判断“已经知道是谁以后，他能做什么”。角色权限放到 Day 6 学；不要把登录成功等同于拥有所有权限。 |
| 凭据 Credentials | 用来证明身份的数据。登录时是用户名和密码；后续浏览器请求主要使用 JWT Cookie。 |
| `Authentication` | Spring Security 表示“本次认证结果”的接口。里面可放当前主体、权限和请求细节。它不是数据库 `User` 实体。 |
| `SecurityContext` | 保存当前请求 `Authentication` 的容器。默认与当前线程关联，请求结束后不会把内容永久写回用户表。 |
| `UserDetails` | Spring Security 读取账号所需的统一接口，提供用户名、密码摘要、权限和账号是否可用等信息。 |
| `PasswordEncoder` | 密码摘要比较接口。本项目的实现是 `BCryptPasswordEncoder`；登录时比较用户输入与数据库中的 BCrypt 摘要。 |
| JWT | JSON Web Token。它是带签名的身份声明，不是用户密码加密工具，也不应存明文密码。 |
| claim | JWT 负载中的一项声明。本项目写入 `roleLevel`、`userId`、`sessionVersion`，另有 subject、jti、签发时间和过期时间。 |
| 签名 | 服务端用密钥对 JWT 头和负载生成的完整性证明。签名能发现篡改，但不会把负载自动变成秘密。 |
| Cookie | 浏览器按域名和规则自动保存、携带的小段数据。项目用 `CSA_AUTH_TOKEN` 保存 JWT。 |
| `HttpOnly` | Cookie 属性。设为 true 后前端 JavaScript 不能读取该 Cookie，可降低 XSS 直接偷取 JWT 的风险。 |
| Zustand | 前端轻量状态库。这里只保存渲染页面需要的 `username` 和 `roleLevel`，不保存认证 JWT。 |
| `localStorage` | 浏览器可被 JavaScript 读取的持久化存储。项目只用它持久化 Zustand 的 user 状态，并主动删除旧版 token。 |
| 缓存 | 把常用查询结果临时保存，减少重复访问数据库。`auth_user` 缓存的是登录查询到的 `User`，不是永久真相；账号修改后必须清缓存。 |
| Token 吊销 | JWT 尚未自然过期时，服务端额外记录“这个 Token 不再可用”。退出登录会把当前 Token 标记到剩余有效期结束。 |
| `sessionVersion` | 用户表和 JWT 里共同保存的会话版本。修改密码或停用账号时可提高数据库版本，使旧 Token 即使签名正确也失效。 |

#### 文件、类和方法职责

| 路径或框架对象 | 它是什么 | 负责什么 | 调用关系 |
| --- | --- | --- | --- |
| `csa-official-frontend/src/components/business/auth/LoginForm.tsx` | React 客户端组件 `LoginForm` | 收集用户名密码；`onSubmit` 调登录 service；成功后更新前端状态并跳转 | 用户提交表单 → `LoginForm.onSubmit` → `authService.login` |
| `csa-official-frontend/src/services/auth.ts` | 前端认证 service 对象 `authService` | 统一封装 login、logout、register 等接口；把后端 `roleLevel` 字符串转成 number；记住 CSRF Token | `LoginForm` 调它；它再调 Axios 实例 `api` |
| `csa-official-frontend/src/lib/axios.ts` | Axios 实例和拦截器 | 配置 `baseURL`、Cookie 携带、CSRF Header、统一拆 `R<T>`、401 跳转 | 所有前端 service 调 `api`；`api` 再发 HTTP 请求 |
| `csa-official-frontend/src/store/useAuthStore.ts` | Zustand store hook | 持久化 user 视图状态；`setLogin`、`setUser`、`logout` 修改状态 | `LoginForm`、`DashboardGuard` 和 Axios 401 处理会调用 |
| `csa-official-frontend/src/components/layout/DashboardGuard.tsx` | 工作台客户端守卫组件 | 进入工作台后调用用户信息接口验证真实会话；失败则清状态并跳登录 | dashboard layout 渲染它；它调用 `userService.getInfo` |
| `csa-official-frontend/src/services/user.ts` | 用户 service 对象 `userService` | `getInfo` 封装 `GET /api/sys/user/info` | `DashboardGuard` 调它；后端会再次校验 JWT |
| `csa-official-frontend/src/proxy.ts` | Next.js 服务端请求代理函数 `proxy` | 在页面进入 `/dashboard` 前检查认证 Cookie 是否存在，并设置安全响应头 | Next.js 在匹配页面请求时触发；它不能验证 JWT 签名或过期时间 |
| `csa-official-backend/src/main/java/com/csa/official/modules/sys/dto/LoginDto.java` | 登录 DTO 类 | 用 `username`、`password` 承载 JSON；字段上的 `@NotBlank` 定义不能为空 | Spring MVC 把请求体转成它；`AuthController.login` 接收它 |
| `csa-official-backend/src/main/java/com/csa/official/modules/sys/controller/AuthController.java` | REST Controller 类 | 接收 `/api/auth` 请求；`login` 组织认证、JWT/CSRF 生成和 Cookie 下发；`logout` 组织吊销与清 Cookie | HTTP 请求进入它；它调用 `AuthenticationManager`、`JwtUtils` 等 |
| `AuthenticationManager` | Spring Security 框架接口，由 `SecurityConfig` 暴露为 Bean | 接收未认证的 `UsernamePasswordAuthenticationToken`，调认证提供者完成用户名密码认证 | `AuthController.login` 调 `authenticate`；框架再找 `UserDetailsService` 和 `PasswordEncoder` |
| `UsernamePasswordAuthenticationToken` | Spring Security 的 `Authentication` 实现 | 登录前承载用户名密码；认证后也可承载已认证主体和权限 | `login` 创建未认证对象；`JwtAuthenticationFilter` 为后续请求创建已认证对象 |
| `csa-official-backend/src/main/java/com/csa/official/modules/sys/service/UserDetailsServiceImpl.java` | `UserDetailsService` 的项目实现 | `loadUserByUsername` 根据用户名取 `User`，并包装成 `LoginUser` | `AuthenticationManager` 和 JWT Filter 会调用 |
| `csa-official-backend/src/main/java/com/csa/official/modules/sys/service/UserAccountCacheService.java` | 账号查询缓存 Service | `findByUsername` 先查 `auth_user` 缓存，未命中才调 `UserMapper`；`evict`/`evictAll` 清缓存 | `UserDetailsServiceImpl` 调它；它再调 `UserMapper` |
| `csa-official-backend/src/main/java/com/csa/official/common/security/LoginUser.java` | `UserDetails` 实现类 | 把项目 `User` 适配给 Spring Security；提供密码摘要、账号可用状态和角色权限 | `UserDetailsServiceImpl` 创建；认证框架和 JWT Filter 使用 |
| `csa-official-backend/src/main/java/com/csa/official/common/util/JwtUtils.java` | JWT 工具组件 | `generateToken` 生成签名 JWT；解析 subject、userId、jti、sessionVersion 和过期时间；启动时检查 secret 至少 32 字节 | `AuthController` 生成；Filter 和吊销 Service 解析 |
| `csa-official-backend/src/main/java/com/csa/official/common/security/AuthCookieService.java` | Cookie 生命周期 Service | `issueAuth` 写 HttpOnly 认证 Cookie；`issueCsrf` 写非 HttpOnly CSRF Cookie；`clear` 清两者；`resolveToken` 可从 Bearer 或 Cookie 取 Token | `AuthController` 的 login/logout 调用 |
| `csa-official-backend/src/main/java/com/csa/official/common/security/CsrfTokenService.java` | CSRF Token 组件 | 生成 32 字节随机 Token；Day 5 再学习比较逻辑 | `AuthController.login` 和 csrf 接口调用 |
| `csa-official-backend/src/main/java/com/csa/official/common/security/JwtRevocationService.java` | JWT 吊销 Service | `revoke` 按 Token 的 jti 写吊销标记，TTL 等于剩余有效期；`isRevoked` 查询是否已吊销 | logout 调 `revoke`；`JwtAuthenticationFilter` 调 `isRevoked` |
| `csa-official-backend/src/main/java/com/csa/official/config/JwtAuthenticationFilter.java` | 每请求一次的认证 Filter | 后续请求先取 Bearer，取不到再读 `CSA_AUTH_TOKEN`；校验签名、过期、吊销、账号状态和 sessionVersion，成功后写 `SecurityContext` | `SecurityFilterChain` 自动触发；再继续后续 Filter 和 Controller |
| `csa-official-backend/src/main/java/com/csa/official/config/SecurityConfig.java` | Spring Security 配置类 | 创建 `AuthenticationManager`、`PasswordEncoder` 和 `SecurityFilterChain`，并把 JWT/CSRF Filter 放到链中 | 应用启动时读取；不是某个 Controller 手工调用 |
| `csa-official-backend/src/main/java/com/csa/official/modules/sys/service/MailService.java` | 验证码业务 Service | 同步生成验证码、写缓存、写 PENDING 投递记录，再调用异步发信组件 | `AuthController.sendCode` 调它 |
| `csa-official-backend/src/main/java/com/csa/official/modules/sys/service/AsyncMailSender.java` | 独立异步邮件组件 | 在 `mailTaskExecutor` 线程池中调用 SMTP，更新投递状态并重试；全部失败时删除验证码和限流键 | `MailService` 从另一个 Bean 调它，保证 `@Async` 代理生效 |

#### 精确阅读顺序

1. 打开 `LoginForm.tsx`，只读 `onSubmit`：确认表单数据传给 `authService.login`，成功结果传给 `setLogin`，然后跳转。
2. 打开 `auth.ts` 的 `login`：确认请求是 `POST /api/auth/login`；响应中的 `csrfToken` 交给 `rememberCsrfToken`；返回给组件的只有 `username` 和数值 `roleLevel`。
3. 打开 `axios.ts` 的 `axios.create`：确认 `baseURL`、`timeout` 和 `withCredentials`；再读响应拦截器，确认成功时返回 `payload.data`，而不是完整 `AxiosResponse`。
4. 打开 `LoginDto`：分清 DTO 是参数对象，`@NotBlank` 是字段规则，`AuthController` 参数上的 `@Valid` 才触发 Bean Validation。
5. 打开 `AuthController.login`：从 `AccountNormalizer.username` 开始，追到 `authenticationManager.authenticate`。
6. 顺着框架认证关系读 `UserDetailsServiceImpl.loadUserByUsername` → `UserAccountCacheService.findByUsername` → `UserMapper.selectOne`。
7. 读 `LoginUser.getPassword`、`getAuthorities` 和 `isEnabled`；`PasswordEncoder` 读取数据库 BCrypt 摘要完成比较。不要在 Controller 中寻找手写 `equals`。
8. 回到 `AuthController.login`：认证成功后把 `Authentication` 放入 `SecurityContext`，再读取 `User`，更新最后登录时间。
9. 读 `JwtUtils.generateToken`：逐个找出 `roleLevel`、`userId`、`sessionVersion`、jti、subject、issuedAt、expiration 和签名。
10. 读 `AuthCookieService.issueAuth` 与 `issueCsrf`：确认认证 Cookie 的 `httpOnly=true`，CSRF Cookie 的 `httpOnly=false`。
11. 回到 login 响应：确认响应体没有 `jwt`/`token` 字段，只有 `csrfToken`、`username`、`roleLevel`。
12. 读 `useAuthStore`：确认 `partialize` 只持久化 user；`setLogin` 和 `logout` 都会删除旧 `localStorage.token`。
13. 读 `proxy.ts` 与 `DashboardGuard`：前者只做 Cookie 存在性预检；后者真正调用 `GET /api/sys/user/info`，由后端 JWT Filter 验证会话。
14. 读 `JwtAuthenticationFilter.doFilterInternal`：确认它写的是 `SecurityContext`，不是 `User` 实体；无效 Token 通常清上下文并继续，后续受保护接口再由 EntryPoint 返回 401。
15. 最后读 logout：`AuthCookieService.resolveToken` → `JwtRevocationService.revoke` → `AuthCookieService.clear`；前端 `finally` 中 `clearCsrfToken`，并清 Zustand user。
16. 登录主链读完后，再把 `sendCode` 当扩展链阅读：`MailService` 同步保存短期状态，`AsyncMailSender` 异步执行慢速 SMTP。

#### 完整链路

```text
LoginForm.onSubmit
→ authService.login
→ Axios POST /api/auth/login（withCredentials=true）
→ LoginDto + @Valid + @NotBlank
→ AuthController.login
→ AuthenticationManager.authenticate
→ UserDetailsServiceImpl.loadUserByUsername
→ UserAccountCacheService.findByUsername
→ UserMapper.selectOne / auth_user 缓存
→ LoginUser 提供密码摘要、账号状态和 authorities
→ PasswordEncoder 比较 BCrypt
→ 返回已认证 Authentication
→ SecurityContextHolder.setAuthentication
→ JwtUtils.generateToken
→ CsrfTokenService.generateToken
→ AuthCookieService.issueAuth + issueCsrf
→ Set-Cookie: CSA_AUTH_TOKEN(HttpOnly) + CSA_CSRF_TOKEN
→ R.ok({csrfToken, username, roleLevel})
→ Axios 拆掉 R 外壳
→ authService 记住 csrfToken，并返回 AuthUser
→ Zustand 持久化 user
→ 浏览器进入 /dashboard
```

刷新后的会话确认是另一条链：

```text
页面请求 /dashboard
→ src/proxy.ts 检查 CSA_AUTH_TOKEN 是否存在
→ DashboardGuard 挂载
→ userService.getInfo
→ GET /api/sys/user/info
→ JwtAuthenticationFilter 校验 JWT、吊销状态、sessionVersion 和账号状态
→ Authentication 写入 SecurityContext
→ Controller 返回真实用户信息
→ DashboardGuard 刷新 Zustand user
```

退出链：

```text
authService.logout
→ POST /api/auth/logout
→ AuthController.logout
→ AuthCookieService.resolveToken
→ JwtRevocationService.revoke
→ 吊销标记保存到 Token 剩余有效期
→ AuthCookieService.clear
→ 前端 clearCsrfToken + Zustand.logout
```

#### 常见混淆

- JWT 负责证明 Token 未被篡改并携带身份声明，不负责加密数据库密码。数据库密码由 BCrypt `PasswordEncoder` 处理。
- Zustand 中的 user 只影响前端渲染。手工改 `localStorage` 只能骗前端界面，不能通过后端 `@PreAuthorize`。
- `proxy.ts` 看到 Cookie 不代表 Cookie 有效；真正的签名、过期、吊销和 `sessionVersion` 校验发生在 `JwtAuthenticationFilter`。
- `Authentication` 是当前请求的认证对象；`User` 是数据库实体。Filter 不会把“当前用户信息保存到 User”。
- `AuthCookieService` 能读写 Cookie，但后续请求真正执行认证的是 `JwtAuthenticationFilter`。
- `JwtAuthenticationFilter` 不负责所有 401。它无法建立认证时通常继续链路，受保护资源最终由 `JwtAuthenticationEntryPoint` 写 401。
- `HttpOnly` 降低 JavaScript 读取风险，但浏览器仍会自动携带 Cookie，所以 Day 5 还需要 CSRF。
- `auth_user` 缓存减少重复查库，但修改账号、密码、角色或状态后必须驱逐缓存，否则会继续读旧账号信息。

#### 动手验证

前置条件：前后端、MySQL 已启动，并有可登录账号。

1. 打开浏览器开发者工具 Network，登录一次。
2. 检查 `POST /api/auth/login`：
   - Request Payload 有 `username`、`password`；
   - Response JSON 的 `data` 只有 `csrfToken`、`username`、`roleLevel`，没有 JWT；
   - Response Headers 有两个 `Set-Cookie`。
3. 在 Application/Storage 的 Cookies 中确认：
   - `CSA_AUTH_TOKEN` 为 `HttpOnly`；
   - `CSA_CSRF_TOKEN` 不是 `HttpOnly`；
   - `localStorage` 的 `csa-auth-storage` 只含 user，不含 token。
4. 刷新 `/dashboard`，观察 `GET /api/sys/user/info`；记录它成功返回后 `DashboardGuard` 更新 user。
5. 正常退出，再用浏览器返回工作台：应被送回登录页；检查认证 Cookie 已清除。
6. 如果能够查看 Redis 或内存缓存日志，确认退出后出现按 jti 记录的吊销键，并且 TTL 不是固定永久时间。

当天应保存：

- 一张登录请求和响应截图；
- Cookie 属性截图；
- 一张刷新后 `/api/sys/user/info` 成功的 Network 记录；
- 一张手画登录与刷新双链路图。

#### 自测题

1. `LoginDto`、`@NotBlank`、`@Valid` 三者分别是什么，谁定义规则，谁启动校验？
2. `AuthController` 为什么没有手写比较密码？`PasswordEncoder` 是怎样被认证框架用到的？
3. `Authentication`、`SecurityContext` 和 `User` 实体分别是什么？
4. `JwtUtils.generateToken` 当前写了哪些 claim 和标准字段？
5. JWT 最终保存在哪里？响应体为什么不返回 JWT？
6. `proxy.ts` 和 `DashboardGuard` 各能证明什么，不能证明什么？
7. 刷新页面时，哪一个 Filter 把 `Authentication` 写入哪里？
8. 退出时为什么只清 Cookie 还不够？吊销记录为什么只保存剩余有效期？
9. `auth_user` 缓存的收益和失效风险分别是什么？
10. `MailService` 与 `AsyncMailSender` 为什么拆成两个 Bean？

#### 完成标准

不看本节链路图，能够从 `LoginForm` 开始一路定位到 `PasswordEncoder`、`JwtUtils`、Cookie，再解释刷新和退出两条链；能够明确说出 JWT 不是密码加密、user 状态不是认证凭据、`SecurityContext` 不是 `User` 实体。

### Day 5：CSRF、CORS、CSP、HSTS 和 HTML 清洗

#### 今日目标

学完后，要能说明 Cookie 登录为什么会带来 CSRF 风险，自定义 Filter 如何校验双提交 Token，CORS 与 CSRF 为什么不是同一个问题，以及 CSP、HSTS、DOMPurify 各自保护哪一层。

#### 前置名词

| 名词 | 解释 |
| --- | --- |
| CSRF | Cross-Site Request Forgery，跨站请求伪造。攻击者利用浏览器自动带 Cookie，让已登录用户在不知情时发起写操作。 |
| 安全方法 | 约定上不应修改服务端状态的方法。本项目把 GET、HEAD、OPTIONS、TRACE 视为安全方法，不做 CSRF 校验。 |
| 双提交 Cookie | 服务端下发同一随机值，客户端请求时同时通过 Cookie 和自定义 Header 交回；后端比较两者。恶意站点能诱导浏览器带 Cookie，但通常读不到值并设置匹配 Header。 |
| Bearer Token | 调用方主动放进 `Authorization: Bearer ...` Header 的 Token。浏览器不会像 Cookie 那样自动替任意网站附加它，因此本项目对 Bearer 请求豁免 CSRF。 |
| Filter | Servlet/Spring Security 请求过滤器，在 Controller 之前处理请求。它可以继续 `filterChain.doFilter`，也可以直接写响应并 return。 |
| `OncePerRequestFilter` | Spring 提供的 Filter 基类，保证一次请求分派中按规则执行一次。`CsrfProtectionFilter` 和 `JwtAuthenticationFilter` 都继承它。 |
| `FilterChain` | 多个 Filter 的调用链。调用 `doFilter` 才会把请求交给下一环；直接 return 时 Controller 不会执行。 |
| CORS | Cross-Origin Resource Sharing。由浏览器根据后端响应头判断某个前端 Origin 是否允许读取响应。它不是身份认证，也不是 CSRF Token。 |
| Origin | 协议、主机、端口三者的组合。任一不同就可能构成跨源，例如 `localhost:3000` 与 `localhost:8080`。 |
| 预检请求 Preflight | 浏览器对部分跨源请求先发 OPTIONS，询问后端是否允许方法、Header 和凭据。 |
| `withCredentials` | Axios/浏览器跨源请求时允许携带 Cookie，并允许接收服务端 Cookie。后端同时必须允许 credentials 和具体 Origin。 |
| XSS | Cross-Site Scripting，攻击者让恶意脚本进入页面执行。CSRF 和 XSS 不是同一种漏洞。 |
| CSP | Content Security Policy，浏览器内容安全策略，限制脚本、连接、图片等资源来源。 |
| nonce | 每次响应生成的随机值。只有携带匹配 nonce 的脚本才被 CSP 允许，降低内联脚本注入风险。 |
| HSTS | Strict-Transport-Security，告诉浏览器在有效期内只使用 HTTPS 访问该站点。只在生产 HTTPS 环境有意义。 |
| DOMPurify | 前端 HTML 白名单清洗库，在把富文本插入 DOM 前移除危险标签和属性。 |
| 常量时间比较 | 比较秘密值时尽量不因第几个字符不同而提前结束。本项目用 `MessageDigest.isEqual` 比较 CSRF Token。 |
| fail-closed | 安全配置缺失或不合法时直接拒绝启动/拒绝请求，而不是默认为全部放行。 |

#### 文件、类和方法职责

| 路径或对象 | 它是什么 | 负责什么 | 调用关系 |
| --- | --- | --- | --- |
| `csa-official-backend/src/main/java/com/csa/official/config/CsrfProtectionFilter.java` | 自定义 CSRF Filter 类 | `shouldValidate` 判断是否需要校验；`hasValidCsrfToken` 读取 Cookie/Header；失败直接写 403 `CSRF_INVALID` | `SecurityFilterChain` 在 JWT Filter 后自动调用 |
| `csa-official-backend/src/main/java/com/csa/official/common/security/CsrfTokenService.java` | CSRF Token 组件 | `generateToken` 生成随机值；`matches` 用 `MessageDigest.isEqual` 比较 | `AuthController` 生成；`CsrfProtectionFilter` 比较 |
| `csa-official-backend/src/main/java/com/csa/official/common/security/SecurityResponseWriter.java` | 安全错误 JSON 写入组件 | 在 MVC Controller 之外统一写 `R` 风格错误结构 | CSRF Filter、EntryPoint、DeniedHandler 等调用 |
| `csa-official-backend/src/main/java/com/csa/official/config/CorsConfig.java` | Spring MVC CORS 配置类 | 配置允许的 Origin、方法、Header、暴露响应头和 credentials；通配 Origin 或空配置时启动失败 | 浏览器跨源请求触发 CORS 判断，Spring 使用该配置生成响应头 |
| `csa-official-backend/src/main/java/com/csa/official/config/SecurityConfig.java` | Security Filter Chain 配置 | 关闭 Spring 内置 CSRF，接入项目自定义 `CsrfProtectionFilter`；启用 CORS 并规定 Filter 顺序 | 应用启动时生效 |
| `csa-official-frontend/src/lib/axios.ts` | Axios 请求/响应拦截器 | 写请求前确保有 CSRF Token，并设置 `X-CSRF-Token`；`withCredentials` 携带 Cookie | 前端 service 发请求时自动触发 |
| `csa-official-frontend/src/proxy.ts` | Next.js `proxy` 函数 | 为页面响应生成 CSP nonce、CSP；生产环境添加 HSTS | Next.js 对匹配的页面请求触发 |
| `csa-official-frontend/src/lib/sanitize-html.ts` | 前端清洗函数 `sanitizeHtml` | 调 DOMPurify，只允许指定标签和属性，禁止 script、iframe、style 等 | 展示富文本的组件在输出前调用 |
| `docs/security-design.md` | 安全设计文档 | 汇总认证、CSRF、CORS、Cookie、文件与 XSS 设计 | 用来建立全局关系，最终仍以源码为准 |

#### 精确阅读顺序

1. 先读 `SecurityConfig` 的 `csrf(AbstractHttpConfigurer::disable)` 和 `addFilterAfter`。这里是关闭 Spring 内置实现，再挂项目自定义实现，不是“整个项目没有 CSRF”。
2. 读 `CsrfProtectionFilter.doFilterInternal`：失败时 `SecurityResponseWriter.write` 后 return；成功才 `filterChain.doFilter`。
3. 读 `shouldValidate`，按顺序记录四个判断：功能是否启用；是否安全方法；是否豁免路径或 Bearer；JWT Filter 记录的 Token 来源是否为 cookie。
4. 读 `isCsrfExempt`，逐个记录真实豁免路径。
5. 读 `hasValidCsrfToken`：Cookie 名、Header 名以及 `CsrfTokenService.matches`。
6. 读 `CsrfTokenService.generateToken` 和 `matches`：32 个随机字节、Base64 URL 编码、`MessageDigest.isEqual`。
7. 回到 `axios.ts`：读 `SAFE_METHODS`、`CSRF_EXEMPT_PATHS`、`needsCsrf`、`ensureCsrfToken` 和请求拦截器。
8. 读 `withCredentials=true`，结合 `CorsConfig` 的 `allowCredentials(true)` 和具体 `allowedOriginPatterns`。
9. 读 `CorsConfig` 构造器：为什么包含星号或空配置会直接启动失败。
10. 读 `proxy.ts` 的 `buildContentSecurityPolicy`、nonce 和 `applySecurityHeaders`。
11. 读 `sanitize-html.ts` 的 `ALLOWED_TAGS`、`ALLOWED_ATTR`、`FORBID_TAGS`、`FORBID_ATTR`。

#### CSRF 请求链

```text
已登录浏览器发 POST/PUT/PATCH/DELETE
→ Axios 请求拦截器 needsCsrf
→ 内存中没有 Token 时 GET /api/auth/csrf
→ 服务端生成 Token，写 CSA_CSRF_TOKEN Cookie，并在 data.csrfToken 返回
→ Axios 设置 X-CSRF-Token Header
→ 浏览器自动携带 CSA_AUTH_TOKEN 和 CSA_CSRF_TOKEN Cookie
→ JwtAuthenticationFilter 认证并记录 tokenSource=cookie
→ CsrfProtectionFilter.shouldValidate=true
→ 比较 Cookie Token 与 Header Token
→ 相同：继续 Controller
→ 缺失或不同：直接 403 + CSRF_INVALID，Controller 不执行
```

当前源码的豁免：

```text
GET / HEAD / OPTIONS / TRACE
/api/auth/login
/api/auth/register
/api/auth/send-code
/api/auth/csrf
/api/public/**
使用 Authorization: Bearer ... 的请求
```

#### CORS、CSRF 和 XSS 的边界

| 问题 | 解决的核心问题 | 本项目主要组件 |
| --- | --- | --- |
| CORS | 浏览器是否允许某个 Origin 的前端读取跨源响应 | `CorsConfig` + 浏览器 |
| CSRF | 自动携带 Cookie 的写请求是否由真实页面主动发起 | Axios CSRF Header + `CsrfProtectionFilter` |
| XSS | 不可信 HTML/脚本是否能在页面执行 | 后端 Jsoup、前端 DOMPurify、CSP |
| HSTS | 用户是否会被降级到明文 HTTP | 生产环境 `Strict-Transport-Security` |

CORS 不是服务端通用防火墙。Postman、curl 和服务器程序不受浏览器 CORS 限制；因此不能用“Postman 调不通/调得通”证明 CORS 是否安全。

#### 常见混淆

- Cookie 登录需要 CSRF，不是因为 JWT 本身不安全，而是因为浏览器会自动带 Cookie。
- Bearer 请求豁免的前提是 Token 由调用方主动设置；前端当前默认不保存或拼接 Bearer。
- CSRF Filter 返回 403 时，请求不会进入 Controller，也不会由 `GlobalExceptionHandler` 接住。
- `withCredentials` 只负责允许浏览器携带 Cookie，不会自动生成 CSRF Token；生成与加 Header 是 Axios 拦截器的另一段逻辑。
- CSP 约束脚本和资源来源；DOMPurify 清洗具体 HTML 字符串；HSTS 强制 HTTPS。三者不能互相替代。
- `proxy.ts` 检查 Cookie 存在性属于页面体验优化，不是后端认证或 CSRF 校验。

#### 动手验证

前置条件：已登录，并从登录响应或 `GET /api/auth/csrf` 得到 csrfToken。

1. 用 Thunder Client/Postman 保存 `CSA_AUTH_TOKEN` 和 `CSA_CSRF_TOKEN` Cookie。
2. 调 `POST /api/auth/logout`，但故意不带 `X-CSRF-Token`：
   - 预期 HTTP 403；
   - `errorCode` 为 `CSRF_INVALID`；
   - logout Controller 不执行，所以认证 Cookie 不应被清掉。
3. 再发同一请求，Header 加 `X-CSRF-Token`，值与 `CSA_CSRF_TOKEN` Cookie 一致：
   - 预期进入 logout 并返回成功；
   - Cookie 被清理。
4. 用浏览器从一个不在 CORS 白名单中的 Origin 发请求，观察浏览器控制台；再说明为什么同样请求用 Postman 不会被 CORS 拦。
5. 打开任一页面响应 Headers，检查 `Content-Security-Policy`；生产 HTTPS 环境再检查 `Strict-Transport-Security`。
6. 给 `sanitizeHtml` 传入包含 script、iframe、onclick、合法 p/a 标签的字符串，记录清洗前后差异。

#### 自测题

1. `CsrfProtectionFilter` 是什么类型的类，何时触发，失败后为什么 Controller 不执行？
2. 当前哪些方法和路径豁免 CSRF？
3. Token 来源为什么由 `JwtAuthenticationFilter` 写到 request attribute？
4. Cookie Token 与 Header Token 分别来自哪里？
5. `withCredentials`、`allowCredentials` 和 `allowedOriginPatterns` 三者如何配合？
6. 为什么 CORS 不能阻止 curl 或 Postman？
7. `MessageDigest.isEqual` 在这里比较什么？
8. CSP nonce、HSTS、DOMPurify 各解决哪一类问题？
9. `SecurityConfig` 关闭内置 csrf 后，为什么项目仍然有 CSRF 防护？

#### 完成标准

能够画出“Axios → JWT Filter → CSRF Filter → Controller”的顺序；能亲手复现缺 Header 的 403 和带正确 Header 的成功；能用一句话分别定义 CSRF、CORS、CSP、HSTS 和 DOMPurify。

### Day 6：角色、权限层级和对象级授权

#### 今日目标

学完后，要能从数据库 `roleLevel` 解释 Spring Security authority 的生成过程，区分 URL 认证、方法级权限、对象级业务权限和前端菜单可见性，并证明前端隐藏按钮不是安全边界。

#### 前置名词

| 名词 | 解释 |
| --- | --- |
| `roleLevel` | 项目自己的数字角色等级，保存在 `User` 中。数字越高，组织权限越高；99 是 Root。 |
| `GrantedAuthority` | Spring Security 表示一项权限的接口，例如 `ROLE_LEVEL_3`。 |
| `SimpleGrantedAuthority` | `GrantedAuthority` 的简单字符串实现，`LoginUser` 用它生成角色权限。 |
| `ROLE_` 前缀 | Spring 的 `hasRole` 会自动在参数前加 `ROLE_`。`hasRole('LEVEL_3')` 实际查找 `ROLE_LEVEL_3`。 |
| 角色层级向下兼容 | Level 4 用户同时获得 `ROLE_LEVEL_0` 到 `ROLE_LEVEL_4`，所以能通过较低等级的检查。 |
| `SecurityFilterChain` | URL 级安全规则的 Filter 链。它先决定公开、需认证或拒绝，再进入方法层。 |
| 方法级安全 | 在 Controller/Service 方法调用前用 `@PreAuthorize` 判断权限；由 `@EnableMethodSecurity` 开启。 |
| `@PreAuthorize` | Spring Security 方法级授权注解，表达式为 true 才调用方法体。 |
| SpEL | Spring Expression Language。`PreAuthorize` 中的 `hasRole`、`#dto.id`、`@csaSec` 都是 SpEL 写法。 |
| 对象级权限 | 不只看“是什么角色”，还要看“能否操作这一个具体对象”，例如是否为竞赛发布者或授权编辑者。 |
| 前端可见性 | 根据角色决定菜单和按钮是否显示，作用是改善体验，不阻止用户手工构造 HTTP 请求。 |

#### 角色表

| roleLevel | 常量 | 含义 | LoginUser 生成的最高层 authority |
| --- | --- | --- | --- |
| 0 | `GUEST` | 游客账号 | `ROLE_LEVEL_0` |
| 1 | `MEMBER` | 普通成员 | `ROLE_LEVEL_1` |
| 2 | `CORE_MEMBER` | 核心成员 | `ROLE_LEVEL_2` |
| 3 | `MINISTER` | 部长 | `ROLE_LEVEL_3` |
| 4 | `PRESIDENT` | 会长 | `ROLE_LEVEL_4` |
| 99 | `ROOT` | Root 管理员 | `ROLE_LEVEL_99`，另外拥有 `ROLE_ADMIN` |

Level 3 实际拥有 `ROLE_LEVEL_0`、`ROLE_LEVEL_1`、`ROLE_LEVEL_2`、`ROLE_LEVEL_3`，不是只有一个 `ROLE_LEVEL_3`。

#### 文件、类和方法职责

| 路径或对象 | 它是什么 | 负责什么 | 调用关系 |
| --- | --- | --- | --- |
| `csa-official-backend/src/main/java/com/csa/official/common/constant/RoleConsts.java` | 角色常量类 | 给 0/1/2/3/4/99 命名，避免业务代码散落魔法数字 | `User`、`LoginUser`、各 Service 引用 |
| `csa-official-backend/src/main/java/com/csa/official/common/security/LoginUser.java` | `UserDetails` 实现 | `getAuthorities` 按 `roleLevel` 生成从 0 到当前等级的 `ROLE_LEVEL_N`；Root 追加 `ROLE_ADMIN` | 登录认证和 JWT Filter 加载后交给 Spring Security |
| `csa-official-backend/src/main/java/com/csa/official/config/SecurityConfig.java` | 安全配置类 | `@EnableMethodSecurity` 打开方法注解；`authorizeHttpRequests` 定义 URL 是否公开、认证或拒绝 | Filter Chain 先执行 URL 级规则 |
| `csa-official-backend/src/main/java/com/csa/official/modules/sys/service/CsaSecurityService.java` | 名为 `csaSec` 的自定义权限 Bean | `canCreateCompetition`、`canEditCompetition`、`canGrantCompetitionEditor` 做竞赛对象级判断 | `@PreAuthorize` 表达式通过 `@csaSec` 调用它 |
| `csa-official-frontend/src/config/menu.ts` | 前端菜单配置 | 为菜单项声明 href、标题和 `minRoleLevel` | `DashboardSidebar` 读取 |
| `csa-official-frontend/src/lib/access.ts` | 前端权限辅助函数 | 判断某个 `roleLevel` 是否达到页面或动作要求，提供角色名称 | 菜单和业务组件调用 |
| `csa-official-frontend/src/components/layout/DashboardSidebar.tsx` | 工作台侧栏组件 | 根据当前 `user.roleLevel` 过滤并渲染可见菜单 | Zustand user 变化时重新渲染 |

#### 四层权限模型

```text
第一层：SecurityConfig URL 规则
→ /api/public/** 允许匿名
→ 其它接口默认要求 authenticated

第二层：JwtAuthenticationFilter
→ 从有效 JWT 恢复 LoginUser 和 authorities

第三层：@PreAuthorize 方法级授权
→ hasRole('LEVEL_3') 检查 ROLE_LEVEL_3
→ 失败时方法体不执行，JwtAccessDeniedHandler 返回 403

第四层：CsaSecurityService 对象级授权
→ 查具体竞赛发布者、授权编辑关系和当前用户等级
→ 决定当前用户能不能改“这一条”竞赛
```

前端另有展示层：

```text
menu.ts.minRoleLevel
→ access.ts 判断
→ DashboardSidebar 隐藏菜单
```

展示层不属于后端四层中的任何一层，不能替代授权。

#### 精确阅读顺序

1. 读 `RoleConsts`，手写一遍六个等级和含义。
2. 读 `LoginUser.getAuthorities`：for 循环从 0 生成到当前 level；每项名称是 `ROLE_LEVEL_N`；Root 额外获得 `ROLE_ADMIN`。
3. 读 `SecurityConfig` 的 `@EnableMethodSecurity`、`requestMatchers` 和 `anyRequest.authenticated`。
4. 在 `ResourceController` 中找 `hasRole('LEVEL_1')` 和 `hasRole('LEVEL_3')`，把它们换算成实际检查的 authority。
5. 读 `CsaSecurityService`：
   - `canCreateCompetition`：当前角色至少部长；
   - `canEditCompetition`：会长及以上、发布者本人或 `comp_editor` 授权编辑者；
   - `canGrantCompetitionEditor`：会长及以上或发布者本人。
6. 回到 `CompetitionController` 的 `@PreAuthorize`，观察 `#dto.id` 如何区分新增与编辑，并通过 `@csaSec` 调对象级方法。
7. 读 `menu.ts`、`access.ts`、`DashboardSidebar`，确认前端只读取 `user.roleLevel` 过滤视图。
8. 对比同一功能的前端条件和后端注解；以后发生冲突时，以后端授权为安全事实。

#### 竞赛保存权限示例

```text
POST /api/biz/comp/save
→ JWT Filter 恢复当前用户 authorities
→ Controller 代理执行 @PreAuthorize
→ dto.id == null：
     @csaSec.canCreateCompetition()
     → roleLevel >= 3 才能新建
→ dto.id != null：
     @csaSec.canEditCompetition(dto.id)
     → Level >= 4，或发布者，或已授权编辑者
→ 表达式为 true 才进入 save 方法体
```

这里使用 `#dto.id`，是因为方法收到的是 `SaveCompetitionDto`；Spring 在真正调用方法前可从参数对象读取 id。`CsaSecurityService` 再根据 id 查询实体与授权关系。

#### 常见混淆

- `hasRole('LEVEL_3')` 不是“角色等级必须刚好等于 3”，而是要求拥有 `ROLE_LEVEL_3`；Level 4 和 99 也拥有它。
- `authenticated` 只表示已认证，不代表满足具体业务角色。
- `@PreAuthorize` 是注解；`CsaSecurityService` 是被表达式调用的 Bean；`canEditCompetition` 是方法。三者不要混成一个名称。
- 前端菜单隐藏只能减少误操作。用户仍可在开发者工具、Postman 或脚本中直接请求接口。
- 对象级权限需要具体 id。只写 `hasRole('LEVEL_3')` 无法表达“发布者本人可以改自己的竞赛”。
- `JwtAccessDeniedHandler` 处理已认证但权限不足的 403；未认证是 `JwtAuthenticationEntryPoint` 的 401。
- `CarouselController` 的旧注释可能写“部长及以上”，但当前保存/删除真实注解是 `LEVEL_4`。学习时以可执行注解为准。

#### 动手验证

1. 用 Level 1 账号访问 `GET /api/sys/user/member-test`：预期成功，说明它拥有 `ROLE_LEVEL_1`。
2. 同一账号访问 `GET /api/sys/user/admin-test`：预期 403 + `ACCESS_DENIED`，Controller 方法体不执行。
3. 用 Level 4 或 Root 再访问 member-test：预期仍成功，证明 authority 向下兼容。
4. 在浏览器中手工修改 localStorage 的 user.roleLevel，让高权限菜单显示，再直接调用受保护接口：后端仍按 JWT 恢复的真实账号权限判断；只选择只读测试接口。
5. 从一条竞赛记录中找 publisherId，再分别用发布者、普通 Level 3、Level 4 解释 `canEditCompetition` 的结果；有条件时用授权编辑者账号实测。

#### 自测题

1. `roleLevel`、`GrantedAuthority`、`SimpleGrantedAuthority` 分别是什么？
2. Level 4 用户会生成哪些 `ROLE_LEVEL_N`？
3. `hasRole('LEVEL_3')` 为什么能让 Level 4 通过？
4. `SecurityConfig`、`@PreAuthorize`、`CsaSecurityService` 各负责哪一层？
5. `#dto.id` 和 `@csaSec` 在 SpEL 中分别表示什么？
6. 新建竞赛、编辑竞赛、授权编辑者的真实规则分别是什么？
7. `menu.ts`、`access.ts`、`DashboardSidebar` 为什么不构成安全边界？
8. 401 和 403 在这条权限链中分别由谁处理？

#### 完成标准

能够从 `User.roleLevel` 画到 `LoginUser` authorities，再画到 `@PreAuthorize`；能够用竞赛编辑说明对象级权限；能够亲手证明修改前端状态无法绕过后端。

### Day 7：资源列表、文件上传、受控下载和并发计数

#### 今日目标

学完后，要能把“资源记录”和“实际文件”分开说明，追完上传、发布、列表、下载四条链路，解释文件类型、大小、配额、路径校验，以及 `ResourceService` 为什么应从 Controller 中抽出。

#### 前置名词

| 名词 | 解释 |
| --- | --- |
| `MultipartFile` | Spring 对 `multipart/form-data` 中上传文件的抽象，能读取文件名、大小、输入流和内容类型。 |
| `FormData` | 浏览器构造 `multipart/form-data` 请求的对象。`fileService.upload` 把 `File` 放入名为 `file` 的字段。 |
| 二进制文件 | 磁盘中真正保存的文件内容。它与数据库中的资源标题、摘要不是同一个东西。 |
| 文件元数据 Metadata | 描述文件的数据，例如 owner、storageKey、原名、大小、SHA-256、状态；本项目存入 `sys_stored_file`。 |
| `storageKey` | 文件服务内部使用的逻辑路径，本地形如 `/files/{userId}/{uuid.ext}`。它不是用户原始文件名。 |
| UUID | 随机唯一标识。本项目用它生成服务端文件名，避免原名冲突和直接信任用户文件名。 |
| 扩展名 | 文件名最后的 jpg、pdf、zip 等后缀。只能作为第一层检查，不能证明文件内容真实。 |
| 文件签名 / magic number | 文件开头或固定位置的特征字节。`FileService` 用它检查内容是否与扩展名相符。 |
| MIME / Content-Type | 表示内容媒体类型的字符串。用户上传时提供的值不完全可信，下载时服务端还会探测。 |
| SHA-256 | 密码学哈希算法。这里给文件内容生成摘要，便于完整性、去重或审计，不是加密文件。 |
| 配额 Quota | 限制单个用户或全站累计可占用的存储空间。 |
| 路径穿越 | 攻击者用 `../` 等片段逃出指定目录读取任意文件。解析和规范化后必须确认路径仍在根目录。 |
| 原子移动 | 临时文件完整写好后一次性移动到目标位置，避免其它请求看到半个文件。 |
| Entity | 与数据库表结构接近的持久化对象，例如 `Resource`、`StoredFile`。 |
| VO | View Object，专门返回给前端的视图对象，例如 `ResourceVO`；只暴露接口真正需要的字段。 |
| DTO | Data Transfer Object，承载请求数据，例如 `ResourceController` 内嵌的 `SaveResourceDto`。 |
| Mapper | MyBatis-Plus 数据访问接口，负责执行查询、插入、更新和删除。 |
| `BaseMapper` | MyBatis-Plus 通用 Mapper 父接口。子接口即使没有手写方法，也继承 `selectPage`、`insert`、`updateById` 等能力。 |
| `Page` | MyBatis-Plus 分页对象，包含 `records`、`current`、`size`、`total`、`pages` 等。 |
| 事务 Transaction | 把一组数据库操作作为一个一致性单元；出现异常时按配置回滚。 |
| 原子更新 | 让数据库在单条 UPDATE 内完成加一，避免并发请求互相覆盖。 |
| `COALESCE` | SQL 函数，返回第一个非 NULL 值；`COALESCE(download_count, 0)` 让空计数按 0 处理。 |

#### 文件、类和方法职责

| 路径或类 | 它是什么 | 负责什么 | 调用关系 |
| --- | --- | --- | --- |
| `csa-official-frontend/src/app/dashboard/resources/page.tsx` | Next.js 页面文件 | 导出 `DashboardResourcesPage`，渲染 dashboard 版 `ResourceLibrary` | 访问 `/dashboard/resources` 时由 App Router 加载 |
| `csa-official-frontend/src/components/business/resources/ResourceLibrary.tsx` | 资源业务组件 | 管理分页、分类、表单、上传、发布、删除、下载和 React state | 页面渲染它；它调用 `resourceService` 和 `fileService` |
| `csa-official-frontend/src/services/file.ts` | 前端文件 service | 用 `FormData` 调 `POST /api/common/file/upload`，返回 `storageKey` | `ResourceLibrary.handleUploadFile` 调用 |
| `csa-official-frontend/src/services/resource.ts` | 前端资源 service | 封装 `list`、`categories`、`save`、`remove`、`trackDownload` | `ResourceLibrary` 调用 |
| `csa-official-backend/src/main/java/com/csa/official/common/controller/FileController.java` | 上传 Controller | `upload` 接收 `MultipartFile`，取当前 `userId`，调用 `FileService.upload`，返回路径 | `/api/common/file/upload` 进入 |
| `csa-official-backend/src/main/java/com/csa/official/modules/sys/service/FileService.java` | 文件业务 Service | 检查空文件、大小、扩展名、签名、配额；保存文件和元数据；安全解析下载路径 | `FileController` 和 `StoredFileController` 调用 |
| `csa-official-backend/src/main/java/com/csa/official/modules/sys/storage/FileStorage.java` | 文件存储接口 | 定义 `provider`、`store`、`resolve`、`delete`，使业务层不绑死具体磁盘实现 | `FileService` 依赖接口 |
| `csa-official-backend/src/main/java/com/csa/official/modules/sys/storage/LocalFileStorage.java` | 本地磁盘实现类 | 将 `storageKey` 限制在 upload 根目录；临时写入后 `ATOMIC_MOVE`；提供安全 resolve/delete | Spring 注入给 `FileService` |
| `csa-official-backend/src/main/java/com/csa/official/modules/sys/entity/StoredFile.java` | 文件元数据 Entity | 映射 `sys_stored_file` 的 owner、storageKey、原名、大小、SHA-256、状态等 | `StoredFileMapper` 读写 |
| `csa-official-backend/src/main/java/com/csa/official/modules/sys/mapper/StoredFileMapper.java` | 文件元数据 Mapper | 统计用户/全站占用、按 storageKey 查询、更新最后访问时间 | `FileService` 调用 |
| `csa-official-backend/src/main/java/com/csa/official/common/controller/StoredFileController.java` | 受控下载 Controller | 校验 Level 1、元数据状态、owner/path 对应关系、本人或已发布资源权限；设置下载响应头 | `GET /files/{ownerId}/{fileName}` 进入 |
| `csa-official-backend/src/main/java/com/csa/official/modules/sys/controller/ResourceController.java` | 资源 HTTP Controller | 接参数、`@PreAuthorize`、`@Valid`、调用 `ResourceService`，并用 `R` 包装结果 | `/api/sys/resource/**` 进入 |
| `csa-official-backend/src/main/java/com/csa/official/modules/sys/service/ResourceService.java` | 资源业务 Service | 分页分类查询、Entity 转 VO、归属校验、保存删除、下载计数 | `ResourceController` 调用；它再调 `ResourceMapper` |
| `csa-official-backend/src/main/java/com/csa/official/modules/sys/mapper/ResourceMapper.java` | 资源 Mapper | 通过 `BaseMapper` 执行 `selectPage`、`insert`、`update`、`delete`、`exists` | `ResourceService` 和 `StoredFileController` 调用 |
| `csa-official-backend/src/main/java/com/csa/official/modules/sys/entity/Resource.java` | 资源 Entity | 映射 `sys_resource`，保存标题、摘要、fileUrl、分类、上传者和下载计数 | `ResourceMapper` 读写 |
| `csa-official-backend/src/main/java/com/csa/official/modules/sys/vo/ResourceVO.java` | 资源返回 VO | `from` 把 `Resource` 转成接口字段，避免直接暴露持久化对象 | `ResourceService.listResources` 映射 |
| `csa-official-backend/src/main/java/com/csa/official/common/util/PageUtils.java` | 分页参数工具类 | `of` 把 page 收敛到至少 1、size 默认 10 且上限 100；`clampLimit` 限制普通列表到 200 | `ResourceService`、`CompetitionService` 等调用 |
| `csa-official-backend/src/main/resources/application.yml` | 后端配置文件 | 定义 multipart 50MB、业务层 50MB、个人 200MB、全站 20GB、上传根目录和允许扩展名 | Spring 启动时注入 `FileService` 和 multipart 解析器 |

#### 四条链必须分开

**一、文件上传**

```text
ResourceLibrary.handleUploadFile
→ fileService.upload(file)
→ FormData 字段 file
→ POST /api/common/file/upload
→ JwtAuthenticationFilter 认证
→ CsrfProtectionFilter 校验写请求
→ FileController.upload
→ SecurityUtils.getUserId
→ FileService.upload
→ 空文件 / 50MB / 扩展名 / 文件签名 / 个人配额 / 全站配额
→ UUID 生成 /files/{userId}/{uuid.ext}
→ LocalFileStorage 临时文件 + 原子移动
→ StoredFileMapper.insert 元数据
→ 返回 storageKey
→ setDraft(... fileUrl=storageKey)
```

**二、资源发布**

```text
用户填写 title、summary、category，且 draft.fileUrl 已有值
→ resourceService.save
→ POST /api/sys/resource/save
→ SaveResourceDto + @Valid
→ ResourceController.save（LEVEL_3）
→ ResourceService.saveResource
→ 新增：设置 uploaderId、downloadCount=0，insert
→ 更新：selectById 做归属校验，再 updateById
→ 返回发布成功
```

上传成功只代表磁盘和 `sys_stored_file` 中有文件；再保存 `Resource` 才代表资源库中发布了一条可查记录。当前前端也允许手填外链，所以 `Resource.fileUrl` 不一定永远是本地 `storageKey`。

**三、资源列表**

```text
ResourceLibrary 的 useEffect
→ loadResources(page, activeCategory)
→ resourceService.list
→ GET /api/sys/resource/list?page=...&size=8&category=...
→ ResourceController.list
→ ResourceService.listResources
→ PageUtils.of
→ category 有文本时 eq(category.trim())
→ orderByDesc(createTime)
→ ResourceMapper.selectPage
→ Page<Resource>
→ ResourceVO.from
→ Page<ResourceVO>
→ R.ok
→ Axios 返回 payload.data
→ setItems(records)、setPages(pages)、setTotal(total)
```

`PageUtils` 只知道传入参数，不知道数据库总页数。它不会把“超过最后一页”的 page 改成 1；`ResourceLibrary` 收到真实 `pages` 后，如果当前页越界，会重新请求最后一页。

**四、文件下载和计数**

```text
ResourceLibrary.handleDownload
→ window.open(fileUrl)
→ GET /files/{ownerId}/{fileName}
→ StoredFileController.download（LEVEL_1）
→ 查 ACTIVE 元数据并核对 owner/path
→ 当前用户是 owner，或 Resource 表已有同一 fileUrl
→ FileService.resolveStoredFile 防路径穿越并确认普通文件
→ Content-Disposition: attachment
→ X-Content-Type-Options: nosniff
→ 浏览器开始下载

同时前端调用 resourceService.trackDownload(id)
→ POST /api/sys/resource/download
→ ResourceService.increaseDownloadCount
→ 单条 SQL：download_count = COALESCE(download_count, 0) + 1
→ 前端本地 state 同步显示 +1
```

#### 为什么需要 ResourceService

旧实现把 Mapper 和业务判断放在 `ResourceController`。抽成 Service 解决三个实际问题：

1. 事务边界：更新是 `selectById` → 判断 owner/高权限 → `updateById` 的读-判断-写序列，应由 Service 的 `@Transactional` 包住。
2. 业务复用：以后 CLI、定时任务或 Agent 工具查询资源，不必伪造 HTTP Controller 调用。
3. 单元测试：`ResourceServiceTest` 可以用 Mockito 隔离 Mapper 和 `AuditService`，不必启动完整 Web 环境。

Controller 的职责应收敛为“接参、授权注解、触发校验、调 Service、包装返回”；业务规则应在 Service。

#### 文件限制的真实配置

| 限制 | 当前默认值 | 位置 |
| --- | --- | --- |
| Spring 单文件上限 | 50MB | `spring.servlet.multipart.max-file-size` |
| Spring 单请求上限 | 50MB | `spring.servlet.multipart.max-request-size` |
| FileService 单文件上限 | 52,428,800 bytes，即 50MB | `csa.upload.max-file-size-bytes` |
| 单用户 ACTIVE 文件配额 | 209,715,200 bytes，即 200MB | `csa.upload.user-quota-bytes` |
| 全站 ACTIVE 文件配额 | 21,474,836,480 bytes，即 20GB | `csa.upload.school-quota-bytes` |
| 允许扩展名 | jpg、jpeg、png、gif、pdf、zip、rar、7z、tar、gz、doc、docx、ppt、pptx | `csa.allow-types` |

扩展名通过后，还会按对应格式检查头部签名。把 exe 简单改名为 pdf 不会通过 `%PDF-` 签名检查。

#### 常见混淆

- `FileController` 负责接上传请求；`FileService` 才负责验证和保存；`LocalFileStorage` 只负责具体磁盘操作。
- `StoredFile` 是文件元数据实体；`Resource` 是资源库业务实体；两者不是同一张表。
- `storageKey` 是服务端逻辑路径；`originalName` 只用于友好下载名，不能直接当磁盘目标路径。
- `ResourceMapper` 继承 `BaseMapper` 不等于“没有执行数据库操作”。`ResourceService` 调 `selectPage` 时，MyBatis-Plus 会生成并执行 SQL。
- `ResourceVO.from` 是 Entity → VO 转换，不是“把后端关键字映射到前端”。准确说法是选择并转换接口允许返回的字段。
- `@Transactional` 不会自动修复所有并发问题；下载计数采用单条原子 SQL 才避免读改写丢更新。
- `PageUtils` 防的是超大 size 造成数据库/JVM 资源耗尽，不知道数据库总页数。
- 受控下载不是完全公开静态目录。即使猜到 UUID，也要通过登录和 owner/发布状态校验。

#### 动手验证

1. 列表：
   - 请求 `page=-2&size=1000000`；
   - 观察返回 `current=1`、`size=100`；
   - 再请求不存在的 category，确认 records 为空但请求正常。
2. 类型：
   - 上传一个允许类型的小文件，记录返回 `storageKey` 和 `sys_stored_file` 元数据；
   - 把一个文本或可执行文件只改后缀为 pdf，确认“文件内容与扩展名不匹配”。
3. 大小与配额：
   - 不必真的上传 20GB；在测试环境临时把业务上限调小，用小文件触发 400/413，随后恢复配置。
4. 下载权限：
   - owner 下载自己的未发布文件应成功；
   - 另一 Level 1 用户访问未发布文件应 403；
   - 将 storageKey 保存为 `Resource.fileUrl` 后，另一 Level 1 用户再访问应成功。
5. 并发计数：
   - 连续或并发调用下载计数接口多次，确认最终 `download_count` 增量与成功请求数一致。
6. 事务和单测：
   - 运行 `ResourceServiceTest`；
   - 找出更新他人资源、资源不存在、下载计数等用例各 mock 了哪个 Mapper 行为。

#### 当天产出

- 一张包含上传、发布、列表、下载四条支线的资源/文件结构图；
- 一张文件限制表；
- 一份 Entity、DTO、VO、Mapper、Service、Controller 对照说明；
- 至少一条失败上传和一条权限拒绝的实际响应证据。

#### 自测题

1. `MultipartFile`、`FormData`、`storageKey`、`StoredFile` 各是什么？
2. `FileController`、`FileService`、`FileStorage`、`LocalFileStorage` 的职责怎么分？
3. 为什么扩展名白名单后还要检查 magic number？
4. 上传文件和发布资源为什么是两个请求？
5. `StoredFile` 与 `Resource` 分别保存什么？
6. `/files/{ownerId}/{fileName}` 的访问条件是什么？如何防路径穿越？
7. `ResourceController` 和 `ResourceService` 各负责什么？
8. `ResourceMapper.selectPage` 在哪里被真正调用？分类和排序条件在哪里设置？
9. `Page<Resource>` 为什么要转换成 `Page<ResourceVO>`？
10. `PageUtils.of` 能修正哪些参数，不能修正什么？
11. 为什么下载计数使用 `COALESCE + 1` 的单条 SQL？
12. `SaveResourceDto`、`@Valid`、`@NotBlank` 分别是什么？

#### 完成标准

能够不看提示分别画出四条链；能从 `application.yml` 找到全部限制；能解释文件内容、文件元数据、资源业务记录三者的关系；能指出 `ResourceMapper` 的真实调用行和 Service 事务边界。

### Day 8：竞赛列表、详情、编辑和协作者授权

#### 今日目标

学完后，要能解释为什么公开入口和后台入口分开，追完新建、编辑、授权、列表、详情五条请求，理解 `Entity`、列表 VO、详情 VO 和富文本摘要的边界，并能指出前后端响应契约变化会造成什么 bug。

#### 前置名词

| 名词 | 解释 |
| --- | --- |
| 竞赛模块 `biz` | 保存协会比赛/训练营/项目活动的业务模块。这里的“业务”不是数据库本身，而是围绕竞赛规则组织的代码。 |
| 公开入口 | 不要求登录、给官网访客使用的 Controller/API。公开入口只能返回已发布信息。 |
| 后台入口 | 需要登录并按角色/对象权限控制的 Controller/API，供工作台管理内容。 |
| `Entity` | 映射数据库表的对象。`Competition` 包含完整正文和持久化字段，不应该直接作为所有接口响应。 |
| VO | View Object，按某个页面用途设计的返回对象。列表和详情需要的字段不同，所以有两个 VO。 |
| 富文本 HTML | 正文中带标签的内容，例如 `<p>`、`<strong>`。标签本身可能携带脚本风险，入库前需要清洗。 |
| Jsoup | Java HTML 解析/清洗库。`Jsoup.clean` 按 `Safelist` 白名单移除危险标签和属性，`Jsoup.parse(...).text()` 可取纯文本。 |
| `Safelist` | Jsoup 允许列表。`basicWithImages()` 表示保留基础文本标签和图片相关内容。 |
| 摘要 `summary` | 后端从完整正文生成的纯文本短版本，列表页展示它而不是整段 HTML。 |
| `@EnumValue` | MyBatis-Plus 注解，告诉 ORM 把枚举的指定字段（本项目是 `code`）写入数据库。 |
| `@PreAuthorize` | 方法真正执行前的授权表达式。新增和编辑可使用不同对象级权限。 |
| 对象级权限 | 结合具体竞赛 id、发布者和 `comp_editor` 关系决定能否操作该条记录。 |
| 契约 Contract | 前后端对字段名称、类型和是否存在的共同约定。列表从 `content` 改为 `summary` 时，前端必须同步。 |

#### 文件、类和方法职责

| 路径或对象 | 它是什么 | 负责什么 | 调用关系 |
| --- | --- | --- | --- |
| `csa-official-frontend/src/app/competitions/page.tsx` | 公开 Next.js 页面 | 渲染 Navbar、Footer 和公开版 `CompetitionBoard` | 浏览器访问 `/competitions` |
| `csa-official-frontend/src/app/dashboard/competitions/page.tsx` | 后台 Next.js 页面 | 以 `variant="dashboard"` 渲染 `CompetitionBoard` | 工作台页面加载 |
| `csa-official-frontend/src/components/business/competitions/CompetitionBoard.tsx` | 竞赛业务组件 | 列表加载、分页、编辑表单、封面上传、授权协作者和状态显示 | 页面渲染；调用 `competitionService`、`userService` |
| `csa-official-frontend/src/services/competition.ts` | 前端竞赛 service | 封装 `list`、`listPublic`、`detail`、`detailPublic`、`save`、`grantEditor` | `CompetitionBoard` 调用 Axios |
| `csa-official-backend/src/main/java/com/csa/official/modules/biz/controller/PublicCompetitionController.java` | 公开竞赛 REST Controller | 接收公开列表和详情请求，不做登录要求 | `/api/public/competitions` 进入 |
| `csa-official-backend/src/main/java/com/csa/official/modules/biz/controller/CompetitionController.java` | 后台竞赛 REST Controller | 接收保存、授权、后台列表和详情请求；参数绑定/注解授权后调用 Service | `/api/biz/comp/**` 进入 |
| `csa-official-backend/src/main/java/com/csa/official/modules/biz/service/CompetitionService.java` | 竞赛业务 Service | 校验时间、清洗正文、写入竞赛、查询分页、生成摘要、计算编辑/授权标记 | 两个 Controller 都调用 |
| `csa-official-backend/src/main/java/com/csa/official/modules/biz/entity/Competition.java` | `biz_competition` Entity | 保存完整 title、content、coverImg、时间、publisherId、status | `CompetitionMapper` 读写 |
| `csa-official-backend/src/main/java/com/csa/official/modules/biz/entity/CompEditor.java` | `biz_comp_editor` Entity | 表示某用户被授予某竞赛的协作编辑关系 | `CompEditorMapper` 读写 |
| `csa-official-backend/src/main/java/com/csa/official/modules/biz/vo/CompetitionListVO.java` | 列表 VO | 返回 `summary`、时间、状态和可编辑/可授权标记，不返回完整 `content` | `CompetitionService.toCompetitionListVO` 创建 |
| `csa-official-backend/src/main/java/com/csa/official/modules/biz/vo/CompetitionDetailVO.java` | 详情 VO | 返回完整清洗后的 `content`，供详情页/编辑弹窗使用 | `CompetitionService.toCompetitionDetailVO` 创建 |
| `csa-official-backend/src/main/java/com/csa/official/modules/biz/enums/CompetitionStatusEnum.java` | 竞赛状态枚举 | 定义 `UNPUBLISHED(0)`、`ONGOING(1)`、`FINISHED(2)`；`@EnumValue` 指定存 code | `Competition`、VO 和前端状态转换使用 |
| `CompetitionMapper` / `CompEditorMapper` | MyBatis-Plus Mapper 接口 | 继承 `BaseMapper` 执行竞赛和授权关系的 SQL | `CompetitionService` 调用 |

#### 接口地图

| 接口 | 认证 | 用途 | 返回 |
| --- | --- | --- | --- |
| `GET /api/public/competitions` | 公开 | 官网列表 | `R<Page<CompetitionListVO>>`，过滤未发布 |
| `GET /api/public/competitions/{id}` | 公开 | 官网详情 | `R<CompetitionDetailVO>`，未发布按 404 |
| `GET /api/biz/comp/list` | 登录 | 后台列表 | `R<Page<CompetitionListVO>>`，带后台权限标记 |
| `GET /api/biz/comp/{id}` | 登录 | 编辑弹窗取完整正文 | `R<CompetitionDetailVO>` |
| `POST /api/biz/comp/save` | 新建需 Level 3；编辑按对象权限 | 新增或更新 | `R<String>` |
| `POST /api/biz/comp/grant` | 发布者本人或 Level 4+ | 授予协作者 | `R<String>` |

#### 精确阅读顺序

1. 先读前端 `competitions/page.tsx` 和 dashboard 页面，确认两页共用一个组件但传入 variant 不同。
2. 读 `competition.ts` 的接口类型：列表项有 `summary`，详情项通过 `Omit` 加回 `content`；`status` 兼容 string/number 是为了兼容旧响应。
3. 读 `CompetitionBoard.loadCompetitions`：公开版选择 `listPublic`，后台版选择 `list`；收到 `pages` 后处理越界页。
4. 读 `CompetitionBoard.handleEdit`：它必须 `await competitionService.detail(item.id)`，再用详情 `content` 填表单。
5. 读 `CompetitionController.SaveCompetitionDto`：这是 Controller 内的静态嵌套 DTO，不是独立文件；title/content 上的 `@NotBlank` 定义校验规则，方法参数上的 `@Valid` 启动校验。
6. 读 `CompetitionController.save`：DTO 被转换成 `Competition` Entity，再把 `SecurityUtils.getUserId()` 传给 Service。
7. 读保存方法上的 `@PreAuthorize`：`dto.id == null` 走 `canCreateCompetition`；有 id 走 `canEditCompetition(#dto.id)`。
8. 读 `CompetitionService.saveCompetition`：结束时间不能早于开始时间；正文通过 `Jsoup.clean` 和 `Safelist` 清洗；新增设置 publisherId，更新调用 `updateById`。
9. 读两个列表方法：后台列表不额外过滤状态；公开列表用 `ne(status, UNPUBLISHED)`，都按 updateTime、createTime 倒序。
10. 读 `buildSummary`：先 `Jsoup.parse(content).text()` 变成纯文本，再截取最多 200 字并追加省略号。
11. 读两个详情方法：后台详情返回完整正文；公开详情发现 `UNPUBLISHED` 时抛 404。
12. 最后读 `CsaSecurityService` 的编辑判断，回到 `toCompetitionListVO`/`toCompetitionDetailVO` 看 canEdit、canGrant 如何写进响应。

#### 列表与详情链路

```text
公开页 /competitions
→ CompetitionBoard(variant=public)
→ competitionService.listPublic
→ GET /api/public/competitions
→ PublicCompetitionController.list
→ CompetitionService.getPublicCompetitionPage
→ 排除 status=UNPUBLISHED
→ CompetitionListVO(summary)
→ Axios 拆 R 外壳
→ 前端只渲染摘要
```

```text
后台页 /dashboard/competitions
→ CompetitionBoard(variant=dashboard)
→ competitionService.list
→ GET /api/biz/comp/list
→ JwtAuthenticationFilter + URL/方法授权
→ CompetitionController.list
→ CompetitionService.getCompetitionPage
→ CompetitionListVO(summary, canEdit, canGrant)
→ 前端显示可用按钮
```

编辑链：

```text
点击编辑
→ handleEdit(item)
→ await competitionService.detail(item.id)
→ GET /api/biz/comp/{id}
→ CompetitionDetailVO(content)
→ 用完整 content 填 draft
→ save DTO
```

#### 为什么列表不返回 content

列表页只显示约 120 字。一页 10 条时，如果每篇正文是 20KB，直接返回正文约 200KB，其中绝大部分不会被显示。当前后端只生成最多 200 字纯文本 `summary`，正文由详情接口按 id 取；这是响应体缩小和职责分离，不是删除数据库正文。

不能直接对 HTML 做 `substring(0, 200)`：可能从 `<strong>` 或 `<p>` 中间切开，得到半个标签。`Jsoup.parse(content).text()` 先剥掉标签，再截断纯文本。

#### 常见混淆

- `SaveCompetitionDto` 是 `CompetitionController` 的嵌套 DTO；`Competition` 是 Entity；两者不能混称。
- `CompetitionListVO` 和 `CompetitionDetailVO` 都是返回对象，不是数据库表。
- 公开 Controller 分开不代表后台 Service 分开；两个 Controller 复用同一个 `CompetitionService`。
- `@PreAuthorize` 是方法调用前的授权注解；`CsaSecurityService` 才是执行具体对象判断的 Bean。
- `status` 返回数字是为了与前端契约一致；数据库存 `CompetitionStatusEnum.code`，不是枚举名字。
- 公开未发布详情返回 404 是隐藏资源存在性；不是说未发布记录真的不存在。
- 前端 `canEdit=false` 只隐藏按钮；后端仍必须检查权限。
- 列表接口没有 `content` 后，编辑不能直接拿列表对象填表单，否则保存可能把正文变成空值或摘要。

#### 动手验证

1. 公开列表不带登录访问 `GET /api/public/competitions`，记录只出现已发布状态。
2. 找一条未发布 id，访问公开详情；预期 404，而不是 403。
3. 登录后台，比较同一条竞赛的列表 JSON 和详情 JSON：前者有 `summary` 无 `content`，后者有完整 `content`。
4. 用没有权限的账号直接调用 `POST /api/biz/comp/save`，不要依赖页面按钮；记录 403 和 `ACCESS_DENIED`。
5. 在正文中输入合法 `<strong>` 和危险 `<script>`，保存后读取详情，记录 Jsoup 清洗结果。
6. 编辑一条长正文，确认弹窗先出现“加载中”，随后填入完整正文，而不是列表摘要。

#### 自测题

1. `CompetitionController`、`CompetitionService`、`CompetitionMapper` 各负责什么？
2. `SaveCompetitionDto` 为什么不是 `Competition` Entity？
3. 新建和编辑的 `@PreAuthorize` 表达式分别走哪个权限方法？
4. 公开列表为什么过滤 `UNPUBLISHED`？
5. `CompetitionListVO` 与 `CompetitionDetailVO` 的字段差异是什么？
6. `buildSummary` 为什么先用 Jsoup 再截断？
7. `@EnumValue` 如何影响 `status` 落库？
8. 未发布详情为什么返回 404？
9. `handleEdit` 为什么必须异步请求详情？
10. `canEdit`/`canGrant` 是安全判断本身，还是给前端的结果提示？

#### 完成标准

能够从两个页面定位到两个 API，再定位到同一个 Service；能解释列表摘要与详情正文的拆分；能说清 DTO、Entity、VO、枚举、对象级权限的区别；能亲手验证未发布 404 和编辑先取详情。

### Day 9：简历模块

#### 今日目标

学完后，要能从成员页面一路追到部长审核页面，讲清楚简历为什么不是“保存一行文本”这么简单：它有状态机、权限边界、隐私边界、并发审核和前后端契约。当前网页已经支持完整闭环，不需要手工拿 `resumeId` 调接口。

读：

```text
ResumeController.java
ResumeService.java
Resume.java
dashboard/resume/page.tsx
resume.ts
ResumeReviewWorkspace.tsx
dashboard/resume-reviews/page.tsx
ResumeReviewListVO.java
ResumeReviewDetailVO.java
V3__resume_review_queue_index.sql
```

#### 状态机

```text
DRAFT（草稿）
  └─ 成员提交 → PENDING（待审核）
       ├─ 部长通过 → APPROVED（已通过）
       └─ 部长驳回 → REJECTED（已驳回）

APPROVED / REJECTED
  └─ 成员修改并保存 → DRAFT
       └─ 再次提交 → PENDING
```

关键约束：

- 只有 `LEVEL_2` 及以上的核心成员能读取和保存自己的简历。
- 只有 `LEVEL_3` 及以上的部长/管理员能读取审核队列、详情和执行审核。
- `DRAFT` 不能出现在审核队列，也不能通过审核详情接口被其他人读取。
- `PENDING` 期间成员不能修改简历，避免部长看到的内容和最终审核内容不一致。
- 驳回必须有原因，最长 500 个字符；正文最长 50,000 个字符，Git 地址最长 255 个字符并限制为 HTTP/HTTPS。

#### 文件和方法职责

| 路径或对象 | 负责什么 |
| --- | --- |
| `dashboard/resume/page.tsx` | 成员编辑自己的简历、保存草稿、提交审核、显示当前状态 |
| `resume.ts` | 封装成员接口和审核接口，定义状态码与 TypeScript 契约 |
| `ResumeController` | 暴露 `/my`、`/save`、`/submit`、`/reviews`、`/reviews/{id}`、`/audit`，做参数校验和角色入口保护 |
| `ResumeService` | 实现状态流转、内容校验、批量装配、原子审核和审计调用 |
| `ResumeReviewWorkspace.tsx` | 部长筛选队列、分页、打开详情、填写驳回原因、提交决定并刷新列表 |
| `ResumeReviewListVO` | 分页列表的轻量字段，只放摘要，不放完整正文 |
| `ResumeReviewDetailVO` | 审核详情白名单，放完整正文和必要身份字段，不暴露密码/Token/支付字段 |
| `V3__resume_review_queue_index.sql` | 为 `(status, update_time)` 建索引，支持按状态查看最新提交 |

#### 完整请求链路

成员侧：

```text
/dashboard/resume
→ resumeService.save / submit
→ Axios Cookie + CSRF
→ ResumeController
→ ResumeService
→ biz_resume
```

部长侧：

```text
/dashboard/resume-reviews
→ listReviews(page, size, status)
→ GET /api/resume/reviews
→ 分页查询简历
→ selectBatchIds(申请人)
→ selectBatchIds(部门)
→ ResumeReviewListVO
```

点击详情和审核：

```text
点击队列项
→ GET /api/resume/reviews/{id}
→ 只允许 PENDING / APPROVED / REJECTED
→ 展示完整正文
→ POST /api/resume/audit
→ UPDATE ... WHERE id = ? AND status = PENDING
→ 写入 audit_by / audit_time / 审计日志
```

#### 为什么列表和详情要拆开

列表每页可能有多条简历。如果直接返回 `Resume` 实体，会把完整正文、`userId`、逻辑删除字段等持久层细节一起带出去，还会让响应体随正文长度增长。现在列表只给摘要，部长点开某一条后再取详情；详情又通过显式白名单控制字段。这是 DTO/Entity/VO 分层的实际价值，不是为了多写几个类。

#### 为什么审核要原子更新

仅仅“先查到 `PENDING`，再 `updateById`”不够。两个部长并发请求时，两个请求都可能先查到待审核。当前实现把状态作为 SQL 更新条件：

```sql
UPDATE biz_resume
SET status = ?, audit_by = ?, audit_time = ?
WHERE id = ? AND status = 1
```

受影响行数为 1 才算成功；为 0 就返回 409，让前端刷新后重试。`@Transactional` 提供事务边界，V3 索引则让队列按状态和更新时间查询更稳定。

#### 动手验证

1. 用核心成员访问 `/dashboard/resume-reviews`，确认菜单不显示，直接请求仍返回 403。
2. 用部长打开审核页，确认默认筛选为待审核，列表按更新时间倒序并能分页。
3. 打开一条待审核简历，确认详情有完整正文、学号/学院/班级和 Git 地址。
4. 不填驳回原因点击驳回，确认前端阻止请求；用接口绕过前端时后端仍返回 400。
5. 通过一条简历，回到成员页面确认状态变为已通过；成员修改并保存后状态回到草稿。
6. 用两个并发审核请求模拟竞态，确认一个成功、一个 409，数据库最终只有一个审核结果。
7. 把一条简历改成草稿后调用详情接口，确认返回 404，且不会查询申请人和部门信息。

#### 自测题

1. 谁能保存简历？
2. 谁能审核简历？
3. 草稿、提交、审核状态怎么变化？
4. 为什么审核逻辑应该在 Service，而不是 React 组件或 Controller？
5. 为什么列表和详情要拆成两个 VO？
6. `WHERE status = PENDING` 如何防止两个部长重复审核？
7. 为什么草稿详情要返回 404，而不是返回空正文？
8. 审核中禁止编辑解决了哪种数据一致性问题？
9. V3 索引的列顺序为什么先放 `status` 再放 `update_time`？
10. HTTP 409 和 HTTP 403 在这个模块分别表示什么？

#### 完成标准

能够在浏览器中完成“成员保存/提交 → 部长打开队列 → 查看详情 → 通过或驳回 → 成员看到状态”的全流程；能够指出六个审核接口、三个角色边界、四个状态码，并能解释批量查询、VO 白名单、原子更新和 V3 索引各自解决的问题。

### Day 10：部门和投票

#### 今日目标

学完后，要能独立说明两条业务链：会长如何任命某个部门的新部长，以及部长/会长如何创建提案并按权重投票。重点不是背接口，而是理解一次组织变更为什么会同时修改多张记录、为什么必须放在事务里，以及“Service 查重”和“数据库唯一约束”为什么要同时存在。

#### 前置名词

| 名词 | 解释 |
| --- | --- |
| 部门 `Dept` | 组织部门的 Entity，对应 `sys_dept`。`leaderId` 保存当前正部长的用户 ID。 |
| 任命 | 把一个用户提升为某部门部长，同时处理旧部长、用户部门归属、职位类型和缓存。它不是只改一个 `roleLevel`。 |
| 事务 Transaction | 把一组数据库写操作视为一个整体。全部成功才提交；中途抛异常则回滚，避免“旧部长已经降级但新部长没有上任”这种半成品状态。 |
| `@Transactional` | Spring 事务注解。`rollbackFor = Exception.class` 表示方法抛出异常时整组写操作回滚。它通过 Spring 代理生效，不是 Java 语法自带能力。 |
| `LambdaUpdateWrapper` | MyBatis-Plus 用 Java 方法引用拼 UPDATE 条件的对象，例如 `.in(User::getId, userIds)` 对应 `WHERE id IN (...)`。 |
| 缓存失效 | 数据库真值被修改后删除相关缓存，让下一次读取重新查库。`@CacheEvict` 删除一个缓存区域，`@Caching` 可组合多个缓存操作。 |
| 提案 `Proposal` | 一次组织决策，对应 `sys_proposal`。记录类型、标题、理由、发起人、状态、截止时间和最终计票快照。 |
| 投票记录 `VoteRecord` | 某个用户对某个提案投的一票，对应 `sys_vote_record`。保存赞成/反对、权重、留言和时间。 |
| 权重 Weight | 一票参与计数时的分值。本项目会长是 2，部长是 1；不是每个人固定一票一分。 |
| 严格多数阈值 | `总可投权重 / 2 + 1`，整数除法向下取整后再加一。只有超过一半才算通过或否决。 |
| 条件聚合 | 在 SQL 中用 `SUM(CASE WHEN ... THEN ... END)` 一次算出赞成权重和反对权重。 |
| `VoteTallyVO` | 只承载计票结果的 View Object，包含 `agreeWeight`、`rejectWeight`，不对应一张数据库表。 |
| `@EnumValue` | 告诉 MyBatis-Plus 枚举落库时使用哪个字段。`VoteResultEnum` 用 `0` 表示反对、`1` 表示赞成。 |
| 并发竞争 Race Condition | 两个请求几乎同时“先查后写”，都可能在查重时看到不存在，然后同时插入。数据库唯一约束用于封死这个时间窗口。 |
| `@Valid` | 在进入 Controller 方法体前启动 DTO 字段校验。`ProposalDto` 的 `@NotBlank` 和 `@Size` 由它触发。 |
| `@PreAuthorize` | Spring Security 的方法授权注解。表达式在方法体执行前判断当前 `Authentication` 是否具有所需角色。 |
| `SecurityUtils.getUserId()` | 从当前请求的 `SecurityContext` 取已认证用户 ID，不是从前端 JSON 信任一个 userId。 |
| `PageUtils.clampLimit` | 收敛不分页列表的返回条数。第二个参数是默认值；真正上限固定为 `MAX_LIST_LIMIT = 200`。 |
| `RoleConsts` | 角色数字常量接口：核心成员 2、部长 3、会长 4、Root 99，避免业务代码散落魔法数字。 |
| DTO / Entity / VO | DTO 接收请求字段，Entity 映射数据库表，VO 定义响应白名单；三者职责不同，不能只因字段相似就混用。 |
| `AppointDto` | `DeptController` 的静态嵌套 DTO，只接收 `deptId` 和 `userId`；Controller 当前用手写 null 判断，不使用 Bean Validation。 |
| `ProposalDto` / `VoteDto` | `VoteController` 的两个嵌套 DTO。前者有 `@NotBlank/@Size` 并由 `@Valid` 触发；后者当前没有字段校验注解。 |
| `AuditService.recordBestEffort` | 尽力写审计的方法；审计失败会记录日志，但不应反向破坏主业务结果。 |

#### 文件、类和方法职责

| 路径或对象 | 它是什么 | 负责什么 | 调用关系 |
| --- | --- | --- | --- |
| `csa-official-frontend/src/app/dashboard/departments/page.tsx` | Next.js 页面函数 `DashboardDepartmentsPage` | 把 `/dashboard/departments` 路由交给部门管理组件 | 页面路由 → `DepartmentCommandCenter` |
| `csa-official-frontend/src/components/business/departments/DepartmentCommandCenter.tsx` | React 客户端组件 | 并行加载部门和成员；筛选候选人；确认后发起任命；成功后重新加载 | 调 `deptService`、`userService` |
| `csa-official-frontend/src/services/dept.ts` | 前端部门 service | `list` 封装 `GET /api/sys/dept/list`；`appoint` 封装 `POST /api/sys/dept/appoint` | 组件调它；它再调 Axios `api` |
| `csa-official-backend/src/main/java/com/csa/official/modules/sys/controller/DeptController.java` | 部门 REST Controller | 接收列表和任命请求；检查任命 DTO 两个 ID 不为空；把业务交给 `DeptService` | HTTP → Controller → Service |
| `csa-official-backend/src/main/java/com/csa/official/modules/sys/service/DeptService.java` | 部门业务 Service | `appointLeader` 完成旧部长降级、新部长升级、部门更新、审计和缓存失效；`batchPromoteToMember` 批量提拔核心成员 | Controller 或其它 Bean 调用；内部调 Mapper |
| `DeptMapper` / `UserMapper` | MyBatis-Plus Mapper | 读取、更新部门和用户；`UserMapper.update` 可执行批量条件 UPDATE | `DeptService` 调用 |
| `DeptVO` | 部门对外 VO | 把 `Dept` 转成接口需要的字段，不暴露 `deleted` | `DeptController.list` 调 `DeptVO.from` |
| `csa-official-frontend/src/app/dashboard/vote/page.tsx` | Next.js 页面函数 `DashboardVotePage` | 把 `/dashboard/vote` 路由交给提案中心组件 | 页面路由 → `ProposalCenter` |
| `csa-official-frontend/src/components/business/vote/ProposalCenter.tsx` | React 客户端组件 | 加载提案、创建提案、提交赞成/反对票、重新加载列表 | 调 `voteService` |
| `csa-official-frontend/src/services/vote.ts` | 前端投票 service | 封装 `list`、`create`、`submit` 三个请求及 TypeScript 类型 | `ProposalCenter` 调用 |
| `csa-official-backend/src/main/java/com/csa/official/modules/sys/controller/VoteController.java` | 投票 REST Controller | 用 `@PreAuthorize` 限制入口；校验创建 DTO；把创建和投票交给 `VoteService`；列表暂时直接调 `ProposalMapper` | HTTP → Controller → Service/Mapper |
| `csa-official-backend/src/main/java/com/csa/official/modules/sys/service/VoteService.java` | 投票业务 Service | 创建提案、校验投票资格、防重复、写投票记录、计算阈值并关闭提案 | `VoteController` 调用 |
| `Proposal` / `VoteRecord` | 两个 Entity | 分别映射 `sys_proposal` 和 `sys_vote_record` | Mapper 读写数据库 |
| `ProposalMapper` | `BaseMapper<Proposal>` | 插入、按 ID 查询和更新提案；列表接口也直接用它查询 | Controller 和 `VoteService` 调用 |
| `VoteRecordMapper` | `BaseMapper<VoteRecord>` + 自定义 SQL Mapper | 插入投票、查重，并通过 `selectTally` 聚合赞成/反对权重 | `VoteService` 调用 |
| `UserMapper.selectEligibleVoteWeight` | 自定义 SQL 方法 | 只统计部长和会长的可投总权重，并排除提案发起人和逻辑删除用户 | `VoteService.checkResult` 调用 |
| `ProposalVO` | 提案对外 VO | 返回提案必要字段，屏蔽 `deleted` | Controller 把 `Proposal` 转成它 |
| `VoteTallyVO` | 计票查询结果 VO | 接收 SQL 别名 `agreeWeight`、`rejectWeight` | `VoteRecordMapper.selectTally` 返回 |
| `VoteResultEnum` | 投票结果枚举 | `REJECT(0)`、`AGREE(1)`；`@EnumValue` 让数据库存整数 code | `VoteService` 写入 `VoteRecord.result` |
| `V1__initial_schema.sql` | Flyway 初始迁移 | 为 `(proposal_id, voter_id)` 建唯一约束，保证并发下仍不能重复投票 | MySQL 最终兜底 |

#### 精确阅读顺序

1. 从两个 `page.tsx` 开始，只确认 URL 对应哪个组件，不要一上来钻业务代码。
2. 读 `DepartmentCommandCenter.loadData`：`Promise.all` 同时请求部门列表和最多 150 名用户；成功后写入两个 `useState`。
3. 读 `handleAppoint`：确认选中部门和候选成员如何变成 `deptService.appoint(deptId, userId)`。
4. 读 `DeptController.appoint`：记录真实路径、权限注解、`AppointDto` 字段和调用的 Service 方法。
5. 逐行读 `DeptService.appointLeader`，把每一次查询和更新写成清单，再解释 `@Transactional` 和三个缓存失效。
6. 单独读 `batchPromoteToMember`。注意它当前没有 Controller 或前端入口，不能把“Service 中存在”讲成“页面已经能用”。
7. 转到 `ProposalCenter`，找 `loadProposals`、`handleCreate`、`handleVote`，再对照 `vote.ts` 的三个接口。
8. 读 `VoteController`：三个接口都要求 `ROLE_LEVEL_3`；创建 DTO 有 Validation，投票 DTO 当前没有字段校验注解。
9. 读 `VoteService.createProposal`：类型标准化、拒绝 `ROOT_APPLY`、状态 0、截止时间加一天、插入数据库。
10. 读 `VoteService.vote`：按顺序记录六个校验，再看权重、插入投票和 `checkResult`。
11. 打开 `VoteRecordMapper.selectTally` 和 `UserMapper.selectEligibleVoteWeight`，把两段 SQL 分别翻译成中文。
12. 最后看 V1 的唯一约束，理解它和 Service 的 `exists` 不是重复劳动。

#### 部长任命完整链路

```text
/dashboard/departments
→ DashboardDepartmentsPage
→ DepartmentCommandCenter
→ loadData 同时调用 deptService.list + userService.list
→ 用户选择部门和候选人
→ handleAppoint 二次确认
→ deptService.appoint(deptId, userId)
→ POST /api/sys/dept/appoint
→ JWT/CSRF Filter
→ @PreAuthorize: ROLE_LEVEL_4 或 ROLE_ADMIN
→ DeptController.appoint
→ DeptService.appointLeader（事务开始）
→ 查部门、查新部长
→ 原部长不是同一人：降为 CORE_MEMBER，positionType=1
→ 新部长若已任其它部门部长：抛 409，全部回滚
→ 新部长设为 MINISTER、目标 departmentId、positionType=3
→ sys_dept.leader_id 改成新部长 ID
→ 写 best-effort 审计
→ 事务提交
→ 清 dept_list、auth_user、public_contributors 缓存
→ 前端重新 loadData 展示新结果
```

为什么必须是事务：原部长用户、新部长用户、部门三处数据必须一起成功。假如新部长更新完成后部门更新失败，没有事务就会出现“用户已经是部长，但部门仍指向旧部长”的不一致。

`batchPromoteToMember` 的真实行为：

```text
检查部门存在
→ userIds 为空则直接返回
→ 一条 UPDATE 更新 id IN (userIds)
→ 只更新 role_level < CORE_MEMBER 的用户
→ 统一设 roleLevel=2、positionType=1、departmentId=目标部门
→ 记录请求人数和实际更新人数
→ 清 auth_user、public_contributors 缓存
```

它当前只是 Service 能力，没有对应 Controller 接口和前端按钮。验收时必须明确说“源码有方法，但当前产品入口未接出”。

#### 提案和投票完整链路

创建提案：

```text
ProposalCenter.handleCreate
→ voteService.create
→ POST /api/sys/vote/create
→ @PreAuthorize("hasRole('LEVEL_3')")
→ @Valid 校验 ProposalDto
→ Controller 拒绝 ROOT_APPLY
→ SecurityUtils.getUserId 取得真实发起人
→ VoteService.createProposal 再次标准化 type，并再次拒绝 ROOT_APPLY
→ status=0（投票中）
→ expireTime=当前时间+1天
→ proposalMapper.insert
→ ProposalVO.from
→ R<ProposalVO>
→ Axios 拆包
→ 前端重新加载列表
```

提交一票：

```text
ProposalCenter.handleVote(proposalId, agree)
→ voteService.submit
→ POST /api/sys/vote/submit
→ @PreAuthorize("hasRole('LEVEL_3')")
→ VoteController.submit
→ VoteService.vote（事务开始）
→ 提案必须存在且 status=0
→ 当前时间不能超过 expireTime
→ 发起人不能投自己的提案
→ 同一 proposalId + userId 不能已有记录
→ 查询当前用户的数据库角色
→ 会长权重2；部长权重1；其它角色拒绝
→ insert sys_vote_record
→ selectTally 聚合赞成/反对权重
→ selectEligibleVoteWeight 计算可投总权重（排除发起人）
→ threshold = totalPossibleWeight / 2 + 1
→ 赞成达到阈值：status=1
→ 反对达到阈值：status=2
→ 否则保持 status=0
→ 更新 finalResultJson 并提交事务
```

`selectTally` 大致等价于：

```sql
SELECT
  SUM(赞成票的 weight) AS agreeWeight,
  SUM(反对票的 weight) AS rejectWeight
FROM sys_vote_record
WHERE proposal_id = 当前提案;
```

数据库中的 `UNIQUE (proposal_id, voter_id)` 负责最终一致性。Service 的 `exists` 给用户友好提示；唯一约束处理两个并发请求同时穿过 `exists` 的情况。

#### 当前源码的边界和易错点

- `@PreAuthorize("hasRole('LEVEL_3')")` 允许拥有 `ROLE_LEVEL_3` 的部长、会长和 Root 进入 Controller，但 `VoteService` 只把数据库角色**恰好为 3 或 4**的人认作投票人，因此当前 Root 会在 Service 被拒绝。
- `ROOT_APPLY` 当前既被 Controller 拒绝，也被 Service 拒绝，所以正常 API 无法创建。`checkResult` 中“不自动提升 Root”的分支主要防御历史数据或其它内部调用，不代表系统会自动授予 Root。
- `/api/sys/vote/list` 目前仍由 Controller 直接调 `ProposalMapper`，没有经过 `VoteService`；这和资源模块已经抽 Service 的结构不同。
- `PageUtils.clampLimit(size, 100)` 表示：未传时默认 100，传入值被限制在 1–200；不是“最大只能 100”。
- 提案列表后端已按 `createTime DESC` 排序，前端 `sortedProposals` 又排一次，属于展示层的防御性排序。
- `VoteDto` 当前没有 `@Valid`、`@NotNull` 或长度限制；不要把 `ProposalDto` 的校验误说成投票 DTO 也有。
- 前端 `canManageDepartments` 和 `canUseVoteCenter` 只是隐藏/展示页面内容；真正权限仍由后端 `@PreAuthorize` 和 Service 规则决定。
- `finalResultJson` 这个名字叫 JSON，但当前实际写入的是 `agree:2, reject:0, threshold:2` 这种字符串，不是标准 JSON 对象。

#### 动手验证

1. 用 Level 4 账号打开 `/dashboard/departments`，确认页面同时发出部门列表和用户目录两个请求。
2. 任命前记录 `sys_dept.leader_id`、新旧两人的 `role_level / position_type / department_id`；任命后逐项对比。
3. 尝试把已经担任其它部门部长的人任命到当前部门，确认返回 409，并检查前面任何更新都没有残留。
4. 用 Level 3 创建普通提案，检查 `status=0`、`expire_time` 约为创建后一日、`proposer_id` 来自登录用户。
5. 用发起人自己投票，预期 403；换部长投票，检查权重 1；换会长投票，检查权重 2。
6. 连续提交同一用户同一提案，第二次应被 Service 或数据库唯一约束拒绝。
7. 请求 `/api/sys/vote/list?size=99999`，用 SQL 日志或断点确认最终 LIMIT 为 200；不传 size 时为 100。
8. 运行：

```powershell
cd D:\CSA-Project\csa-official-backend
.\mvnw.cmd "-Dtest=DeptServiceTest,VoteServiceTest,PageUtilsTest" test
```

#### 自测题

1. `DeptController` 和 `DeptService` 各负责什么？为什么不能把任命的多步更新全塞进 Controller？
2. `appointLeader` 会查询和更新哪些记录？原部长与新部长分别变成什么状态？
3. `@Transactional(rollbackFor = Exception.class)` 在这条链路中防止哪种半成功状态？
4. 任命完成后为什么要同时清 `dept_list`、`auth_user`、`public_contributors`？
5. `batchPromoteToMember` 为什么使用一条条件 UPDATE？哪些用户不会被它降级或覆盖？它当前有没有 HTTP 入口？
6. `ProposalDto`、`Proposal`、`ProposalVO` 三者分别处在哪一层？
7. 谁能进入提案接口，谁最终能被 `VoteService` 认定为合法投票人？Root 为什么是一个特殊边界？
8. 发起人为什么不计入可投总权重，也不能给自己的提案投票？
9. 总可投权重为 5、6 时，阈值分别是多少？
10. `VoteRecordMapper.selectTally` 为什么返回 `VoteTallyVO`，而不是 `VoteRecord` 列表？
11. Service 已经查重，数据库为什么仍要 `UNIQUE (proposal_id, voter_id)`？
12. `clampLimit(size, 100)` 中的 100 和 200 分别表示什么？
13. `ROOT_APPLY` 目前能否通过正常接口创建？即使历史记录通过，为什么也不能自动授予 Root？

#### 完成标准

不看答案能画出“部门页 → appoint 接口 → 三处数据更新 → 缓存失效”和“提案页 → submit → 资格检查 → 权重聚合 → 阈值 → 状态更新”两张链路图；能指出每个 Entity、VO、Mapper、Service 的真实文件；能解释事务、权重、严格多数、条件聚合、并发竞争和唯一约束，而不是只背结果。

### Day 11：贡献和公开内容

#### 今日目标

学完后，要能区分三件容易混在一起的事：

1. `@LogContribution` + AOP 自动记录“完成了一次贡献”；
2. 公开接口如何从配置、用户和贡献流水拼出首页数据；
3. 哪些写操作可以异步，哪些操作必须让请求立即知道成功或失败。

本日还要明确一个边界：`/api/public/contributors` 是“核心成员目录”，`/api/public/contribution/wall` 才是“按贡献流水统计的贡献墙”，名字相似但数据来源和含义不同。

#### 前置名词

| 名词 | 解释 |
| --- | --- |
| 贡献流水 | 一条“某用户在某类事情上获得多少分”的记录，对应 `sys_contribution_log`。它是业务积分数据，不等于安全审计日志。 |
| `ContributionType` | 贡献类型枚举：`DEV` 官网建设、`RES` 资源贡献、`COMP` 发布比赛、`OPS` 首页维护。 |
| 注解 Annotation | 写在类、方法或字段上的元数据。它本身通常不执行逻辑，框架或 AOP 读取它后才会触发行为。 |
| AOP / 切面 | 把多个方法都需要的旁路逻辑（这里是记贡献）集中写在一个类里，不把相同代码复制到每个 Controller。 |
| `@Aspect` | 声明一个类是切面，里面可以定义“在哪些方法前后执行”的规则。 |
| `@AfterReturning` | 只在被拦截方法正常返回后执行；如果原方法抛异常，不进入这个 advice。 |
| `JoinPoint` | AOP 当前拦截到的调用点对象，可读取方法签名、参数等信息。 |
| `@LogContribution` | 项目自定义方法注解，写明贡献类型和描述模板。当前标在资源保存、竞赛保存、轮播图保存三个方法上。 |
| 异步 `@Async` | 调用方法后把工作交给线程池，调用方不必等待方法体完成。异步方法里的异常默认不能直接回传给原 HTTP 请求。 |
| Bean | Spring 容器管理的对象。只有从 Spring Bean 之间调用，`@Async` 这类代理注解才有机会生效。 |
| 线程池 | 预先管理的一组工作线程和等待队列，限制并发数量，避免每个任务都新建线程。 |
| `CallerRunsPolicy` | 线程池满时让提交任务的调用线程自己执行任务；会变慢，但不会像 `AbortPolicy` 那样直接丢任务。 |
| 旁路信息 | 不影响主业务结果、但用于统计或展示的附加数据。贡献流水属于旁路信息。 |
| 条件聚合 | SQL 根据条件分别求和/计数，例如只把 `type='DEV'` 的 score 加起来。 |
| HTML 清洗 | 按白名单删除危险标签和属性，再保存或展示用户提供的 HTML，降低 XSS 风险。 |
| `Safelist` | Jsoup 的允许列表，规定哪些标签和属性可以保留。 |
| `@Cacheable` | 方法第一次执行后把结果放进缓存；相同 key 的后续调用可直接取缓存。 |
| `@CacheEvict` | 数据写入成功或准备写入时删除相关缓存，避免继续返回旧内容。 |
| `@Target(METHOD)` | 限定自定义注解只能标在方法上；这里保证 `@LogContribution` 不会误贴到字段或参数。 |
| `@Retention(RUNTIME)` | 注解保留到程序运行期，Spring AOP 才能通过反射读到它。 |
| `@EnableAsync` | 开启 Spring 的异步方法代理能力；没有它，`@Async` 不会把调用提交到线程池。 |
| `@Scheduled` / cron | 声明定时执行方法。cron `0 0 4 * * ?` 表示每天 04:00 触发。 |
| `SecurityContext` / ThreadLocal | `SecurityContext` 保存当前请求认证；默认绑定请求线程，因此异步线程不能直接继承其中的用户。 |
| 幂等 Idempotency | 同一个业务版本重复执行时，最终效果仍只有一次。定时任务用 Redis 锁和数据库唯一键共同保证。 |
| Redis 锁 | 用带过期时间的 key 抢占执行权，减少多实例同时执行；它可能过期或失效，所以还需要数据库最终兜底。 |
| 公开接口 | 路径在 `/api/public/**` 下，`SecurityConfig` 允许匿名访问；它仍然必须限制数据量和清洗输出。 |
| 贡献目录 vs 贡献墙 | 目录展示 Level 2+ 成员的身份、部门、头衔；贡献墙按流水聚合 DEV/RES/COMP/OPS 数值。 |

#### 文件、类和方法职责

| 路径或对象 | 它是什么 | 负责什么 | 调用关系 |
| --- | --- | --- | --- |
| `csa-official-backend/src/main/java/com/csa/official/common/annotation/LogContribution.java` | 自定义方法注解 | 声明 `type` 和 `detail`，不直接写数据库 | `ContributionAspect` 读取 |
| `ContributionType` | 贡献类型枚举 | 给流水类型提供固定名字和说明 | 注解、Writer、定时任务使用 |
| `csa-official-backend/src/main/java/com/csa/official/common/aspect/ContributionAspect.java` | AOP 切面 Bean | 监听标了 `@LogContribution` 且正常返回的方法；在请求线程取 userId，再提交异步写入 | Spring AOP 自动调用 |
| `csa-official-backend/src/main/java/com/csa/official/modules/sys/service/ContributionLogWriter.java` | 异步写库 Bean | 创建 `ContributionLog`，默认 score=`1`，调用 Mapper 插入；失败只记录日志 | `ContributionAspect` 调它；`@Async` 交给专用线程池 |
| `csa-official-backend/src/main/java/com/csa/official/modules/sys/entity/ContributionLog.java` | 贡献流水 Entity | 一条记录对应一个用户、类型、分数、说明和时间 | Writer、手动发放、定时结算创建；Mapper 落库 |
| `csa-official-backend/src/main/java/com/csa/official/config/AsyncConfig.java` | 异步配置类 | `@EnableAsync` 开启代理；定义邮件池和贡献池、队列容量、拒绝策略 | Spring 启动时加载 |
| `csa-official-backend/src/main/java/com/csa/official/modules/sys/controller/ResourceController.java` | 资源 Controller | `save` 标有 `@LogContribution(RES)`，成功后自动记资源贡献 | `ContributionAspect` 拦截 |
| `csa-official-backend/src/main/java/com/csa/official/modules/biz/controller/CompetitionController.java` | 竞赛 Controller | `save` 标有 `@LogContribution(COMP)`，成功后自动记比赛贡献 | `ContributionAspect` 拦截 |
| `csa-official-backend/src/main/java/com/csa/official/modules/sys/controller/CarouselController.java` | 轮播图 Controller | `save` 标有 `@LogContribution(OPS)`；公开读取、管理保存和删除；管理写入会清缓存 | AOP、Mapper、AuditService |
| `csa-official-backend/src/main/java/com/csa/official/modules/sys/controller/ContributionController.java` | 贡献接口 Controller | 提供统计贡献墙、占位的 rank，以及会长手动发放贡献 | 公开 wall 调 Mapper；award 同步 insert |
| `csa-official-backend/src/main/java/com/csa/official/modules/sys/mapper/ContributionLogMapper.java` | 贡献流水 Mapper | `selectWall(limit)` 用一条 SQL 连接用户/部门并按用户聚合 | `ContributionController.getWall` 调用 |
| `csa-official-backend/src/main/java/com/csa/official/modules/sys/vo/ContributionWallVO.java` | 贡献墙 VO | 承载用户、部门、DEV 分数、三类次数和排序总分 | Mapper SQL 直接映射 |
| `csa-official-backend/src/main/java/com/csa/official/modules/sys/controller/PublicController.java` | 公开内容 Controller | 读取/修改协会介绍、核心成员目录、隐私说明；配置内容保存前用 Jsoup 清洗 | 前端 `publicService` 调用 |
| `PublicController.ConfigDto` / `ContributorVo` | Controller 内嵌请求 DTO / 响应对象 | 前者接协会介绍正文；后者只返回公开成员所需的 ID、姓名、头像、部门、头衔和等级 | `updateAbout` 接收 / `getContributors` 返回 |
| `SysConfig` / `SysConfigMapper` | 配置 Entity/Mapper | 以 `configKey` 读写 `CSA_INTRO` 等系统配置 | `PublicController`、`ContributionTask` 调用 |
| `csa-official-backend/src/main/java/com/csa/official/modules/sys/task/ContributionTask.java` | 定时任务 Bean | 每天 04:00 检查协会介绍版本存活超过 7 天后，给最后修改者结算一次 OPS 贡献 | `@Scheduled` 自动触发；调用 `ScheduledJobService` 防重复 |
| `ScheduledJobService` | 定时任务幂等 Service | Redis 锁先挡并发，数据库唯一幂等键再挡重复执行 | `ContributionTask` 调用 |
| `csa-official-frontend/src/services/public.ts` | 前端公开内容 service | 封装 `getAbout`、`getContributors`、`getCarousel`、`updateAbout` | 首页、关于页、成员页、编辑器调用 |
| `csa-official-frontend/src/components/business/community/ContributorWall.tsx` | 成员目录展示组件 | 渲染 `ContributorVo[]` 的头像、姓名、部门、头衔和等级 | 首页/关于页/成员页调用 |
| `csa-official-frontend/src/components/business/settings/AboutEditor.tsx` | 协会介绍编辑组件 | Level 4 才显示编辑器；本地预览也先调用 `sanitizeHtml` | 设置页调用 `publicService` |

#### 精确阅读顺序

1. 先用 `rg -n "@LogContribution"` 找出当前真正被自动记录的三个方法，不要凭 Controller 名称猜。
2. 读 `LogContribution`：理解 `@Target(METHOD)` 和 `@Retention(RUNTIME)` 的含义——它只能贴方法，运行时 AOP 才能读到。
3. 读 `ContributionAspect.doAfterReturning`：标出切点、正常返回条件、`JoinPoint`、`SecurityUtils.getUserId()`、detail 选择和异常吞掉的位置。
4. 读 `ContributionLogWriter.write`：看它如何创建实体、默认加 1 分、调用 `logMapper.insert`，以及为什么是另一个 Bean。
5. 读 `AsyncConfig`：记住 `@EnableAsync`、两个线程池的名字、容量和 `CallerRunsPolicy`。
6. 读 `ContributionController.getWall` 和 `ContributionLogMapper.selectWall`，先看返回 VO，再把 SQL 每个 SELECT 别名翻译成中文。
7. 读 `PublicController.getAbout/updateAbout`：对照 `@Cacheable`、`@CacheEvict`、Jsoup `Safelist` 和首次插入/后续更新两个分支。
8. 读 `PublicController.getContributors`：它只取 Level 2+ 的必要字段，批量查部门名称，再组装内部 `ContributorVo`。
9. 读 `CarouselController`：公开列表只取 `status=1`，按 `sortOrder ASC` 再 `createTime DESC`；保存/删除要求真实注解 `LEVEL_4`，旧注释“部长及以上”不可信。
10. 最后读 `ContributionTask`，理解“修改后立即加分”和“版本存活七天后才结算”是两种不同规则。
11. 用 `rg` 检查前端调用者：当前 `getContributors` 被首页、关于页、成员页使用；`getCarousel` 在 service 中存在，但当前源码没有搜到页面调用；`/api/public/contribution/wall` 也没有前端 service 调用。

#### 自动贡献记录链路

```text
用户成功调用 ResourceController.save / CompetitionController.save / CarouselController.save
→ Spring AOP 发现方法带 @LogContribution
→ 原方法正常返回后触发 ContributionAspect.doAfterReturning
→ 在请求线程从 SecurityContext 取 userId
→ 从注解取 type 和 detail
→ 调 ContributionLogWriter.write(userId, type, detail)
→ @Async 把写库任务提交给 contributionTaskExecutor
→ 创建 ContributionLog（score=1）
→ ContributionLogMapper.insert
→ sys_contribution_log
```

这里的“成功后”很关键：如果原方法抛异常，`@AfterReturning` 不触发，因此失败的资源保存不会凭空获得贡献分。切面自己的异常被 catch 后只记日志，不让旁路统计故障反过来把主业务请求判失败。

#### 贡献墙 SQL 怎么读

`selectWall(limit)` 做了四件事：

1. `sys_contribution_log l JOIN sys_user u`：只有能对应到未逻辑删除用户的流水才展示；
2. `LEFT JOIN sys_dept d`：用户没有部门也不丢掉，只把部门名变成空字符串；
3. `GROUP BY u.id, ...`：一个用户多条流水合并成一行；
4. 条件聚合并排序：

```text
DEV → 把 score 相加，得到 devScore
RES → 计条数，得到 resCount
COMP → 计条数，得到 compCount
OPS → 计条数，得到 opsCount
totalSortScore = devScore + resCount + compCount + opsCount
ORDER BY totalSortScore DESC
LIMIT limit
```

`ContributionWallVO` 没有数据库表，它只是 SQL 别名对应的返回容器。不要把它和 `ContributionLog` Entity 混为一谈：前者是一人一行的统计视图，后者是一条一条的流水。

#### 公开介绍、成员目录和轮播图

协会介绍：

```text
GET /api/public/about
→ public_about 缓存命中则直接返回
→ 未命中时按 config_key='CSA_INTRO' 查 sys_config
→ R<String>
```

```text
POST /api/sys/config/update-about（LEVEL_4）
→ Jsoup.clean(content, ABOUT_CONTENT_SAFELIST)
→ 配置不存在：插入 CSA_INTRO
→ 配置存在：更新 config_value 和 update_by
→ 记录审计
→ 清 public_about 缓存
```

后端清洗是安全边界；前端 `AboutPage` 和 `AboutEditor` 再用 `sanitizeHtml` 做展示前清洗，是第二道防线。前端实时预览不能替代后端清洗，因为攻击者可以绕过页面直接发 HTTP 请求。

成员目录 `GET /api/public/contributors` 不查贡献流水，而是从 `sys_user` 取 `role_level >= 2` 的用户，再批量补部门名并计算“创始人/运维、会长、副会长、部长、副部长、核心成员”等头衔。它的排序是等级降序、ID 升序，且最多返回 `PageUtils.MAX_LIST_LIMIT = 200` 条。

轮播图 `GET /api/public/carousel/list` 只取启用项（`status=1`），按 `sort_order ASC`、`create_time DESC` 排序，最多 200 条并缓存到 `public_carousel`。保存和删除都要求 `LEVEL_4`，成功后清缓存；保存方法还记一条 `OPS` 自动贡献。当前 `CarouselController` 的注释写着“部长及以上”，但可执行注解是 `hasRole('LEVEL_4')`，以注解为准。

#### 为什么异步不是“加一个注解”

- `SecurityContext` 默认和请求线程绑定。异步线程不是原线程，所以 `ContributionAspect` 必须先在请求线程执行 `SecurityUtils.getUserId()`，再把普通参数传给 Writer；不能在 Writer 里重新取当前用户。
- `@Async` 依赖 Spring 代理。同一个类里 `this.write(...)` 这种自调用会绕过代理，注解不会把调用切到线程池；所以 Writer 单独做成 Bean，由切面注入后调用。
- `contributionTaskExecutor` 队列满时用 `CallerRunsPolicy`：调用线程自己完成写入，接口可能变慢，但不会静默丢贡献记录。`AbortPolicy` 会直接拒绝任务，旁路数据可能丢失。
- `ContributionController.award` 是会长手动发放分数，代码直接 `logMapper.insert`，保持同步。操作者需要在本次 HTTP 响应里知道是否成功；如果异步化，数据库失败只能事后查日志。
- 贡献记录失败不会回滚资源/竞赛/轮播图主操作，因为它是旁路统计；这和审计记录的可靠性要求不是同一条业务规则。

#### 定时结算链路（补充）

```text
每天 04:00
→ ContributionTask.settleIntroContribution
→ 查 CSA_INTRO 和最后修改人
→ 修改未满 7 天：跳过
→ 满 7 天：以 updateTime 作为幂等 key
→ ScheduledJobService 取得 Redis 锁
→ 数据库 uk_job_idempotency 再确认这版未成功结算
→ 插入一条 OPS +1 流水
→ 标记任务 SUCCESS
→ 释放 Redis 锁
```

Redis 锁解决多个实例同时执行，数据库幂等键解决锁过期、进程崩溃或重复触发后的再次执行。两层都要有，不能只靠一个布尔缓存标记。

#### 动手验证

1. 用已登录的 Level 3 账号成功保存一条资源或竞赛，马上查 `sys_contribution_log`；确认有对应 `RES` 或 `COMP`、`score=1`、`user_id` 是当前用户。
2. 让保存接口失败，再查流水；确认 `@AfterReturning` 不会记录贡献。
3. 暂停/替换贡献 Mapper 使异步写库失败，确认主接口仍按自己的结果返回，错误只出现在日志。
4. 请求 `/api/public/contribution/wall?limit=99999`，确认最终 LIMIT 不超过 200；不传时默认 100。
5. 用包含 `<script>、onclick、iframe、p、a` 的内容调用 `update-about`，检查数据库里只保留白名单内容。
6. 修改 `CSA_INTRO` 后立刻读取公开介绍，确认缓存被清掉；不清缓存会继续看到旧文本。
7. 用 Level 3 请求轮播图保存，预期 403；用 Level 4 保存后再读公开列表，确认新增/修改立即可见。
8. 阅读 `ContributionWallVO` 与 SQL 别名，手算一个用户的 `totalSortScore`，再和接口返回对照。
9. 运行：

```powershell
cd D:\CSA-Project\csa-official-backend
.\mvnw.cmd "-Dtest=AuditServiceTest,AsyncMailSenderTest,ScheduledJobServiceTest" test
```

当前没有专门的 `ContributionAspectTest`，所以第 1–3 项应通过本地请求、日志和数据库查询手工验收，不能把“相关 Service 测试通过”说成切面已经被自动测试覆盖。

#### 自测题

1. `@LogContribution` 是什么，它自己会不会插入数据库？谁读取它？
2. `@AfterReturning` 和“方法抛异常后仍执行”有什么区别？
3. 当前源码中哪三个方法真正带有 `@LogContribution`？各自的贡献类型是什么？
4. 为什么 `ContributionAspect` 要在请求线程先取 `userId`？
5. `ContributionLogWriter` 为什么必须是单独的 Spring Bean？
6. `@EnableAsync`、`@Async("contributionTaskExecutor")` 和线程池三者分别做什么？
7. `CallerRunsPolicy` 在队列满时会发生什么？它和 `AbortPolicy` 的取舍是什么？
8. `ContributionLog` 和 `ContributionWallVO` 分别表示一条流水还是一人统计结果？
9. `selectWall(limit)` 为什么要 `GROUP BY`，`DEV` 与 `RES/COMP/OPS` 的统计方式有什么不同？
10. `/api/public/contributors` 和 `/api/public/contribution/wall` 的数据来源、用途和排序有什么不同？
11. 协会介绍为什么前后端都清洗 HTML？前端清洗能不能替代后端清洗？
12. 修改协会介绍后为什么要清 `public_about`，轮播图保存后为什么要清 `public_carousel`？
13. 当前轮播图保存真实要求哪个等级？为什么不能照抄旧注释“部长及以上”？
14. `ContributionController.award` 为什么保持同步？
15. `ContributionTask` 为什么同时使用 Redis 锁和数据库幂等键？

#### 完成标准

能从三个真实 `@LogContribution` 方法追到 AOP、异步线程池和 `sys_contribution_log`；能把 `selectWall` 的 SQL 翻译成每个 VO 字段；能解释公开目录、贡献墙、协会介绍、轮播图四条接口的区别；能说清 `SecurityContext` 跨线程、Spring 代理、自调用、缓存失效、HTML 白名单和幂等键，而不是只说“用了 AOP/Redis”。

### Day 12：测试

#### 今日目标

学完后，要能看见一个测试类就判断它测的是纯 Java 逻辑、Service 协作、Spring MVC/Security 链路、真实基础设施，还是浏览器端流程；能解释 Mock 的边界；能独立运行全部测试和单个测试；失败时知道先看哪一层，而不是只记住“项目有 133 个测试”。

#### 前置名词

| 名词 | 解释 |
| --- | --- |
| 测试用例 Test Case | 对一个明确行为安排输入、执行代码并断言结果。一个 `@Test` 方法通常就是一个用例。 |
| JUnit 5 | Java 测试框架。负责发现 `@Test`、运行生命周期方法、报告通过/失败/跳过。 |
| AssertJ | 断言库，提供 `assertThat(...).isEqualTo(...)`、`assertThatThrownBy(...)` 等可读写法。 |
| BCrypt | 带随机盐、可调成本的单向密码哈希算法。相同密码每次生成的摘要通常不同，验证时由 `PasswordEncoder.matches(明文, 摘要)` 计算并比较，不能从摘要“解密”出原密码。 |
| 单元测试 Unit Test | 只验证一个较小单元，依赖用假对象替代，不启动完整应用，速度快、定位清楚。 |
| 集成测试 Integration Test | 验证多个真实组件一起工作，例如 Spring Security Filter + MVC，或 Flyway + MySQL + Redis。 |
| Mock | 可编程的假对象。测试用 `when(...).thenReturn(...)` 规定返回，用 `verify(...)` 检查调用，不访问真实数据库/邮件等外部系统。 |
| Stub | 主要负责提供预设返回值的假实现。Mockito 的 Mock 可以同时承担 stub 和交互验证。 |
| `@Mock` | 让 Mockito 创建一个普通 Mock；它不进入 Spring 容器。 |
| `@InjectMocks` | 让 Mockito 创建被测对象，并把当前测试里的 `@Mock` 注入构造器或字段。 |
| `@ExtendWith(MockitoExtension.class)` | 让 JUnit 5 在测试前初始化 Mockito 注解。 |
| `@MockBean` | 在 Spring 测试上下文中用 Mock 替换同类型 Bean。Controller 测试用它阻断真实数据库、邮件或外部服务。 |
| `when` | 规定 Mock 在某个调用下返回什么或抛什么。 |
| `verify` | 断言某个依赖是否被调用、调用次数和参数；`never()` 可证明权限失败后业务方法没有执行。 |
| `ArgumentCaptor` | 捕获传给 Mock 的真实参数，用于检查 Service 最终组装出的 Entity 或查询对象。 |
| `@SpringBootTest` | 启动 Spring Boot 测试上下文，Bean、配置、AOP、Security 等可参与测试；比纯单元测试更慢。 |
| `@AutoConfigureMockMvc` | 在 Spring 测试中创建 `MockMvc`，不用真实监听端口也能模拟 HTTP 请求。 |
| `MockMvc` | 对 Servlet/Spring MVC 管线发模拟请求，可断言 HTTP 状态、Header、JSON、Filter 和 Controller 行为。 |
| `@WithMockUser` | Spring Security Test 提供的模拟登录用户；可指定 username、roles 或 authorities。它不会自动创建数据库 User。 |
| H2 | 测试 profile 使用的内存数据库，启动快，但 SQL 行为不完全等于 MySQL。 |
| Testcontainers | 测试时用 Docker 临时启动真实 MySQL、Redis 等容器，跑完销毁。比 H2 更接近生产，但要求 Docker 可用。 |
| Test Profile | `application-test.yml` 和 Maven Surefire 把测试环境切到 `test`，避免连接真实生产资源。 |
| Fixture | 测试准备的数据、文件或容器状态，例如临时上传文件、V1 旧用户记录。 |
| Flaky Test | 同样代码有时过、有时失败的测试。它说明测试输入或环境不确定，不能靠“重跑一次”解决。 |
| Vitest | 前端 TypeScript 单元测试框架；当前用于验证 Axios 响应拆包与错误对象。 |
| Playwright | 浏览器端 E2E 框架；真正启动页面并验证公开页、重定向、登录、CSRF、权限和上传。 |

#### 测试依赖和运行入口

后端 `pom.xml` 中与测试直接相关的依赖：

| 依赖 | 作用 |
| --- | --- |
| `spring-boot-starter-test` | 汇总 JUnit 5、AssertJ、Mockito、Spring Test 等常用测试能力 |
| `spring-security-test` | `@WithMockUser`、Security MockMvc 处理器等安全测试工具 |
| `h2` | 默认测试上下文的内存数据库 |
| `testcontainers` | Testcontainers 核心生命周期与 Docker 接入 |
| `junit-jupiter`（Testcontainers） | 把容器生命周期接进 JUnit 5 |
| `mysql`（Testcontainers） | `MySQLContainer` 支持 |

前端 `package.json` 中：

```text
npm test      → vitest run
npm run test:e2e → playwright test
```

`vitest.config.ts` 只包含 `src/**/*.test.ts(x)`，环境是 Node；`playwright.config.ts` 的 E2E 在 Chromium 中运行，默认会启动 `127.0.0.1:3000` 的 Next.js 开发服务器，也可用 `E2E_BASE_URL` 指向已部署环境。

#### 五类测试怎么区分

| 类型 | 典型标志 | 当前代表文件 | 真正验证什么 |
| --- | --- | --- | --- |
| 纯 Java/工具单元测试 | 没有 Spring 类级注解，直接 `new` 被测对象 | `JwtUtilsTest`、`PageUtilsTest`、`AccountNormalizerTest`、`SecurityStartupValidatorTest`、`ProductionStartupValidatorTest` | 输入输出、边界值、异常和配置规则 |
| Mockito Service 单元测试 | `@ExtendWith(MockitoExtension.class)`、`@Mock`、`@InjectMocks` | `ResourceServiceTest`、`CompetitionServiceTest`、`DeptServiceTest`、`VoteServiceTest`、`AccountServiceTest` | Service 业务分支、传给 Mapper 的数据、依赖是否被调用 |
| Spring MVC/Security 测试 | `@SpringBootTest` + `@AutoConfigureMockMvc`，常配 `@MockBean` | `StoredFileControllerTest`、`CompetitionControllerAuthorizationTest`、`AuthControllerRateLimitTest`、`CommonErrorResponseTest` | 请求绑定、Filter、CSRF、权限、异常 JSON、Controller 是否被拦截 |
| 定制 Spring 安全集成测试 | `@SpringJUnitConfig`、手工 `MockMvcBuilders`、导入安全 Bean | `SecurityErrorResponseTest` | 401/403、CORS、HSTS/CSP 等 Security 组件协作 |
| 真实基础设施测试 | `@Testcontainers`、`MySQLContainer`、`GenericContainer` | `FlywayMySqlRedisIntegrationTest` | 真 MySQL 上迁移与数据升级、真 Redis round trip |
| 前端单元测试 | `describe/it/expect` + Vitest | `src/lib/axios.test.ts` | 成功 envelope 拆包、业务错误、HTTP 错误 |
| 浏览器 E2E | Playwright `test` + 页面/API context | `e2e/critical-workflows.spec.ts` | 页面能否访问、未登录跳转、真实登录、CSRF、角色权限、上传 |

这里的“Controller 测试”不是只测 Controller 方法体。只要用了完整 Spring Security/MockMvc 链，请求会先经过 Filter、异常处理、方法授权，再决定是否进入 Controller，因此它更接近 Web 层集成测试。

#### 重点测试文件的职责

| 文件 | 它守住的风险 |
| --- | --- |
| `CommonErrorResponseTest` | 畸形 JSON、Validation、缺参数、运行时异常都返回稳定 `R` JSON、`errorCode` 和 `traceId` |
| `SecurityErrorResponseTest` | 未登录 401、无权限 403、Token 内部异常 500、CORS 预检和安全响应头不能漂移 |
| `CompetitionControllerAuthorizationTest` | 公开接口匿名可读；角色、Cookie CSRF、撤销 Token、数据库角色降级和对象级授权不能绕过 |
| `StoredFileControllerTest` | 文件所有者、已发布资源、元数据归属和 ACTIVE 状态共同决定能否下载 |
| `AuthControllerRateLimitTest` | 登录/注册限流、数据库故障、身份与注册边界返回正确结果 |
| `ResourceServiceTest` | 分页上限、Entity→VO、资源归属、字段标准化、原子下载计数 |
| `CompetitionServiceTest` | 授权编辑、公开未发布隐藏、详情内容、摘要清洗、枚举状态必须返回数字 code |
| `DeptServiceTest` | 任命时旧部长降级、新部长升级、部门 leaderId 同步 |
| `VoteServiceTest` | `ROOT_APPLY` 创建被拒绝；历史 ROOT_APPLY 即使通过也不会自动提权 |
| `GitServiceTest` | 仓库 URL 只允许 HTTPS 和白名单主机，拒绝 userinfo、内网 IP、非标准端口、SSH/file 等危险地址 |
| `JwtUtilsTest` | 密钥最短 32 字节、签发解析、payload 和签名篡改必须失败 |
| `SecurityStartupValidatorTest` | 直接构造 `SecurityStartupValidator`，验证任何环境下 `SameSite=None` 都必须配 `Secure=true`，以及 `prod/production` 不能使用不安全认证 Cookie |
| `ProductionStartupValidatorTest` | 直接构造 `ProductionStartupValidator`，验证非生产环境不强制部署项；生产必须配置 HTTPS 公网地址、Redis、可信代理 CIDR、forwarded headers 和 CSRF |
| `SchemaConsistencyTest` | Entity 字段与所有 Flyway migration 列做双向比较，防止新环境出现 `Unknown column` |
| `SeedDataPasswordTest` | `seed.sql` 只能保留运行时密码占位符，不能提交固定 BCrypt 或共享明文口令 |
| `AsyncMailSenderTest` | 邮件状态流转、最多重试、最终失败后清验证码和限流键 |
| `ScheduledJobServiceTest` | Redis 锁挡并发、数据库幂等键挡重复、失败状态允许后续重试 |
| `FlywayMySqlRedisIntegrationTest` | 用真实 MySQL/Redis 验证空库迁移、旧数据升级和 Redis 读写，而不是只相信 H2 |
| `src/lib/axios.test.ts` | 前端响应拦截器确实返回 `payload.data`，且 HTTP/业务错误不会被误判成功 |
| `e2e/critical-workflows.spec.ts` | 最终用户真正看到的页面、重定向、登录、CSRF、权限和上传链路 |

#### 读一个 Mockito Service 测试的方法

以 `ResourceServiceTest` 为例：

```text
@Mock ResourceMapper / AuditService
→ @InjectMocks 创建 ResourceService
→ when(...) 安排 Mapper 返回
→ 调用 resourceService 的真实方法
→ assertThat(...) 检查返回或异常
→ verify(...) 检查是否 insert/update
→ ArgumentCaptor 捕获写入对象或 Wrapper
```

这里真实执行的是 `ResourceService`，假的只是它的外部依赖。Mock 不应该把被测类本身也替掉，否则测试只是在验证自己写的假答案。

`verify(..., never())` 对权限测试尤其重要：只断言 HTTP 403 还不够，还要证明受保护的 Service/Mapper 没有执行。否则可能出现“响应是 403，但业务写入已经发生”的严重错误。

#### 读一个 MockMvc 测试的方法

以 `CompetitionControllerAuthorizationTest` 的 Cookie 写请求为例：

```text
生成真实 JWT
→ MockMvc POST /api/biz/comp/save
→ 放入 CSA_AUTH_TOKEN Cookie
→ 不放 X-CSRF-Token
→ JwtAuthenticationFilter 建立认证
→ CsrfProtectionFilter 拒绝
→ 断言 HTTP 403
→ verify(competitionService, never())
```

补上匹配的 `CSA_CSRF_TOKEN` Cookie 和 `X-CSRF-Token` Header 后，预期请求进入 Controller，并验证 `competitionService.saveCompetition` 收到数据库中的真实 userId。

`@WithMockUser` 适合只测试权限表达式；需要验证项目 JWT Filter、吊销或 sessionVersion 时，应生成项目真实 Token，而不是只用 `@WithMockUser` 绕过认证链。

#### 为什么外部依赖要 Mock，什么时候不能 Mock

适合 Mock：

- Service 单元测试里的 Mapper、邮件发送器、审计写入；
- Controller 权限测试里的数据库 Mapper 和业务 Service；
- 目标是验证调用顺序、参数或分支，而不是基础设施本身。

不能只 Mock：

- Flyway SQL 能不能在 MySQL 执行；
- Redis 序列化、锁和实际 round trip；
- 浏览器是否真的带 Cookie、跳转、渲染页面；
- Docker/Caddy/同源部署是否可启动。

因此项目同时保留单元测试、Spring Web 测试、Testcontainers 和 Playwright。层次越高越慢、环境要求越多，但能发现低层 Mock 看不到的问题。

#### 三个值得记住的测试故事

1. `SchemaConsistencyTest`

   它从 `@TableName` 读取表名，把 Entity 字段转成 snake_case；再解析所有 `V数字__*.sql` 的 `CREATE TABLE` 和 `ALTER TABLE ADD COLUMN`，做双向 diff。防止本地手工改过表、换机器按迁移初始化时才报 `Unknown column`。

2. `SeedDataPasswordTest`

   它确认 `db/seed.sql` 含 `__DEMO_PASSWORD_HASH__`，且不含固定 BCrypt 格式或历史共享口令。真正演示密码只在 dev/test 启动时由 `DevSeedDataInitializer` 用 `DEMO_SEED_PASSWORD` 生成临时哈希。

3. `JwtUtilsTest` 的 flaky 修复

   HS256 签名是 32 字节，Base64URL 后通常 43 字符；最后一个字符包含未使用 bit。过去只改签名末字符时，有些字符变化解码后字节并没变，测试会偶发“篡改后仍通过”。当前测试改为：一例直接篡改 payload；另一例翻转签名首字符的高位，保证字节一定变化。方法论是把随机失败条件变成确定性输入，而不是重跑。

#### 运行命令和当前基线

后端全部测试：

```powershell
cd D:\CSA-Project\csa-official-backend
.\mvnw.cmd test
```

单个测试类或多个类：

```powershell
.\mvnw.cmd "-Dtest=ResourceServiceTest" test
.\mvnw.cmd "-Dtest=CommonErrorResponseTest,SecurityErrorResponseTest" test
```

显式真实 MySQL/Redis/Flyway：

```powershell
.\mvnw.cmd "-Dit.containers=true" "-Dtest=FlywayMySqlRedisIntegrationTest" test
```

前端：

```powershell
cd D:\CSA-Project\csa-official-frontend
npm test
npm run lint
npm run build
npm run test:e2e
```

截至 **2026-08-26**，本轮在当前工作区实际执行 `mvnw.cmd test` 的结果是：**174 tests，0 failures，0 errors，1 skipped，BUILD SUCCESS**。跳过的是 `FlywayMySqlRedisIntegrationTest`：它要求显式 `-Dit.containers=true`，并且本机本轮没有可用 Docker 环境。历史上通过过不能替代当前源码的重新验证；Docker 恢复后要再跑显式命令。

本机 `java -version` 和 Maven 实际使用的是 **JDK 21.0.8**，但项目 `pom.xml` 的 `<java.version>` 仍是 **17**，后端 Docker 构建阶段也使用 Temurin 17。前者表示“这次本地测试运行在 JDK 21 上”，后者才是项目声明的编译/运行目标；不能因为电脑安装了 JDK 21 就把项目版本回答成 Java 21。

同一轮前端验证结果是：干净 `npm ci` 后，`npm test` **6 files、12 tests passed**，`npm run lint` exit code 0（保留 1 条既有导航 warning），`npm run build` 使用 Next.js 16.3.3 并完成页面生成 **25/25**，完整与 production `npm audit` 都是 0 vulnerabilities。Playwright 第一次因本机缺 Chromium 失败；安装 Playwright Chromium 151 后再次执行，又在等待配置的 Next.js dev server 时达到 120 秒超时。因此当前 E2E 仍是“环境阻断、未通过”，不能写成全绿。即使 dev server 正常启动，缺少 `E2E_USERNAME/E2E_PASSWORD` 时三个认证用例也会按设计跳过，只运行公开页和未登录跳转用例。

这次 CI 修复也说明一个容易误判的点：日志显示 `test` profile 已激活，不代表所有测试基础设施都已经隔离。Spring 的 Redis 自动配置仍会在空 `REDIS_HOST` 下尝试创建连接工厂。正确修复是只在 `application-test.yml` 排除 Redis 自动配置，并在 `CsaOfficialApplicationTests` 断言不存在 `RedisConnectionFactory`；不要把生产 profile 改成 memory 来掩盖 CI 问题。这个 checkpoint 不含 Flyway migration、数据库结构或生产环境变量变更，回退时使用 Git revert 并重新执行 `npm ci`。

#### 常见混淆

- “用了 Mockito”不等于“这个测试是假的”。被测业务类仍是真实执行，只有外部依赖是假对象。
- `@Mock` 不进 Spring 容器；`@MockBean` 会替换 Spring Bean。两者不能随便互换。
- `MockMvc` 不监听真实 TCP 端口，但会执行配置进测试上下文的 MVC、Filter、Security 和异常处理链。
- H2 测试通过不证明 MySQL migration 一定正确；这正是 Testcontainers 存在的原因。
- 测试日志里故意打印 ERROR 不等于测试失败。应看 Surefire 最终的 failures/errors 和进程退出码。
- `@WithMockUser` 创建的是测试认证上下文，不会验证 JWT 签名、Cookie、吊销或数据库 sessionVersion。
- E2E 中带登录凭据的用例会在缺 `E2E_USERNAME/E2E_PASSWORD` 时跳过；公开页和未登录跳转仍可执行。
- 测试总数会随源码变化，不能把 133 当永久常量；每次验收都以最新命令输出为准。

#### 动手验证

1. 只运行 `PageUtilsTest`，故意把一个期望值改错，观察失败报告定位到具体断言；恢复后再跑绿。
2. 在 `ResourceServiceTest` 中跟踪 `when → 真实 Service → ArgumentCaptor → verify`，能口头说明每一行是在安排、执行还是断言。
3. 跑 `CommonErrorResponseTest`，观察一次畸形 JSON 为什么返回 `MALFORMED_REQUEST`，而不是进入 Controller 正文。
4. 跑 `CompetitionControllerAuthorizationTest#memberCannotCreateCompetition`，确认 403 后 `competitionService` 从未调用。
5. 跑 `JwtUtilsTest` 多次，确认当前篡改用例是确定性的；再解释旧末字符方案为什么偶发失败。
6. 读 `SchemaConsistencyTest`，任选一个 Entity 字段，手工走一遍驼峰转 snake_case 和 migration 列对照。
7. 运行 `npm test`，从 Axios adapter 构造的假响应追到拦截器最终返回值。
8. Docker 可用时再运行显式 Testcontainers；检查 `flyway_schema_history` 最新版本与当前 migration 链一致。

#### 自测题

1. JUnit 5、AssertJ、Mockito 各自负责什么？
2. `@Mock`、`@InjectMocks`、`@MockBean` 有什么区别？
3. `when`、`verify`、`ArgumentCaptor` 分别用于测试的哪一步？
4. 为什么权限测试要同时断言 HTTP 403 和 `verify(service, never())`？
5. `@SpringBootTest + @AutoConfigureMockMvc` 会测试到哪些层？为什么仍不等于真实浏览器 E2E？
6. `@WithMockUser` 能验证什么，不能验证什么？
7. `ResourceServiceTest` 为什么比把所有逻辑留在 Controller 后再测更快、更清楚？
8. `SecurityStartupValidatorTest` 和 `ProductionStartupValidatorTest` 分别在防哪类错误配置？
9. `GitServiceTest` 为什么要拒绝 `file://`、内网 IP、带 userinfo 的 URL 和非标准端口？
10. `SchemaConsistencyTest` 如何处理 V1 建表和后续 `ALTER TABLE ADD COLUMN`？
11. `SeedDataPasswordTest` 为什么检查占位符和 BCrypt 格式，而不是把演示密码写死后尝试登录？
12. 当前 `JwtUtilsTest` 怎样保证 Token 篡改一定改变有效字节？
13. H2、Testcontainers MySQL、Playwright 三者各能发现什么独有问题？
14. 当前默认后端测试为什么有 1 个 skipped？如何显式运行它？
15. 前端 Vitest 当前钉死了 Axios 哪三个响应场景？

#### 完成标准

能把当前测试按“纯单元、Mockito Service、Spring Web/Security、真实基础设施、前端单元、浏览器 E2E”分类；能独立解释至少一个 Service 测试和一个 MockMvc 权限测试；能正确使用 `@Mock`、`@MockBean`、`when`、`verify`、`ArgumentCaptor`；能运行全部、指定类和显式 Testcontainers，并以最新输出而不是旧文档判断是否通过。

### Day 13：数据库结构

#### 今日目标

学完后，要能从当前 Flyway 版本链解释 17 张表怎么产生、Entity 如何映射列、逻辑删除和状态字段怎么设计、索引对应哪条真实查询，以及空库、旧库和演示数据分别由谁处理。数据库学习不能停在“用 MySQL”，要能说明结构如何可重复地交付到另一台机器。

#### 前置名词

| 名词 | 解释 |
| --- | --- |
| Schema | 数据库结构，包括表、列、类型、索引和约束；不是表里的业务数据。 |
| Migration | 按版本保存的一次结构或数据变更。Flyway 只执行尚未执行的版本。 |
| Flyway | 数据库迁移工具。应用启动时扫描 `db/migration`，检查 history 和 checksum，再按版本执行 SQL。 |
| Versioned Migration | 文件名形如 `V1__initial_schema.sql`。`V1` 是版本，双下划线后是描述。已发布文件不可原地改写。 |
| `flyway_schema_history` | Flyway 自动维护的历史表，记录版本、描述、checksum、安装顺序和是否成功。 |
| Checksum | 对 migration 内容计算的校验值。执行后再改旧文件会导致校验失败，防止不同环境的同一版本内容不一致。 |
| Baseline | 告诉 Flyway“这个旧库已有某个基础版本”，让它从更高版本继续迁移；不是自动把任意旧库修成正确结构。 |
| Forward-only | 已发布数据库变更通过新版本继续向前修复，不直接改旧 migration 或依赖 MySQL 自动回滚 DDL。 |
| DDL | 定义结构的 SQL，例如 `CREATE TABLE`、`ALTER TABLE`、`CREATE INDEX`。 |
| DML | 修改数据的 SQL，例如 `INSERT`、`UPDATE`、`DELETE`。V2 开头先规范化旧邮箱/学号就是 DML。 |
| Entity | Java 持久化实体。它表达一行数据库记录在 Java 中有哪些字段，但不等于前端应该直接收到的 VO。 |
| `@TableName` | MyBatis-Plus 的类级注解，明确 Entity 对应的表。例如 `User` 上的 `@TableName("sys_user")`。 |
| `@TableId` | MyBatis-Plus 的主键字段注解。当前实体使用 `type = IdType.AUTO`，表示插入时由 MySQL `AUTO_INCREMENT` 生成 id，再回填到对象。 |
| `@TableLogic` | MyBatis-Plus 的逻辑删除字段注解。对带该注解的 `deleted` 字段，常规删除会改标志，常规查询会过滤已删除行；它不会让所有表自动拥有逻辑删除。 |
| `@EnumValue` | MyBatis-Plus 的枚举持久化注解，指定枚举用哪个字段写入/读出数据库，例如存 `code`。它只管 ORM，不控制 Jackson 返回给前端的 JSON。 |
| `map-underscore-to-camel-case` | MyBatis 映射开关。当前为 `true`，使数据库 `create_time` 可自动映射 Java `createTime`；它不会重命名真实数据库列。 |
| snake_case | 下划线命名，例如数据库 `create_time`；Java 通常用 camelCase 的 `createTime`。 |
| 主键 Primary Key | 唯一标识一行。本项目多数主键是 `BIGINT AUTO_INCREMENT`，由 `@TableId(type = IdType.AUTO)` 对应。 |
| 唯一约束 UNIQUE | 不只是加速查询，还强制一组值不能重复，例如同一提案同一用户只能一票。 |
| 普通索引 INDEX | 主要优化特定 WHERE、ORDER BY 或 JOIN；不保证唯一。 |
| 联合索引 Composite Index | 由多个列按顺序组成，例如 `(category, create_time)` 同时服务分类过滤和时间排序。 |
| 逻辑删除 | 不删除物理行，只把 `deleted` 改为 1；MyBatis-Plus 自动给常规查询补“未删除”条件。 |
| 物理删除 | 真正执行 DELETE 或通过其它状态字段表示生命周期。没有 `@TableLogic` 的表不会自动走项目的 deleted 规则。 |
| 逻辑关联 | 表间用 ID 表示关系，但数据库不声明 FOREIGN KEY；一致性由 Service、查询条件、唯一约束和测试维护。 |
| Seed | 为开发/测试准备的演示数据，不属于生产结构迁移。 |
| Schema Drift | Entity、migration、结构快照或真实数据库之间出现不一致。 |
| `flyway repair` | 修复 Flyway 历史元数据的维护命令。它不撤销已经执行的 DDL、不恢复数据、也不会自动把错误表结构改正确；本项目只允许在人工确认目标结构与 migration 一致后，用它清理/校正 history。 |

#### 当前版本链

| 版本 | 文件 | 负责什么 |
| --- | --- | --- |
| V1 | `csa-official-backend/src/main/resources/db/migration/V1__initial_schema.sql` | 创建最初 12 张业务表及核心索引/唯一约束 |
| V2 | `csa-official-backend/src/main/resources/db/migration/V2__production_operations.sql` | 规范化旧用户数据、增加账号生命周期字段与唯一约束，再创建审计、文件、邮件、定时任务 4 张运营表 |
| V3 | `csa-official-backend/src/main/resources/db/migration/V3__resume_review_queue_index.sql` | 给现有 `biz_resume` 增加 `(status, update_time)` 索引，服务部长审核队列 |
| V4 | `csa-official-backend/src/main/resources/db/migration/V4__account_storage_and_git_sync.sql` | 增加账号匿名化时间、`sys_file_usage` 原子配额计数表、邮件恢复索引和简历 Git 同步字段 |
| V5 | `csa-official-backend/src/main/resources/db/migration/V5__contribution_award_audit.sql` | 为贡献流水增加 `source`、`awarded_by` 和后台历史索引 |

V3 和 V5 不增加新表，V4 新增 `sys_file_usage`，所以当前是 **17 张表**。其中 16 张有对应 Entity；配额计数表只由 `FileUsageMapper` 执行定向 SQL，不需要把计数行暴露成业务 Entity。`db/schema.sql`、Flyway 运维文档和 `FlywayMySqlRedisIntegrationTest` 已同步到 V5；真实 MySQL 验证仍要在 Docker 恢复后重跑。

#### 17 张表怎么分类

| 模块 | 表 | 数量 |
| --- | --- | ---: |
| sys | `sys_user`、`sys_dept`、`sys_carousel`、`sys_invite_code`、`sys_proposal`、`sys_resource`、`sys_config`、`sys_vote_record`、`sys_contribution_log`、`sys_audit_log`、`sys_stored_file`、`sys_file_usage`、`sys_mail_delivery`、`sys_scheduled_job_execution` | 14 |
| biz | `biz_competition`、`biz_comp_editor` | 2 |
| resume | `biz_resume` | 1 |

表前缀表达业务归属，不代表 Java package 名一定完全相同。例如简历 package 是 `modules/resume`，历史表名仍叫 `biz_resume`。

#### 文件、类和配置职责

| 路径或对象 | 它是什么 | 负责什么 | 调用关系 |
| --- | --- | --- | --- |
| `application.yml` 的 `spring.flyway` | Flyway 启动配置 | 开关、迁移位置、baseline、validate、clean 和 out-of-order 规则 | Spring Boot 启动时自动创建 Flyway 并迁移 |
| `application.yml` 的 `mybatis-plus` | ORM 映射配置 | 开启下划线到驼峰映射，声明逻辑删除字段名为 `deleted` | Mapper 查询/删除时使用 |
| `V1__initial_schema.sql` | 初始 migration | 建 12 张表，空库从这里开始 | Flyway 第一次迁移执行 |
| `V2__production_operations.sql` | 运营 migration | 更新旧数据、ALTER `sys_user`、建 4 张运营表 | V1 之后执行 |
| `V3__resume_review_queue_index.sql` | 索引 migration | 在现有表上新增审核队列索引 | V2 之后执行 |
| `modules/**/entity/*.java` | 16 个 Entity | 定义 Java 字段、表名、主键、枚举和逻辑删除标记 | MyBatis-Plus Mapper 使用；`sys_file_usage` 是 mapper-only 计数表 |
| `FileUsageMapper` | 原子配额 SQL Mapper | 创建计数行、条件预占、释放计数 | `FileAccountingService` 在文件元数据事务中调用 |
| `db/schema.sql` | 当前结构快照 | 供学习、人工核对和本地查看；不是生产升级入口 | 人读取，不由生产 Flyway 版本链替代 |
| `db/seed.sql` | dev/test 演示数据 | 插入角色、部门、资源等演示数据，密码保留运行时占位符 | `DevSeedDataInitializer` 读取 |
| `DevSeedDataInitializer` | dev/test 启动器 | 只在 dev/test + 显式开启时，用 BCrypt 替换密码占位符并执行 seed | 应用启动后运行 |
| `SchemaConsistencyTest` | 结构漂移测试 | Entity 字段和所有 migration 的建表/加列做双向 diff | `mvn test` 自动执行 |
| `SeedDataPasswordTest` | Seed 安全测试 | 阻止固定 BCrypt 和共享口令进入仓库 | `mvn test` 自动执行 |
| `FlywayMySqlRedisIntegrationTest` | 显式基础设施测试 | 用真实 MySQL 跑迁移和旧数据升级，并验证 Redis | `-Dit.containers=true` 时运行 |
| `docs/database.md` | 数据库说明 | 表关系、字段约定、索引依据和初始化说明 | 学习辅助，若与 migration 冲突以 migration 为准 |
| `docs/production-readiness/flyway.md` | 迁移运行手册 | 空库、旧库 baseline、失败迁移和向前修复流程 | 部署人员使用 |

#### 精确阅读顺序

1. 先读 `application.yml` 的 `spring.flyway` 和 `mybatis-plus`，知道框架何时介入。
2. 只看 V1 的 12 个 `CREATE TABLE`，列出表名、主键、`deleted`、索引和唯一约束，不先钻每个字段。
3. 看 V2 前 23 行：先 `LOWER(TRIM(email))`、`TRIM(student_id)`，再加唯一键。理解为什么必须先清洗旧数据再收紧约束。
4. 看 V2 后四个 `CREATE TABLE`，分别对应审计、文件元数据、邮件投递、定时任务幂等。
5. 看 V3：它只有一次 `ALTER TABLE ... ADD INDEX`，说明 migration 不一定每版都建新表。
6. 看 V4：它新建 `sys_file_usage`，并在已有表上增加匿名化、邮件恢复和 Git 同步字段；再追 `FileAccountingService` 的条件更新。
7. 看 V5：它只为贡献流水增加来源和操作人字段，理解为什么旧数据统一为 `LEGACY`。
8. 用 `rg -n "@TableName|@TableLogic|@EnumValue" modules` 对照 Entity，确认哪些列由注解决定。
9. 打开 `SchemaConsistencyTest`，看它如何解析 `CREATE TABLE` 和 `ALTER TABLE ADD COLUMN`。注意它当前比较列，不验证索引、类型、默认值或外键。
10. 打开 `db/seed.sql` 和 `DevSeedDataInitializer`，确认 seed 只在 dev/test、显式开关和至少 12 字符临时密码下执行。
11. 最后读 `flyway.md` 的空库、baseline 和失败恢复。先理解流程，再背命令。

#### 空库启动链路

```text
部署系统先创建空数据库和独立应用用户
→ backend 启动
→ Spring Boot 读取 spring.flyway 配置
→ Flyway 连接目标数据库
→ 没有 flyway_schema_history：创建 history
→ 按版本执行 V1 → V2 → V3 → V4 → V5
→ 每个成功版本写 history 和 checksum
→ migration 全部成功后应用继续启动
→ migration 失败则 readiness 不应通过
```

Flyway 不负责创建 MySQL 服务、数据库账号或目标数据库本身。Compose 中的 MySQL 环境变量先创建数据库和应用用户，Flyway 只管理这个库内部的结构。

#### 已有库升级和 baseline

情况一：数据库已经由 Flyway 管理。

```text
读取 flyway_schema_history
→ 校验已执行 migration 的 checksum
→ 只执行更高版本
```

情况二：旧库在引入 Flyway 前已经手工建好，并且结构确认等同 V1。

```text
临时 FLYWAY_BASELINE_ON_MIGRATE=true
→ baseline-version=1
→ Flyway 把旧结构登记为 V1
→ 继续执行 V2、V3
→ 验证 history 后立刻恢复 baseline-on-migrate=false
```

Baseline 不是“发现什么表都算 V1”。旧库不等于 V1 时，必须先盘点差异、备份并写兼容 migration；不能用 baseline 或 `repair` 掩盖错误结构。

#### 逻辑删除的真实范围

当前 8 张表有 `@TableLogic deleted`：

```text
sys_user
sys_dept
sys_carousel
sys_invite_code
sys_proposal
sys_resource
biz_competition
biz_resume
```

当前另外 8 张表没有项目统一的 `deleted` 列：

```text
sys_config
sys_vote_record
sys_contribution_log
biz_comp_editor
sys_audit_log
sys_stored_file
sys_mail_delivery
sys_scheduled_job_execution
```

这里不能简单说“没有 deleted 就一定直接 DELETE”。例如 `sys_stored_file` 自己有 `status` 和 `deleted_at`，通过 `markDeleted` 做文件生命周期；`sys_audit_log` 设计为不可变记录；投票和贡献流水也有各自保留语义。判断删除方式要看 Entity、Mapper 和 Service，不能只看列名。

MyBatis-Plus 的逻辑删除配置：

```yaml
mybatis-plus:
  global-config:
    db-config:
      logic-delete-field: deleted
```

Entity 仍需声明 `@TableLogic`。配置告诉框架字段名，注解告诉它哪些实体真正使用该规则。

#### 为什么没有物理外键

项目表之间使用逻辑 ID 关系，但 migration 没有 `FOREIGN KEY`。当前设计理由是：

- 多个核心表使用逻辑删除，数据库外键只能看到物理行是否存在，无法表达 `deleted=1` 已经“业务删除”；
- 删除、状态流转和历史保留由各 Service 控制，不希望数据库级联在框架外自动删除相关数据；
- 关联查询必须自己过滤逻辑删除，例如贡献墙 SQL 明确写 `u.deleted=0`、`d.deleted=0`；
- 一致性规则通过事务、唯一约束、Service 检查、审计和测试维护。

代价也要承认：数据库不会自动阻止孤儿 ID，因此 Service 和清理任务必须认真维护关系。没有外键不是“数据库会自动处理”，而是把责任移到了应用层。

#### 枚举与列类型

判断落库类型要看 Entity 字段和 `@EnumValue`：

| Java 字段 | 数据库列 | 原因 |
| --- | --- | --- |
| `Competition.status: CompetitionStatusEnum` | `INT` | 枚举的 int `code` 标了 `@EnumValue` |
| `VoteRecord.result: VoteResultEnum` | `INT` | `REJECT=0`、`AGREE=1` 的 code 落库 |
| `ContributionLog.type: String` | `VARCHAR(16)` | Entity 本身是 String，写入 `DEV/RES/COMP/OPS` 名字 |
| `Proposal.status: Integer` | `INT` | 普通整数状态 0/1/2，不是枚举字段 |
| `StoredFile.status: String` | `VARCHAR(24)` | 使用 `ACTIVE/DELETED` 等字符串生命周期状态 |

不能因为业务上“看起来是枚举”就断定 VARCHAR，也不能看到 Java enum 就断定存名字。

#### 索引和唯一约束要对应真实查询

| 索引/约束 | 服务的代码路径 |
| --- | --- |
| `uk_user_username(username)` | 登录、注册用户名查重 |
| `uk_user_email(email)` / `uk_user_student_id(student_id)` | 账号唯一身份；V2 在规范化旧值后建立 |
| `idx_user_department(department_id)` | 用户目录按部门筛选 |
| `idx_user_role_level(role_level)` | 公开核心成员按等级过滤/排序 |
| `idx_resource_category_time(category, create_time)` | 资源列表按分类等值过滤，再按创建时间倒序 |
| `idx_comp_status_time(status, update_time, create_time)` | 公开竞赛过滤状态并按更新时间/创建时间排序 |
| `uk_editor_comp_user(competition_id, user_id)` | 同一竞赛不重复授权同一编辑者 |
| `uk_vote_proposal_voter(proposal_id, voter_id)` | 同一用户对同一提案最多投一次，防并发穿透 Service 查重 |
| `idx_contrib_user_type(user_id, type)` | 贡献流水按用户和类型聚合 |
| `uk_config_key(config_key)` | `selectOne(config_key)` 保证一个配置键只有一行 |
| `idx_carousel_status_sort(status, sort_order, create_time)` | 首页只取启用轮播并按顺序展示 |
| `idx_proposal_create_time(create_time)` | 提案列表按创建时间倒序 |
| `idx_resume_status_update(status, update_time)` | V3 新增；审核队列按状态筛选并按更新时间读取，业务细节 Day 9 再讲 |
| `uk_stored_file_key(storage_key)` | 一个存储 key 对应一份文件元数据 |
| `uk_job_idempotency(job_name, idempotency_key)` | 定时任务同一业务版本只能成功认领一次 |

联合索引顺序有意义。`(category, create_time)` 适合“category 等值 + create_time 排序”，不等同于给两个字段各建一个完全独立的索引。

#### Seed 与生产 migration 的边界

```text
生产结构：Flyway V1-V5
开发/测试演示数据：db/seed.sql
执行者：DevSeedDataInitializer
启用条件：profile 为 dev/test 且 csa.seed.enabled=true
密码条件：DEMO_SEED_PASSWORD 至少 12 字符
处理方式：运行时 BCrypt → 替换 __DEMO_PASSWORD_HASH__ → 执行 SQL
```

生产 profile 不创建共享演示账号，seed 也不能混进 V1-V5。结构迁移必须在任何环境都安全；演示数据只服务本地学习和测试。

#### 迁移失败怎么处理

MySQL 的许多 DDL 不能保证像普通业务事务一样完整回滚。标准思路：

1. 停止 backend 和写流量；
2. 保存日志、history 和当前备份；
3. 查清哪些 DDL 已经落地；
4. 在隔离副本恢复备份或写更高版本的向前修复 migration；
5. 只有结构已人工确认一致时才使用 `flyway repair` 修 history；
6. 不修改已经发布的旧 migration。

`repair` 只修复 Flyway 元数据，不会替你撤销已经执行一半的 ALTER TABLE。

#### 动手验证

1. 用 `rg "^CREATE TABLE" V1/V2/V4` 手工数出 V1 的 12 张表、V2 的 4 张表和 V4 的 1 张配额计数表，再确认 V3/V5 不建表。
2. 在 16 个 Entity 中统计 `@TableLogic`，与上面的 8/8 清单对照。
3. 任选 `Resource.category/createTime`，从 Service 查询条件追到 V1 的联合索引。
4. 任选 `VoteRecord.result`，从 Java enum 的 `@EnumValue` 追到数据库 INT。
5. 运行 `SchemaConsistencyTest`，再临时给测试副本中的一个 Entity 加字段观察它如何失败；恢复改动。
6. 在隔离空库启动后查询：

```sql
SELECT installed_rank, version, description, success
FROM flyway_schema_history
ORDER BY installed_rank;
```

当前源码期望依次看到 1、2、3、4、5，而不是只到 2 或 3。

7. 只在 dev/test 开启 seed，确认缺少或短于 12 字符的 `DEMO_SEED_PASSWORD` 会拒绝启动。
8. Docker 可用后显式跑 Testcontainers；确认最新版本断言为 V5，并验证 `idx_resume_status_update`、`sys_file_usage` 和贡献流水的 `source` / `awarded_by` 在真实 MySQL 中存在。

#### 自测题

1. Flyway、migration、`flyway_schema_history`、checksum 分别是什么？
2. 当前为什么是 V1→V2→V3→V4→V5？每一版分别做了什么？
3. 当前有多少张表？sys、biz、resume 各多少张？
4. V2 为什么先规范化 email/studentId，再创建唯一约束？反过来有什么风险？
5. `db/schema.sql`、`db/seed.sql`、Flyway migration 三者用途有什么区别？
6. `@TableName`、`map-underscore-to-camel-case`、`@TableId(AUTO)` 分别影响什么？
7. 哪 8 张表使用 `@TableLogic`？没有 `deleted` 的表是否都采用直接 DELETE？
8. 逻辑删除为什么需要关联 SQL 主动过滤？没有物理外键带来了什么责任？
9. `CompetitionStatusEnum`、`VoteResultEnum` 和 `ContributionLog.type` 为什么落库类型不同？
10. `UNIQUE` 和普通索引的核心区别是什么？为什么投票需要唯一约束？
11. `(category, create_time)` 与 `(create_time, category)` 是否完全等价？当前查询为什么选择前者？
12. 空库初始化、Flyway 之前的旧库、正常已管理数据库分别怎么处理？
13. `baseline` 和 `repair` 各自不能替你完成什么？
14. `SchemaConsistencyTest` 能检查哪些漂移，不能检查哪些漂移？
15. 为什么 seed 只允许 dev/test，并使用运行时密码占位符？

#### 完成标准

能不看答案列出 V1-V5 的职责和 17 张表分类；能从一个 Entity 字段追到 migration 列、类型和索引；能解释逻辑删除、唯一约束、联合索引、无物理外键、baseline、checksum、forward-only 和 seed 边界；能在空库查询 Flyway history 并判断当前是否真正迁到 V5。

### Day 14：一键启动和部署

#### 今日目标

今天不是背“敲一条 Docker 命令就完事”，而是要能解释一套部署为什么能启动、什么时候算健康、请求怎样经过反向代理、数据放在哪里，以及哪一步会造成不可逆的数据丢失。本日只讲容器、部署和运行边界；简历业务仍留在 Day 9，暂不展开。

#### 阅读范围（以当前源码为准）

```text
docker-compose.yml                         # 本地开发四服务
compose.production.yml                     # 生产五服务
csa-official-backend/Dockerfile            # 后端镜像构建
csa-official-frontend/Dockerfile           # 前端镜像构建
deploy/Caddyfile                           # 生产同源 HTTPS 路由
docs/deployment.md                         # 本地一键启动说明
docs/production-readiness/runbook.md       # 生产发布和健康验收
docs/production-readiness/backup-restore.md
deploy/backup.ps1、deploy/restore.ps1      # 备份和恢复脚本
```

#### 前置名词、类和配置项

| 名词 / 类 / 配置 | 它是什么 | 在本项目中的作用 |
| --- | --- | --- |
| Docker | 打包和运行隔离进程的容器平台 | 把 MySQL、Redis、Java 后端和 Next.js 分开运行 |
| Image（镜像） | 启动容器所需的只读模板 | `mysql:8.0`、`redis:7-alpine` 是现成镜像；两个应用镜像由 Dockerfile 构建 |
| Container（容器） | 镜像的一次运行实例 | `backend` 服务的容器就是一个运行中的 Java 进程隔离环境 |
| Docker Compose | 用 YAML 描述多个容器、网络、卷和依赖的编排工具 | `docker compose up` 一次启动整套系统 |
| Service（服务） | Compose 文件中的一个可启动单元 | `mysql`、`redis`、`backend`、`frontend`、生产中的 `caddy` |
| Dockerfile | 构建镜像的逐步说明文件 | 先编译/构建，再把产物放进较小的运行镜像 |
| JDK | Java Development Kit，包含 Java 编译器、运行时和开发工具 | 后端 build stage 用 JDK 17 编译项目；本机装 JDK 21 不会改变 `pom.xml` 声明的 Java 17 目标 |
| JRE | Java Runtime Environment，只提供运行 Java 程序所需环境，不含完整开发工具链 | 后端 runner stage 用 `eclipse-temurin:17-jre-alpine` 运行 jar，不带 Maven 和完整 JDK |
| Maven | Java 依赖管理和构建工具 | 读取 `pom.xml`，下载依赖，并由 `mvn clean package -DskipTests` 生成 jar |
| Node.js | 在服务器/构建机执行 JavaScript 的运行时 | 前端镜像用 Node 20 安装依赖、执行 Next.js build，并在容器内运行 `next start` |
| npm | Node.js 包管理器和脚本入口 | `npm ci` 按 lock 文件安装依赖，`npm run build/start` 执行 `package.json` 中的脚本 |
| Build context | `docker build` 能看到的目录 | backend 的 context 是 `./csa-official-backend`，frontend 的是 `./csa-official-frontend` |
| Multi-stage build（多阶段构建） | 一个 Dockerfile 使用多个 `FROM` 阶段，最后只复制运行所需产物 | 后端不把 Maven 工具链带进运行镜像；前端把 devDependencies 和运行依赖分开 |
| `FROM` / `AS` | `FROM` 选择基础镜像并开始新阶段，`AS` 给阶段命名 | `FROM maven:... AS build` 创建后端编译阶段，之后可用 `COPY --from=build` 取产物 |
| `WORKDIR` | 设置后续 Dockerfile 指令和容器默认命令的工作目录 | 两个应用镜像都把工作目录设为 `/app` |
| `COPY` | 把 build context 中的文件复制进镜像，或从前一构建阶段复制产物 | 后端把 `target/*.jar` 从 build stage 复制到 runner；前端复制 `.next`、`public` 和生产依赖 |
| `RUN` | 在镜像构建时执行命令并生成镜像层 | 用于 Maven/npm 构建，以及创建非 root 用户和目录；它不是容器每次启动时重复执行的命令 |
| `ARG` | 仅构建期可用的参数，可为 Dockerfile 提供默认值 | frontend 在 `next build` 前接收 `NEXT_PUBLIC_API_URL`；普通 `ARG` 不自动成为运行期环境变量，也不应用来保存秘密 |
| `ENV` | 写入镜像或容器进程环境的变量 | 例如 `NODE_ENV`、`PORT`、`REDIS_SSL`；但客户端 `NEXT_PUBLIC_*` 已在 build 时固化，运行期同名变量不能重写已生成 bundle |
| `USER` | 指定后续指令和容器启动进程使用的操作系统用户 | backend 用 `csa`，frontend 用 `nextjs`，避免应用默认以 root 运行 |
| `EXPOSE` | Dockerfile 中对镜像监听端口的元数据说明 | `EXPOSE 8080/3000` 不发布宿主机端口，也不是防火墙规则；是否发布由 Compose `ports` 决定 |
| `ENTRYPOINT` | 镜像固定的主启动程序，运行容器时通常仍会执行 | backend 固定执行 `java -jar app.jar` |
| `CMD` | 镜像的默认命令或默认参数，运行容器时较容易被覆盖 | frontend 默认执行 `npm run start` |
| Volume（命名卷） | 由 Docker 管理、独立于容器生命周期的持久化目录 | `mysql-data`、`redis-data`、`backend-uploads` 保存数据库、缓存和上传文件 |
| Network（网络） | 容器之间通信的虚拟网络 | 生产 `edge` 给代理/应用，`data` 只给后端、MySQL、Redis，且 `data` 是 internal 网络 |
| `ports` | 把容器端口发布到宿主机 | 开发发布 3000/8080/3306/6379；生产只发布 Caddy 的 80/443 |
| `expose` | Compose 中声明容器预期提供的内部端口，但不发布到宿主机 | 生产声明 backend 8080、frontend 3000；同一网络的服务即使没有 `expose` 也可能直接通信，真正的边界来自网络成员关系和是否配置 `ports`，不能把 `expose` 当防火墙 |
| `depends_on` | 描述服务启动依赖 | 生产 backend 等 MySQL/Redis 健康，frontend 等 backend 健康，Caddy 等前后端健康 |
| `healthcheck` | 容器级健康探测命令和重试规则 | 不只看进程存在，还检查 MySQL、Redis、Spring readiness、Next 页面是否可访问 |
| `condition: service_healthy` | `depends_on` 的条件形式 | 只有依赖服务的 healthcheck 为 healthy 才继续启动依赖者 |
| `restart: unless-stopped` | 容器异常退出时自动重启，手动停止后不强行拉起 | 四/五个长期服务都使用它 |
| Caddy | Web server / reverse proxy（反向代理） | 自动 HTTPS，并把 `/api`、`/files` 转给 backend，其余路径转给 frontend |
| `reverse_proxy` | Caddy 的转发指令 | `reverse_proxy backend:8080` 使用 Compose 服务名在容器网络内转发 |
| TLS / HTTPS | 加密浏览器与服务器之间的连接 | 生产 Caddy 监听 443；后端生产 Cookie 要求 `Secure=true` |
| Cookie `Secure` | 告诉浏览器只通过 HTTPS 发送该 Cookie | 生产认证 Cookie 必须开启；它不负责加密 Cookie 内容本身 |
| Cookie `SameSite` | 控制跨站请求是否自动携带 Cookie，常见值有 Strict、Lax、None | 本项目默认 Lax；若设为 None，浏览器规范要求同时使用 Secure |
| CSP | Content Security Policy，限制页面可加载/执行的脚本、连接、图片等来源 | 当前主要由前端 `proxy.ts` 按请求生成 nonce 和 CSP Header，不是 Caddy 的 API 路由规则 |
| HSTS | Strict-Transport-Security，要求浏览器在有效期内只用 HTTPS 访问站点 | 生产 Caddy 明确添加该 Header；只能在已正确部署 HTTPS 的环境启用 |
| CIDR | 用“网络地址/前缀长度”表示一个 IP 范围 | `172.30.0.0/24` 表示生产 edge 子网，被配置为可信代理范围；不能随意写成所有来源 |
| Forwarded headers | 反向代理传给后端的原始协议、主机和客户端地址信息，例如 `X-Forwarded-Proto`、`X-Forwarded-For` | `server.forward-headers-strategy=framework` 让 Spring 解析这些 Header；必须和可信代理边界一起使用，不能无条件信任公网伪造值 |
| `liveness` | “进程还活着吗”的探针 | `/actuator/health/liveness` 只包含 Spring 的存活状态 |
| `readiness` | “现在能接流量吗”的探针 | `/actuator/health/readiness` 生产还纳入数据库和 Redis 状态 |
| Actuator | Spring Boot 的运行状态和指标端点模块 | 提供 health、info、Prometheus；生产不直接暴露给公网 |
| Prometheus | 通过定期抓取指标端点收集时间序列监控数据的系统/格式 | Actuator 可提供 Prometheus 指标给内部监控抓取；“端点能打开”不等于告警规则已经配置完成 |
| `Flyway` | 数据库版本迁移工具 | backend 启动时按 V1→V2→V3→V4→V5 执行未落地 SQL |
| `flyway_schema_history` | Flyway 的迁移历史表 | 记录已执行版本、checksum 和成功状态，决定下次从哪版继续 |
| `seed` | 开发/测试演示数据 | `db/seed.sql` 不是生产结构迁移，生产 profile 不执行 |
| `DevSeedDataInitializer` | Spring `ApplicationRunner` 实现类 | 仅在 `dev`/`test` 且 `csa.seed.enabled=true` 时，用临时密码生成 BCrypt 后执行 seed |
| `ApplicationRunner` | Spring Boot 启动回调接口；应用上下文建好后调用一次 `run(ApplicationArguments)` | `DevSeedDataInitializer.run` 在启动阶段检查 seed 文件和临时密码，再决定是否执行演示 SQL；它不是定时任务 |
| `@Profile` | 只有指定 Spring profile 激活时才注册 Bean | `@Profile({"dev", "test"})` 保证 seed 初始化器不会在 production profile 中创建 |
| `@ConditionalOnProperty` | 只有配置属性满足指定条件时才注册 Bean | seed 还要求 `csa.seed.enabled=true`；profile 和属性两个条件必须同时满足 |
| `ResourceDatabasePopulator` | Spring JDBC 提供的 SQL 资源执行器 | 将替换好 BCrypt 占位符的内存 SQL 对 `DataSource` 执行，`continueOnError=false` 表示出错即失败 |
| `JwtUtils` | 项目的 JWT 工具 Bean | `@PostConstruct validateSecret()` 启动时检查 JWT 密钥至少 32 个 UTF-8 字节 |
| `@PostConstruct` | Bean 完成依赖注入后执行一次的生命周期回调 | `JwtUtils.validateSecret()` 在应用接收请求前检查密钥，失败会阻止 Spring Boot 启动；它不是每次生成 Token 都运行 |
| HS256 | HMAC-SHA-256 的 JWT 对称签名算法，签发和验证使用同一秘密密钥 | 用签名发现 Header/Payload 被篡改，不加密 JWT 内容；本项目按 UTF-8 字节检查至少 256 bit（32 字节）密钥 |
| `SecurityStartupValidator` | Spring 启动校验 Bean | 检查 `SameSite=None` 是否同时设置 `Secure`，生产 Cookie 是否 `Secure=true` |
| `ProductionStartupValidator` | 生产 profile 启动校验 Bean | 检查 HTTPS 公网地址、Redis、可信代理、forward headers 和 CSRF |
| `mysqldump` | MySQL 的逻辑备份工具，把表结构和数据导出为可重新执行的 SQL | `backup.ps1` 在 MySQL 容器内生成压缩的 `database.sql.gz`；它不是磁盘卷的原始块级快照 |
| SHA-256 | 对文件内容计算 256 bit 摘要的密码学哈希算法 | 备份脚本生成 `SHA256SUMS`，恢复前检查文件是否改变；它不能加密备份，也不能证明备份业务上完整可用 |
| `read_only: true` | 容器根文件系统只读 | 生产 backend 不能随意写镜像层，只能写挂载卷或 `/tmp` tmpfs |
| `tmpfs` | 内存中的临时文件系统 | 给只读 backend 提供可写的 `/tmp`，容器删除后内容消失 |
| `no-new-privileges` | Linux 安全选项 | 防止进程通过 setuid 等方式获得额外权限 |

#### 两套 Compose 不是一套口径

##### 开发 Compose：四个服务

`docker-compose.yml` 的服务是：

```text
mysql      MySQL 8       宿主机回环地址 3306；命名卷 mysql-data
redis      Redis 7       宿主机回环地址 6379；命名卷 redis-data
backend    Spring Boot   宿主机 8080；上传目录 backend-uploads
frontend   Next.js       宿主机 3000；无持久化卷
```

开发启动链是：

```text
docker compose up --build
→ 构建 backend / frontend 镜像
→ 启动 mysql、redis
→ 两者 healthcheck 通过
→ 启动 backend
→ backend 的 Spring Boot / Flyway 启动
→ 启动 frontend
```

开发文件对 backend 使用 `condition: service_healthy`，所以 backend 不会在 MySQL/Redis 还没准备好时启动。开发 frontend 使用短写法 `depends_on: - backend`，它表达“先创建 backend 容器”，不等价于“backend readiness 已经 UP”；真正的健康等待主要由 backend 自己的 healthcheck 和日志验收完成。生产文件把 frontend 也改成了 `condition: service_healthy`。

##### 生产 Compose：五个服务

`compose.production.yml` 额外加入 Caddy：

```text
mysql → data 网络
redis → data 网络
backend → edge + data 网络，声明 expose 8080，不发布宿主机端口
frontend → edge 网络，声明 expose 3000，不发布宿主机端口
caddy → edge 网络，发布宿主机 80/443
```

生产启动顺序是：

```text
mysql healthy + redis healthy
→ backend 启动
→ backend readiness healthy
→ frontend 启动
→ frontend healthy
→ caddy 启动并接收公网流量
```

生产 backend 和 frontend 没有 `ports`，因此宿主机不直接发布它们；Caddy 是生产 Compose 唯一发布的 HTTP/HTTPS 入口。容器间能否互访主要由是否加入同一 Docker 网络决定，`expose` 只是内部端口声明，不提供访问控制。`data: internal: true` 表示该网络不直接连接外部网络，但它不是 TLS，不能把“网络隔离”误说成“链路加密”。

#### `healthcheck`、liveness、readiness 到底检查什么

`healthcheck` 是 Docker 反复执行的命令；`interval` 是检查间隔，`timeout` 是单次超时，`retries` 是连续失败次数，`start_period` 是给慢启动留出的宽限期。它决定 Compose 看到的 `healthy/unhealthy`，不是 Java 注解。

当前探测对象：

| 服务 | 当前探测 | 意义 |
| --- | --- | --- |
| mysql | `mysqladmin ping ...` | MySQL 进程已经能接受管理连接 |
| redis | `redis-cli ping`；生产还带密码认证 | Redis 能响应 PING |
| backend 开发 | `GET /actuator/health` | Spring 聚合健康端点可访问 |
| backend 生产 | `GET /actuator/health/readiness` | 应用已准备好接流量，且生产组包含 `db`、`redis` |
| frontend | `GET /` | Next.js HTTP 服务能返回首页 |

`liveness` 和 `readiness` 的区别：liveness 失败通常表示进程需要重启；readiness 失败表示先不要把请求送进来，进程本身可以继续等待依赖恢复。生产 Caddyfile 没有把 `/actuator` 转给 backend，且 backend 没有宿主机端口，所以 Actuator 应从部署网络内部用 `docker compose exec backend wget ...` 验收，不应公开到公网。

#### 后端 Dockerfile：为什么分成编译镜像和运行镜像

`csa-official-backend/Dockerfile` 的真实步骤：

```text
FROM maven:3.9-eclipse-temurin-17 AS build
→ WORKDIR /app，COPY 源码
→ mvn clean package -DskipTests 生成 target/*.jar

FROM eclipse-temurin:17-jre-alpine
→ 只带 Java 17 JRE，不带 Maven 和 JDK
→ 创建 UID/GID 10001 的 csa 非 root 用户
→ 创建 /app/uploads 并把目录交给 csa
→ COPY jar，USER csa
→ ENTRYPOINT ["java", "-jar", "app.jar"]
```

`AS build` 给阶段命名，`COPY --from=build` 从前一阶段复制产物；`JRE` 只负责运行 Java，体积和攻击面都比完整 JDK 小。`EXPOSE 8080` 只是镜像元数据声明，不会自动发布宿主机端口；是否能从宿主机访问，取决于 Compose 的 `ports`。镜像构建用了 `-DskipTests`，所以发布前仍必须单独运行 `mvnw test`，不能把“镜像构建成功”当成“测试通过”。

#### 前端 Dockerfile：四阶段和构建期变量

`csa-official-frontend/Dockerfile` 有四个阶段：

```text
deps      node:20-alpine，npm ci，安装构建依赖
builder   注入 ARG NEXT_PUBLIC_API_URL，执行 npm run build
prod-deps 再次 npm ci --omit=dev，只保留生产依赖
runner    非 root 用户 nextjs，执行 npm run start
```

`npm ci` 按 `package-lock.json` 做可重复安装；`--omit=dev` 不安装只用于构建/测试的开发依赖。当前 `next.config.ts` 没有 `output: 'standalone'`，所以运行阶段带 `.next`、`public`、生产 `node_modules`，用 `next start` 启动，而不是复制 standalone 目录。

`NEXT_PUBLIC_*` 是 Next.js 的特殊约定：在 `next build` 时会把值内联进浏览器 bundle。它不是“容器启动时每次读取”的普通服务端秘密。因此：

```text
开发 frontend.build.args.NEXT_PUBLIC_API_URL = http://localhost:8080
生产 frontend.build.args.NEXT_PUBLIC_API_URL = /
```

生产设为 `/` 是因为浏览器访问 Caddy 的同一个来源，前端请求 `/api/...`，再由 Caddy 转到 `backend:8080`。若误把浏览器端值写成 `http://backend:8080`，浏览器会尝试解析自己的网络环境中的 `backend`，通常直接失败；`backend` 只对容器网络里的 Caddy 或其它容器可解析。生产 Compose 同时给 frontend 传运行期 `NEXT_PUBLIC_API_URL=/`，供 `proxy.ts` 构造 CSP 时读取；这不能替代构建期 ARG。

#### `REDIS_SSL=false` 为什么必须写出来

`application.yml` 的默认值是：

```yaml
spring.data.redis.ssl.enabled: ${REDIS_SSL:true}
```

开发和生产 Compose 都使用 `redis:7-alpine`，该容器命令没有配置 TLS，只是开发环境无密码、生产环境 `--requirepass` 的明文 Redis。若删掉 `REDIS_SSL: "false"`，Spring Data Redis 会按默认值尝试 TLS 握手，连接明文 Redis 时失败。生产的 `data` 网络是 internal 只能减少暴露面，不会自动把 Redis 连接变成加密连接；若以后改用强制 TLS 的托管 Redis，应按托管服务要求改成 `true` 并补齐证书/连接配置，而不是盲改当前 Compose。

#### JWT 密钥和生产启动校验

`JwtUtils` 的 `@PostConstruct validateSecret()` 在 Spring 创建 Bean 后、应用正式接收请求前执行：

```text
secret == null 或 UTF-8 字节数 < 32
→ 抛 IllegalStateException
→ Spring Boot 启动失败
```

32 字节是 HS256 所需的最低 256 bit 密钥长度；“32 个字符”只有在字符都是单字节时才恰好相等，源码实际检查的是 UTF-8 字节数。开发 Compose 有明确标注的本地默认值，生产 Compose 用 `${JWT_SECRET:?required}`，缺失就让 Compose 插值失败。生产不要使用仓库里的示例值或多人共享值。

另外两个启动 Bean 负责不同边界：

- `SecurityStartupValidator`：`SameSite=None` 必须和 `Secure=true` 一起使用；启用 `prod/production` profile 时 Cookie 也必须 `Secure=true`。
- `ProductionStartupValidator`：生产必须有 HTTPS `PUBLIC_BASE_URL`、Redis、可信代理 CIDR、`server.forward-headers-strategy=framework` 和 CSRF。它不负责 JWT 长度，不能把三个校验类混成一个。

#### Caddy 路由：浏览器只看见一个站点

当前 `deploy/Caddyfile` 的顺序是：

```text
{$SITE_ADDRESS}
→ encode zstd gzip
→ path /api/* 或 /files/*：reverse_proxy backend:8080
→ 其它路径：reverse_proxy frontend:3000
→ 加 HSTS、nosniff、Referrer-Policy、Permissions-Policy
→ 输出 JSON 日志
```

`handle @backend` 先匹配 API/文件路径，后面的无条件 `handle` 才接收页面、静态资源和其它路径。生产 Caddy 的 80/443 端口负责外部 HTTP/HTTPS；Caddy 数据卷和配置卷保存证书/运行状态，不能随便删掉。生产 `PUBLIC_ORIGIN` 与 `SITE_ADDRESS` 都必须来自受保护的环境文件，并且 `PUBLIC_ORIGIN` 要是 HTTPS origin。

#### Flyway、seed 和启动时机

当前结构迁移是：

```text
MySQL 容器创建数据库和应用账号
→ backend 启动时 Spring Boot 初始化 Flyway
→ 扫描 classpath:db/migration
→ 执行未落地的 V1__initial_schema.sql
→ V2__production_operations.sql
→ V3__resume_review_queue_index.sql
→ V4__account_storage_and_git_sync.sql
→ V5__contribution_award_audit.sql
→ 写入 flyway_schema_history
→ migration 成功后 readiness 才能通过
```

MySQL 容器不负责把 `db/schema.sql` 当作生产结构入口；当前结构以 Flyway V1-V5 为准。`db/seed.sql` 是演示数据，生产 Compose 不挂载它，也使用 `production` profile。只有 `DevSeedDataInitializer` 同时满足以下条件才执行 seed：

```text
profile = dev 或 test
且 csa.seed.enabled=true（Compose 对应 DEMO_SEED_ENABLED=true）
且 DEMO_SEED_PASSWORD 至少 12 个字符
且 SQL 含 __DEMO_PASSWORD_HASH__ 占位符
```

初始化器在运行时用 `PasswordEncoder` 生成 BCrypt 哈希后替换占位符，再通过 `ResourceDatabasePopulator` 执行 SQL。这样仓库里没有固定演示密码，也不会把演示账号混进生产 migration。已有数据库再次启动时，Flyway 读取 `flyway_schema_history`，只执行更高版本；不能靠删除生产数据卷来模拟回滚。

#### 开发重置和生产回滚边界

开发环境可以明确选择是否丢数据：

```powershell
docker compose down      # 删除容器，保留 mysql-data、redis-data、backend-uploads
docker compose up --build

docker compose down -v    # 同时删除命名卷：数据库、Redis、上传文件全部清空
docker compose up --build
```

`down -v` 是破坏性操作，适合隔离的本地练习，不是生产回滚。生产应用回滚指切换到已验证的旧镜像，并保持数据库向前兼容；数据库迁移失败要停写、备份、恢复副本或发布更高版本修复，不能直接删卷。

#### 备份、恢复和校验

`deploy/backup.ps1` 当前做四件事：

1. 确认 Compose 中 `mysql`、`backend` 正在运行。
2. 在 MySQL 容器中用 `mysqldump --single-transaction --quick --routines --events --triggers --hex-blob` 生成 `database.sql.gz`。
3. 从 `/app/uploads` 打包 `uploads.tar.gz`，同时写 `metadata.json`。
4. 对三个文件生成 `SHA256SUMS`，并按 `RetentionDays` 清理过期的每日备份。

数据库快照和文件快照不是跨系统原子快照；高频写入期间可能不是同一瞬间的状态，所以生产备份窗口要降低写入并定期做恢复演练。SHA-256 只能证明文件未被意外改动，不能提供保密性；备份离开部署机前还要加密。

`deploy/restore.ps1` 是替换数据的破坏性脚本，必须显式传 `-ConfirmDestructiveRestore`。它先验证 `SHA256SUMS`，要求 `mysql` 运行，再停止 `caddy/frontend/backend` 这些写服务，导入数据库，并可选择恢复上传卷；`-SkipUploads` 跳过文件，`-NoRestart` 不自动拉起原来运行的写服务。恢复失败时应用保持停止，先从最后一个已验证备份处理，不能让半恢复状态继续接收流量。

生产最小流程：

```powershell
docker compose --env-file C:\secure\csa-production.env -f compose.production.yml config
docker compose --env-file C:\secure\csa-production.env -f compose.production.yml up -d --build
docker compose --env-file C:\secure\csa-production.env -f compose.production.yml ps
docker compose --env-file C:\secure\csa-production.env -f compose.production.yml exec backend wget -qO- http://127.0.0.1:8080/actuator/health/liveness
docker compose --env-file C:\secure\csa-production.env -f compose.production.yml exec backend wget -qO- http://127.0.0.1:8080/actuator/health/readiness
```

`config` 先做变量展开和结构检查；`.env.production.example` 中的空必填秘密不能直接当生产配置。生产首次启动不创建共享演示账号、不执行 seed，管理员账号必须走受控流程创建或导入合规密码哈希。

#### 动手验证（不改业务代码）

1. 执行 `docker compose config --services`，说出开发四个服务；再用临时完整环境变量执行生产 Compose 的 `config --services`，说出五个服务。
2. 打开两个 Compose 文件，分别圈出 `ports`、`expose`、`depends_on`、`healthcheck` 和 `volumes`，解释它们是否对宿主机可见、是否持久化。
3. 从 backend Dockerfile 找出 Maven 17 build stage、JRE 17 runner stage、`USER csa`、`ENTRYPOINT`；从 frontend Dockerfile 找出四个阶段和 `npm run build/start`。
4. 断点或阅读 `JwtUtils.validateSecret()`、`SecurityStartupValidator`、`ProductionStartupValidator`，分别说出三者阻止哪类错误配置。
5. 阅读 Caddyfile，给出 `/api/users`、`/files/a.pdf`、`/about` 各自会转到哪个容器。
6. 画出“浏览器 → Caddy → backend/frontend”和“backend → mysql/redis”的两条网络路径。
7. 在隔离开发卷中验证 `down` 保留数据、`down -v` 清空数据；不要对生产项目执行 `down -v`。
8. 只在 dev/test 临时设置 `DEMO_SEED_ENABLED=true` 和随机 `DEMO_SEED_PASSWORD`，确认缺密码或少于 12 字符时启动器拒绝执行。
9. 检查当前 Flyway history 应到 V5；看到旧文档只写 V2/V3 时，以 migration 文件和真实 history 为准并记下漂移。
10. 在 staging/隔离环境运行一次 backup → checksum 校验 → restore → liveness/readiness 验收，记录数据库行数、上传文件和耗时。

#### 自测题

1. Image、Container、Service、Compose 四个词分别指什么？一个 service 是否等于一个 Java 类？
2. 开发 Compose 和生产 Compose 分别有几个服务？为什么生产 backend/frontend 不发布 `ports`？
3. `depends_on` 的短写法和 `condition: service_healthy` 的差别是什么？healthcheck 的 `interval`、`timeout`、`retries`、`start_period` 各控制什么？
4. MySQL/Redis 的 healthcheck 通过，是否能保证业务接口一定正确？还需要看哪一个 backend readiness？
5. `ports` 与 `expose` 的可见范围有什么不同？`EXPOSE 8080` 为什么不会自动打开宿主机 8080？
6. `REDIS_SSL` 在当前 Compose 为什么必须显式为 `false`？如果换成强制 TLS 的 Redis，应该检查哪些配置？
7. `NEXT_PUBLIC_API_URL` 为什么要在 frontend build stage 通过 `ARG` 注入？开发值、生产值分别是什么？
8. 浏览器为什么不能把 `http://backend:8080` 当作公开 API 地址？Caddy 为什么可以使用 `backend:8080`？
9. backend Dockerfile 的 build stage 和 runner stage 各自带什么？`USER csa`、`read_only`、`tmpfs`、`no-new-privileges` 分别解决什么问题？
10. Caddyfile 中 `/api/*`、`/files/*` 和其它路径分别走哪条 `handle`？为什么不能把所有路径都转给 backend？
11. liveness 和 readiness 的故障含义有什么区别？为什么生产 readiness 还包含 `db`、`redis`？
12. `JwtUtils.validateSecret()`、`SecurityStartupValidator`、`ProductionStartupValidator` 各自在哪个启动阶段阻止什么错误？JWT 密钥检查的是字符数还是 UTF-8 字节数？
13. 当前 V1-V5 分别做什么？`flyway_schema_history` 在启动链中扮演什么角色？
14. 为什么 `db/seed.sql` 不能混进生产 migration？`DevSeedDataInitializer` 的 profile、开关和密码条件是什么？
15. `docker compose down`、`down -v`、应用镜像回滚、数据库恢复分别会不会删除命名卷？哪些只能在隔离环境做？
16. backup 生成哪几个文件？SHA-256 能证明什么、不能证明什么？restore 为什么必须显式 `-ConfirmDestructiveRestore`？

#### 完成标准

能不看答案画出开发四服务和生产五服务的启动/网络图；能从两个 Dockerfile 指出每个阶段、构建期变量和非 root 运行点；能解释 healthcheck、liveness/readiness、Caddy 路由、`REDIS_SSL`、`NEXT_PUBLIC_API_URL`、JWT 32 字节校验、Flyway V1-V5、seed 边界和备份恢复风险；能在隔离环境完成一次 `config → up → health → backup/checksum → restore` 验收，并明确说出哪些操作绝不能对生产数据卷执行。

## 6. 演示脚本

### 6.1 最短演示

适合 3-5 分钟：

1. 打开首页，说明这是公开官网。
2. 打开关于页，说明内容来自后端配置。
3. 登录，说明 JWT Cookie 和 CSRF。
4. 进入工作台，说明菜单根据角色等级展示。
5. 打开资源库，说明资源列表和分类。
6. 打开接口文档或 README，说明后端测试和前端构建通过。

### 6.2 完整演示

适合 10-15 分钟：

1. 首页：讲公开官网。
2. 贡献者页：讲用户和贡献墙。
3. 竞赛页：讲公开竞赛 VO。
4. 登录页：讲登录链路。
5. 工作台：讲会话校验和角色菜单。
6. 个人资料：讲 `/api/sys/user/info`。
7. 资源库：讲资源分页、分类、上传、文件访问。
8. 简历：讲核心成员简历草稿和审核。
9. 竞赛管理：讲编辑权限和授权。
10. 提案中心：讲投票权重和提案状态。
11. 部门人事：讲任命部长和事务。
12. 公开设置：讲 HTML 清洗。
13. 文档：讲架构、安全、RAG/Agent 后续路线。

### 6.3 演示时的讲解重点

不要只说“这个页面能点”。每个页面都带一个技术点：

| 页面 | 业务点 | 技术点 |
| --- | --- | --- |
| 登录 | 用户进入系统 | AuthenticationManager、JWT、Cookie、CSRF |
| 工作台 | 根据身份显示入口 | src/proxy.ts、Zustand、DashboardGuard、roleLevel |
| 资源库 | 协会资料管理 | Controller/Service 分层、`ResourceVO`、`PageUtils` 分页收敛、文件访问控制 |
| 竞赛 | 比赛发布和授权 | `SaveCompetitionDto`、自定义权限服务 `csaSec`、列表摘要与详情接口拆分 |
| 简历 | 成员简历审核 | 状态流转、Service 业务逻辑 |
| 部门 | 任命部长 | 事务、一致性 |
| 投票 | 组织治理 | `VoteTallyVO`、权重、阈值、防高危自动执行、DB 唯一约束防重复投票 |
| 设置 | 富文本维护 | 后端 Jsoup、前端 DOMPurify、防 XSS |
| 贡献墙 | 成员贡献统计 | Mapper 层聚合、`@Async` 异步落库、ThreadLocal 跨线程问题 |

演示到最后，可以额外补一段「工程化」收尾，这部分往往比点页面更有说服力：

```text
最后讲三件事：

1. 打开 `csa-official-backend/src/main/resources/db/migration/`，说明生产结构由
   Flyway V1-V5 版本链管理；`db/schema.sql` 只是学习快照，`db/seed.sql` 只给 dev/test
   使用。再打开 SchemaConsistencyTest，说明实体和全部 migration 的列一致性是被测试守住的。

2. 打开 docker-compose.yml，说明一条命令就能起 MySQL + Redis + 前后端，
   演示环境不依赖「我这台电脑的配置」。

3. 跑一次 mvnw.cmd test，让当前 174 个用例（其中 1 个 Docker Testcontainers 用例按开关跳过）在面试官面前跑绿，
   顺带说一句里面有一个是我修掉的偶发失败用例（见 7.15）。
```

## 7. 常见面试题和回答

### 7.1 你这个项目解决什么问题？

回答：

```text
它解决的是计算机协会官网展示和内部协作管理的问题。公开用户可以了解协会、查看贡献者和比赛活动；协会成员登录后可以访问资源库、维护简历、管理比赛、参与提案投票；管理员可以做部门人事和公开内容维护。对我个人来说，这个项目也是一个 Spring Boot + Next.js 的综合练习，用来把登录鉴权、权限控制、文件上传、统一异常、测试和后续 RAG/Agent 扩展串起来。
```

### 7.2 你的项目为什么需要 Spring Security？

回答：

```text
因为项目有登录用户和不同角色等级，不同接口权限不同。比如会员可以看资源，部长可以发布资源和管理竞赛，会长可以任命部长。如果只靠前端隐藏菜单，用户可以直接发请求绕过。所以后端用 Spring Security 做认证和授权，JWT 过滤器解析当前用户，接口上用 @PreAuthorize 控制角色。
```

### 7.3 JWT 和 Session 有什么区别？

回答：

```text
Session 通常是服务端保存会话 ID 和用户状态，浏览器只保存 sessionId。JWT 是把用户身份和过期时间等信息签名后放到 Token 中，服务端可以通过签名验证 Token 是否可信，不一定每次查 session。JWT 的优点是无状态，适合前后端分离；缺点是签发后在过期前难以失效，所以我的项目增加了 Token 吊销服务，退出登录时把 Token 放入黑名单。
```

### 7.4 为什么用了 HttpOnly Cookie 又要 CSRF？

回答：

```text
HttpOnly Cookie 可以防止 JavaScript 直接读取 Token，降低 XSS 窃取 Token 的风险。但 Cookie 会被浏览器自动携带，如果用户登录后访问恶意网站，恶意网站可能诱导浏览器向我的后端发 POST 请求。所以 Cookie 登录还需要 CSRF Token。我的项目要求 Cookie 登录的非 GET 请求必须带 X-CSRF-Token，并且和 CSRF Cookie 匹配。
```

### 7.5 CORS 和 CSRF 有什么区别？

回答：

```text
CORS 是浏览器同源策略相关的跨域访问控制，决定哪个前端域名可以访问后端。CSRF 是防止用户在已登录状态下被第三方网站诱导发起写操作。CORS 不是 CSRF 的替代品，因为某些表单提交或浏览器行为仍可能带 Cookie，安全上不能只依赖 CORS。
```

### 7.6 为什么 Redis 可以切到内存缓存？

回答：

```text
本地学习时不一定想装 Redis，所以我把缓存抽象成 KeyValueStore，有 RedisKeyValueStore 和 MemoryKeyValueStore 两种实现。生产环境应该用 Redis，因为多实例共享、重启不丢；本地可以用 memory，方便跑项目和测试。
```

### 7.7 统一异常有什么好处？

回答：

```text
统一异常可以避免每个 Controller 写重复 try-catch，也能保证前端拿到统一格式。比如参数错误返回 400，权限错误返回 403，业务异常返回 CsaException 指定的 code，未知异常统一返回系统繁忙，同时后端记录日志。这样前端 Axios 可以统一拆包和提示错误。
```

### 7.8 文件上传怎么保证安全？

回答：

```text
上传时会校验文件大小和允许的扩展名，保存到配置的 UPLOAD_PATH 下。访问时不是直接开放静态目录，而是通过 /files/{ownerId}/{fileName} 控制：当前用户是 owner 可以访问，或者这个文件已经作为资源发布才允许访问，否则返回 403。同时响应里加了 nosniff 和 attachment，减少浏览器错误执行文件的风险。
```

### 7.9 投票模块有什么业务难点？

回答：

```text
投票不是简单 CRUD，它要考虑谁能创建提案、谁能投票、能不能重复投票、不同角色是否有不同权重、达到阈值后状态怎么变，以及高风险提案是否能自动执行。当前项目对 ROOT_APPLY 这类高风险提案做了保守处理，不允许普通创建和自动执行。
```

### 7.10 如果要新增活动报名模块，你会怎么做？

回答：

```text
后端我会新增 modules/activity，里面放 controller、service、mapper、entity、dto、vo。公开活动列表放 /api/public/activities，报名接口要求登录，创建和编辑活动要求 Level 3，导出报名名单要求 Level 4。前端新增 /activities 和 /dashboard/activities 页面，API 调用集中放 src/services/activity.ts。业务规则，比如报名截止、重复报名、名额限制，会放在 Service 层并加事务。
```

### 7.11 你怎么把这个项目接 RAG？

回答：

```text
我会从资源库接入。资源上传后，后端创建 kb_document，解析文件内容，清洗文本，按 chunk size 切片，再调用 embedding 模型写入向量库，同时在 MySQL 保存 chunk 元数据和权限信息。用户提问时，系统根据当前用户 roleLevel 和部门生成过滤条件，只检索有权限的文档，然后把 TopK 片段组装进 Prompt，让模型基于资料回答并返回引用。每次问答写 qa_log，方便排查检索错还是模型回答错。
```

### 7.12 Agent 和 RAG 有什么区别？

回答：

```text
RAG 主要是检索资料并基于资料回答，解决知识更新和引用来源问题。Agent Tool Calling 是让模型调用后端工具，比如查询资源、查询竞赛、生成通知草稿。Agent 风险更高，所以工具必须有参数 schema、权限校验、limit 限制和调用审计。高风险操作不能让 Agent 直接执行，只能生成草稿或等待人工确认。
```

### 7.13 你这个项目做过哪些优化？（高频追问）

这题最容易答砸 —— 大部分人会说「加了缓存」「优化了 SQL」，
但讲不出**为什么要优化**和**优化前后差多少**。按「问题 → 原因 → 做法 → 验证」讲：

```text
我做过三类优化。

第一类是可运行性。项目早期的表结构主要依赖手工创建，
只存在于开发机上，换台机器很难稳定复现。
我按实体整理了 db/schema.sql 结构快照和 seed.sql 演示数据，随后把生产结构迁入
Flyway V1-V5 版本链，并用 docker-compose 把 MySQL、Redis、前端、后端串起来。
现在本地可以用 docker compose up 复现空库；需要演示账号时，再显式注入一次性 seed 密码。
为了防止以后再漂移，我写了一个 SchemaConsistencyTest，
它把实体字段转成下划线格式，和全部 Flyway migration 的列做双向 diff，对不上就构建失败。

第二类是接口健壮性。我发现资源列表和竞赛列表的分页 size 参数是完全没有上限的，
任何一个登录用户请求 ?size=1000000，数据库就会真的去捞一百万行塞进堆里，
一个请求就能把服务打挂。而其他接口各写各的 Math.min，上限还不一致。
我抽了一个 PageUtils 统一收敛，分页上限 100，列表上限 200，并补了边界测试。

第三类是响应体积和分层。竞赛列表页每条只显示 120 字摘要，
但接口把整段富文本 HTML 都发过来了，一页 10 条就是几百 KB 的无效流量。
我把列表 VO 的 content 换成后端截好的 summary，
再加了详情接口按需取正文，前端编辑弹窗改成点开时才拉详情。
另外把资源模块的业务逻辑从 Controller 抽到 Service，这样才能加事务、才能单测。

验证方式是后端全量用例、显式 Testcontainers、前端测试、lint 和 next build 全绿，并能拿出 Phase 1 的 Flyway、备份恢复与 HTTPS E2E 记录；当前源码的 Docker 镜像和 Playwright 若受环境故障影响，必须如实标为阻断，不能用旧镜像结果冒充。
```

### 7.14 为什么接口不直接返回 Entity？

```text
三个原因。

一是安全和信息泄露：实体上有 deleted 这种逻辑删除标记，
属于持久层细节，没必要让前端看到。轮播图实体还有 sortOrder、status，
公开接口把这些发给未登录用户没有意义。

二是契约稳定：直接返回实体等于把数据库表结构当成 API 契约。
以后给表加一个字段，接口响应就会悄悄变宽，
万一加的是敏感字段（比如手机号），就是一次事故。

三是按场景裁剪：同一个实体在不同接口需要的字段不一样。
比如竞赛列表只要摘要，详情才要完整正文，
所以我拆成了 CompetitionListVO 和 CompetitionDetailVO。
```

### 7.15 讲一个你排查过的 bug

如果没有真实线上 bug，这个 flaky test 是很好的素材，因为它考的是推理过程：

```text
我跑测试时遇到一个偶发失败：JwtUtilsTest 里「篡改 token 应该抛异常」的用例，
大概每三十次会挂一次，重跑又好了。

我没有直接重跑糊过去，而是先问「什么东西每次运行都不一样」。
JWT 里 jti 用的是随机 UUID，所以每次签名都不同 —— 随机性来自这里。

然后看用例怎么篡改的：它把 token 最后一个字符，
如果是 'a' 就换成 'b'，否则换成 'a'。
问题就在这：HS256 签名是 32 字节，base64url 编码后是 43 个字符，
43 乘 6 等于 258 bit，比 256 bit 多 2 bit，
也就是最后一个字符只有 4 个有效 bit，剩下 2 bit 解码时被丢弃。
'a' 和 'b' 的 6 bit 值只差最低位，解码出来是完全相同的字节。
所以只要 token 末位恰好是 a 或 b，「篡改」后的签名和原来一模一样，
校验自然通过，断言就失败了。

我写了个小程序验证了这个猜想，然后把用例改成篡改 payload 段
（一定会改变签名覆盖的内容），另外单独加了一个篡改签名首字符的用例，
连续跑 8 次都稳定通过。

结论是：偶发失败不要靠重跑掩盖，
先找到「每次都不一样的那个东西」，再顺着它推出确定的失败条件。
```

## 8. 自测清单

### 8.1 项目结构

- [ ] 我能说出前端和后端分别在哪个目录。
- [ ] 我能说出 `common`、`config`、`modules` 的区别。
- [ ] 我能说出 `sys`、`biz`、`resume` 的职责。
- [ ] 我能说出前端 `app`、`components`、`services`、`store` 的职责。

### 8.2 请求链路

- [ ] 我能追踪登录接口。
- [ ] 我能追踪资源列表接口。
- [ ] 我能追踪上传文件接口。
- [ ] 我能追踪竞赛保存接口。
- [ ] 我能解释 `useEffect` 为什么触发资源请求，`useMemo` 为什么不会发请求。
- [ ] 我能指出 `resourceMapper.selectPage(pageParam, query)` 才是真正执行分页查询的调用。
- [ ] 我能解释 `Page<Resource>` 为什么要转换成 `Page<ResourceVO>`。
- [ ] 我能区分 Axios 的 TypeScript 泛型与响应拦截器的运行时拆包。
- [ ] 我能解释 `setItems`、`setPages`、`setTotal` 如何触发 React 重新渲染。

### 8.3 统一返回和异常

- [ ] 我能说出 `R<T>` 的五个字段，并说明成功和失败时哪些字段有值。
- [ ] 我能区分 `ApiErrorCode`、`CsaException`、`GlobalExceptionHandler` 的职责。
- [ ] 我能区分缺少 query 参数、参数类型错误、JSON 解析错误和 Bean Validation 失败。
- [ ] 我能为上述四类错误分别说出异常类、处理方法和 `errorCode`。
- [ ] 我能判断参数绑定/校验失败时 Controller 方法体不会执行。
- [ ] 我能解释为什么 `SaveResourceDto` 不是校验注解，`@NotBlank` 才是字段规则，`@Valid` 负责触发。
- [ ] 我能说出 `traceId` 从哪里来、用于什么，以及为什么每次请求可能不同。
- [ ] 我能解释为什么 Controller 不需要重复 try-catch，以及什么时候局部 catch 才有意义。

### 8.4 安全

- [ ] 我能解释 JWT 怎么生成和校验。
- [ ] 我能解释 Cookie 登录为什么需要 CSRF。
- [ ] 我能解释 `JwtAuthenticationFilter`、`JwtAuthenticationEntryPoint`、`JwtAccessDeniedHandler` 各自负责什么。
- [ ] 我能解释“未认证 = 401”和“已认证但无权 = 403”的完整调用链。
- [ ] 我能说明 Authentication 只写进当前请求的 `SecurityContext`，不是保存到 `User` 实体。
- [ ] 我能解释前端菜单隐藏不等于权限控制。
- [ ] 我能解释退出登录为什么要吊销 Token。
- [ ] 我能解释文件访问控制。

### 8.5 业务

- [ ] 我能讲资源库。
- [ ] 我能讲竞赛编辑授权。
- [ ] 我能讲简历审核。
- [ ] 我能讲部门任命。
- [ ] 我能讲提案投票。
- [ ] 我能讲贡献墙。

### 8.6 工程化

- [ ] 我能从零启动后端。
- [ ] 我能从零启动前端。
- [ ] 我能跑 `mvnw.cmd test`。
- [ ] 我能跑 `npm run build`。
- [ ] 我能看懂 `.env.example`。
- [ ] 我能根据报错定位 CORS、CSRF、数据库、Redis 问题。
- [ ] 我能用 `docker compose up --build` 启动开发四服务，并说清生产为什么增加 Caddy 形成五服务及其启动顺序。
- [ ] 我能用 Flyway V1-V5 在空库初始化，并解释 `db/schema.sql` 为什么只保留为学习快照。
- [ ] 我能说清 `SchemaConsistencyTest` 防的是什么事故。

### 8.7 优化与排错（面试高频）

- [ ] 我能说出这个项目做过哪三类优化，每类都能讲「问题—原因—做法—验证」。
- [ ] 我能解释分页 size 不设上限会造成什么后果。
- [ ] 我能解释为什么接口要返回 VO 而不是 Entity。
- [ ] 我能解释竞赛列表为什么只返回摘要，详情为什么要单独开接口。
- [ ] 我能解释 `@Async` 为什么要单独成 Bean，以及 SecurityContext 为什么跨不了线程。
- [ ] 我能完整复述那个 flaky test 的排查过程。
- [ ] 我能说清「Service 层判重」和「数据库唯一约束」各自解决什么，为什么两个都要。

## 9. 学习日志模板

学习日志不要只写“今天看了什么”，要能留下证据，方便一周后复盘，也方便面试前快速恢复手感。每天至少写清楚四件事：追了哪条链路、验证了什么现象、发现了什么偏差、下一步怎么闭环。

### 9.1 每日学习日志

```text
日期：
学习时长：
源码基线：
- 当前分支/commit（若工作区未提交，写明“dirty”并记录相关 diff）：

今日目标：
-

今天看的代码/文档：
-

今天追踪的接口或页面：
-

请求链路：
- 前端页面：
- 组件：
- service：
- axios/proxy：
- Controller：
- Service：
- Mapper：
- 表：

今天理解的技术点：
-

掌握等级（独立找到 / 经提示掌握 / 只记住结果 / 尚未掌握）：
-

答案来源：
- 我自己定位的文件/方法：
- 经提问或纠错后才知道的：
- 仍然只是 AI 解释、尚未用源码验证的：

今天验证的现象：
-

证据：
- 命令：
- 关键输出：
- 截图/页面：
- 对应文件：

今天发现的偏差：
- 文档和代码是否一致：
- 之前理解哪里错了：
- 是否发现题目使用了旧代码或不存在的占位类：

今天修改的内容：
-

运行命令：
-

结果：
-

今天能讲出口的一句话：
-

还没懂的问题：
-

下次复测题（不能抄本次答案）：
-

明天第一件事，也就是可执行动作：
-
```

### 9.2 单接口深挖日志

当你专门学一个接口时，用这个模板，比普通打卡更有用。

```text
接口：
方法：
权限：
前端入口：
后端入口：
源码基线：

正常请求：
- 参数：
- 返回：

异常请求：
- 未登录：
- 低权限：
- 参数错误：
- CSRF 缺失：

异常链路：
- 失败发生在哪一层：Filter / 参数绑定 / Validation / Controller / Service / Mapper
- 实际异常类：
- 最终处理组件和方法：
- HTTP 状态、code、errorCode：
- Controller 方法体是否执行：

业务规则：
-

安全点：
-

我能不能不用看代码讲清楚：
- [ ] 能
- [ ] 不能，卡在：

面试回答草稿：
-
```

### 9.3 问题排查日志

遇到 bug 或启动失败时，不要只写“报错了”，要记录判断过程。

```text
问题现象：

触发命令/操作：

完整错误关键行：

我的第一判断：

排查路径：
1.
2.
3.

最终原因：

修复方式：

验证命令：

防止下次再犯：
- 要补的文档：
- 要补的测试：
- 要记住的配置：
```

## 10. 周复盘模板

```text
本周完成：
-

本周追通的请求链路：
-

本周验证过的命令：
- 后端：
- 前端：
- 数据库/缓存：

本周能讲清楚的内容：
-

本周还讲不清楚：
-

本周发现的文档偏差：
-

我修复的问题：
-

我新增的文档/测试/功能：
-

本周最有价值的证据：
- 测试：
- 构建：
- 页面：
- 代码文件：

下周要补的闭环：
-

下周目标：
-

可以写进简历的点：
-

面试 1 分钟表达更新：
-
```

## 11. 简历项目经历版本

### 11.1 简洁版

```text
CSA 计算机协会官网与管理平台
技术栈：Spring Boot 3、Spring Security、JWT、MyBatis-Plus、MySQL、Redis、Next.js、TypeScript

项目描述：面向计算机协会的官网和内部管理平台，支持公开内容展示、登录注册、资源库、竞赛管理、简历投递、部门人事、提案投票和贡献墙。

个人工作：
- 基于 Spring Security + JWT + HttpOnly Cookie + CSRF 实现登录鉴权和接口权限控制。
- 使用 MyBatis-Plus 实现用户、资源、竞赛、简历、投票等模块的数据访问。
- 封装统一返回、全局异常、限流、Token 吊销和文件访问控制。
- 使用 Next.js + TypeScript 搭建公开官网和角色化工作台，统一封装 Axios 请求和错误处理。
- 补充后端测试、前端构建验证、本地启动文档、接口地图和架构说明。
```

### 11.3 突出「优化」的版本

简历上写「优化」一定要带**问题**和**结果**，不能只写「进行了性能优化」。
下面每条都是这次真实做的，面试官追问时你能对着代码讲：

```text
CSA 计算机协会官网与管理平台
技术栈：Spring Boot 3、Spring Security、JWT、MyBatis-Plus、MySQL、Redis、Docker Compose、Next.js、TypeScript

个人工作：
- 基于 Spring Security + JWT + HttpOnly Cookie + CSRF 双提交实现登录鉴权与分级权限控制，
  后端接口用 @PreAuthorize 做真实校验，退出登录时将 Token 写入吊销缓存。
- 排查并修复分页接口未限制 size 的资源耗尽风险：抽取统一分页收敛工具，
  将分页上限收敛至 100、列表上限 200，替换各处不一致的内联判断，并补充边界测试。
- 优化竞赛列表响应体积：将列表 VO 中的完整富文本正文替换为服务端截断的纯文本摘要，
  新增详情接口按需返回正文，前端编辑弹窗改为按需拉取，列表响应体积下降一个数量级。
- 重构资源模块分层：将分页、归属校验、下载计数等业务逻辑从 Controller 抽取至 Service 层，
  使其可加事务、可复用、可脱离 Web 环境单元测试；接口统一改为返回 VO，避免实体字段外泄。
- 将贡献记录落库改为异步执行，从 HTTP 请求链路中移除一次数据库往返，
  并处理 SecurityContext 无法跨线程传递的问题。
- 补全数据库结构快照与演示数据，接入 Docker Compose 实现 MySQL/Redis/前后端一键启动，
  再将生产结构迁入 Flyway 版本链；编写实体与 migration 一致性测试，将「改了实体忘改 DDL」
  从运行时故障提前到构建期失败。
- 定位并修复一个偶发失败的单元测试（base64url 末位字符只承载 4 bit 导致篡改无效），
  当前默认后端测试已扩充到 133 个用例；最近报告为 0 failures、0 errors、1 skipped，
  skipped 项是默认关闭的 Testcontainers 集成测试，不能写成 133/133 全部执行。
```

### 11.2 带 AI 扩展版

```text
CSA 协会智能管理平台
技术栈：Spring Boot 3、Spring Security、JWT、MyBatis-Plus、MySQL、Redis、Next.js、TypeScript、RAG/Agent 规划

项目描述：面向计算机协会的官网与管理平台，包含用户、资源、竞赛、简历、部门、投票等业务模块，并规划将资源库扩展为 RAG 知识库和 Agent 工具调用平台。

个人工作：
- 梳理并完善前后端模块边界，建立公开官网和后台工作台两套使用路径。
- 基于 JWT、HttpOnly Cookie、CSRF Token 和 Spring Security 实现登录认证、接口授权和安全错误响应。
- 使用 Redis/内存缓存抽象支持验证码、限流和 Token 黑名单，本地开发可降级运行。
- 完善资源上传、受控文件访问、竞赛编辑授权、部门任命、提案投票等业务规则。
- 设计 RAG/Agent 后续方案，包括知识库文档表、切片表、问答日志、资源查询工具、竞赛查询工具和工具调用审计。
```

## 12. 本次代码改动后的校准记录

这几条是当前源码已经变化、学习时必须按新口径讲的地方：

1. 登录不再把 JWT token 放进响应体；JWT 只通过 `CSA_AUTH_TOKEN` HttpOnly Cookie 下发，响应体只有 `csrfToken`、`username`、`roleLevel`。
2. 前端 `src/lib/axios.ts` 不再拼 Bearer Token，只负责 `withCredentials`、CSRF Header、401 跳转和统一错误包装。
3. `src/proxy.ts` 新增了 `/dashboard` 未登录跳转、CSP nonce、CSP Header 和生产环境 HSTS。
4. 前端富文本展示和设置预览增加 `isomorphic-dompurify`，后端仍用 Jsoup 做入库清洗，属于前后端双层 XSS 防护。
5. 登录查询链路新增 `UserAccountCacheService` 的 `auth_user` 缓存；验证码发送改为同步写验证码/限流缓存，再由 `AsyncMailSender` 异步发 SMTP。
6. 竞赛保存接口使用 `SaveCompetitionDto`，权限表达式按 `#dto.id` 区分新建和编辑，title/content 有 `@NotBlank` 校验。
7. 贡献墙不再在 Controller 里 Java Stream 聚合，而是走 `ContributionLogMapper.selectWall(limit)` 返回 `ContributionWallVO`，limit 被限制在 1-200。
8. 投票列表新增 size 上限和 createTime 倒序；投票统计用 `VoteTallyVO` 表达同意/反对/总权重。
9. 轮播图保存/删除、公开内容更新、贡献手动发放都按 LEVEL_4 讲，不要再说部长 LEVEL_3 能维护公开设置。
10. 测试清单要包含 `JwtUtilsTest`、`SecurityStartupValidatorTest`、`GitServiceTest`，这些覆盖 JWT 密钥/过期、生产 Cookie 配置和 JGit 行为。
11. `session_version` 是全会话吊销的权威版本；没有该 claim 的旧 JWT 会被拒绝，不能把旧 Token 当成版本 0。
12. `V2__production_operations.sql` 不仅补账号字段，还建立审计、文件元数据、邮件投递和定时任务幂等表；修改已执行 SQL 不是回滚方式。
13. 删除申请先进入 `DELETION_PENDING`；保留期到后由带数据库幂等键的任务自动匿名化符合条件的账号。它不等于立即物理删除，备份副本仍按备份保留策略处理。

### 12.1 本轮「可运行性 + 分层 + 性能」优化后的新口径

这一轮改动比较大，下面这些必须按新说法讲，旧说法会和源码对不上：

1. **资源模块有 Service 层了**。`ResourceController` 从 148 行瘦到只做参数接收，
   分页、归属校验、下载计数搬进 `modules/sys/service/ResourceService.java`，并加了 `@Transactional`。
   Day 2 第 4 题里你写的「没有 Service 层」是重构前的答案。
2. **实体不再直接出现在响应里**。资源、提案、部门、轮播图分别改成
   `ResourceVO` / `ProposalVO` / `DeptVO` / `CarouselVO`，逻辑删除标记 `deleted` 不再泄露到接口上。
   轮播图公开接口现在只返回 id/imgUrl/targetUrl/title 四个字段。
3. **竞赛列表和详情拆开了**。`CompetitionListVO` 的 `content` 换成了 `summary`
   （Jsoup 剥标签后截断到 200 字），完整正文改由新接口
   `GET /api/biz/comp/{id}` 和 `GET /api/public/competitions/{id}` 提供。
   公开详情对未发布的比赛返回 404。前端 `handleEdit` 相应改成异步拉详情。
4. **分页上限统一了**。新增 `common/util/PageUtils.java`，
   `MAX_PAGE_SIZE=100`、`MAX_LIST_LIMIT=200`。
   在这之前资源列表和竞赛列表的 `size` 是**完全没有上限**的，
   `?size=1000000` 能直接把数据库和堆打满；其余接口各写各的 `Math.min`，上限还不一致。
5. **贡献记录改成异步落库**。`ContributionAspect` 现在在请求线程上取好 `userId`，
   再交给 `ContributionLogWriter`（`@Async("contributionTaskExecutor")`）写库，
   不再占用 HTTP 请求线程。手动发放 `award` 仍保持同步。
6. **数据库结构进入 Flyway 版本链了**。新增 `V1__initial_schema.sql` 和
   `V2__production_operations.sql`，`db/seed.sql` 只保留运行时密码占位符，另有
   `docs/database.md` 说明表、索引和已有库迁移。生产不会创建共享演示账号。
7. **支持一键启动**。新增 `docker-compose.yml`（MySQL + Redis + 后端 + 前端）、
   前端 `Dockerfile`、`docs/deployment.md`。
8. **参数校验改用 Bean Validation**。`SaveResourceDto`、`ProposalDto` 从手写 `validate()`
   改成 `@Valid + @NotBlank/@Size`，错误统一由 `GlobalExceptionHandler` 转成 400。
9. **早期重构阶段测试从 56 个涨到 81 个**，新增 `PageUtilsTest`、`ResourceServiceTest`、
   `SchemaConsistencyTest`、`SeedDataPasswordTest`，并扩充了 `CompetitionServiceTest`；
   当前 Phase 2 代码默认 133 个，另有显式 Testcontainers 1 个（默认跳过）。
10. **修掉了一个偶发失败的老测试**。`JwtUtilsTest` 里篡改 token 的用例原来改的是签名最后一个字符，
    而 base64url 最后一个字符只有 4 个有效 bit，`'a'` 和 `'b'` 解码结果相同，
    所以约每 30 次会假失败一次。详见 Day 12 第 10 题。
11. **顺带修掉了两个真实存在的枚举序列化 bug**，详见下面 12.2。

### 12.2 枚举序列化：一个藏了很久的前后端契约 bug

这个问题是在给简历模块补 VO 时发现的，值得单独讲，因为它属于「功能看起来正常、
实际一直是坏的」那一类，而且两处的表现完全不同。

根因只有一句话：

```text
Jackson 默认把 Java 枚举序列化成【名字】，不是 @EnumValue 标注的 code。
```

也就是说 `ResumeStatusEnum.APPROVED` 出去是 `"status":"APPROVED"`，
而不是数据库里存的 `2`。可以自己验证：

```java
new ObjectMapper().writeValueAsString(ResumeStatusEnum.APPROVED)  // => "APPROVED"
```

注意 `@EnumValue` 是 **MyBatis-Plus** 的注解，只影响「怎么存进数据库」，
对 Jackson「怎么序列化成 JSON」没有任何影响。这两件事经常被搞混。

**表现一：简历状态标签永远显示「草稿」**

前端 `services/resume.ts` 声明的是 `status: number`，页面里这样比较：

```ts
if (status === RESUME_STATUS.APPROVED) // RESUME_STATUS.APPROVED === 2
```

后端发来的是字符串 `"APPROVED"`，而 `"APPROVED" === 2` 恒为 false，
所以所有分支都走不到，状态标签一律落到默认的「草稿」。
简历明明已经通过审核，页面上还写着草稿。

**表现二：编辑已结束的比赛，会被悄悄改成「进行中」**

竞赛列表页的显示是好的，因为 `normalizeCompetitionStatus` 同时兼容了数字和枚举名。
但编辑弹窗回填状态时是这么写的：

```ts
typeof status === "number" ? status : Number.parseInt(String(status ?? 1), 10) || 1
```

拿到 `"FINISHED"` 时，`Number.parseInt("FINISHED")` 得到 `NaN`，
`NaN || 1` 又回退成 `1`。于是你只是想改个标题，一保存状态就从「已结束」变成「进行中」。
这种 bug 特别隐蔽 —— 页面不报错，数据静悄悄地错了。

**修法**：在 VO 层显式返回 `code`，不改枚举本身、也不动请求反序列化那条路径。

```java
vo.setStatus(competition.getStatus() == null ? null : competition.getStatus().getCode());
```

这样前端拿到的就是它本来就期望的数字，两个 bug 一起消失，而且前端一行都不用改。

**为什么不直接给枚举加 `@JsonValue`？** 因为 `@JsonValue` 会**同时**影响反序列化，
而 `SaveCompetitionDto` 目前接收数字是靠 Jackson 的「按 ordinal 索引」行为
（这个枚举恰好 ordinal 和 code 相等，所以一直没出事）。
改动面更大、更容易踩到别的坑，所以选了只改出参的方案。

**留下的测试**：`ResumeVOTest`、`CompetitionServiceTest.detailReturnsNumericStatusCodeNotEnumName`
把「status 必须是数字」这条契约钉死了。

面试可以这么讲：

```text
我给简历接口补 VO 的时候，顺手用 ObjectMapper 打印了一下实际的 JSON，
发现 status 出去是 "APPROVED" 而不是 2。
翻前端代码发现它是拿 number 去比的，字符串永远等不上，
所以简历状态标签一直显示草稿 —— 功能不报错，但一直是坏的。

顺着这条线查竞赛模块，发现同一个根因在那边表现完全不同：
列表显示是对的（因为兼容函数把两种格式都处理了），
但编辑弹窗回填时 parseInt("FINISHED") 得到 NaN，
回退成 1，会把已结束的比赛悄悄改成进行中。

根因是 @EnumValue 只管 MyBatis-Plus 怎么存库，不管 Jackson 怎么序列化，
这两个注解的职责经常被混淆。
我在 VO 层显式返回 code 修掉了，没动枚举本身，
因为给枚举加 @JsonValue 会连带影响反序列化，
而入参那条路目前是靠 ordinal 巧合工作的，改动面太大。
最后补了测试把这个契约钉住。
```

## 13. 文档同步规则

以后只要代码改到下面这些地方，就必须同步检查对应文档：

```text
Controller 路径 / HTTP 方法 / @PreAuthorize
→ docs/api-map.md

登录响应、Cookie、CSRF、CORS、JWT、限流、文件访问
→ docs/security-design.md
→ docs/local-dev.md
→ README.md

目录结构、依赖版本、配置类、前端目录
→ README.md
→ docs/architecture.md
→ docs/study-and-demo-guide.md

学习路线、演示话术、面试回答
→ docs/study-and-demo-guide.md

前端启动、环境变量、axios/proxy 行为
→ csa-official-frontend/README.md
→ docs/local-dev.md

Entity 增删字段 / 新建实体 / 改 @TableName
→ csa-official-backend/src/main/resources/db/migration/V*.sql  ← 必须新增向前迁移
→ db/seed.sql            ← 仅 dev/test 演示数据且只用运行时密码占位符
→ docs/database.md

VO 字段增删、接口响应结构变化
→ docs/api-map.md
→ 前端 src/services/*.ts 里的 interface  ← 前后端契约要一起改

新增环境变量、改 application.yml 默认值
→ csa-official-backend/.env.example
→ docker-compose.yml     ← 容易漏，漏了就是「本地能跑，compose 起不来」
→ docs/local-dev.md
→ docs/deployment.md
```

最小检查命令：

```powershell
rg -n "@RequestMapping|@GetMapping|@PostMapping|@PreAuthorize" csa-official-backend/src/main/java/com/csa/official
rg -n "token|Bearer|CSRF|LEVEL_3|LEVEL_4|proxy\\.ts|SWAGGER_ENABLED" README.md docs csa-official-frontend/README.md
.\tools\check-study-guide-drift.ps1
```

`check-study-guide-drift.ps1` 是只读契约检查：它从当前 `pom.xml`、`package.json` 和源码读取版本、资源请求链路、Axios 拆包、Validation 与 401/403 处理，再确认本指南仍包含相同口径。它不能证明你已经学会，但能在代码重构后阻止明显的旧答案继续留在学习材料里。

实体和建表脚本是否漂移，现在不用靠人肉检查，跑测试就行：

```powershell
cd D:\CSA-Project\csa-official-backend
.\mvnw.cmd test -Dtest=SchemaConsistencyTest
```

它会把每个 `@TableName` 实体的字段转成 snake_case，和全部 Flyway migration 解析出的列做双向 diff，
对不上直接让构建失败。这条规则不再依赖「记得改文档」的自觉。

## 14. 最终验收标准

当你能做到下面这些，项目就真正变成你的能力了：

1. 不看稿讲 1 分钟项目。
2. 不看稿讲 3 分钟技术架构。
3. 能画登录链路。
4. 能画资源列表请求链路。
5. 能解释 JWT、Cookie、CSRF、CORS。
6. 能解释 roleLevel 和 @PreAuthorize。
7. 能新增一个简单业务接口。
8. 能定位一个 401/403/500。
9. 能跑后端测试和前端构建。
10. 能说明下一步怎么接 RAG 和 Agent。
11. 在一台没有这个项目的电脑上，能只用 `docker compose up --build` 把它跑起来。
12. 能讲清楚这个项目做过哪三类优化，每一类都说得出「原来什么样、为什么是问题、改成什么样、怎么验证的」。

第 11 和第 12 条是这轮补上的。它们的意义在于：
前 10 条证明你**看懂了**这个项目，后 2 条证明你**能把它交付给别人**、并且**能自己发现问题**。
面试里区分度最高的往往是后者 —— 会讲功能的人很多，能说清「我发现了什么问题、怎么量化、怎么验证修好了」的人少。

## 15. 生产就绪学习路线（新增）

这一轮改造不只是“把服务放进 Docker”。建议按下面顺序亲手跑一遍，并把每一步的输出留在自己的实验记录里：

1. 读 `application.yml`、`application-production.yml` 和两个 Compose 文件，画出浏览器、Caddy、frontend、backend、MySQL、Redis 的网络边界。
2. 用空 MySQL 卷启动 production profile，查询 `flyway_schema_history`，确认 V1-V5 成功且没有演示账号。
3. 把旧版结构导入隔离数据库，使用一次性 `FLYWAY_BASELINE_ON_MIGRATE=true` 把旧结构登记为 V1，确认随后执行 V2-V5，再恢复为 `false`。
4. 手工制造一个失败 migration，观察 MySQL DDL 的非事务特征；理解为什么恢复路径必须依赖备份，而不是盲目 `repair`。
5. 运行 `deploy/backup.ps1` 和 `deploy/restore.ps1`，验证数据库行数、上传文件 SHA-256 和应用健康状态恢复一致；恢复失败时应用应保持停止。
6. 查看 `X-Request-ID`、JSON 日志、`/actuator/health/readiness`、`/actuator/health/liveness` 和 Prometheus 指标，分别说明它们服务于排错、编排还是告警。
7. 用生产配置缺失秘密的故障演练确认 Compose fail-fast；再用临时假值验证配置可展开，绝不把假值写入仓库或日志。

完整操作、迁移、回滚和验收表见 [`production-readiness/learning-guide.md`](production-readiness/learning-guide.md)；Phase 2 账号、审计、隐私、上传、邮件和幂等演练见 [`production-readiness/phase-2-learning-guide.md`](production-readiness/phase-2-learning-guide.md)，实际结果见 [`production-readiness/phase-2-verification.md`](production-readiness/phase-2-verification.md)。

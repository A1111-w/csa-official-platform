# Defense Journal

## 当前状态

- 更新时间：2026-07-13 08:55:32 +08:00
- 防守阶段：BLOCKED_WAITING_FOR_ATTACK_EVIDENCE_AND_SCOPE
- 当前发现数：0
- 待分析：0
- 待修复：0
- 待攻击方复测：0
- VERIFIED：0
- DISPUTED：0

## 范围与约束

- 已完整读取 `security-war/SCOPE.md`。
- 授权范围为当前工作区的源码和配置审查；生产系统、生产数据及未明确列出的第三方目标禁止触碰。
- `SCOPE.md` 中本地 URL、本地服务启动、本地自动化安全测试和测试数据修改仍标记为 `TODO`。本轮仅运行项目自带的非破坏性测试、lint 和构建，没有向任何线上目标发送请求。
- 攻击方目录 `security-war/attack/` 和 `security-war/retest/` 只读；本轮未删除、覆盖或修改其中任何文件。
- 仓库在接管时已有大量已修改和未跟踪文件。全部视为用户既有工作并保留；未使用破坏性 Git 操作。

## 项目基线

- 项目：Spring Boot 3.5.8 / Java 17 后端，Next.js 16.2.10 / React 19 / TypeScript 前端。
- 后端入口：`csa-official-backend/src/main/java/com/csa/official/CsaOfficialApplication.java`。
- 后端安全入口：`SecurityConfig`、`JwtAuthenticationFilter`、`CsrfProtectionFilter` 和方法级 `@PreAuthorize`。
- 数据访问：MyBatis-Plus + MySQL；会话：无状态 JWT + HttpOnly Cookie；缓存/限流：Redis 或内存实现。
- 测试目录包含 10 个测试类，覆盖文件访问、竞赛授权、认证限流、统一错误响应和部分 Service 业务规则。

## 修改前验证基线

| 检查 | 命令 | 结果 |
| --- | --- | --- |
| 后端修改前基线 | `cd csa-official-backend; .\\mvnw.cmd test` | PASS；10 个测试类、35 个测试，0 failures / 0 errors / 0 skipped |
| 后端当前全量与打包 | `cd csa-official-backend; .\\mvnw.cmd verify` | PASS；13 个测试类、56 个测试，0 failures / 0 errors / 0 skipped；Spring Boot jar 打包成功 |
| 前端 lint | `cd csa-official-frontend; npm run lint` | PASS；退出码 0 |
| 前端生产构建与类型检查 | `cd csa-official-frontend; npm run build` | PASS；Next.js 编译、TypeScript、18 个静态页面生成均成功 |

说明：首次并行执行被外层 124 秒时限截断。后端结果由本轮新生成的 Surefire 报告确认；遗留的前端 Node 子进程已按精确 PID 和命令行清理，随后 lint 与 build 分别前台重跑并取得退出码 0。

## 防御基线增强

本轮在攻击方尚未提交 `F-XXX` 时只增加测试，不改变业务行为：

- `CompetitionControllerAuthorizationTest.staleMinisterClaimCannotBypassDatabaseRoleDowngrade`：恶意用例。JWT 签发时为部长、数据库中已降为会员时，竞赛写接口必须返回 403，且 Service 不得执行。
- `SecurityErrorResponseTest.trustedOriginPreflightReceivesCredentialedCorsHeaders`：正常控制用例。允许列表中的 Origin 预检返回 200、精确 Origin 和 credentials header。
- `SecurityErrorResponseTest.untrustedOriginPreflightIsRejected`：恶意用例。未授权 Origin 预检返回 403，且不得产生 `Access-Control-Allow-Origin`。
- `SecurityErrorResponseTest.secureResponseIncludesBrowserSecurityHeaders`：HTTPS 响应包含 HSTS、CSP、Frame、Referrer-Policy 和 Permissions-Policy。

定向验证：`./mvnw.cmd "-Dtest=CompetitionControllerAuthorizationTest,SecurityErrorResponseTest" clean test`，15/15 通过。首次测试失败是精简测试上下文没有激活 `dev` profile，导致预检没有匹配真实 Handler；激活 profile 后使用真实 `/api/test/users` 路由验证通过。随后全量后端 39/39 通过。

新增 `GitServiceTest` 离线出站 URL 策略矩阵，共 11 个用例：允许标准 HTTPS GitHub URL；拒绝伪造后缀、userinfo 混淆、IPv4/IPv6 loopback、凭据、8443 端口、SSH、file URI、无效和缺失 URL。该测试不触发任何真实网络请求。定向 11/11、当前全量 50/50 通过。

新增 `JwtUtilsTest` 与 `SecurityStartupValidatorTest` 共 6 个用例：HS256 密钥短于 32 字节时启动失败；合法密钥正常签发和解析；篡改 token 验签失败；生产 profile 禁止不安全认证 Cookie；`SameSite=None` 强制 `Secure=true`；安全生产配置正常通过。最终 `mvnw verify` 为 56/56，并成功生成 `target/csa-official-0.0.1-SNAPSHOT.jar`。

非网络启动检查：`CsaOfficialApplicationTests.contextLoads` 通过，证明当前测试配置下 Spring 容器可启动。真实监听端口和 HTTP 启动检查未执行，因为 `SCOPE.md` 对“启动本地服务”仍标记为 `TODO`。

## 发现队列与修复顺序

`security-war/attack/findings/` 当前只有 `.gitkeep`，攻击方 journal 也没有已提交结论。因此目前不存在可独立复现的 `F-XXX`，也不存在可合法创建的 `P-XXX-for-F-XXX.md`。

新发现到达后按以下顺序处理：

1. Critical/High 且可达的认证、授权、对象所有权、租户边界和高价值写操作问题。
2. 注入、上传、SSRF、敏感数据暴露、密钥和会话问题。
3. CSRF/CORS/安全响应头、限流、并发、异常和日志问题。
4. Medium/Low 问题及系统性同类路径清理。

## 剩余风险

- 尚无攻击方发现可供复现或复测，不能把当前状态声明为 `VERIFIED`。
- 路由静态盘点显示部分 Controller 内联 DTO 的结构化校验覆盖不一致；是否构成可利用问题需结合攻击方证据和具体数据流独立确认。
- 当前自动化测试覆盖了若干关键安全边界，但尚不能证明全部认证、对象级授权、输入校验、上传、CSRF/CORS 和敏感字段路径均已覆盖。
- Git 出站策略目前只在初始 URI 上执行协议、端口和 host allowlist 校验；尚无证据证明 JGit redirect、DNS 重解析或连接地址变化仍受同一策略约束，不能声称 SSRF 已完全封闭。
- `SCOPE.md` 的本地测试 URL、测试账号/角色及本地服务权限仍未填写；需要真实 HTTP 复现时必须先以范围文件中的明确授权为准。

## 当前阻塞

- 连续三轮读取 `security-war/attack/findings/` 与 `security-war/retest/`，均只有 `.gitkeep`。没有 `F-XXX` 就无法独立复现、创建对应补丁记录或请求精确复测；没有攻击方复测结果就无法把任何发现推进为 `VERIFIED`/`DISPUTED`。
- `security-war/attack/journal.md` 只有 H1-H6 假设，没有可复现结论，不能将假设冒充发现关闭。
- `SCOPE.md` 未授权启动本地服务、真实 HTTP 自动化和测试数据修改，因此无法完成目标要求的真实服务启动检查或动态请求复现。
- 已完成所有当前可安全推进的静态审查、项目自带验证、离线安全回归测试和构建。后续需要攻击方写入发现/复测 Markdown，或由范围维护者在 `SCOPE.md` 明确本地动态测试授权。

## 下一轮动作

1. 重新读取 `security-war/attack/findings/` 和 `security-war/retest/` 的新增 Markdown。
2. 对每个 `F-XXX` 保存独立控制用例和恶意用例证据。
3. 检查同类路径、修复 root cause、增加自动化回归测试并全量验证。
4. 创建对应 `security-war/defense/patches/P-XXX-for-F-XXX.md`，状态推进到 `RETEST_REQUESTED`。
5. 读取攻击方独立复测结果；对绕过重新分析，对通过项更新为 `VERIFIED`。

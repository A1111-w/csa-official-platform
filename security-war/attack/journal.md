# Attack Journal — 计协官网攻防演练

记录攻击面清单、假设、命令、观察与发现链接。

---

## 2026-07-13 阶段一：白盒源码侦察

### 授权范围确认 (SCOPE.md)
- 源码 + 配置审计：**Allowed**
- 启动本地服务 / 动态自动化测试：**TODO（未授权）** → 当前仅做静态白盒审计，不启服务、不发真实请求
- 生产、第三方、破坏性/DoS：**禁止**

### 架构指纹
- 后端：Spring Boot 3.5.8 (Java 17) + Spring Security + MyBatis-Plus 3.5.5 + Redis + JWT(jjwt 0.12.6) + jgit 6.8.0 + easyexcel 3.3.4 + jsoup 1.17.2 + knife4j 4.3.0
- 前端：Next.js (App Router, TS)
- 认证：httpOnly Cookie `CSA_AUTH_TOKEN`(JWT) + 自定义 CSRF filter(`X-CSRF-Token` header vs `CSA_CSRF_TOKEN` cookie)，Bearer 亦支持
- 防御方已大量加固（git status 显示 CSRF/JWT撤销/CORS/CSP/Actuator 锁定/启动校验等新增文件）

### 角色模型 (RoleConsts)
GUEST=0, MEMBER=1, CORE_MEMBER=2, MINISTER=3, PRESIDENT=4, ROOT=99
权限累加：LoginUser 生成 ROLE_LEVEL_0..N，Root 额外 ROLE_ADMIN。

### 访问控制矩阵（源码推断，待动态验证）
| 路径前缀 | 保护 |
|---|---|
| /api/auth/** | permitAll (login/register/send-code/csrf) |
| /api/public/** | permitAll |
| /api/test/** | hasRole ADMIN + @Profile(dev,local) |
| /actuator/health,info | permitAll；其余 denyAll |
| 其它所有 | authenticated + 方法级 @PreAuthorize/@csaSec |

### 已确认加固到位（暂不作为发现）
- SecurityConfig：CSP default-src none、HSTS、STATELESS、Actuator 锁定
- JWT：DB 加载权限（token roleLevel 不用于鉴权，防篡改）；jti 撤销；HS256≥256bit 启动强校验
- CSRF：常量时间比对(MessageDigest.isEqual)；SecureRandom 32B token
- 注册：merchantNo 提权被显式忽略；roleLevel/balance 代码硬设，无 mass assignment；验证码常量时间比对+消费即删
- CORS：fail-closed、禁通配、显式 origin、credentials
- 导出：LEVEL_4、列名白名单、xlsx（非csv）
- 全局异常：不泄栈、CsaException 分级
- UserInfoVO：无 password/openid/deleted；BeanUtil 同名拷贝
- 竞赛授权 @csaSec：publisher/editor/president 三层校验
- MyBatis：核心 mapper 全 #{} 参数化

### 进行中的假设（待深挖 / 侦察兵处理）
- H1: `auth_user` 缓存(按username存整User)——若存在改角色/封禁/软删路径不 evict → 鉴权用陈旧角色。需枚举所有 User 写点。
- H2: register 限流 key 含 username，轮换 username 绕过 email 维度限流 → 配合"验证码错不失效"可爆破6位码。中低危。
- H3: ResumeService 存储 gitRepoUrl，消费点若做 clone → SSRF（jgit）。侦察兵A468 查 GitService。
- H4: SQL 注入 / QueryWrapper orderBy/last 列名注入。侦察兵A10B 查。
- H5: 文件上传/下载路径遍历、类型绕过、存储型XSS(SVG/HTML)。侦察兵AD6C 查。
- H6: 前端 XSS / 开放重定向 / token存储 / 客户端鉴权绕过。侦察兵A38F 查。

### 侦察兵派遣
- A10B: SQL/NoSQL 注入审计
- A468: GitService SSRF/注入审计
- AD6C: 文件上传/下载审计
- A38F: 前端安全审计

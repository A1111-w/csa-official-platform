# 20261108-vue-integration | Vue 3 前端迁移与双分支交付

- 日期：2026-11-08（Asia/Shanghai）。
- 执行：Codex 主 agent；并行子模块记录见 20261108-vue-public-auth、vue-business、vue-admin-ai。
- 用户要求：将前端改为 Vue 3，保留 React 版本并做分支；随后明确要求上传 GitHub。此前要求教学计划兼顾正确性与师生使用体验。
- 工作区：D:/CSA-Project-vue，分支 codex/vue3-frontend；common Git dir 为 D:/CSA-Project/.git。
- React 保留：bf1415c，codex/agent-rag-evaluation，D:/CSA-Project-agent，已推送 origin。原 main 工作区的已有修改保持原样。
- 状态：页面迁移、最终依赖复验和双分支 GitHub 推送均已完成；远程确认见本文交付补记。

## 实际变更

24 个原公开/认证/工作台路由迁移到 Vue SFC；另有权限不足、会话失败及404兜底。Vue Router懒加载、Pinia内存会话、原service API、Cookie/CSRF、DOMPurify、响应式和暗色主题。保留资源、竞赛、简历审核、投票、人事、审计、贡献、导出、轮播、账户安全与Agent全部主要操作。

Agent保留知识问答/引用/轨迹、权限感知资料列表、文本导入删除、规则评测/JSON导出。未标注维度明确显示 N/A，空知识库不生成伪成功示例。

登录状态以 /api/sys/user/info 为准，不使用旧 Zustand缓存角色放行。会话请求去重、账号切换/退出后丢弃旧请求结果；网络失败给出重试。后端源码、数据库和迁移未改动。

部署改为 Vite dist + unprivileged Nginx；同源 /api、/files代理，深层URL回退，CSP和其它安全头。Compose/CI使用 VITE_API_URL、Node22。公开页采用客户端SPA，不声称保留Next SSR。

## 文档和治理

重写当前README、前端README、Vue学习指南；React原长讲义移至 study-and-demo-guide-react.md作历史对照。同步迁移/架构/启动/部署/安全/AI/教学/完成度说明和52项漂移检查。每节写先修、目标、步骤、成功表现、排错与教师提示，选修与核心课分开；未做真实课堂试教。

用户指定框架覆盖原默认Next约束，AGENTS、开发约束和章程1.0.1同步记录这一分支事实，其它安全与交付规则保持。

## 验证

执行目录 D:/CSA-Project-vue/csa-official-frontend，除特别说明外：

| 检查 | 实际结果 |
| --- | --- |
| vue-tsc / npm run build | 通过，Vite静态产物与页面懒加载chunks生成 |
| npm run lint | 0 errors / 0 warnings |
| npm test | 16文件、71项通过 |
| npm run test:e2e | Chromium 9项通过、3项真实后端用例跳过（未配置隔离账号） |
| 路由覆盖 | 浏览器遍历24个原路由，无JS异常、无遗漏fixture请求、无页面错误提示 |
| UI检查 | 桌面首页/Agent、390px手机首页/深色Agent截图已人工检查；截图是隔离fixture数据 |
| 文档漂移 | tools/check-study-guide-drift.ps1：52项通过 |
| Spec Kit | check-prerequisites -Json -RequireTasks -IncludeTasks 通过；requirements清单10/10 |
| Compose | 开发 config --quiet 与临时CI值的生产 config --quiet 通过 |
| 后端 | 本轮未改Java或migration，不将历史Maven数字冒充本轮重跑 |
| Docker镜像 | 未执行：Docker Desktop Linux daemon未运行，docker info连接管道失败 |
| 生产/外部模型 | 未部署、未连接生产服务；真实模型质量不在这次框架迁移验收内 |

本机旧npm代理不可达，安装命令仅本次使用 --proxy=null --https-proxy=null；未修改全局npm配置。初始完整安装成功。审计发现保留的axios与follow-redirects高风险条目，Vue分支升级到axios1.21.0 / follow-redirects1.16.2，生产依赖审计0漏洞。开发依赖补丁与干净安装的最终结果在交付补记中记录，npm10自动修复曾报内部 edgesOut 错误，没有把失败写成通过。

## 风险和边界

- 3项真实后端浏览器流程未执行；fixture通过不证明真实邮件、上传、数据库或模型环境通过。
- SPA在服务器返回静态壳，SEO/首次HTML数据与React SSR不同；部署必须提供history fallback。
- 原产品存在“后端可授权Level1比赛编辑，但React/Vue工作台入口均要求Level3”的不一致，本次保留既有入口门槛。
- GitHub源码推送不等于远程CI已通过或生产已发布。

## 回退

使用保留的React分支/工作区，独立 npm ci 并按其README运行；无需清库、删除上传卷或重写Flyway。Vue修正使用新的提交，避免覆盖原用户修改。


## 依赖复验补记

采用隔离缓存中的 npm 11.6.2 重新生成锁文件，解决宿主 npm10.9.2 的 peers/edgesOut 内部错误；没有升级或改写用户全局工具。将锁文件复制回前端后，使用宿主 npm ci 验证标准安装路径。

最终固定 Vitest4.1.12、axios1.21.0；锁文件更新 follow-redirects1.16.2、brace-expansion1.1.21、@humanfs/node0.16.8。完整锁文件审计 --audit-level=high 为 exit0：无high/critical，剩12项开发工具链moderate（glob/minimatch及受影响上层）；生产依赖0漏洞。未使用 audit fix --force 的跨版本降级建议；后续随开发工具上游修补继续更新。

本机Node22.17在可选canvas/node-gyp依赖 nopt/abbrev 上产生engine警告；推荐教学环境使用Node22最新LTS补丁，避免这类提示。前端直接依赖要求仍为Node>=22.12，最终安装/测试结果单独记录。

## 最终交付补记

2026-11-08 在最终锁文件上使用宿主 npm10.9.2 完成 `npm ci`（仅本次指定官方registry并禁用不可达代理），296个包安装成功，耗时约6分钟。随后依次执行 `npm run lint`、`npm test`、`npm run build`，均 exit0：lint无错误/警告，Vitest4.1.12下16文件71项通过，vue-tsc及Vite构建通过。重新执行 `npm run test:e2e` 得9项通过、3项因缺少隔离账号跳过；24路由、权限、Agent与响应式场景均通过。再次执行教学漂移检查得52项通过，`npm audit --omit=dev` 得0漏洞。安装时的开发工具弃用/可选依赖engine警告不等同于lint错误。

提交前显式审阅暂存文件，`git diff --cached --check` 通过；未纳入node_modules、构建产物、日志、测试截图或环境秘密。Vue业务迁移提交为 `e4cc74a3b18558429fedcdd7b9c0409192c5e9ec`，已成功推送 `origin/codex/vue3-frontend`。React保留提交 `bf1415c7370c5da3bf4deb66ea7c5bf0b53ab413` 已成功推送 `origin/codex/agent-rag-evaluation`。使用 `git ls-remote origin` 核对两个分支均与本地提交一致。

最终只读教学复查确认先修、教师演示、验收、排错及选修安排一致；修正学习指南中CSRF验收措辞，明确登录、注册、发验证码接口的豁免。任务T018/T020及项目状态随本补记完成，补记以新的文档提交追加到同一Vue分支，不重写已推送历史。Docker镜像、真实后端/模型和课堂试教仍遵循上面的未验证边界；未声称GitHub Actions或生产部署已通过。

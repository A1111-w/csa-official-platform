# Vue migration validation

先安装 Node >=22.12，在 `csa-official-frontend` 执行 `npm ci`，复制脱敏环境示例并按前端 README 配置开发后端地址。后端只使用隔离测试数据。

1. `npm run test`：服务及新增权限/表单测试通过。
2. `npm run build`：类型检查和 Vite 构建通过，生成 dist。
3. `npm run dev`：公开页加载；直接刷新 `/dashboard/ai` 仍能进入路由守卫。
4. 检查登录返回、外站 redirect 拒绝、注册隐私同意及政策读取失败、未登录/低权限深链访问。
5. 用隔离 fixtures 验证核心 CRUD、AI 引用与 N/A、报告下载；测试运行命令和实际结果见最终记录。
6. 查看 GitHub 两个分支，分别确认 React 与 Vue 框架入口。

教学准备：先选一个版本，教师提前安装依赖并准备隔离账号，再带学生从公开读取→登录→角色权限→Agent 展示逐步实践。每步确认界面可观察结果；接口失败先检查网络/后端，再检查权限，避免让初学者同时排查两套前端。

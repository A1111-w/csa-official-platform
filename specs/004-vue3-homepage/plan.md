# 实施计划 · Vue 3 官网首页改版

## 技术范围

- `HomeHero.vue` 与 `src/views/public/home/` 使用 Vue 生命周期管理画布、输入、时间和动效。原创 WebP 由前端同源提供。
- `HomeStarMap.vue` 使用真实 `RouterLink`，以 CSS 布局保证小屏和键盘可访问性。
- `HomeView.vue` 组合首屏、星图、活动、协会数据和成员，继续调用 `publicService`。
- 使用本地 API fixture 验证页面，运行仓库现有前端检查。保持 npm lockfile 及服务边界。

## 章程检查

在独立 Git 克隆中开发，改版以 Vue 3 分支为基线。前端代码、受影响的功能说明、批次记录和测试同批提交，明确区分本地验证、GitHub 分支和已部署站点。生产数据及认证凭据不参与测试。

## 验证与交付

运行 `npm run lint`、`npm run build`、`npm test`、`npm run test:e2e`，并验证 320px 至桌面宽度、键盘、光照渐变、动效暂停、API fixture 和静态资源。同步 README、公开页说明、完成度和变更记录。向 `codex/vue3-homepage-redesign` 推送并核查远端提交。

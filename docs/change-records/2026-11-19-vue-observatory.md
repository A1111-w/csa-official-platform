# 20261119-vue-observatory · 宽屏首页与栏目星图

2026-11-19（Asia/Shanghai），Codex 主任务与首屏、星体和导航协作者。基于 Vue 首页提交 `d56e0e7` 处理宽屏反馈，并补充[验收规格](../../specs/004-vue3-homepage/spec.md)。

## 体验变化

- `HomeHero.vue`、`home/planet-scene.ts` 和 `home/solar-theme.ts` 使用原创银河图、低处山脊、微缩卫星和独立星环渲染。星环有前后遮挡、细环带和投影，星空响应拖动、点击、键盘和 CSA 粒子聚形。
- `App.vue`、`HomeView.vue` 和 `HomeStarMap.vue` 调整导航、首页列宽、功能星图和页脚。时段默认跟随北京时间。访客通过「星空设置」展开时间轴和动效控制，Esc 可收起并归还焦点。
- `PublicSkyHeading.vue` 为关于、竞赛、资源、贡献者与隐私页提供独立图案，分别使用电路、流星、分层星图、节点网络和护盾。仅用于公开页面。支持减少动态效果。
- 使用现有接口、路由、角色守卫与 npm 锁文件。新图片位于 `csa-official-frontend/public/brand/csa-observatory-sky.webp`，为 2048 × 768 的同源 WebP。前后端 README、公开页和[首屏说明](../vue-home-hero.md)随代码更新。

## 验收与回退

- 前端运行 `npm run lint`、`npm run build`、`npm test` 均通过。Vitest 共 71 项通过。
- `npm run test:e2e -- --workers=2` 使用隔离响应，13 个用例通过，覆盖真实路由、宽屏边缘、折叠和焦点归还、部分接口失败重试、新栏目图案与减少动态效果。另 3 个真实后端流程因缺少隔离账号而跳过。
- Chromium 使用 2124 × 1106 和 390 × 844 视口实拍北京时间 22:30 的画面，确认背景载入、设置默认收起、页面无横向溢出与浏览器脚本异常。另检查了 320px 布局、清晨和白天色调。预览服务的活动和成员是本机示例数据。
- 本次交付在 `codex/vue3-homepage-redesign`。可对本次提交执行 `git revert` 恢复上一版视觉。真实账号与生产环境需分别验证。

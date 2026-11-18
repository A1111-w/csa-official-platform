# 20261118-vue3-homepage · Vue 3 官网首页改版

2026-11-18（Asia/Shanghai），Codex 主任务及星环、星图与浏览器验收协作者。关联 [首页改版规格](../../specs/004-vue3-homepage/spec.md)。基线为 `codex/vue3-frontend` 的 `f85dc0b`，交付分支为 `codex/vue3-homepage-redesign`。

## 实现与影响

- `csa-official-frontend/src/views/public/HomeHero.vue` 和 `home/` 下的画布及时间模块实现原创星谷、行星环、卫星、视角控制、点击星波、CSA 聚形和连续昼夜光照。星谷素材位于 `csa-official-frontend/public/brand/csa-horizon-landscape.webp`。
- `HomeStarMap.vue` 把五个现有路由组织为可操作星图。`HomeView.vue` 集成星环、星图、活动轮播、协会介绍、排名和公开成员。四个公开请求仍独立完成。失败可重试，成功的返回值仍可展示。轮播保留安全链接过滤和手动前后切换，尊重减少动态效果设置。
- 沿用 Vue 3、原有锁文件、API、鉴权和数据库结构。时间、动效和合成图像在前端计算，素材随站点同源提供。变更只涉及官网首页和对应验收。
- 同步 [公开页说明](../vue-public-auth.md)、[星环首屏](../vue-home-hero.md)、[完成度](../module-completeness.md)、[项目状态](../project-status.md)、两份 README 和变更历史。

## 核验与回退

- 在 `csa-official-frontend/` 用 Node.js 24 执行 `npm run lint`、`npm run build`、`npm test`，均通过。Vitest 共 16 个文件、71 个用例。验证环境复用了与项目锁文件版本一致的已安装依赖，并核对了 296 条实际安装记录。
- `npm run test:e2e -- --workers=2` 通过 12 个 Chromium 用例，覆盖 24 条路由，包括 3 个新增的首页用例。浏览器回归使用隔离 API fixture，3 个真实认证用例因未配置隔离测试账号而跳过。
- `npm audit --package-lock-only --omit=dev --audit-level=high` 报告 0 个生产依赖漏洞。仓库根目录的 `tools/check-study-guide-drift.ps1` 通过 52 项检查。
- 交付独立 Vue 改版分支。生产环境部署与真实账号验收另行管理。需要回退时检出 Vue 3 基线，或在已合并的分支上 revert 此次首页提交。

# Migration decisions

- Decision: Vue SFC + Vite + Router + Pinia。Rationale: 用户明确要求 Vue 3，现有 API 已独立，可在客户端完成工作台路由。Alternatives: Nuxt SSR 可保留更多服务器渲染行为，但会引入新的服务器层；本次使用 SPA 并明确部署差异。
- Decision: 复用 TypeScript service 与现有 Cookie/CSRF 拦截器。Rationale: 避免迁移同时改变协议；React store 耦合由 Pinia 替代。Alternatives: 全面重写 fetch 增加迁移风险。
- Decision: 原生控件 + 共享 CSS。Rationale: 语义表单、键盘访问清晰，减少 UI 库迁移依赖。Alternatives: 原 Radix/shadcn React 组件不能直接用于 Vue。
- Decision: React 提交保留在独立分支，Vue 使用新工作区。Rationale: 回退可直接检出原分支，保留原未提交修改。
- Primary references: [Vue guide](https://vuejs.org/guide/quick-start.html)、[Router guards](https://router.vuejs.org/guide/advanced/navigation-guards.html)、[Vite guide](https://vite.dev/guide/)。具体版本以 lockfile 与本机验证为准。

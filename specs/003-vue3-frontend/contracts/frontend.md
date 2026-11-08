# Frontend contract

- Public: `/`, `/about`, `/competitions`, `/contributors`, `/resources`, `/privacy`。
- Auth: `/login`, `/register`, `/forgot-password`, `/reset-password`。
- Dashboard: `/dashboard`、`/dashboard/profile`、`/dashboard/settings` 登录；resources/ai >=1，resume >=2，resume-reviews/competitions/vote >=3，departments/audit/contributions/member-export/carousels >=4。
- Role guard reads current server session. Menu visibility is supplementary; backend authorization remains authoritative.
- Preserve all existing `services/*.ts` endpoint paths and transport envelope unwrapping. Browser credentials use withCredentials; protected mutations use X-CSRF-Token.
- No credentials in localStorage, URLs, sample docs or commits.
- Dynamic HTML only through sanitizeHtml; image/navigation URLs reject executable schemes.
- React reference: `codex/agent-rag-evaluation` at `bf1415c`; Vue: `codex/vue3-frontend`.
- Host must serve index.html for frontend deep links and route /api to Spring Boot; do not serve API failures as HTML.

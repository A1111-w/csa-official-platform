# Implementation Plan: Cache and Concurrency Hardening

## Technical Context

- Backend: Java 17, Spring Boot 3.5.14, MyBatis-Plus, MySQL, Redis.
- Existing cache abstraction: `KeyValueStore`, with Redis and memory implementations.
- Existing public competition API: `PublicCompetitionController` and
  `CompetitionService`.
- Existing optimistic review guard: conditional status update; this feature adds
  a persisted version column.
- Existing proposal vote uniqueness: `uk_vote_proposal_voter`.

## Constitution Check

- Git changes remain in the standalone CSA repository.
- Existing user changes are preserved and are not staged by this feature.
- New database structure uses a forward-only Flyway migration.
- Code, tests, affected docs, and a feature-specific change record are delivered
  together.
- No secrets, production data, or unsupported infrastructure claims are added.

## Design

1. Add stable cache key, JSON codec, TTL jitter, and cached page value types under
   `common/cache`.
2. Implement competition list/detail Cache-Aside in `CompetitionService`, including
   negative caching, bounded hot-key lock rebuild, post-commit invalidation, and
   warning-based dependency fallback.
3. Add application-ready warmup for the public competition first page.
4. Add public endpoint rate limits.
5. Add `biz_resume.version` in V6 and compare-and-set review transitions.
6. Lock proposal rows during votes and translate unique-key races into a conflict.
7. Update the README, API map, architecture, database, local development,
   production migration, study guide, project status, module completeness, and
   changelog.

## Verification Plan

- `.\mvnw.cmd -q -DskipTests compile`
- `.\mvnw.cmd -q "-Dtest=CompetitionServiceTest,CacheTtlPolicyTest,VoteServiceTest,ResumeServiceTest" test`
- `.\mvnw.cmd test`
- `.\mvnw.cmd "-Dit.containers=true" "-Dtest=FlywayMySqlRedisIntegrationTest" test`
- `powershell -ExecutionPolicy Bypass -File tools/check-study-guide-drift.ps1`

The JDK-backed compile and focused tests are executed when the local JDK is
available. Docker/Testcontainers and staging checks remain environment-dependent.

# Tasks: Cache and Concurrency Hardening

## Phase 1: Foundation

- [x] T001 Add stable cache key, JSON codec, cached page shape, and TTL policy in `csa-official-backend/src/main/java/com/csa/official/common/cache/`
- [x] T002 Add application-ready public competition warmup in `csa-official-backend/src/main/java/com/csa/official/common/cache/CacheWarmupRunner.java`
- [x] T003 Add V6 resume version migration in `csa-official-backend/src/main/resources/db/migration/V6__resume_review_compare_and_set.sql`

## Phase 2: Public Competition Cache

- [x] T004 [US1] Implement public competition list and detail Cache-Aside in `csa-official-backend/src/main/java/com/csa/official/modules/biz/service/CompetitionService.java`
- [x] T005 [US1] Add negative caching, bounded hot-key locking, second-check behavior, and token-safe release in `csa-official-backend/src/main/java/com/csa/official/modules/biz/service/CompetitionService.java`
- [x] T006 [US2] Add post-commit competition cache generation invalidation in `csa-official-backend/src/main/java/com/csa/official/modules/biz/service/CompetitionService.java`
- [x] T007 [US1] Rate-limit public competition list and detail routes in `csa-official-backend/src/main/java/com/csa/official/modules/biz/controller/PublicCompetitionController.java`

## Phase 3: Review and Voting Concurrency

- [x] T008 [US3] Apply `status + version` compare-and-set to resume submission and review in `csa-official-backend/src/main/java/com/csa/official/modules/resume/service/ResumeService.java`
- [x] T009 [US3] Lock proposal rows and translate concurrent unique vote inserts into conflict in `csa-official-backend/src/main/java/com/csa/official/modules/sys/mapper/ProposalMapper.java` and `csa-official-backend/src/main/java/com/csa/official/modules/sys/service/VoteService.java`

## Phase 4: Verification and Documentation

- [x] T010 [US1] Add cache and TTL tests in `csa-official-backend/src/test/java/com/csa/official/common/cache/CacheTtlPolicyTest.java` and `csa-official-backend/src/test/java/com/csa/official/modules/biz/service/CompetitionServiceTest.java`
- [x] T011 [US3] Add concurrent duplicate-vote conflict test in `csa-official-backend/src/test/java/com/csa/official/modules/sys/service/VoteServiceTest.java`
- [x] T012 Update API, architecture, database, local development, migration, study, status, and module documentation in `README.md`, `docs/`, and `db/schema.sql`
- [x] T013 Add feature-specific record to `CHANGELOG.md` and `docs/change-records/2026-09-16-cache-and-concurrency-hardening.md`
- [ ] T014 Run Docker/Testcontainers and staging verification when a healthy Docker environment is available in `csa-official-backend/src/test/java/com/csa/official/integration/FlywayMySqlRedisIntegrationTest.java`

## Dependencies

`T001` and `T003` precede `T004-T009`; `T004-T009` precede `T010-T013`;
`T014` depends on a healthy Docker environment.

## Phase 5: Convergence

The 2026-09-18 source audit found unmet requirements despite the earlier checks.
The earlier checkmarks record the initial implementation, not final acceptance.

- [x] T015 CRITICAL Correct completion claims and record the audit and current evidence in `docs/project-status.md`, `CHANGELOG.md` and a new change record per Constitution I/II/V (contradicts).
- [x] T016 Preserve versioned JSON through `RedisKeyValueStore` and add actual adapter regression tests per FR-001 and US1/AC1 (contradicts).
- [x] T017 Add bounded public-read local fallback and outage-aware public rate limiting without relaxing authentication endpoints per US1/AC4 and FR-007 (partial).
- [x] T018 Fence detail refills with generation keys, verify after-commit/rollback invalidation and lock ownership, and run deterministic concurrent-miss tests per FR-003/004 and SC-003 (partial).
- [x] T019 Close resume save/submit/review races with versioned conditional writes and illegal-transition tests per FR-008 and US3/AC1 (partial).
- [x] T020 Update V6 integration assertions and add Redis/MySQL concurrency scenarios, keeping unavailable infrastructure explicitly pending per SC-004/005 and Constitution V (partial).

T020 is complete as a code-and-test-definition task: the V6 schema, CAS, unique-vote,
and Redis `SETNX` assertions are present. T014 remains open because this workstation
still has no healthy Docker/Testcontainers or staging environment to execute them.

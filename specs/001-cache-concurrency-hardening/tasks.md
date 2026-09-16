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

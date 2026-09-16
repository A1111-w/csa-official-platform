# Feature Specification: Cache and Concurrency Hardening

**Feature Branch**: `codex/development`
**Created**: 2026-09-16
**Status**: Implemented with infrastructure verification pending
**Input**: `03-计算机协会项目完善清单-20260917.md`

## User Scenarios & Testing

### User Story 1 - Public competition reads remain fast and bounded (Priority: P1)

As a visitor, I want public competition pages to remain responsive during a short
traffic burst, while missing or unpublished IDs do not cause unlimited database
queries.

**Acceptance Scenarios**:

1. Given a published competition, when the public detail is requested twice, the
   first request may read the database and the second request reads the cached
   JSON value.
2. Given a missing or unpublished ID, when the public detail is requested twice
   within the negative-cache TTL, the second request returns 404 without another
   database read.
3. Given simultaneous misses for one detail ID, when one request owns the rebuild
   lock, other requests wait for the rebuilt value or return a bounded retry
   response.
4. Given a cache dependency failure, when the public endpoint is called, the
   application falls back to a bounded database path and does not claim a cache hit.

### User Story 2 - Published competition changes become visible (Priority: P1)

As an administrator, I want a successful competition write to invalidate affected
public reads only after the database transaction commits.

**Acceptance Scenarios**:

1. Given a successful competition insert or update, when the transaction commits,
   the public list generation advances and the detail and negative-detail keys for
   that ID are removed.
2. Given a rolled-back competition write, when a visitor reads the public page,
   no invalidation is emitted by the failed transaction.

### User Story 3 - Review and voting state cannot be overwritten by stale requests (Priority: P1)

As a member or reviewer, I want concurrent submissions, reviews, and votes to
preserve one valid state transition and one vote per proposal and voter.

**Acceptance Scenarios**:

1. Given two review requests using the same resume version, only one conditional
   update succeeds and the other receives a conflict.
2. Given two votes by the same voter for one proposal, the database uniqueness
   boundary converts the losing insert into a conflict.
3. Given concurrent votes for one proposal, the proposal row is locked while the
   vote, tally, and final status transition run in one transaction.

## Functional Requirements

- **FR-001**: The system MUST use versioned, explicit public competition cache keys
  and JSON values.
- **FR-002**: The system MUST cache missing or unpublished public competition
  details with a short-lived `__NULL__` marker.
- **FR-003**: The system MUST use a random lock token, bounded lock TTL, second
  cache check, and token-checked release for hot detail rebuilds.
- **FR-004**: The system MUST invalidate public competition caches after commit,
  and failed invalidation MUST be observable through warning logs and audit data.
- **FR-005**: The system MUST apply bounded TTL jitter to competition list and
  detail cache values.
- **FR-006**: Cache warmup MUST be limited to the public competition first page
  and MUST NOT prevent application readiness when it fails.
- **FR-007**: Public competition list and detail endpoints MUST apply rate limits.
- **FR-008**: Resume review transitions MUST compare both the expected status and
  the persisted version.
- **FR-009**: Proposal voting MUST lock the proposal row in the transaction and
  preserve the existing unique `(proposal_id, voter_id)` constraint.
- **FR-010**: Documentation and the change record MUST describe implementation,
  verification, limitations, and rollback.

## Success Criteria

- **SC-001**: Unit tests prove positive caching, negative caching, TTL bounds, and
  concurrent duplicate-vote conflict behavior.
- **SC-002**: A public cache miss never waits indefinitely for another rebuild.
- **SC-003**: A successful write cannot expose a stale public competition value
  through the old generation after commit.
- **SC-004**: The Flyway chain contains the resume version column without rewriting
  V1-V5.
- **SC-005**: The feature documentation identifies all unexecuted Docker/staging
  checks instead of reporting them as passed.

## Assumptions

- Redis remains the distributed implementation and memory remains a local
  single-instance fallback.
- Existing public competition response shapes remain unchanged.
- P2 additions such as Bloom filters and RocketMQ are outside this feature.

## Out of Scope

- Production QPS claims or load-test results not supported by a recorded run.
- Bloom filters, RocketMQ, Redis Cluster, or tenant isolation.

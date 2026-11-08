# CSA Official Constitution

## Core Principles

### I. Source-Based Documentation

Documentation of implemented behavior MUST agree with source, configuration,
and observed verification. Agents MUST distinguish implemented functionality,
planned work, historical evidence, and unverified claims. Specifications define
accepted requirements; agents MUST NOT weaken them to disguise code defects.
This branch contains a Spring Boot backend and Vue 3 frontend. The user-requested
2026-11-08 migration retains React/Next.js on `codex/agent-rag-evaluation`.

### II. Documentation and Records Are Part of Every Feature

Every agent MUST update affected documentation and append a separate entry to
`CHANGELOG.md` before declaring a feature complete. This applies to fixes,
refactors, configuration, documentation-only changes, and delegated work.
For functional work, a change record alone does not replace actual behavior or
usage documentation. Documentation-only work updates its target document and
the record. Every record MUST contain author, date, scope, affected paths,
related task where applicable, verification results, risks, and rollback.

### III. Independent Git and Preserved User Work

Git commands MUST explicitly target the verified current project root and its
own standalone `.git`. Other projects, the old CSA worktree, and global Git
configuration MUST remain unchanged. Agents MUST preserve existing user edits,
stage explicit paths, and review staged changes. Destructive operations,
remote changes, and pushes require an explicit user request.
Secrets, agent sessions, and `.migration-backup/` MUST NOT be committed.

### IV. Scoped Changes and Security Boundaries

Changes MUST remain within agreed scope and follow existing module boundaries.
Authentication cookies, CSRF, backend authorization, current account state,
session versions, and file ownership checks MUST remain enforced.
Database changes require forward Flyway migrations; applied migrations MUST
NOT be rewritten. Production data, secret files, and recovery scripts MUST NOT
be used as automatic development or test setup.

### V. Reproducible Verification and Accountable Handoffs

Each feature MUST have risk-appropriate acceptance checks and recorded outcomes.
Failed, skipped, and unexecuted checks MUST be identified separately. Required
checks that have not passed prevent completion. A coordinating agent MUST
verify delegated code, documentation, and records before accepting delivery.
Historical test counts or CI runs MUST NOT be presented as current evidence.

## Development Constraints

`AGENTS.md` is the canonical agent entry point; `agent.md` is navigation only.
`docs/development-constraints.md` defines implementation rules, documentation
impact, and the completion checklist. `docs/project-status.md` describes the
current checkout and evidence limitations. `docs/templates/change-record.md`
defines record fields; `CHANGELOG.md` retains delivery history.

The backend Maven and frontend npm manifests define actual dependency versions.
Avoid incidental upgrades and additional lockfile formats. Project-local
Spec Kit skills and `.specify/` remain versioned. Bundled templates, scripts,
and generated skills MUST NOT be rewritten solely to encode project policy.

## Delivery Workflow

1. Verify repository identity and inspect existing changes.
2. Read rules, source, configuration, and relevant specifications.
3. Define acceptance criteria and affected documentation.
4. Implement and validate the scoped change.
5. Update actual behavior documentation and append its change record.
6. Reconcile task status, review the diff, and report evidence and limitations.

Every feature or user story in plans and tasks MUST include its own validation,
documentation update, and record. These cannot be postponed to a global final
phase. Missing artifacts leave work incomplete. Shared-file updates MUST be
integrated without discarding another agent's changes.

These are agent and review obligations. They MUST NOT be described as an
installed automatic Git hook or documentation CI gate unless one is implemented.

## Governance

This constitution governs project development. Dependent instructions MUST
remain consistent with it. Changes require a user request or explicit approval,
a rationale, a version/date update, synchronized dependent guidance, and a
`CHANGELOG.md` entry. Surface conflicts rather than silently bypassing rules.

Use a major version for incompatible governance changes, a minor version for
new or materially expanded obligations, and a patch version for clarifications.
Review compliance for every delivered feature. Missing documentation or a
missing change record is a delivery failure, not optional follow-up work.

**Version**: 1.0.1 | **Ratified**: 2026-09-16 | **Last Amended**: 2026-11-08

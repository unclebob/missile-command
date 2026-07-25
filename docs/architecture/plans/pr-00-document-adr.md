# PR 0 — Document ADR and plans

**Task:** `arch-docs-adr`  
**Priority:** Docs only  
**Depends on:** none  
**Behavior change:** none

## Goal

Record architecture decisions and the extraction sequence so later PRs share one plan.

## Deliverables

1. `docs/architecture/ADR-001-modular-core-and-extraction-plan.md`
2. Design doc §3 pointer to the ADR
3. `docs/architecture/plans/*` (this directory)

## Steps

1. Confirm ADR describes current modules (`wave-schedule`, `wave-lifecycle`, dual hosts).
2. Confirm design doc links to ADR.
3. Add per-PR implementation plans and index README.
4. Commit docs only (no production code).

## Verification

- [ ] ADR and plans readable from repo root
- [ ] Design doc relative link resolves
- [ ] No code changes required for this PR

## Handoff

Docs may land with the first code handoff. No separate coder work if files already committed.

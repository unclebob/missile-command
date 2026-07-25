# PR 9 — Docs + property invariants

**Task:** `arch-docs-invariants`  
**Priority:** P3  
**Depends on:** PR 1–6 ideally; can partial-ship earlier  
**Behavior change:** none (tests only + docs)

## Goal

Docs match code; core invariants are executable.

## Docs

1. Refresh design doc §3 module list to match ADR (or “see ADR” only).  
2. Mark ADR status items completed as PRs land (checkbox section).  
3. US-10 bonus cities text: place only at wave end (align with Gherkin).  

## Property / unit invariants

Add or extend property tests:

| Invariant | Check |
|-----------|--------|
| Living cities ≤ 6 | after any place/award sequence |
| Place only at wave end | score sync never increases living when destroyed exist |
| THE END | living=0 ∧ reserve=0 ⇔ the-end after evaluate |
| wave-attack | nil or 1..`attacks-per-wave` after tick transitions |
| Sky origins | in [0, width) |

Prefer pure functions from bonus-cities / wave-schedule modules.

## Acceptance cleanup (opportunistic)

When touching features, replace multi-tick inline loops in steps with named helpers already tested in unit specs.

## Verification

- [ ] Property suite for new invariants  
- [ ] Docs review  
- [ ] Arch check  

## Done when

New contributor can read ADR + this plan index and run invariant tests to understand the rules.

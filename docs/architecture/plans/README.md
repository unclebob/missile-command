# Modular core — implementation plans

Parent ADR: [`../ADR-001-modular-core-and-extraction-plan.md`](../ADR-001-modular-core-and-extraction-plan.md)

Hand off **one task at a time** (SwarmForge: complete pipeline before starting the next).

| Order | Task name | Plan | Status |
|------:|-----------|------|--------|
| 0 | `arch-docs-adr` | [pr-00-document-adr.md](pr-00-document-adr.md) | Done (docs landed) |
| 1 | `wave-start-in-core` | [pr-01-wave-start-in-core.md](pr-01-wave-start-in-core.md) | **Done** (QA merged) |
| 2 | `sfx-event-contract` | [pr-02-sfx-event-contract.md](pr-02-sfx-event-contract.md) | **Done** (QA merged) |
| 3 | `extract-bonus-cities` | [pr-03-extract-bonus-cities.md](pr-03-extract-bonus-cities.md) | **Done** (QA merged) |
| 4 | `extract-combat` | [pr-04-extract-combat.md](pr-04-extract-combat.md) | **Done** (QA merged; defensive/fireball slice) |
| 5 | `extract-shell` | [pr-05-extract-shell.md](pr-05-extract-shell.md) | Queued to coder (prio 54) |
| 6 | `core-testing-api` | [pr-06-core-testing-api.md](pr-06-core-testing-api.md) | Queued to coder (prio 55) |
| 7 | `host-input-split` | [pr-07-host-input-split.md](pr-07-host-input-split.md) | Queued to coder (prio 56) |
| 8 | `seedable-sky-rng` | [pr-08-seedable-sky-rng.md](pr-08-seedable-sky-rng.md) | Queued to coder (prio 57) |
| 9 | `arch-docs-invariants` | [pr-09-arch-docs-invariants.md](pr-09-arch-docs-invariants.md) | Queued to coder (prio 58) |

## Rules for every PR

1. Behavior-preserving unless the plan explicitly changes rules.
2. Re-export from `core` until hosts/acceptance are updated.
3. `bb script/arch_check.bb` green.
4. Unit specs green; acceptance for touched features green.
5. Do not start the next plan until this task’s SwarmForge chain completes (or user overrides).

# Modular core — implementation plans

Parent ADR: [`../ADR-001-modular-core-and-extraction-plan.md`](../ADR-001-modular-core-and-extraction-plan.md)

Hand off **one task at a time** (SwarmForge: complete pipeline before starting the next).

| Order | Task name | Plan | Status |
|------:|-----------|------|--------|
| 0 | `arch-docs-adr` | [pr-00-document-adr.md](pr-00-document-adr.md) | **Done** (docs landed) |
| 1 | `wave-start-in-core` | [pr-01-wave-start-in-core.md](pr-01-wave-start-in-core.md) | **Done** (QA merged) |
| 2 | `sfx-event-contract` | [pr-02-sfx-event-contract.md](pr-02-sfx-event-contract.md) | **Done** (QA merged) |
| 3 | `extract-bonus-cities` | [pr-03-extract-bonus-cities.md](pr-03-extract-bonus-cities.md) | **Done** (QA merged) |
| 4 | `extract-combat` | [pr-04-extract-combat.md](pr-04-extract-combat.md) | **Done** (full combat: defensive, enemies, MIRV, smart, flyers) |
| 5 | `extract-shell` | [pr-05-extract-shell.md](pr-05-extract-shell.md) | **Done** (QA merged) |
| 6 | `core-testing-api` | [pr-06-core-testing-api.md](pr-06-core-testing-api.md) | **Done** (route/static helpers in `testing.cljc`; core re-exports) |
| 7 | `host-input-split` | [pr-07-host-input-split.md](pr-07-host-input-split.md) | **Done** (`jvm.input` play + `cli` / `telemetry` / `scenario`) |
| 8 | `seedable-sky-rng` | [pr-08-seedable-sky-rng.md](pr-08-seedable-sky-rng.md) | **Done** (QA merged) |
| 9 | `arch-docs-invariants` | [pr-09-arch-docs-invariants.md](pr-09-arch-docs-invariants.md) | **Done** (QA merged) |

## Post-review implementation plans

| Order | Task name | Plan | Status |
|------:|-----------|------|--------|
| R1 | `core-facade-boundary` | [rec-01-core-facade-boundary.md](rec-01-core-facade-boundary.md) | Ready |
| R2 | `shared-host-input-policy` | [rec-02-shared-host-input-policy.md](rec-02-shared-host-input-policy.md) | Ready |
| R3 | `jvm-sketch-coordinator-split` | [rec-03-jvm-sketch-coordinator-split.md](rec-03-jvm-sketch-coordinator-split.md) | Ready |
| R4 | `unify-event-sfx-contract` | [rec-04-unify-event-sfx-contract.md](rec-04-unify-event-sfx-contract.md) | Ready |
| R5 | `qa-wait-wall-clock` | [rec-05-qa-wait-wall-clock.md](rec-05-qa-wait-wall-clock.md) | Ready |
| R6 | `acceptance-step-structure` | [rec-06-acceptance-step-structure.md](rec-06-acceptance-step-structure.md) | Ready |
| R7 | `generated-artifacts-policy` | [rec-07-generated-artifacts-policy.md](rec-07-generated-artifacts-policy.md) | Ready |

## QA improvement plans

| Order | Task name | Plan | Status |
|------:|-----------|------|--------|
| Q1 | `qa-consolidation-efficiency` | [qa-consolidation-efficiency.md](qa-consolidation-efficiency.md) | Ready |

## Rules for every PR

1. Behavior-preserving unless the plan explicitly changes rules.
2. Re-export from `core` until hosts/acceptance are updated.
3. `bb script/arch_check.bb` green.
4. Unit specs green; acceptance for touched features green.
5. Do not start the next plan until this task’s SwarmForge chain completes (or user overrides).

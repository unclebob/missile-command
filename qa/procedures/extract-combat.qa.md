# QA: Extract combat module (defensive / fireball phase)

**Task:** `extract-combat`  
**Suite:** defensive-missiles-fireballs / fire keys / click zone / wave-banner  
**Gherkin:** `features/defensive-missiles-fireballs.feature`, fire features  
**Plan:** `docs/architecture/plans/pr-04-extract-combat.md`

Verify defensive missiles, fireballs, and destroyable-target hits live in `missile-command.combat`. Core `tick` uses `combat/tick-defensive-phase` (or the three combat steps). Enemy/flyer ticks may still live on core for this PR slice.

## Rules

| Item | Detail |
|------|--------|
| Module | `tick-defensive`, `tick-fireballs`, `destroy-targets-in-fireballs`, `spawn-fireball-at` |
| Core playing tick | Calls combat for defensive/fireball phase |
| Behavior | Unchanged fire/intercept/boom path |

## Procedure

### A. Automated — unit + acceptance (defensive-missiles-fireballs + fire) + arch + property.

### B. Static — combat module owns defensive/fireball ticks; core does not define private `tick-defensive-missiles` body.

### C. Host fire — start, aim, fire key → `missiles_in_flight` ≥ 1 (defensive path live via combat).

### D. Look-and-feel — deferred until further notice.

## Pass criteria

- Automated green; host C holds; L&F skipped.

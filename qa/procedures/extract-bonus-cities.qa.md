# QA: Extract bonus-cities module

**Task:** `extract-bonus-cities`  
**Suite:** bonus-cities / the-end  
**Gherkin:** `features/bonus-cities.feature`, `features/the-end.feature`  
**Plan:** `docs/architecture/plans/pr-03-extract-bonus-cities.md`

Verify bonus award/place algorithms live in `missile-command.bonus-cities` with core re-exports only. Player-visible behavior unchanged: award reserve mid-wave, place only at wave end.

## Rules

| Item | Detail |
|------|--------|
| Module | `src/missile_command/bonus_cities.cljc` owns `sync-from-score`, `apply-from-reserve` |
| Core | Re-exports / thin wrappers; no award/place loop body |
| Behavior | Threshold → reserve; place only after wave resolution; living ≤ 6 |

## Procedure

### A. Automated — unit + acceptance (bonus-cities, the-end) + arch-check + property.

### B. Static — module has apply/sync; core does not contain `lowest-destroyed-city-id` / place loop.

### C. Host mid-wave — score+destroyed+reserve while playing: living unchanged, reserve held.

### D. Look-and-feel — deferred until further notice.

## Pass criteria

- Automated green; host C holds; L&F skipped.

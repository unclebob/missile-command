# QA: Sequential attacks and wave banner

**Task:** `sequential-attacks-banner`  
**Suite:** waves-and-rearm / wave-banner / advanced enemies  
**Gherkin:** `features/waves-and-rearm.feature`, `features/wave-banner.feature`

Verify each wave runs **3 sequential attacks (salvos)**. Attacks 1–2 are **ballistic-only** (3 missiles each). The **final attack** also spawns MIRVs / smart bombs / flyers per wave metrics. Only after the **last** attack is cleared does the **wave banner** appear.

## Rules

| Item | Detail |
|------|--------|
| Attacks per wave | 3 |
| Missiles per attack | 3 ballistics |
| Specials | Only on attack 3 (last) |
| Advance | Next attack starts only when sky is clear (no enemies/flyers) |
| Banner | After attack 3 clears; then next wave attack 1 |

## Procedure

### A. Automated — unit + accept (waves + banner) + arch-check.

### B. Attack 1 — start wave → `wave_attack=1`, ballistics only, specials 0.

### C. Sequence — clear sky → `wave_attack=2` (ballistics only) → clear → `wave_attack=3`.

### D. Final specials — on wave ≥ 5, attack 3 includes specials (mirv/smart/flyer as scheduled).

### E. Banner — after attack 3 clears → `screen=wave-banner`, then wave advances and attack 1 of next wave.

## Pass criteria

- Host B–E match rules.

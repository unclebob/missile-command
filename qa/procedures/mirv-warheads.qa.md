# QA: MIRV warheads

**Task:** `mirv-warheads`  
**Suite:** mirv-warheads  
**Gherkin:** `features/mirv-warheads.feature`

Verify MIRV-capable enemy missiles: single target before split; split into multiple independently targeted child warheads at a documented progress; destroying the parent before split prevents children; children destroy cities if unintercepted and can be destroyed by fireballs; later waves schedule MIRVs.

## Design defaults (parameterized in Gherkin)

| Parameter | Default / intent |
|-----------|------------------|
| Split progress | ~0.5 along parent path (examples vary) |
| Child count | 2–4 (examples) |
| Wave intro | No MIRVs on early waves (1–3); MIRVs from mid waves (e.g. wave 4+) |
| Children | Independent targets (distinct cities/batteries when available) |
| Parent after split | Removed; only children remain |

Out of scope: smart bombs (US-12), flyers (US-13).

## Preconditions

- US-07/US-08 (enemies, fireballs, waves).
- Documented QA launch:
  - `bb play --qa`
  - `--qa-scenario` with MIRV staging (see below)
  - `--qa-events` / `--qa-speed` for intercept timing

## UI Event Boundary

- Automated: unit + acceptance only.
- Running app: scenario + events + telemetry only.
- Telemetry must distinguish MIRV parent vs child when present (e.g. `enemy_kind=mirv|child` or equivalent), plus origin/position/target, `enemy_missiles=`.

## Scenario staging

**Single MIRV (parent before/after split):**
```edn
{:enemies [{:kind :mirv
            :target [:city 0]
            :split-progress 0.5
            :child-count 3}]}
```

**Early intercept (kill parent before split):**
```edn
{:enemies [{:kind :mirv :target [:city 1] :split-progress 0.5 :child-count 3}]}
```
```text
aim 400 100
key x
wait 1.5
quit
```
(with `--qa-speed` as needed)

**Wave schedule (no staged enemies):**
```edn
{:wave 4}
```
or normal play advanced to wave 4+.

## Telemetry

| Signal | Use |
|--------|-----|
| `enemy_missiles=` | 1 before split; `child_count` after |
| Parent vs child kind | Confirm parent gone after split |
| `enemy_target=` | Children use more than one distinct target after split |
| Fireball radius / enemy position | Child destroy within radius |

## Procedure

### A. Automated

1. Unit tests — success.
2. Acceptance including `mirv-warheads` — success.
3. Arch-check if documented — success.

### B. Pre-split

4. Stage one MIRV. Assert one enemy in flight, progress &lt; split, kind parent/MIRV.

### C. Split

5. Advance until split. Assert parent gone, exactly `child_count` children, progress advancing, **more than one distinct target** among children.

### D. Kill parent early

6. Fireball on parent path before split progress. Assert 0 enemies, all cities living, no children ever appeared.

### E. Child impact / intercept

7. Let children fly unintercepted: living cities decrease by number of city-bound children that impact.
8. After split, place fireball on one child path: that child destroyed; others remain.

### F. Wave schedule

9. Wave 1–2: MIRV schedule count 0.
10. Wave 4+: schedule includes ≥1 MIRV (per feature table).

## Pass criteria

- Acceptance for `mirv-warheads` passes.
- Split, pre-split kill, child impact, and child fireball kill match Gherkin.
- Early waves have no MIRVs; mid/later waves schedule them.

# QA: Defensive missiles and fireballs

**Task:** `defensive-missiles-fireballs`  
**Suite:** defensive-missiles-fireballs  
**Gherkin:** `features/defensive-missiles-fireballs.feature`

Verify missiles fly to aim, become fireballs (start/max/shrink/end times), report center/radius each sample, destroy in-blast stubs, leave out-of-blast stubs.

## Preconditions

- US-06 implemented.
- `bb play --qa [--qa-scenario …] [--qa-events …]`

## UI Event Boundary

Scenario for optional `:targets`; events for aim/fire/wait; telemetry for vectors and fireball phases — no private core API.

## Scenario / events examples

**Destroyable on detonation point:**
```edn
{:targets [{:x 400 :y 200}]}
```
```text
aim 400 200
key x
wait 180
quit
```

**Far miss target:**
```edn
{:targets [{:x 50 :y 50}]}
```
```text
aim 400 200
key x
wait 180
quit
```

## Telemetry

`qa-fire` vectors; `qa-fireball phase=start|max|shrink|end` with **`center_x` `center_y` `radius`** on every live sample; target `destroyed=`. See README.

## Procedure

### A. Automated — unit + acceptance.

### B. Flight and phases

1. `bb play --qa --qa-events …` fire at known aim; assert missile then fireball at aim.
2. Assert telemetry **start → max → shrink → end** with `t` monotonic; radius grows then shrinks; center stable.

### C. Targets

3. Scenario target on aim; fire; assert destroyed when radius covers target.
4. Scenario target far; fire through max; assert not destroyed.

## Pass criteria

- Phases and center/radius on every fireball line; hit/miss correct.

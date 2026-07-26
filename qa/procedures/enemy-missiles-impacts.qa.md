# QA: Enemy missiles impacts

**Task:** `enemy-missiles-impacts`  
**Suite:** enemy-missiles-impacts  
**Gherkin:** `features/enemy-missiles-impacts.feature`

Verify enemy missiles destroy cities/batteries on impact, are destroyed when inside a fireball radius, ignore fireballs they never enter, and destroyed batteries cannot fire.

## Preconditions

- US-07 (+ US-06 fireballs).
- `bb play --qa --qa-scenario … [--qa-events …]`

## UI Event Boundary

Scenario for enemies/cities/batteries; events for defensive fire; telemetry for positions, fireball center/radius, living state — no private core API.

## Scenario examples

**Enemy toward city 0 (unintercepted impact):**
```edn
{:enemies [{:target [:city 0]}]}
```

**Enemy toward left battery:**
```edn
{:enemies [{:target [:battery :left]}]}
```

**Enemy + defensive intercept** (spawn enemy; events aim/fire so fireball covers path):
```edn
{:enemies [{:target [:city 1]}]}
```
```text
aim 400 250
key x
wait 120
quit
```

## Telemetry

Enemy position/target; fireball `center_x`/`center_y`/`radius`; city/battery living; assert distance ≤ radius on intercept. README for full schema.

## Procedure

### A. Automated — unit + acceptance (enemy-missiles-impacts).

### B. Impact

1. Scenario enemy → city; no defense; assert city dead, enemy gone.
2. Scenario enemy → left battery; assert destroyed; key-fire left → none.

### C. Radius intercept

3. Scenario enemy → city; events create fireball on path; assert enemy destroyed while **within fireball radius** (telemetry distance); city living.
4. Scenario enemy + fireball far from path; assert city dies (no within-radius intercept).

## Pass criteria

- Gherkin pass; within-radius destroy; outside radius no save; impacts work.

# QA: Enemy battery targets

**Task:** `enemy-battery-targets`  
**Suite:** enemy-battery-targets  
**Gherkin:** `features/enemy-battery-targets.feature`

Verify **wave / scheduled incoming missiles** choose targets from **living cities and non-destroyed batteries** (not cities only). Unintercepted battery-targeted missiles destroy that battery; destroyed batteries are never selected.

Design: §5.5 — “choose among living cities and non-destroyed batteries.”

## Preconditions

- US-07/US-08 wave spawn and battery destruction.
- `bb play --qa` with scenario/events/speed.

## UI Event Boundary

- Scenario + telemetry only for targeting.
- Telemetry: `enemy_target=city:N` and `enemy_target=battery:left|center|right` (or equivalent).
- Look-and-feel: optional; trails toward batteries are enough for visual check.

## Scenario staging

**Wave spawn (host normal or empty scenario):**
```edn
{}
```
Let wave-1 schedule launch; inspect targets among enemies.

**Force battery-bound wave enemy:**
```edn
{:enemies [{:target [:battery :left]}]}
```

**Destroyed left battery:**
```edn
{:batteries {:left {:destroyed true}}
 :enemies []}
```
Then allow wave spawn / remaining schedule and assert no `battery:left` targets.

## Procedure

### A. Automated — unit + acceptance (`enemy-battery-targets`) + arch-check.

### B. Mixed targets

1. Launch wave with enough scheduled enemies (e.g. 9 = 6 cities + 3 batteries).
2. Assert telemetry includes **at least one city** and **at least one battery** target.
3. Full sweep of 9: every living city and every non-destroyed battery appears once as a target.

### C. Battery impact

4. Stage enemy → left/center/right battery; no defense.
5. Assert that battery destroyed; cities still 6.

### D. Exclude destroyed

6. Destroy left battery; spawn wave enemies (8 eligible).
7. Assert no target is left battery; all targets are living cities or intact batteries.

### E. Look-and-feel — confirm some trails aim at batteries; optional user nod.

## Pass criteria

- Acceptance passes.
- Wave targeting pool = living cities ∪ non-destroyed batteries.
- Battery hits destroy batteries; destroyed batteries not retargeted.

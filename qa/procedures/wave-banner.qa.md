# QA: Wave banner

**Task:** `wave-banner`  
**Suite:** wave-banner  
**Gherkin:** `features/wave-banner.feature`

Verify that **between waves** the game shows a **banner page** announcing the **next wave**. Banner text **moves in** (toward center) then **moves out**; after the sequence, play resumes on that wave (rearm, etc.). No new enemy advance during the banner.

## Presentation

| Item | Spec |
|------|------|
| Trigger | Wave complete (all threats gone) |
| Screen | `wave-banner` (banner page between playing waves) |
| Copy | `WAVE N` where N is the **upcoming** wave number |
| Motion | Text enters from off-screen → center (~2s), then exits off-screen (~2s) |
| After | Screen returns to `playing`; wave number is N; batteries rearmed per US-08 |

## Preconditions

- US-08 waves/rearm; host `bb play --qa`.
- Optional `--qa-speed` to shorten waits.

## UI Event Boundary

- Scenario/events + telemetry: `screen=wave-banner`, banner text, phase `enter|exit`, text position, wave.

## Scenario staging

```edn
{:batteries {:left {:ammo 2} :center {:ammo 2} :right {:ammo 2}}
 :enemies [{:target [:city 0]}]}
```
Clear the enemy (impact or intercept); observe banner for **WAVE 2**.

## Procedure

### A. Automated — unit + acceptance (`wave-banner`) + arch-check.

### B. Trigger

1. Play or stage one enemy on wave 1; clear it.
2. Assert `screen=wave-banner`, text **WAVE 2** (or announced next wave).

### C. Motion

3. During enter: text moves toward center over time.
4. After center: exit phase; text moves away from center.
5. No enemy missiles in flight during banner.

### D. Resume

6. After banner finishes: `screen=playing`, wave 2, rearm to 10, next threats can spawn.

## Pass criteria

- Acceptance for `wave-banner` passes.
- Banner announces next wave; text in then out; then play resumes.

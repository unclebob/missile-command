# QA: Fire by click zone

**Task:** `fire-click-zone`  
**Suite:** fire-click-zone  
**Gherkin:** `features/fire-click-zone.feature`

Verify click-zone fire (horizontal thirds), adjacent fallback when preferred battery is empty or destroyed, key fire still works, flight vectors toward click. Drive the app with **`--qa`**, optional **`--qa-scenario`**, and **`--qa-events`**; verify via telemetry.

## Preconditions

- US-05 implemented; README QA mode documented.
- Launch: `bb play --qa [--qa-scenario …] [--qa-events …]`

## UI Event Boundary

- Automated tests: documented `bb test` / `bb accept` only.
- Running app: scenario for destroyed/empty batteries; events for aim/click/key; **telemetry** for results — not core APIs.
- Look-and-feel: **explicit human approval**.

## Scenario / events examples

**Left battery destroyed (fallback on left-third click):**
```edn
{:batteries {:left {:destroyed true}}}
```

**Left empty (ammo 0), others full:**
```edn
{:batteries {:left {:ammo 0} :center {:ammo 10} :right {:ammo 10}}}
```

**Events (clicks in thirds + key fire):**
```text
click 100 150
click 450 150
click 800 150
aim 400 200
key x
quit
```

```sh
bb play --qa --qa-scenario tmp/click-zone-left-dead.edn --qa-events tmp/click-zone.txt
```

## Telemetry

Fire lines: `battery=`, `missiles_in_flight=`, per-missile `origin_*` / `target_*` matching click/aim. See README.

## Procedure

### A. Automated

1–4. Unit, acceptance (fire-click-zone), optional arch-check — all pass.

### B. Stocked batteries

5. `bb play --qa --qa-events …` with clicks in left/center/right thirds; assert telemetry batteries and vectors toward click coords.
6. Key-fire after aim; assert correct battery and vector toward aim.
7. Empty center via keys then key-fire center; assert `battery=none` (no key fallback).

### C. Destroyed / empty preferred zone

8. Scenario with left destroyed; click left third; assert `battery=center` (or next able) and vector to click.
9. Scenario with left+center destroyed; click left third; assert `battery=right`.
10. All batteries destroyed in scenario; click/key assert `battery=none`.

### D. Look-and-feel

11. **Request user approval** for look and feel.

## Pass criteria

- Acceptance + unit pass; scenario/events drive click-zone and fallback; vectors correct; **user approved look and feel**.

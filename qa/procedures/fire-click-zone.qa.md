# QA: Fire by click zone

**Task:** `fire-click-zone`  
**Suite:** fire-click-zone  
**Gherkin:** `features/fire-click-zone.feature`

Verify a click in the sky prefers the battery for that horizontal third of the current width, fires toward the click point, falls back to an adjacent able battery when the preferred one is empty or destroyed, and still allows key fire. Automated unit/acceptance checks remain; end-to-end QA drives the **running app** with real click and key events and checks documented **QA command-line switches** (telemetry and battery configuration).

## Preconditions

- Checkout includes US-05 implementation (depends on key fire, aim, layout).
- Commands are run from the project root.
- A documented command starts the application with a visible window.
- Documented command-line switches (user-interface affordances on the normal launch command, not a private in-process API) provide:
  1. **QA telemetry** that prints, for each fire, at least:
     - which battery fired (`left` / `center` / `right`), or that no battery fired
     - how many defensive missiles are in flight after the fire
     - for **each** defensive missile in flight: its **flight vector** (origin and target, or equivalent) so QA can verify launch toward the intended aim/click point
  2. **Destroyed-battery configuration** so QA can start (or reconfigure) a session with one or more batteries already destroyed (e.g. left, center, right, or a list). Exact flag syntax is documented in the README.

## UI Event Boundary

### Automated test portion

Executable QA must:

- Discover unit-test and acceptance-test commands from user-facing project documentation.
- Invoke those commands from a shell at the project root.
- Judge that portion from process exit status and command output only.

### Running-app portion

Executable QA must:

- Start the app with the documented launch command **including the QA telemetry switch** (and, when testing destroyed batteries, the **destroyed-battery configuration switch**).
- Deliver **real UI events** into the running app: pointer clicks on the playfield and keyboard key presses for the default fire keys.
- Observe **telemetry lines printed by the app** (stdout or the documented log stream from that launch) to verify which battery fired, missile-in-flight counts, and each missile’s flight vector.
- Not substitute core API calls, step handlers, or direct namespace invokes for click/key delivery, battery destruction setup, or reading fire results.

Allowed: OS-level or host automation that targets the real window (click coordinates, key events), documented CLI configuration of destroyed batteries, and reading the process’s published telemetry output.

### Manual look-and-feel

- After automated UI-event checks, request **explicit human approval** of look and feel before the suite passes.

## Telemetry contract (documented surface)

With the QA telemetry switch enabled, after each successful or attempted fire the app prints parseable output that QA can read, including at least:

- `battery=` one of `left`, `center`, `right`, or `none`
- `missiles_in_flight=` non-negative integer
- For each defensive missile currently in flight (or at least the missile created by this fire when `battery` is not `none`):
  - `origin_x=` / `origin_y=` — launch point (battery position)
  - `target_x=` / `target_y=` — aim/detonation point the missile is flying toward  
  — or an equivalent documented flight-vector representation (e.g. `from=(x,y) to=(x,y)`) that uniquely identifies origin and destination in playfield coordinates

Exact line format is defined in the project README (or equivalent); QA follows that documentation.

### Destroyed-battery configuration contract

A documented CLI switch configures which batteries start destroyed. Examples of acceptable documentation shapes (pick one and document it):

- `--destroy-batteries left` or `--destroy-batteries left,center`
- `--qa-destroy left`

After launch with that switch, the named batteries must behave as destroyed: they cannot fire on key press, and click-zone preference skips them in the fallback order (same as empty for click fallback).

### Vector verification rule

For a click at playfield coordinates `(cx, cy)` that produces a successful fire:

- The newly reported missile’s **target** must match the click point (within a documented small numeric tolerance if floats are used; exact equality if integers).
- The missile’s **origin** must be the firing battery’s position (consistent with layout for left/center/right).

For a key fire after aiming at `(ax, ay)`:

- The newly reported missile’s **target** must match that aim point (same tolerance rules).

When `battery=none`, no new flight vector for a successful launch is required (in-flight list may still be printed unchanged).

## Procedure

### A. Automated acceptance/unit

1. Open project documentation for test commands, app launch, QA telemetry, and destroyed-battery configuration switches.
2. Run the documented unit-test command; assert exit code 0 and no failures.
3. Run the documented normal acceptance command; assert exit code 0 including fire-click-zone scenarios.
4. If documentation lists an architecture check command, run it and assert exit code 0.

### B. Running app: stocked batteries — click and key events + telemetry

5. Start the app with the documented launch command **and** the QA telemetry switch (no batteries destroyed). Capture stdout (or the documented telemetry stream).
6. Confirm a window opens with playfield and crosshair.
7. **Click** in the **left** third of the playfield at a known coordinate `(cx, cy)` (real pointer event).
   - Assert telemetry reports `battery=left` and `missiles_in_flight` increased by 1 from the prior baseline (or equals 1 if starting from zero in flight).
   - Assert the new missile’s flight vector **targets** `(cx, cy)` and **originates** from the left battery position.
8. **Click** in the **center** third at a known `(cx, cy)`; assert `battery=center`, missiles-in-flight increases, and the new missile’s vector targets that click and originates from the center battery.
9. **Click** in the **right** third at a known `(cx, cy)`; assert `battery=right`, missiles-in-flight increases, and the new missile’s vector targets that click and originates from the right battery.
10. Empty the left battery using **key** events (default left fire key `Z` or `1` repeatedly until telemetry shows `battery=none` for a further left-key press, or ammo is known spent via documented behavior).
11. **Click** again in the **left** third at a known `(cx, cy)`; assert telemetry reports an **adjacent** battery (`battery=center` when only left is empty), missiles-in-flight increases, and the new missile’s vector **still targets the click** `(cx, cy)` (from the center battery origin).
12. Move aim / prepare, then **key-fire** center (`X` or `2`) toward a known aim point; assert telemetry `battery=center` and the new missile’s vector targets that aim point.
13. With center empty (or after spending as needed), **key-fire** center again; assert telemetry `battery=none` (keys do **not** fall back to adjacent batteries) and no new launch vector for a successful fire.
14. Quit the app through a normal user-facing quit path; process exits cleanly.

### C. Running app: destroyed batteries (CLI configuration)

15. Start the app again with QA telemetry **and** the destroyed-battery switch configuring **left** as destroyed (e.g. documented `--destroy-batteries left`). Capture telemetry.
16. **Key-fire** left (`Z` or `1`); assert telemetry `battery=none` (destroyed battery cannot fire; no key fallback).
17. **Click** in the **left** third at a known `(cx, cy)`; assert telemetry `battery=center` (adjacent fallback), missiles-in-flight increases, and the new missile’s vector targets `(cx, cy)` from the **center** battery origin.
18. Quit the app cleanly.
19. Start the app with QA telemetry and destroyed-battery switch configuring **center** as destroyed.
20. **Key-fire** center; assert `battery=none`.
21. **Click** in the **center** third at a known `(cx, cy)`; assert telemetry falls back per order (`battery=left` when center is destroyed and left can fire), vector targets the click from that battery’s origin.
22. Quit the app cleanly.
23. Start the app with QA telemetry and destroyed-battery switch configuring **left** and **center** as destroyed.
24. **Click** in the **left** third at a known `(cx, cy)`; assert telemetry `battery=right` (fallback continues), vector targets the click from the right battery origin.
25. Quit the app cleanly.
26. Start the app with QA telemetry and all three batteries destroyed (if the switch allows a full list).
27. **Click** in any third; assert `battery=none` and missiles-in-flight does not increase.
28. **Key-fire** any battery; assert `battery=none`.
29. Quit the app cleanly.

### D. Manual look-and-feel

30. Visually assess zone aim alignment, launches, destroyed-battery presentation (if visible), and dual input feel during the sessions above.
31. **Request approval from the user** for look and feel; do not mark the suite passed until the user explicitly approves.
32. If the user rejects look and feel, record feedback and treat the suite as failed until fixed and re-approved.

## Pass criteria

- Documented unit tests and acceptance pipeline pass, including fire-click-zone Gherkin.
- App launches with the documented QA telemetry switch and prints parseable battery, missiles-in-flight, and **per-missile flight vector** (origin and target) messages.
- Documented **destroyed-battery CLI switch** configures which batteries start destroyed.
- Real **click** events produce telemetry for the correct zone battery; empty or **destroyed** preferred batteries fall back along the zone order, with flight vectors aimed at the **click coordinates**.
- Real **key** events produce telemetry for the keyed battery only when it can fire; empty or **destroyed** key fire reports `none` (no click-style fallback).
- **User has explicitly approved look and feel.**

# QA: Aim crosshair

**Task:** `aim-crosshair`  
**Suite:** aim-crosshair  
**Gherkin:** `features/aim-crosshair.feature`

Verify the player can aim a crosshair inside the playfield and that aim points outside the playfield clamp to the nearest in-bounds position, without changing cities, batteries, ammo, or score. Automated checks use the documented test commands; look-and-feel requires a running app and explicit user approval.

## Preconditions

- Checkout includes US-03 implementation (and its US-02 layout dependencies).
- Commands are run from the project root.
- A documented command exists to start the application with a visible window so the crosshair and playfield can be judged by eye.

## UI Event Boundary

### Automated portion

Executable QA must:

- Discover unit-test and acceptance-test commands from user-facing project documentation.
- Invoke those commands from a shell at the project root.
- Judge automated success from process exit status and command output only.
- Not call project namespaces, core APIs, step handlers, or test helpers as a substitute for the documented commands.

Command-line flags that are published as part of the documented developer interface are allowed.

### Manual portion

Executable QA must:

- Start the app using the documented launch command (user-facing CLI or equivalent UI affordance).
- Interact with the running app at the user interface (pointer/mouse aim on the visible playfield).
- Not drive look-and-feel judgment solely through core APIs or acceptance step handlers.
- **Stop and request explicit approval from the human user** for look and feel before marking this suite passed.

## Procedure

### A. Automated verification

1. Open the project documentation that describes how to run tests and how to start the app (README or equivalent).
2. From the project root, run the documented unit-test command.
3. Assert exit code 0 and no failures.
4. From the project root, run the documented normal acceptance command.
5. Assert exit code 0 and that acceptance includes the aim-crosshair feature with no failures.
6. If documentation lists an architecture check command, run it and assert exit code 0.

### B. Manual look-and-feel verification

7. From the project root, start the app with the documented launch command.
8. Confirm a window opens showing the playfield (cities/batteries layout as available for this slice).
9. Move the pointer across the playfield and confirm the crosshair follows the pointer while inside the window.
10. Move the pointer toward and past the edges of the playfield and confirm the crosshair stays clamped on-screen (does not disappear off the playfield).
11. Visually assess look and feel: crosshair readability, motion responsiveness, and fit with the playfield.
12. **Request approval from the user** (human in the loop): present a short summary of what was observed and ask whether the look and feel is acceptable.
13. Do not mark the suite passed until the user explicitly approves the look and feel.
14. If the user rejects look and feel, record their feedback and treat the suite as failed until fixed and re-approved.
15. Quit the app through a normal user-facing quit path when finished.

## Pass criteria

- Documented unit tests pass.
- Documented acceptance pipeline passes, including aim-crosshair scenarios (in-bounds aim, clamp outside, forces/score unchanged).
- App starts from the documented launch command and shows a usable playfield and crosshair.
- Crosshair visibly tracks the pointer and clamps at playfield edges.
- **User has explicitly approved look and feel.**

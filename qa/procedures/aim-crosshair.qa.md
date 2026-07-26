# QA: Aim crosshair

**Task:** `aim-crosshair`  
**Suite:** aim-crosshair  
**Gherkin:** `features/aim-crosshair.feature`

Verify the player can aim a crosshair inside the playfield and that aim points outside the playfield clamp to the nearest in-bounds position, without changing cities, batteries, ammo, or score. Automated checks use the documented test commands.

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

- Start the app with documented launch (`bb play`, optionally `bb play --qa --qa-events …` for scripted aim).
- Interact at the user interface (pointer/mouse or `--qa-events` aim lines).

## Procedure

### A. Automated verification

1. Open the project documentation that describes how to run tests and how to start the app (README or equivalent).
2. From the project root, run the documented unit-test command.
3. Assert exit code 0 and no failures.
4. From the project root, run the documented normal acceptance command.
5. Assert exit code 0 and that acceptance includes the aim-crosshair feature with no failures.
6. If documentation lists an architecture check command, run it and assert exit code 0.

## Pass criteria

- Documented unit tests pass.
- Documented acceptance pipeline passes, including aim-crosshair scenarios (in-bounds aim, clamp outside, forces/score unchanged).
- App starts from the documented launch command and shows a usable playfield and crosshair.
- Crosshair visibly tracks the pointer and clamps at playfield edges.

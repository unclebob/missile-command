# QA: Fire batteries with keys

**Task:** `fire-batteries-keys`  
**Suite:** fire-batteries-keys  
**Gherkin:** `features/fire-batteries-keys.feature`

Verify left, center, and right batteries fire toward the crosshair with the default key bindings, spend ammo correctly, refuse empty or destroyed batteries, and that center missiles are faster. Automated checks use documented test commands.

## Preconditions

- Checkout includes US-04 implementation (depends on aim/crosshair and layout).
- Commands are run from the project root.
- A documented command exists to start the application with a visible window.
- Default fire keys (design): left `Z`/`1`, center `X`/`2`, right `C`/`3`.

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

- Start the app with documented launch (`bb play`, optionally `bb play --qa --qa-scenario … --qa-events …`).
- Fire through real keyboard or `--qa-events` `key` lines (default keys), not by injecting core fire commands.
- Optional scenario: `{:batteries {:left {:ammo 0}}}` etc. for empty-battery checks.

## Procedure

### A. Automated verification

1. Open the project documentation for test commands and app launch (README or equivalent).
2. From the project root, run the documented unit-test command; assert exit code 0 and no failures.
3. From the project root, run the documented normal acceptance command; assert exit code 0 including fire-batteries-keys scenarios.
4. If documentation lists an architecture check command, run it and assert exit code 0.

## Pass criteria

- Documented unit tests and acceptance pipeline pass, including fire-batteries-keys scenarios.
- Default keys fire the correct batteries toward the aim point in the running app.

# QA: Fire batteries with keys

**Task:** `fire-batteries-keys`  
**Suite:** fire-batteries-keys  
**Gherkin:** `features/fire-batteries-keys.feature`

Verify left, center, and right batteries fire toward the crosshair with the default key bindings, spend ammo correctly, refuse empty or destroyed batteries, and that center missiles are faster. Automated checks use documented test commands; look-and-feel requires a running app and explicit user approval.

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

- Start the app using the documented launch command.
- Fire batteries through the real keyboard UI (default keys), not by injecting core fire commands.
- **Stop and request explicit approval from the human user** for look and feel before marking this suite passed.

## Procedure

### A. Automated verification

1. Open the project documentation for test commands and app launch (README or equivalent).
2. From the project root, run the documented unit-test command; assert exit code 0 and no failures.
3. From the project root, run the documented normal acceptance command; assert exit code 0 including fire-batteries-keys scenarios.
4. If documentation lists an architecture check command, run it and assert exit code 0.

### B. Manual look-and-feel verification

5. Start the app with the documented launch command; confirm a window opens with playfield and crosshair.
6. Move the pointer to a clear aim point in the sky.
7. Press default left fire (`Z` or `1`); confirm a defensive missile launches from the left battery toward the crosshair and left ammo decreases by one on the HUD or equivalent visible ammo display if present.
8. Press default center fire (`X` or `2`); confirm launch from center and only center ammo decreases.
9. Press default right fire (`C` or `3`); confirm launch from right and only right ammo decreases.
10. Optionally exhaust or observe that empty/destroyed batteries do not launch (if the UI exposes those states; otherwise rely on acceptance for those cases).
11. Visually assess look and feel: launch readability, trajectory toward aim, battery distinction (especially faster center motion if visible).
12. **Request approval from the user** for look and feel; do not mark the suite passed until the user explicitly approves.
13. If the user rejects look and feel, record feedback and treat the suite as failed until fixed and re-approved.
14. Quit the app through a normal user-facing quit path.

## Pass criteria

- Documented unit tests and acceptance pipeline pass, including fire-batteries-keys scenarios.
- Default keys fire the correct batteries toward the aim point in the running app.
- **User has explicitly approved look and feel.**

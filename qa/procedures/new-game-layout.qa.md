# QA: New game layout

**Task:** `new-game-layout`  
**Suite:** new-game-layout  
**Gherkin:** `features/new-game-layout.feature`, `features/playfield-resize.feature`

Verify a new game places six cities and three batteries on a full-resolution playfield, layout scales with size (not a fixed low-res buffer), and resize reflows a fresh game. There is still no playable host UI for this story.

## Preconditions

- Checkout includes US-02 implementation on top of the project foundation.
- Commands are run from the project root.
- Network is available if dependency download is required.

## UI Event Boundary

The user interface for this story remains the **documented command line** at the project root (README or equivalent). Observable game layout is verified by the documented acceptance pipeline (which exercises the pure core through the project’s acceptance entrypoints), not by opening a Quil window.

Executable QA must:

- Discover unit-test and acceptance-test commands from user-facing project documentation.
- Invoke those commands from a shell at the project root.
- Judge success from process exit status and command output only.

Executable QA must not:

- Call project namespaces, core APIs, step handlers, or test helpers as a substitute for the documented commands.
- Require a game host window.

Command-line flags that are published as part of the documented developer interface are allowed.

## Procedure

1. Open the project documentation that describes how to run tests (README or equivalent).
2. From the project root, run the documented unit-test command.
3. Assert exit code 0 and that the output reports no failures.
4. From the project root, run the documented normal acceptance command.
5. Assert exit code 0 and that acceptance covers the new-game layout and playfield-resize features with no failures.
6. Assert the unit-test and acceptance runs complete headlessly (no game window required).
7. If documentation lists an architecture check command, run it and assert exit code 0 (pure core still free of host/IO coupling).

## Pass criteria

- Documented unit tests pass.
- Documented acceptance pipeline passes, including new-game layout and playfield resize scenarios.
- No game host UI is required for these checks.
- Architecture check passes when documented.

# QA: Project foundation

**Task:** `project-foundation`  
**Suite:** project-foundation  
**Gherkin:** `features/project-foundation.feature`

Verify a developer can run unit and acceptance checks from the project root using documented commands. There is no game UI yet.

## Preconditions

- Checkout includes the implemented project foundation (build files, pure core placeholder, test harness, acceptance pipeline).
- Commands are run from the project root.
- Network is available if the first dependency download is required.

## UI Event Boundary

The user interface for this story is the **documented command line** at the project root (README or equivalent project documentation).

Executable QA must:

- Discover the unit-test and acceptance-test commands from user-facing project documentation.
- Invoke those commands from a shell at the project root exactly as a developer would.
- Judge success from process exit status and command output only.

Executable QA must not:

- Call project namespaces, core APIs, step handlers, or test helpers as a substitute for the documented commands.
- Open a game host window (none is required for this story).

Command-line flags that are published as part of the documented developer interface are allowed.

## Procedure

1. Open the project documentation that describes how to run tests (README or equivalent).
2. Confirm it documents a unit-test command and a normal acceptance-test command (or a single documented acceptance convenience command that runs the normal pipeline).
3. From the project root, run the documented unit-test command.
4. Assert exit code 0 and that the output reports no failures.
5. From the project root, run the documented normal acceptance command (parser → generator → generated tests, or the project’s documented wrapper for that sequence).
6. Assert exit code 0 and that the output reports no acceptance failures.
7. Assert that the unit-test run completed without launching a game window (headless: no Quil/sketch window is required to pass).

## Pass criteria

- Documented unit tests pass from the project root.
- Documented acceptance pipeline passes from the project root against `features/project-foundation.feature`.
- Core unit verification does not require a game host UI.

# Recommendation 3 — JVM sketch coordinator split

**Task:** `jvm-sketch-coordinator-split`  
**Priority:** P1  
**Behavior change:** none

## Problem

`missile-command.jvm.sketch` is responsible for too many host concerns:

- launch option state
- QA event queue and timed waits
- telemetry emission
- SFX cursor/playback coordination
- persistence triggers
- frame clock and substepping
- Quil callbacks
- no-keyfocus startup and callback gating

That makes small QA or desktop-host changes risky.

## Goal

Keep `jvm.sketch` as the Quil wiring namespace and move host coordination
policies into focused JVM namespaces.

## Plan

1. Extract QA event queue handling into `missile-command.jvm.qa-runner` or
   equivalent:
   - pending event state
   - `wait` timing
   - event-to-command dispatch helpers
   - `quit` behavior hook supplied by sketch
2. Extract telemetry emission into `missile-command.jvm.telemetry-emitter`:
   - `qa-sim`
   - fire telemetry
   - fireball phase telemetry
   - SFX telemetry formatting calls
3. Extract frame timing/substep policy into `missile-command.jvm.frame`:
   - wall-clock dt
   - `--qa-speed`
   - max-dt substepping
4. Keep in `jvm.sketch`:
   - Quil `q/sketch` construction
   - setup/update/draw callback registration
   - direct calls to render/audio/persist/window adapters
5. Preserve existing public launch functions and CLI behavior.

## Guardrails

- Do not move rules into JVM namespaces.
- Do not change telemetry field names or line formats.
- Do not change QA script behavior.
- Do not change visible desktop behavior.
- Keep no-keyfocus behavior covered by the focused QA script.

## Verification

- Unit specs for any newly pure/extractable helpers.
- Parse all `qa/scripts/*.qa.bb`.
- `qa/scripts/desktop-key-focus.qa.bb`
- A representative event-heavy QA script, such as title or pause.
- `bb test`
- `bb accept`
- `bb arch-check`

## Done When

- `jvm.sketch` is mostly Quil lifecycle glue.
- QA event dispatch and telemetry emission have named homes.
- Future focus, telemetry, or QA timing changes can be made without editing the
  full sketch host loop.

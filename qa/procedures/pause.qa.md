# QA: Pause

**Task:** `pause`  
**Suite:** pause  
**Gherkin:** `features/pause.feature`

Verify **pause / resume** during play: toggle from playing (default **P** and **Esc**); while paused, **no simulation** and **no fire**; resume returns to playing with entities continuing from frozen state; pause does nothing on **title**.

Depends on **US-15** title/start. Out of scope: options from pause, full overlay polish beyond readable paused state.

## Preconditions

- Title → start → playing available.
- Documented pause/resume keys (README: P and Esc toggle, or separate resume).
- `bb play --qa` with events/speed.

## UI Event Boundary

- Scenario + events + telemetry only.
- Telemetry: `screen=playing|paused|title`, enemy progress/positions frozen while paused, ammo unchanged on paused fire.
- Events: `pause`, `resume`, or `key p` / `key escape` as documented.

## Events examples

```text
start
wait 0.5
pause
wait 1
key z
resume
wait 0.5
quit
```

## Procedure

### A. Automated — unit + acceptance (`pause`) + arch-check.

### B. Enter pause — start game; pause; assert `screen=paused`.

### C. Frozen sim — with enemies in flight, pause; wait; assert enemy positions/progress unchanged in telemetry.

### D. No fire — while paused, fire keys/click; no missiles; ammo unchanged.

### E. Resume — resume; `screen=playing`; enemies advance again; fire works.

### F. Title — on title, pause input; stay on title.

## Pass criteria

- Acceptance for `pause` passes.
- Pause freezes motion and blocks fire; resume continues run.

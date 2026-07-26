# Recommendation 5 — QA wait wall-clock timing

**Task:** `qa-wait-wall-clock`  
**Priority:** P1  
**Behavior change:** clarify/fix QA harness timing

## Problem

Manual trials showed `wait 60` sometimes ended after roughly 30 seconds, while
`wait 120` stayed up for roughly one minute. README currently says `wait` is
wall-clock seconds, independent of `--qa-speed`. QA scripts depend heavily on
this contract.

## Goal

Make QA event `wait N` mean `N` wall-clock seconds in desktop QA, regardless of
simulation speed or frame rate, and verify it with a focused script/spec.

## Plan

1. Add a focused QA timing feature or procedure entry that states:
   - `wait N` delays subsequent QA events for approximately `N` real seconds.
   - `--qa-speed` affects simulation time, not QA wait duration.
2. Add a small executable QA script that launches a short wait, measures process
   elapsed wall time, and asserts an acceptable tolerance.
3. Inspect the current wait path in the JVM QA event runner:
   - event parsing
   - pending wait state
   - frame update loop
   - process exit behavior
4. Fix any discrepancy so waits are measured by a monotonic wall-clock source
   if practical.
5. Keep long manual waits out of normal suites; use a short duration with a
   reasonable tolerance to avoid slow/flaky QA.

## Suggested Acceptance Bounds

For automated QA, prefer a short wait such as `wait 1.5` or `wait 2.0`.

- Minimum elapsed: requested seconds minus small startup/timer tolerance.
- Maximum elapsed: requested seconds plus platform startup/timer tolerance.
- Record `--qa-speed 10` in at least one case to prove simulation speed does
  not shorten the wait.

## Guardrails

- Do not change `--qa-speed` simulation semantics.
- Do not make normal acceptance tests slow.
- Do not depend on private core APIs; measure through process behavior and
  documented QA CLI affordances.
- Do not make QA timing depend on telemetry volume.

## Verification

- New focused QA wait script.
- Existing representative QA script.
- `bb test`
- `bb accept`

## Done When

- README/procedure and implementation agree on `wait` semantics.
- A focused QA check catches early-exit regressions.
- Manual one-minute trials no longer need `wait 120` to approximate one minute.

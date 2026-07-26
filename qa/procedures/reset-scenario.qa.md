# QA: Reset scenario event

Verify that scripted QA events can reset the running host to a fresh scenario
without starting a second JVM process.

## Preconditions

- README documents `reset-scenario <file>` as a QA event command.
- `bb play --qa --no-keyfocus --qa-scenario <file> --qa-events <file>` is
  available.

## Procedure

1. Create scenario A with `:screen :playing` and `:wave 2`.
2. Create scenario B with `:screen :playing` and `:wave 5`.
3. Launch the JVM host once with scenario A and an event file that waits,
   issues `reset-scenario` for scenario B, waits, and quits.
4. Assert telemetry first reports playing wave 2.
5. Assert later telemetry reports playing wave 5.

## Pass Criteria

- The process exits successfully.
- The telemetry includes both wave 2 before reset and wave 5 after reset.

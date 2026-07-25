# QA: Host input / QA split

**Task:** `host-input-split`  
**Suite:** desktop-host / project foundation  
**Plan:** `docs/architecture/plans/pr-07-host-input-split.md`  
**Status:** **Done** (`jvm.cli` / `jvm.telemetry` / `jvm.scenario` split with re-exports)

Verify:

- `jvm/input.clj` stays pure (no Quil): command mapping + re-exports of CLI/scenario/telemetry.
- Sibling modules own CLI parse, telemetry formatters, and scenario apply.
- `jvm/sketch.clj` depends on `input` for QA paths; live play still works.
- Full automated suite green; host QA path still loads scenarios.

## Procedure

### A. Automated — unit + accept + arch + property.

### B. Static — plan marks Done; input ns has no Quil; sketch requires input; cli/telemetry/scenario modules present.

### C. Host — `--qa` + scenario EDN still plays (start → attack 1).

### D. Look-and-feel — deferred until further notice.

## Pass criteria

- Suite green; B–C hold; L&F skipped.

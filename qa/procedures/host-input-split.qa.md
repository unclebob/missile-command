# QA: Host input / QA split

**Task:** `host-input-split`  
**Suite:** desktop-host / project foundation  
**Plan:** `docs/architecture/plans/pr-07-host-input-split.md`  
**Status:** **Deferred** (plan index: hosts already slimmer after wave-start-in-core)

Verify the deferred outcome is intentional and hosts remain maintainable:

- `jvm/input.clj` stays pure (no Quil): CLI, scenario, telemetry, command mapping.
- `jvm/sketch.clj` depends on `input` for QA paths; live play still works.
- Full automated suite green; host QA path still loads scenarios.

## Procedure

### A. Automated — unit + accept + arch + property.

### B. Static — plan marks Deferred; input ns has no Quil require; sketch requires input; scenario/telemetry entry points present.

### C. Host — `--qa` + scenario EDN still plays (start → attack 1).

### D. Look-and-feel — deferred until further notice.

## Pass criteria

- Suite green; B–C hold; L&F skipped.

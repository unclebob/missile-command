# QA Scripts

Executable QA scripts live in `qa/scripts/` and their procedure documents live
in `qa/procedures/`.

## Run Modes

Use `bb qa-suite smoke` for a fast confidence pass. It runs global architecture
and property checks once, then runs representative host and documentation
scripts.

Use `bb qa-suite feature <name>` for focused debugging of one script, for
example:

```sh
bb qa-suite feature sound-events
```

Use `bb qa-suite full` for the complete executable QA script suite. This mode
runs `bb arch-check` and `bb property` once, then runs the individual scripts
with duplicate in-script global checks skipped by the suite wrapper.

Direct script execution remains supported:

```sh
bb qa/scripts/wave-banner.qa.bb
```

Direct execution may still run a script's own defensive global checks. Suite
mode is the preferred path when running more than one QA script.

## Shared Helpers

Common Babashka helpers are in `qa/scripts/lib/common.bb`. New or migrated
scripts should load this file instead of copying helpers for shell execution,
telemetry parsing, event writing, scenario writing, and standard QA host
launches.

## Coverage Ownership

Wave and banner coverage is split as follows:

- `wave-banner` owns banner text, phase, movement, frozen combat during banner,
  and resume to the next wave.
- `sequential-attacks-banner` owns attack sequencing and special-threat
  scheduling before the banner.
- `wave-start-in-core` owns the architecture invariant that the core starts
  wave attacks.

Sound coverage is split as follows:

- `sound-events` owns gameplay sound emission timing, including launch and
  THE END events.
- `sfx-event-contract` owns static SFX contract shape and host cursor/mute
  behavior.

## Migration-Era Scripts

The migration-era scripts are still kept because they protect current
architecture boundaries:

- `extract-shell`
- `extract-combat`
- `extract-bonus-cities`
- `finish-modular-extraction`
- `core-testing-api`
- `host-input-split`
- `wave-start-in-core`
- `sfx-event-contract`

They should stay narrow. If an invariant moves into `bb arch-check`, retire the
corresponding script from `qa/scripts/run-suite.bb` in the same change.

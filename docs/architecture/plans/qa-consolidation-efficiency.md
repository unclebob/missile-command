# QA Consolidation and Efficiency Plan

**Task:** `qa-consolidation-efficiency`  
**Owner:** QA  
**Behavior change:** none intended  

## Problem

The QA procedures and executable scripts have grown one feature at a time.
They now repeat common runner code, rerun project-wide checks in many scripts,
launch the JVM host more often than necessary, and keep several migration-era
scripts that overlap with permanent feature QA. This makes the QA suite slower
and harder to maintain without adding proportional coverage.

## Goal

Keep the same externally visible coverage while reducing duplicated script code,
redundant global checks, and unnecessary host launches.

## Stage 1 - Shared QA Script Library

Create a shared Babashka helper namespace or loadable script under
`qa/scripts/lib/`.

Move common helpers into it:

- `die!`
- `assert!`
- shell runner returning `{:exit :out}`
- `field`, numeric field parsers, and telemetry line filters
- `write-edn!` and `write-events!`
- standard `bb play ... --qa --no-keyfocus` launch builder
- README command discovery, if still needed

Update a small representative set first, such as:

- `qa/scripts/wave-banner.qa.bb`
- `qa/scripts/sound-events.qa.bb`
- `qa/scripts/high-scores.qa.bb`

After that pattern is stable, migrate the rest.

Verification:

- Every migrated script still parses.
- The migrated representative scripts pass.
- Script output and failure messages remain clear enough for handoff diagnosis.

## Stage 2 - Suite-Level Global Checks

Stop running project-wide checks inside every feature script.

Move these to one wrapper or explicit suite step:

- `bb arch-check`
- `bb property`
- README command discovery checks that are not feature-specific

Individual feature scripts should only run global checks when the script's
purpose is the global check itself, such as architecture invariant QA.

Verification:

- A full QA run still performs `arch-check` once.
- A full QA run still performs property tests once when required.
- Feature scripts no longer spend time rerunning global checks unnecessarily.

## Stage 3 - Wave and Banner Coverage Split

Consolidate overlapping wave/banner coverage across:

- `qa/scripts/wave-banner.qa.bb`
- `qa/scripts/sequential-attacks-banner.qa.bb`
- `qa/scripts/wave-start-in-core.qa.bb`
- related procedures

Keep one end-to-end host transition for:

- final attack clear
- `screen=wave-banner`
- banner enter/exit telemetry
- resume to next wave

Then narrow the other scripts:

- `wave-banner` owns banner text, phase, movement, and paused gameplay during
  banner.
- `sequential-attacks-banner` owns attack sequencing and special-threat
  scheduling.
- `wave-start-in-core` owns the architectural invariant that hosts do not start
  waves directly.

Verification:

- No user-visible wave/banner requirement is lost.
- The number of long `wait` host launches is reduced.
- Procedure pass criteria match the new ownership split.

## Stage 4 - Sound and SFX Contract Split

Remove overlap between:

- `qa/scripts/sound-events.qa.bb`
- `qa/scripts/sfx-event-contract.qa.bb`

Ownership:

- `sound-events` verifies gameplay emits required SFX events at the right
  moments.
- `sfx-event-contract` verifies static contract shape and host cursor behavior,
  such as `sfx-take-new` use and mute suppressing playback without suppressing
  core event logging.

Avoid testing `sfx/launch` mute behavior in both scripts unless one script is
the only place that behavior can be observed.

Verification:

- Required sound event cases remain covered.
- Contract checks remain covered.
- Duplicate launch/mute sessions are removed.

## Stage 5 - Batch Host Scenarios

Reduce repeated JVM startup cost in high-launch scripts.

Priority targets:

- `qa/scripts/options.qa.bb`
- `qa/scripts/high-scores.qa.bb`
- `qa/scripts/scoring-and-multiplier.qa.bb`
- `qa/scripts/fire-click-zone.qa.bb`

Preferred implementation:

- Add a user-facing QA event command that resets the running QA host to a named
  scenario file, for example `reset-scenario tmp/case.edn`.
- Keep this as a QA-only command-line/event affordance.
- Use one process per script where isolation is not the behavior under test.

Keep separate launches where relaunch behavior is itself required, such as
high-score persistence across process restart.

Verification:

- The batched scripts still verify the same user-visible states.
- Runtime drops measurably for the target scripts.
- State reset is explicit and does not leak between cases.

## Stage 6 - Retire Migration-Era QA Duplication

Review migration-focused scripts:

- `extract-shell.qa.bb`
- `extract-combat.qa.bb`
- `extract-bonus-cities.qa.bb`
- `wave-start-in-core.qa.bb`
- `finish-modular-extraction.qa.bb`
- `core-testing-api.qa.bb`
- `host-input-split.qa.bb`
- `sfx-event-contract.qa.bb`

For each script, choose one outcome:

- keep, if it protects a current architectural invariant
- fold static assertions into `bb arch-check`
- fold host smoke into an existing feature QA script
- delete, if it only verifies completed migration bookkeeping

Verification:

- Architecture invariants still fail loudly when violated.
- Permanent feature scripts still cover the user-visible behavior.
- Procedure docs no longer require running retired scripts.

## Stage 7 - QA Procedure Index and Run Modes

Document run modes so QA can choose the right level of confidence:

- `smoke`: global checks once plus fast host sanity scripts
- `feature <name>`: only the script for the changed feature
- `full`: all non-retired feature scripts, with global checks once

Add or update a wrapper script only if it makes the above modes executable.

Verification:

- README or QA docs explain which mode to use.
- The wrapper avoids duplicate global checks.
- Existing direct script execution remains possible for focused debugging.

## Done When

- Shared QA helpers remove most duplicated script boilerplate.
- Global checks run once per suite instead of once per feature script.
- Wave/banner and sound/SFX coverage have clear ownership boundaries.
- High-launch scripts use fewer JVM process starts where behavior allows.
- Migration-era scripts are either retired or reduced to current invariants.
- QA docs describe the new suite modes and ownership split.

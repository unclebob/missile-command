# Recommendation 4 — Unified event/SFX contract

**Task:** `unify-event-sfx-contract`  
**Priority:** P1  
**Behavior change:** none audible

## Problem

The codebase still has two mental models for events:

- `:sfx-events` on state, consumed by hosts with a cursor
- `{:state s :events [...]}` returned from `core/handle` and `core/tick`

The current behavior works, but host authors and acceptance steps can easily
misread which channel is authoritative.

## Goal

Declare and enforce one public contract for host side effects, especially SFX.

## Recommended Contract

Use `:sfx-events` plus an explicit cursor as the official host contract:

1. Core appends SFX events to `:sfx-events`.
2. Hosts keep a local cursor count.
3. Hosts play `(sfx-take-new state cursor)`.
4. Hosts advance the cursor to `(count (sfx-events state))`.
5. `:events` returned by `handle`/`tick` is either removed, deprecated, or
   documented as non-SFX legacy command feedback.

This is the smallest change because both hosts already largely behave this way.

## Plan

1. Update `sfx.cljc` docs to define the cursor contract.
2. Audit all consumers of `:events` returned from `core/handle` and
   `core/tick`.
3. Either:
   - remove unused `:events` return fields where safe, or
   - rename/document them as non-SFX command feedback.
4. Make JVM and browser host SFX cursor handling call a named API, not raw
   vector slicing logic.
5. Add tests that pin:
   - `sfx-take-new` cursor behavior
   - host cursor does not replay old sounds
   - core still records expected SFX for launch, boom, warning, bonus, THE END
6. Update README/ADR wording if needed so docs do not imply two SFX channels.

## Guardrails

- Do not change audible behavior.
- Do not drop telemetry fields used by QA scripts.
- Do not clear logs in a way that breaks acceptance checks unless those checks
  are intentionally migrated in the same task.
- Keep browser autoplay unlock behavior unchanged.

## Verification

- SFX unit specs.
- `qa/scripts/sound-events.qa.bb`
- `qa/scripts/sfx-event-contract.qa.bb`
- `bb test`
- `bb accept`

## Done When

- There is one documented SFX source of truth.
- Both hosts use the same named contract.
- `:events` no longer creates ambiguity for SFX behavior.

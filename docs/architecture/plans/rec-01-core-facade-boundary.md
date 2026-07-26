# Recommendation 1 — Core facade boundary

**Task:** `core-facade-boundary`  
**Priority:** P1  
**Behavior change:** none

## Problem

`missile-command.core` remains a large coordination module. It is the public
host facade, but it also exposes testing/staging helpers and owns some logic
that has already been extracted into focused modules. That makes host authors
and acceptance steps depend on a broad, hard-to-review surface.

## Goal

Make `core.cljc` a deliberately small facade:

- construction: `new-game`, `resize`
- command entry: `handle`
- simulation entry: `tick`
- stable read APIs needed by hosts/renderers/telemetry
- temporary compatibility re-exports with clear removal notes

Move testing-only setup/staging helpers behind `missile-command.testing`.

## Plan

1. Inventory every public var in `core.cljc`.
2. Classify each public var as host-facing, acceptance-facing, spec/property
   helper, or internal compatibility re-export.
3. Move testing-only helpers into `missile-command.testing` when they are not
   required by production hosts.
4. Keep short compatibility wrappers in `core` only where existing acceptance
   steps or hosts need stable names.
5. Add a concise doc block in `core.cljc` or ADR docs describing the intended
   public surface.
6. Update specs and acceptance step handlers to use `missile-command.testing`
   for staging/setup helpers when that keeps the feature language unchanged.

## Guardrails

- Do not change game behavior.
- Do not expose host namespaces to pure modules.
- Do not force feature files to mention implementation/testing helpers.
- Do not remove a `core` wrapper until all call sites are migrated.

## Verification

- `bb test`
- `bb accept`
- `bb arch-check`

## Done When

- `core.cljc` reads as a facade instead of a mixed facade/test-helper module.
- Testing/staging helpers have an owned namespace.
- Hosts still depend only on the public core facade.

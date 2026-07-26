# Recommendation 7 — Generated artifacts policy

**Task:** `generated-artifacts-policy`  
**Priority:** P2  
**Behavior change:** none

## Problem

The repository ignores generated acceptance files and build output, but local
runs still create visible generated trees such as `acceptance/generated/`,
`build/`, and browser build assets. The intended source-vs-build-artifact
policy is not explicit enough, which increases the chance of accidental commits
or confusing dirty worktrees.

## Goal

Make generated artifact ownership explicit and give developers a simple way to
clean or inspect generated outputs without committing them.

## Plan

1. Inventory generated outputs:
   - `acceptance/generated/`
   - `build/acceptance/ir/`
   - `build/acceptance/reports/`
   - `resources/public/js/`
   - cache directories such as `.cpcache/` and `.shadow-cljs/`
2. Decide which generated outputs are never committed and which, if any, are
   source-controlled snapshots.
3. Update README or a short docs note with the policy.
4. Ensure `.gitignore` matches the policy.
5. Add a cleanup task if useful, for example `bb clean-generated`, that removes
   generated acceptance/build/browser outputs without touching source files.
6. Add a lightweight status/check task only if the team wants enforcement, such
   as failing when ignored generated files are staged.

## Guardrails

- Do not delete source files.
- Do not commit generated artifacts unless the policy explicitly says they are
  source-controlled.
- Do not edit generated acceptance files by hand.
- Do not interfere with SwarmForge local state under `.swarmforge/` or
  worktrees under `.worktrees/`.

## Verification

- Run the cleanup task in a dirty generated-output-only state and confirm source
  files remain.
- `bb accept` regenerates needed acceptance artifacts.
- `bb browser` or `npx shadow-cljs compile browser` regenerates browser assets
  when needed.

## Done When

- Developers can tell which generated files are disposable.
- `.gitignore`, docs, and available cleanup commands agree.
- Generated outputs no longer create uncertainty during commits or handoffs.

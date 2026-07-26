# Recommendation 6 — Acceptance step structure

**Task:** `acceptance-step-structure`  
**Priority:** P2  
**Behavior change:** none

## Problem

Acceptance step glue is large and unevenly distributed. Large registries such
as `acceptance/steps.clj` and `acceptance/enemy_steps.clj` increase the cost of
adding or changing feature language and make unsupported or overlapping steps
harder to diagnose.

## Goal

Make acceptance step ownership explicit and add lightweight checks that catch
unsupported, duplicate, or ambiguous step patterns before they surprise a coder
late in `bb accept`.

## Plan

1. Inventory all step handler namespaces and count handler patterns per file.
2. Define ownership rules:
   - generic game setup/assertions
   - enemy behavior
   - fireball/combat behavior
   - shell/title/pause/options/high-score behavior
   - host/browser/desktop behavior
3. Move handlers from oversized catch-all files into focused step namespaces
   where the feature behavior already has an obvious home.
4. Keep feature text unchanged unless a step is genuinely unclear.
5. Add a checker or unit spec for step registry health:
   - no duplicate regex strings
   - no unsupported steps for all current feature files
   - optional warning for broad catch-all regexes that shadow specific steps
6. Document the step ownership rules near the acceptance runtime or in an
   architecture plan.

## Guardrails

- Do not change feature behavior.
- Do not make acceptance steps depend on host namespaces or private layout
  internals.
- Do not rewrite feature files as part of mechanical step movement unless the
  existing text is ambiguous.
- Keep generated acceptance files as generated artifacts.

## Verification

- Step registry unit/spec checks.
- `bb accept`
- `bb test`
- `bb arch-check`

## Done When

- Large step files are smaller or have a clear reason to remain broad.
- Each behavior area has an obvious step namespace.
- Unsupported/duplicate step problems are caught by a focused check.

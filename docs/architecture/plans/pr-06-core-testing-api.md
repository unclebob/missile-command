# PR 6 — `core.testing` / public API trim

**Task:** `core-testing-api`  
**Priority:** P2  
**Depends on:** PR 4–5 (after combat/shell moved)  
**Behavior change:** none for hosts; test requires update

## Goal

Host-facing `core` API is small. Staging helpers live in `missile-command.testing`.

## Move to `missile-command.testing`

Examples (grep-driven; adjust list during PR):

- `route-first-smart-bomb-through-point`, `route-smart-bomb-*`, `route-flyer-through-point`, `route-enemy-through-point`, `route-first-mirv-child-through-point`
- `add-static-fireball`, `add-destroyable-target`
- Possibly absolute `set-score` / reserve setters if only tests use them (keep on core if acceptance steps call `core/set-score`)

## Keep on core (host + acceptance stable names)

- `new-game`, `resize`, `handle`, `tick`
- Entity getters, screen predicates, hud
- `activate-wave-schedule`, `begin-wave-attack` (or ensure-only after PR 1)
- Spawn helpers still referenced by acceptance steps **unless** steps are updated in same PR

## Process

1. Create `src/missile_command/testing.cljc` (or `testing.clj` if JVM-only — prefer `.cljc` if pure).  
2. Move helpers; `testing` may require `core` for state shape or combat.  
3. Update `spec/**` and `test-property/**` requires.  
4. Grep for moved symbols from production hosts — must be zero.  
5. Do **not** break acceptance step patterns without updating step handlers.

## Arch check

If acceptance must not depend on testing ns, keep step handlers calling `core` re-exports that delegate to testing, or allow acceptance → testing only for setup steps (document in arch_check if policy changes).

## Verification

- [ ] Arch check (update if new rule)  
- [ ] Full unit + property  
- [ ] Full acceptance suite  

## Done when

Host code only needs a short list of `core` symbols; testing helpers are clearly non-host.

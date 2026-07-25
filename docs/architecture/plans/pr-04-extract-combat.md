# PR 4 — Extract `combat` module

**Task:** `extract-combat`  
**Priority:** P1  
**Depends on:** PR 1; PR 3 optional  
**Behavior change:** none  
**Risk:** highest LOC move — keep PR focused on tick pipeline only if needed

## Goal

`core/tick` playing path becomes a short pipeline; enemy/flyer/fireball simulation lives in `missile-command.combat`.

## Move from core (combat tick)

Private/public as needed:

- `tick-defensive-missiles`, `tick-fireballs`, `destroy-targets-in-fireballs`
- `tick-enemy-missiles`, `tick-one-enemy`, `resolve-*`, MIRV split
- `tick-flyers`, flyer drops, flyer destroy
- smart bomb evade helpers
- `spawn-fireball-at` / from missile (if only combat uses)

## Keep on core for this PR (re-export later in PR 6)

Public acceptance spawn/route helpers:

- `spawn-enemy-*`, `spawn-mirv-*`, `spawn-smart-bomb-*`, `spawn-flyer`
- `route-*` through-point helpers  

They may call into combat private pieces or stay in core until PR 6.

## API sketch

```clojure
(ns missile-command.combat
  (:require [missile-command.missiles :as missiles]
            [missile-command.flyers :as flyers]
            [missile-command.scoring :as scoring]
            [missile-command.sfx :as sfx]
            ...))

(defn tick-defensive [state dt] ...)
(defn tick-fireballs [state dt] ...)
(defn destroy-targets-in-fireballs [state] ...)
(defn tick-enemies [state dt] ...)
(defn tick-flyers [state dt] ...)
```

## Core tick after

```clojure
(-> state
    (advance-clock applied)
    (combat/tick-defensive applied)
    (combat/tick-fireballs applied)
    combat/destroy-targets-in-fireballs
    (combat/tick-enemies applied)
    (combat/tick-flyers applied)
    maybe-advance-wave-attack
    maybe-complete-wave
    ensure-attack-started   ; from PR 1
    evaluate-game-over)
```

## Tests

- Entire `core_spec` combat sections  
- enemy/MIRV/smart/flyer features acceptance  
- property: missiles, core combat-related  

## Migration strategy (recommended)

1. Create `combat.cljc` by **moving** functions with same names.  
2. Core calls combat; temporarily re-export for any external require.  
3. No behavior edits in the same commit as the move.  
4. If PR too large, split: (4a) defensive+fireballs (4b) enemies (4c) flyers.

## Verification

- [ ] Arch check  
- [ ] Full unit suite  
- [ ] Acceptance: enemy-missiles, defensive-missiles-fireballs, mirv, smart-bombs, bombers-satellites  
- [ ] Manual: fire intercept + city impact  

## Done when

Combat simulation body is not in `core.cljc`; tick pipeline is readable in one screen.

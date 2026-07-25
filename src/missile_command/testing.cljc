(ns missile-command.testing
  "Staging helpers for unit/acceptance specs — not for production hosts.

  Prefer requiring this ns from `spec/` and `test-property/` when calling
  route-*, add-static-fireball, or other scenario-only tools. Production
  hosts should use `handle`/`tick` only (plus start/aim/fire).

  Core re-exports the same symbols for acceptance step stability."
  (:require [missile-command.core :as core]))

(def route-enemy-through-point core/route-enemy-through-point)
(def route-first-smart-bomb-through-point core/route-first-smart-bomb-through-point)
(def route-smart-bomb-centered-in-fireball core/route-smart-bomb-centered-in-fireball)
(def route-smart-bomb-edge-band-in-fireball core/route-smart-bomb-edge-band-in-fireball)
(def route-flyer-through-point core/route-flyer-through-point)
(def route-first-mirv-child-through-point core/route-first-mirv-child-through-point)
(def add-static-fireball core/add-static-fireball)
(def add-destroyable-target core/add-destroyable-target)
(def set-score core/set-score)
(def set-bonus-city-reserve core/set-bonus-city-reserve)
(def set-bonus-city-threshold core/set-bonus-city-threshold)
(def set-wave-enemies-active core/set-wave-enemies-active)
(def begin-wave-attack core/begin-wave-attack)
(def activate-wave-schedule core/activate-wave-schedule)
(def with-rng-seed core/with-rng-seed)

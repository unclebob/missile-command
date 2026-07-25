(ns missile-command.combat
  "Combat simulation: defensive missiles, fireballs, destroyable targets.
  Enemy and flyer ticks remain on core until a follow-up extraction; this
  module owns the defensive/fireball half of the playing tick pipeline."
  (:require [missile-command.missiles :as missiles]
            [missile-command.sfx :as sfx]))

(defn- defensive-missiles [state]
  (or (:defensive-missiles state) []))

(defn- fireballs [state]
  (or (:fireballs state) []))

(defn- next-entity-id
  [state]
  (let [id (long (or (:next-entity-id state) 0))]
    [id (assoc state :next-entity-id (inc id))]))

(defn spawn-fireball-at
  "Allocate and attach an expanding fireball centered at x,y.
  Emits :sfx/boom at onset (defensive intercept or ground impact)."
  [state x y]
  (let [[fid state] (next-entity-id state)
        fireball (missiles/make-fireball fid x y)]
    (-> state
        (update :fireballs (fnil conj []) fireball)
        (sfx/emit :sfx/boom))))

(defn- spawn-fireball-from-missile
  [state missile]
  (spawn-fireball-at state (:x1 missile) (:y1 missile)))

(defn tick-defensive
  "Advance defensive missiles; arrived missiles become fireballs."
  [state dt]
  (reduce (fn [s missile]
            (let [result (missiles/advance-defensive missile dt)]
              (if (missiles/arrived? result)
                (spawn-fireball-from-missile s missile)
                (update s :defensive-missiles (fnil conj []) result))))
          (assoc state :defensive-missiles [])
          (defensive-missiles state)))

(defn tick-fireballs
  "Advance fireballs; drop expired ones."
  [state dt]
  (reduce (fn [s fireball]
            (let [result (missiles/advance-fireball fireball dt)]
              (if (= missiles/expired result)
                s
                (update s :fireballs (fnil conj []) result))))
          (assoc state :fireballs [])
          (fireballs state)))

(defn- target-hit-by-fireball?
  [target fbs]
  (some #(missiles/point-in-fireball? % (:x target) (:y target)) fbs))

(defn destroy-targets-in-fireballs
  "Mark destroyable targets hit by any live fireball."
  [state]
  (let [fbs (fireballs state)]
    (update state :destroyable-targets
            (fn [targets]
              (mapv (fn [target]
                      (if (or (:destroyed? target)
                              (target-hit-by-fireball? target fbs))
                        (assoc target :destroyed? true)
                        target))
                    (or targets []))))))

(defn tick-defensive-phase
  "Defensive missiles + fireballs + destroyable targets for one dt step."
  [state dt]
  (-> state
      (tick-defensive dt)
      (tick-fireballs dt)
      destroy-targets-in-fireballs))

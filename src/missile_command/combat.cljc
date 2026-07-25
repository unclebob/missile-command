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

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-07-25T11:11:56.658861-05:00", :module-hash "-1667877736", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 6, :hash "572951404"} {:id "defn-/defensive-missiles", :kind "defn-", :line 8, :end-line 9, :hash "757290843"} {:id "defn-/fireballs", :kind "defn-", :line 11, :end-line 12, :hash "-1238649430"} {:id "defn-/next-entity-id", :kind "defn-", :line 14, :end-line 17, :hash "-1630959644"} {:id "defn/spawn-fireball-at", :kind "defn", :line 19, :end-line 27, :hash "1465751780"} {:id "defn-/spawn-fireball-from-missile", :kind "defn-", :line 29, :end-line 31, :hash "1839324960"} {:id "defn/tick-defensive", :kind "defn", :line 33, :end-line 42, :hash "128741151"} {:id "defn/tick-fireballs", :kind "defn", :line 44, :end-line 53, :hash "-1331599335"} {:id "defn-/target-hit-by-fireball?", :kind "defn-", :line 55, :end-line 57, :hash "1741845857"} {:id "defn/destroy-targets-in-fireballs", :kind "defn", :line 59, :end-line 70, :hash "-1400979185"} {:id "defn/tick-defensive-phase", :kind "defn", :line 72, :end-line 78, :hash "1446767887"}]}
;; clj-mutate-manifest-end

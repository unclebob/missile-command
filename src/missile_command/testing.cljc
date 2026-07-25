(ns missile-command.testing
  "Staging helpers for unit/acceptance specs — not for production hosts.

  Prefer requiring this ns from `spec/` and `test-property/` when calling
  route-*, add-static-fireball, or other scenario-only tools. Production
  hosts should use `handle`/`tick` only (plus start/aim/fire).

  Implementations of pure staging transforms live here (or in combat for
  spawn filters). Core re-exports the same symbols so acceptance steps that
  call `core/...` remain stable."
  (:require [missile-command.combat :as combat]
            [missile-command.missiles :as missiles]))

(defn- enemy-attrs-to-preserve
  [enemy]
  (select-keys enemy [:enemy-kind :child-count :split-progress
                      :smart-evaded? :last-enemy-fate-local]))

(defn- retarget-enemy-from
  "Rebuild an enemy path starting at x,y while preserving MIRV attrs."
  [enemy x y]
  (merge (missiles/make-enemy (:id enemy)
                              {:x x :y y}
                              {:x (:x1 enemy) :y (:y1 enemy)}
                              (:speed enemy)
                              (:target-kind enemy)
                              (:target-id enemy))
         (enemy-attrs-to-preserve enemy)))

(defn- first-enemy-index
  [ms pred]
  (first (keep-indexed (fn [i e] (when (pred e) i)) ms)))

(defn- retarget-enemy-at-index
  [ms idx x y]
  (assoc (vec ms) idx (retarget-enemy-from (nth ms idx) x y)))

(defn- route-first-enemy-matching
  "Retarget the first enemy matching pred so its path starts at x,y."
  [state pred x y]
  (update state :enemy-missiles
          (fn [ms]
            (if-let [idx (first-enemy-index ms pred)]
              (retarget-enemy-at-index ms idx x y)
              (vec ms)))))

(defn- route-first-fn
  "Build a (fn [state x y]) that retargets the first enemy matching pred."
  [pred]
  (fn [state x y]
    (route-first-enemy-matching state pred x y)))

(def route-first-smart-bomb-through-point
  "Retarget the first smart bomb so its path starts at the given point."
  (route-first-fn combat/smart-bomb?))

(def route-first-mirv-child-through-point
  "Retarget the first MIRV child so its path starts at the given point."
  (route-first-fn combat/mirv-child?))

(defn route-smart-bomb-centered-in-fireball
  "Place the smart bomb path through the fireball center (well-centered kill)."
  [state fb-x fb-y _center-limit]
  (route-first-smart-bomb-through-point state fb-x fb-y))

(defn route-smart-bomb-edge-band-in-fireball
  "Place the smart bomb path through the edge band of the fireball (evade once)."
  [state fb-x fb-y edge-inner radius]
  (let [mid (/ (+ (double edge-inner) (double radius)) 2.0)
        px (+ (double fb-x) mid)
        py (double fb-y)]
    (route-first-smart-bomb-through-point state px py)))

(defn route-flyer-through-point
  "Retarget the first flyer so its path starts at the given point."
  [state x y]
  (update state :flyers
          (fn [fs]
            (if (seq fs)
              (let [f (first fs)
                    retargeted (assoc f
                                      :x0 (double x)
                                      :y0 (double y)
                                      :x (double x)
                                      :y (double y)
                                      :progress 0.0)]
                (into [retargeted] (rest fs)))
              (vec fs)))))

(defn route-enemy-through-point
  "Retarget the first enemy so its path starts at the given point (e.g. fireball)."
  [state x y]
  (update state :enemy-missiles
          (fn [ms]
            (if (seq ms)
              (into [(retarget-enemy-from (first ms) x y)] (rest ms))
              ms))))

(defn add-static-fireball
  "Test/setup helper: place a fixed-radius fireball."
  [state x y radius]
  (let [[fid state] (combat/allocate-entity-id state)
        fb (missiles/make-static-fireball fid x y radius)]
    (update state :fireballs (fnil conj []) fb)))

(defn add-destroyable-target
  "Test/setup helper: place a fireball-vulnerable stub at x,y."
  [state x y]
  (let [[id state] (combat/allocate-entity-id state)
        target {:id id :x x :y y :destroyed? false}]
    (update state :destroyable-targets (fnil conj []) target)))

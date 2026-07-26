(ns missile-command.testing
  "Staging helpers for unit/acceptance specs — not for production hosts.

  Prefer requiring this ns from `spec/` and `test-property/` when calling
  route-*, add-static-fireball, or other scenario-only tools. Production
  hosts should use `handle`/`tick` only (plus start/aim/fire).

  Implementations of pure staging transforms live here (or in combat for
  spawn filters). Core re-exports the same symbols so acceptance steps that
  call `core/...` remain stable."
  (:require [missile-command.batteries :as batteries]
            [missile-command.bonus-cities :as bc]
            [missile-command.cities :as cities]
            [missile-command.combat :as combat]
            [missile-command.flyers :as flyers]
            [missile-command.missiles :as missiles]
            [missile-command.rng :as rng]
            [missile-command.sfx :as sfx]
            [missile-command.wave-lifecycle :as wave-lifecycle]
            [missile-command.wave-schedule :as wave-schedule]
            [missile-command.waves :as waves]))

(def ^:private wave-flag-off false)
(def ^:private wave-flag-on true)
(def ^:private wave-starts-complete? wave-flag-off)
(def ^:private wave-starts-with-enemies? wave-flag-off)

(defn- city
  [state city-id]
  (cities/by-id (:cities state) city-id))

(defn- living-cities
  [state]
  (cities/living (:cities state)))

(defn- battery
  [state battery-id]
  (first (filter #(= battery-id (:id %)) (or (:batteries state) []))))

(defn- playfield-width
  [state]
  (:width state))

(defn- playfield-height
  [state]
  (:height state))

(defn- update-battery
  [state battery-id f]
  (update state :batteries #(batteries/update-battery % battery-id f)))

(defn- update-city
  [state city-id f]
  (update state :cities #(cities/update-city % city-id f)))

(defn set-battery-ammo
  "Testing/setup helper: set remaining missiles for a battery."
  [state battery-id ammo]
  (update-battery state battery-id #(batteries/set-ammo % ammo)))

(defn destroy-battery
  "Testing/setup helper: mark a battery destroyed; emit sfx when newly destroyed."
  [state battery-id]
  (let [bat (battery state battery-id)]
    (sfx/maybe-emit (update-battery state battery-id batteries/destroy)
                    (and bat (not (:destroyed? bat)))
                    :sfx/battery-destroyed)))

(defn destroy-city
  "Testing/setup helper: mark a city destroyed; emit sfx when newly destroyed."
  [state city-id]
  (let [c (city state city-id)]
    (sfx/maybe-emit (update-city state city-id cities/destroy)
                    (and c (:alive? c))
                    :sfx/city-destroyed)))

(defn set-score
  "Testing/setup helper: set absolute score and process bonus city thresholds."
  [state score-value]
  (-> state
      (assoc :score (long score-value))
      bc/sync-from-score))

(def set-bonus-city-threshold bc/set-threshold)
(def set-bonus-city-reserve bc/set-reserve)
(def apply-bonus-cities-from-reserve bc/apply-from-reserve)

(defn set-wave
  "Testing/setup helper: jump to a wave number without auto-completing."
  [state wave-number]
  (wave-lifecycle/set-wave state wave-number
                           wave-starts-complete?
                           wave-starts-with-enemies?))

(defn rearm-surviving-batteries
  "Testing/setup helper: restore every battery to full ammo."
  [state]
  (wave-lifecycle/rearm-all-batteries state))

(defn start-next-wave
  "Testing/setup helper: leave banner and rearm for the next wave."
  [state]
  (wave-lifecycle/start-next-wave state
                                  wave-starts-complete?
                                  wave-starts-with-enemies?))

(defn set-non-destroyed-battery-ammo
  "Testing/setup helper: set ammo on every non-destroyed battery."
  [state ammo]
  (update state :batteries #(batteries/set-living-ammo % ammo)))

(defn with-rng-seed
  "Testing/setup helper: attach a deterministic RNG seed."
  [state seed]
  (rng/with-seed state seed))

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

(defn spawn-enemy-at
  "Testing/setup helper: spawn an enemy from origin toward target."
  ([state origin target target-kind target-id]
   (combat/spawn-enemy-at state origin target target-kind target-id nil))
  ([state origin target target-kind target-id attrs]
   (combat/spawn-enemy-at state origin target target-kind target-id attrs)))

(defn spawn-enemy-targeting-city-from
  "Testing/setup helper: spawn an enemy from an explicit sky origin toward a city."
  [state origin-x origin-y city-id]
  (let [c (city state city-id)]
    (when-not c
      (throw (ex-info (str "unknown city " city-id) {:city-id city-id})))
    (spawn-enemy-at state
                    {:x origin-x :y origin-y}
                    {:x (:x c) :y (:y c)}
                    :city city-id)))

(defn spawn-enemy-targeting-city
  "Testing/setup helper: spawn an enemy from above its target city."
  [state city-id]
  (if-let [c (city state city-id)]
    (spawn-enemy-targeting-city-from state (:x c) 0 city-id)
    (throw (ex-info (str "unknown city " city-id) {:city-id city-id}))))

(defn spawn-enemy-targeting-battery-from
  "Testing/setup helper: spawn an enemy from an explicit sky origin toward a battery."
  [state origin-x origin-y battery-id]
  (let [b (battery state battery-id)]
    (when-not b
      (throw (ex-info (str "unknown battery " battery-id) {:battery-id battery-id})))
    (spawn-enemy-at state
                    {:x origin-x :y origin-y}
                    {:x (:x b) :y (:y b)}
                    :battery battery-id)))

(defn spawn-enemy-targeting-battery
  "Testing/setup helper: spawn an enemy from above its target battery."
  [state battery-id]
  (if-let [b (battery state battery-id)]
    (spawn-enemy-targeting-battery-from state (:x b) 0 battery-id)
    (throw (ex-info (str "unknown battery " battery-id) {:battery-id battery-id}))))

(defn spawn-enemies-targeting-distinct-cities
  "Testing/setup helper: spawn n enemies aimed at different living cities."
  [state n]
  (let [ids (mapv :id (take n (living-cities state)))]
    (reduce spawn-enemy-targeting-city state ids)))

(defn spawn-mirv-targeting-city
  "Testing/setup helper: spawn a MIRV parent toward a city."
  [state city-id child-count split-progress]
  (let [c (city state city-id)]
    (when-not c
      (throw (ex-info (str "unknown city " city-id) {:city-id city-id})))
    (spawn-enemy-at state
                    {:x (:x c) :y 0}
                    {:x (:x c) :y (:y c)}
                    :city city-id
                    {:enemy-kind combat/enemy-kind-mirv
                     :child-count (long child-count)
                     :split-progress (double split-progress)})))

(defn spawn-smart-bomb-targeting-city
  "Testing/setup helper: spawn a smart bomb toward a city."
  [state city-id]
  (let [c (city state city-id)]
    (when-not c
      (throw (ex-info (str "unknown city " city-id) {:city-id city-id})))
    (let [[ox state] (rng/next-sky-origin-x state (playfield-width state))]
      (spawn-enemy-at state
                      {:x ox :y 0}
                      {:x (:x c) :y (:y c)}
                      :city city-id
                      {:enemy-kind combat/enemy-kind-smart
                       :smart-evaded? false}))))

(defn spawn-flyer
  "Testing/setup helper: spawn a bomber or satellite."
  [state flyer-kind start-x start-y end-x end-y speed]
  (let [[fid state] (combat/allocate-entity-id state)
        flyer (flyers/make fid flyer-kind start-x start-y end-x end-y speed)]
    (-> state
        (update :flyers (fnil conj []) flyer)
        (assoc :wave-had-enemies? wave-flag-on
               :wave-complete? wave-starts-complete?))))

(defn set-flyer-drops
  "Testing/setup helper: configure drop events on the first flyer."
  [state drops]
  (update state :flyers
          (fn [fs]
            (if (seq fs)
              (assoc (vec fs) 0 (assoc (first fs) :drops (vec drops) :drops-fired #{}))
              (vec fs)))))

(defn set-flyer-drops-toward-living-cities
  "Testing/setup helper: first flyer drops toward living cities."
  [state drop-count drop-progress]
  (let [ids (mapv :id (living-cities state))
        targets (if (seq ids)
                  (vec (take drop-count (cycle ids)))
                  [])
        drops (mapv (fn [i city-id]
                      {:id i
                       :at-progress (double drop-progress)
                       :target [:city city-id]})
                    (range (count targets))
                    targets)]
    (set-flyer-drops state drops)))

(defn set-flyer-drop-targeting-city
  "Testing/setup helper: first flyer drops one missile toward a city."
  [state city-id drop-progress]
  (set-flyer-drops state
                   [{:id 0
                     :at-progress (double drop-progress)
                     :target [:city city-id]}]))

(defn flyers-of-kind
  [state flyer-kind]
  (filterv #(= (keyword flyer-kind) (:kind %)) (or (:flyers state) [])))

(defn spawn-wave-enemy-targeting-battery
  "Testing/setup helper: spawn one scheduled wave enemy aimed at a battery."
  [state battery-id]
  (spawn-enemy-targeting-battery-from
   state
   (waves/sky-origin-x (playfield-width state) 0 1)
   0
   battery-id))

(defn set-wave-enemies-active
  "Testing/setup helper: replace in-flight enemies with n scheduled wave enemies."
  [state n]
  (wave-schedule/set-enemies-active
   state n
   {:living-cities living-cities
    :batteries-living (fn [s] (batteries/living (:batteries s)))
    :playfield-width playfield-width
    :spawn-city-from spawn-enemy-targeting-city-from
    :spawn-battery-from spawn-enemy-targeting-battery-from
    :wave-starts-complete? wave-starts-complete?}))

(defn- wave-schedule-hooks
  []
  {:wave (fn [state] (or (:wave state) waves/initial-wave))
   :living-cities living-cities
   :city city
   :playfield-width playfield-width
   :playfield-height playfield-height
   :set-wave-enemies-active set-wave-enemies-active
   :spawn-enemy-at spawn-enemy-at
   :spawn-smart-bomb-targeting-city spawn-smart-bomb-targeting-city
   :spawn-flyer spawn-flyer
   :enemy-kind-mirv combat/enemy-kind-mirv})

(defn begin-wave-attack
  "Testing/setup helper: begin attack k for the current wave."
  [state k]
  (wave-schedule/begin-attack state k (wave-schedule-hooks)))

(def start-wave-attack begin-wave-attack)

(defn activate-wave-schedule
  "Testing/setup helper: start attack 1 of the current wave."
  [state]
  (begin-wave-attack state 1))

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

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-07-26T10:18:59.636527-05:00", :module-hash "988597387", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 21, :hash "-472074156"} {:id "def/wave-flag-off", :kind "def", :line 23, :end-line 23, :hash "1734145513"} {:id "def/wave-flag-on", :kind "def", :line 24, :end-line 24, :hash "1660732832"} {:id "def/wave-starts-complete?", :kind "def", :line 25, :end-line 25, :hash "1259204240"} {:id "def/wave-starts-with-enemies?", :kind "def", :line 26, :end-line 26, :hash "929188796"} {:id "defn-/city", :kind "defn-", :line 28, :end-line 30, :hash "-2029908419"} {:id "defn-/living-cities", :kind "defn-", :line 32, :end-line 34, :hash "1670160642"} {:id "defn-/battery", :kind "defn-", :line 36, :end-line 38, :hash "-2097468789"} {:id "defn-/playfield-width", :kind "defn-", :line 40, :end-line 42, :hash "203569928"} {:id "defn-/playfield-height", :kind "defn-", :line 44, :end-line 46, :hash "1007372003"} {:id "defn-/update-battery", :kind "defn-", :line 48, :end-line 50, :hash "-2009265330"} {:id "defn-/update-city", :kind "defn-", :line 52, :end-line 54, :hash "390069012"} {:id "defn/set-battery-ammo", :kind "defn", :line 56, :end-line 59, :hash "806945089"} {:id "defn/destroy-battery", :kind "defn", :line 61, :end-line 67, :hash "1238715085"} {:id "defn/destroy-city", :kind "defn", :line 69, :end-line 75, :hash "1872878898"} {:id "defn/set-score", :kind "defn", :line 77, :end-line 82, :hash "448674806"} {:id "def/set-bonus-city-threshold", :kind "def", :line 84, :end-line 84, :hash "-1065553678"} {:id "def/set-bonus-city-reserve", :kind "def", :line 85, :end-line 85, :hash "-399484399"} {:id "def/apply-bonus-cities-from-reserve", :kind "def", :line 86, :end-line 86, :hash "-849731520"} {:id "defn/set-wave", :kind "defn", :line 88, :end-line 93, :hash "1220861974"} {:id "defn/rearm-surviving-batteries", :kind "defn", :line 95, :end-line 98, :hash "-145480718"} {:id "defn/start-next-wave", :kind "defn", :line 100, :end-line 105, :hash "476450175"} {:id "defn/set-non-destroyed-battery-ammo", :kind "defn", :line 107, :end-line 110, :hash "-1981795345"} {:id "defn/with-rng-seed", :kind "defn", :line 112, :end-line 115, :hash "-1095354281"} {:id "defn-/enemy-attrs-to-preserve", :kind "defn-", :line 117, :end-line 120, :hash "-111533279"} {:id "defn-/retarget-enemy-from", :kind "defn-", :line 122, :end-line 131, :hash "-550286216"} {:id "defn-/first-enemy-index", :kind "defn-", :line 133, :end-line 135, :hash "-1572668638"} {:id "defn-/retarget-enemy-at-index", :kind "defn-", :line 137, :end-line 139, :hash "1058188477"} {:id "defn-/route-first-enemy-matching", :kind "defn-", :line 141, :end-line 148, :hash "181163451"} {:id "defn-/route-first-fn", :kind "defn-", :line 150, :end-line 154, :hash "209156702"} {:id "def/route-first-smart-bomb-through-point", :kind "def", :line 156, :end-line 158, :hash "717343399"} {:id "def/route-first-mirv-child-through-point", :kind "def", :line 160, :end-line 162, :hash "965011384"} {:id "defn/route-smart-bomb-centered-in-fireball", :kind "defn", :line 164, :end-line 167, :hash "-541649650"} {:id "defn/route-smart-bomb-edge-band-in-fireball", :kind "defn", :line 169, :end-line 175, :hash "1354861353"} {:id "defn/route-flyer-through-point", :kind "defn", :line 177, :end-line 191, :hash "-723558539"} {:id "defn/route-enemy-through-point", :kind "defn", :line 193, :end-line 200, :hash "1758214460"} {:id "defn/add-static-fireball", :kind "defn", :line 202, :end-line 207, :hash "-1804709237"} {:id "defn/add-destroyable-target", :kind "defn", :line 209, :end-line 214, :hash "-404487409"} {:id "defn/spawn-enemy-at", :kind "defn", :line 216, :end-line 221, :hash "791191758"} {:id "defn/spawn-enemy-targeting-city-from", :kind "defn", :line 223, :end-line 232, :hash "1861119153"} {:id "defn/spawn-enemy-targeting-city", :kind "defn", :line 234, :end-line 239, :hash "1461433437"} {:id "defn/spawn-enemy-targeting-battery-from", :kind "defn", :line 241, :end-line 250, :hash "-370340469"} {:id "defn/spawn-enemy-targeting-battery", :kind "defn", :line 252, :end-line 257, :hash "1436629350"} {:id "defn/spawn-enemies-targeting-distinct-cities", :kind "defn", :line 259, :end-line 263, :hash "1243323092"} {:id "defn/spawn-mirv-targeting-city", :kind "defn", :line 265, :end-line 277, :hash "-1497358936"} {:id "defn/spawn-smart-bomb-targeting-city", :kind "defn", :line 279, :end-line 291, :hash "779175881"} {:id "defn/spawn-flyer", :kind "defn", :line 293, :end-line 301, :hash "641874812"} {:id "defn/set-flyer-drops", :kind "defn", :line 303, :end-line 310, :hash "298933668"} {:id "defn/set-flyer-drops-toward-living-cities", :kind "defn", :line 312, :end-line 325, :hash "-921311834"} {:id "defn/set-flyer-drop-targeting-city", :kind "defn", :line 327, :end-line 333, :hash "-1993903089"} {:id "defn/flyers-of-kind", :kind "defn", :line 335, :end-line 337, :hash "1788294730"} {:id "defn/spawn-wave-enemy-targeting-battery", :kind "defn", :line 339, :end-line 346, :hash "-1637493815"} {:id "defn/set-wave-enemies-active", :kind "defn", :line 348, :end-line 358, :hash "1336895216"} {:id "defn-/wave-schedule-hooks", :kind "defn-", :line 360, :end-line 371, :hash "2009050190"} {:id "defn/begin-wave-attack", :kind "defn", :line 373, :end-line 376, :hash "-1421209205"} {:id "def/start-wave-attack", :kind "def", :line 378, :end-line 378, :hash "1999025037"} {:id "defn/activate-wave-schedule", :kind "defn", :line 380, :end-line 383, :hash "347348519"}]}
;; clj-mutate-manifest-end

(ns missile-command.core
  (:require [missile-command.batteries :as batteries]
            [missile-command.cities :as cities]
            [missile-command.input :as input]
            [missile-command.missiles :as missiles]
            [missile-command.scoring :as scoring]
            [missile-command.waves :as waves]
            [missile-command.world :as world]))

(def initial-score 0)
(def initial-entity-id 0)
(def initial-bonus-cities 0)
(def initial-bonus-cities-awarded 0)
(def initial-bonus-city-earned-events 0)
(def wave-flag-off false)
(def wave-flag-on true)
(def wave-starts-complete? wave-flag-off)
(def wave-starts-with-enemies? wave-flag-off)
(def clamp-lo 0)
(def default-crosshair {:x clamp-lo :y clamp-lo})
(def target-starts-destroyed? wave-flag-off)

(defn- clamp
  [n lo hi]
  (max lo (min hi n)))

(defn- clamp-point
  [width height x y]
  {:x (clamp x clamp-lo (dec width))
   :y (clamp y clamp-lo (dec height))})

(defn- center-crosshair
  [width height]
  (clamp-point width height (quot width 2) (quot height 2)))

(defn- reclamp-crosshair
  [state width height]
  (let [crosshair (or (:crosshair state) default-crosshair)]
    (clamp-point width height (:x crosshair) (:y crosshair))))

(defn- update-battery
  [state battery-id f]
  (update state :batteries #(batteries/update-battery % battery-id f)))

(defn- next-entity-id
  [state]
  (let [id (or (:next-entity-id state) initial-entity-id)]
    [id (assoc state :next-entity-id (inc id))]))

(defn- no-events
  [state]
  {:state state :events []})

(defn new-game
  "Create a new game state for the given playfield size."
  [{:keys [width height]}]
  (merge {:width width
          :height height
          :score initial-score
          :wave waves/initial-wave
          :wave-complete? wave-starts-complete?
          :wave-had-enemies? wave-starts-with-enemies?
          :crosshair (center-crosshair width height)
          :defensive-missiles []
          :fireballs []
          :enemy-missiles []
          :destroyable-targets []
          :bonus-cities initial-bonus-cities
          :bonus-city-threshold scoring/default-bonus-city-threshold
          :bonus-cities-awarded initial-bonus-cities-awarded
          :bonus-city-earned-events initial-bonus-city-earned-events
          :sim-time 0.0
          :last-applied-dt 0.0
          :last-enemy-fate nil
          :next-entity-id initial-entity-id}
         (world/apply-layout width height)))

(defn resize
  "Reflow layout for a new playfield size, preserving entity progress fields."
  [state width height]
  (merge state
         {:width width
          :height height
          :crosshair (reclamp-crosshair state width height)}
         (world/apply-layout width height state)))

(defn playfield-width
  [state]
  (:width state))

(defn playfield-height
  [state]
  (:height state))

(defn cities
  [state]
  (:cities state))

(defn living-cities
  [state]
  (cities/living (cities state)))

(defn batteries
  [state]
  (:batteries state))

(defn battery
  [state id]
  (first (filter #(= id (:id %)) (batteries state))))

(defn on-ground?
  "True when the entity's y sits in the ground band for this playfield."
  [state entity]
  (world/in-ground-band? (:y entity) (playfield-height state)))

(defn city-on-ground?
  [state city]
  (on-ground? state city))

(defn crosshair
  [state]
  (:crosshair state))

(defn score
  [state]
  (:score state))

(defn wave
  [state]
  (or (:wave state) waves/initial-wave))

(defn multiplier
  "Current score multiplier derived from the active wave."
  [state]
  (waves/multiplier (wave state)))

(defn- long-state
  [state k default]
  (long (or (get state k) default)))

(defn bonus-cities
  "Bonus cities held in reserve (not yet placed on the playfield)."
  [state]
  (long-state state :bonus-cities initial-bonus-cities))

(defn bonus-city-threshold
  [state]
  (long-state state :bonus-city-threshold scoring/default-bonus-city-threshold))

(defn bonus-city-earned-events
  "How many threshold-crossing awards have been recorded this run."
  [state]
  (long-state state :bonus-city-earned-events initial-bonus-city-earned-events))

(defn wave-complete?
  [state]
  (boolean (:wave-complete? state)))

(defn hud
  "Minimal HUD projection for hosts and tests."
  [state]
  {:wave (wave state)
   :score (score state)
   :multiplier (multiplier state)
   :bonus-cities (bonus-cities state)})

(defn defensive-missiles
  [state]
  (or (:defensive-missiles state) []))

(defn fireballs
  [state]
  (or (:fireballs state) []))

(defn enemy-missiles
  [state]
  (or (:enemy-missiles state) []))

(defn destroyable-targets
  [state]
  (or (:destroyable-targets state) []))

(defn last-enemy-fate
  [state]
  (:last-enemy-fate state))

(defn city
  [state city-id]
  (cities/by-id (cities state) city-id))

(defn living-city?
  [state city-id]
  (boolean (:alive? (city state city-id))))

(defn sim-time
  [state]
  (double (or (:sim-time state) 0.0)))

(defn last-applied-dt
  [state]
  (double (or (:last-applied-dt state) 0.0)))

(defn max-fireball-radius
  [_]
  missiles/fireball-max-radius)

(defn set-battery-ammo
  "Test/setup helper: set remaining missiles for a battery."
  [state battery-id ammo]
  (update-battery state battery-id #(batteries/set-ammo % ammo)))

(defn destroy-battery
  "Test/setup helper: mark a battery destroyed."
  [state battery-id]
  (update-battery state battery-id batteries/destroy))

(defn add-destroyable-target
  "Test/setup helper: place a fireball-vulnerable stub at x,y."
  [state x y]
  (let [[id state] (next-entity-id state)
        target {:id id :x x :y y :destroyed? target-starts-destroyed?}]
    (update state :destroyable-targets (fnil conj []) target)))

(defn- update-city
  [state city-id f]
  (update state :cities #(cities/update-city % city-id f)))

(defn destroy-city
  [state city-id]
  (update-city state city-id cities/destroy))

(defn- enemy-speed-for-state
  [state]
  (waves/enemy-speed (wave state)))

(def enemy-kind-ballistic :ballistic)
(def enemy-kind-mirv :mirv)
(def enemy-kind-mirv-child :mirv-child)

(defn- mirv-parent?
  [enemy]
  (= enemy-kind-mirv (:enemy-kind enemy)))

(defn- mirv-child?
  [enemy]
  (= enemy-kind-mirv-child (:enemy-kind enemy)))

(defn mirv-parents
  [state]
  (filterv mirv-parent? (enemy-missiles state)))

(defn mirv-children
  [state]
  (filterv mirv-child? (enemy-missiles state)))

(defn spawn-enemy-at
  "Spawn an enemy missile from origin toward a target point.
  Optional attrs merge onto the missile (e.g. MIRV fields)."
  ([state origin target target-kind target-id]
   (spawn-enemy-at state origin target target-kind target-id nil))
  ([state origin target target-kind target-id attrs]
   (let [[mid state] (next-entity-id state)
         missile (merge (missiles/make-enemy mid origin target
                                             (enemy-speed-for-state state)
                                             target-kind target-id)
                        {:enemy-kind enemy-kind-ballistic}
                        attrs)]
     (-> state
         (update :enemy-missiles (fnil conj []) missile)
         (assoc :wave-had-enemies? wave-flag-on
                :wave-complete? wave-starts-complete?)))))

(defn spawn-enemy-targeting-city-from
  "Spawn an enemy missile from an explicit sky origin toward a city."
  [state origin-x origin-y city-id]
  (let [c (city state city-id)]
    (when-not c
      (throw (ex-info (str "unknown city " city-id) {:city-id city-id})))
    (spawn-enemy-at state
                    {:x origin-x :y origin-y}
                    {:x (:x c) :y (:y c)}
                    :city city-id)))

(defn spawn-enemy-targeting-city
  "Spawn an enemy missile from the top of the sky toward a living city."
  [state city-id]
  (if-let [c (city state city-id)]
    (spawn-enemy-targeting-city-from state (:x c) 0 city-id)
    (throw (ex-info (str "unknown city " city-id) {:city-id city-id}))))

(defn spawn-enemy-targeting-battery-from
  "Spawn an enemy missile from an explicit sky origin toward a battery."
  [state origin-x origin-y battery-id]
  (let [b (battery state battery-id)]
    (when-not b
      (throw (ex-info (str "unknown battery " battery-id) {:battery-id battery-id})))
    (spawn-enemy-at state
                    {:x origin-x :y origin-y}
                    {:x (:x b) :y (:y b)}
                    :battery battery-id)))

(defn spawn-enemy-targeting-battery
  "Spawn an enemy missile from the top of the sky toward a battery."
  [state battery-id]
  (if-let [b (battery state battery-id)]
    (spawn-enemy-targeting-battery-from state (:x b) 0 battery-id)
    (throw (ex-info (str "unknown battery " battery-id) {:battery-id battery-id}))))

(defn spawn-enemies-targeting-distinct-cities
  "Spawn n enemy missiles each aimed at a different living city."
  [state n]
  (let [ids (mapv :id (take n (living-cities state)))]
    (reduce spawn-enemy-targeting-city state ids)))

(defn spawn-mirv-targeting-city
  "Spawn a MIRV parent toward a city; splits into child-count warheads at split-progress."
  [state city-id child-count split-progress]
  (let [c (city state city-id)]
    (when-not c
      (throw (ex-info (str "unknown city " city-id) {:city-id city-id})))
    (spawn-enemy-at state
                    {:x (:x c) :y 0}
                    {:x (:x c) :y (:y c)}
                    :city city-id
                    {:enemy-kind enemy-kind-mirv
                     :child-count (long child-count)
                     :split-progress (double split-progress)})))

(defn add-static-fireball
  "Test/setup helper: place a fixed-radius fireball."
  [state x y radius]
  (let [[fid state] (next-entity-id state)
        fb (missiles/make-static-fireball fid x y radius)]
    (update state :fireballs (fnil conj []) fb)))

(defn- enemy-attrs-to-preserve
  [enemy]
  (select-keys enemy [:enemy-kind :child-count :split-progress]))

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

(defn route-enemy-through-point
  "Retarget the first enemy so its path starts at the given point (e.g. fireball)."
  [state x y]
  (update state :enemy-missiles
          (fn [ms]
            (if (seq ms)
              (into [(retarget-enemy-from (first ms) x y)] (rest ms))
              ms))))

(defn- first-mirv-child-index
  [enemies]
  (first (keep-indexed (fn [i e] (when (mirv-child? e) i)) enemies)))

(defn route-first-mirv-child-through-point
  "Retarget the first MIRV child so its path starts at the given point."
  [state x y]
  (update state :enemy-missiles
          (fn [ms]
            (let [ms (vec ms)
                  idx (first-mirv-child-index ms)]
              (if idx
                (assoc ms idx (retarget-enemy-from (nth ms idx) x y))
                ms)))))

(defn- impact-target
  [state enemy]
  (case (:target-kind enemy)
    :city (destroy-city state (:target-id enemy))
    :battery (destroy-battery state (:target-id enemy))
    state))

(defn- enemy-hit-by-fireball?
  [enemy fireballs]
  (some #(missiles/point-in-fireball? % (:x enemy) (:y enemy)) fireballs))

(defn- fire-battery
  [state battery-id]
  (let [bat (battery state battery-id)]
    (if-not (batteries/can-fire? bat)
      (no-events state)
      (let [[missile-id state] (next-entity-id state)
            missile (missiles/make-defensive missile-id battery-id bat
                                             (crosshair state))]
        {:state (-> state
                    (update :defensive-missiles (fnil conj []) missile)
                    (update-battery battery-id batteries/spend-ammo))
         :events [{:type :sfx/launch :battery battery-id}]}))))

(defn- aim
  [state x y]
  (no-events
   (assoc state :crosshair
          (clamp-point (playfield-width state)
                       (playfield-height state)
                       x y))))

(defn click-zone
  "Horizontal third for a playfield x coordinate: :left, :center, or :right."
  [width x]
  (input/click-zone width x))

(defn click-fallback-order
  "Battery preference order for a click zone, preferred first."
  [zone]
  (input/click-fallback-order zone))

(defn- click-fire
  "Aim at the click point, then fire preferred zone battery with adjacent fallback."
  [state x y]
  (let [aimed (:state (aim state x y))
        zone (input/click-zone (playfield-width aimed) (:x (crosshair aimed)))
        battery-id (input/first-preferred
                    zone
                    #(batteries/can-fire? (battery aimed %)))]
    (if battery-id
      (fire-battery aimed battery-id)
      (no-events aimed))))

(defn handle
  "Apply a player command. Returns {:state s :events [...]}."
  [state command]
  (case (:type command)
    :aim (aim state (:x command) (:y command))
    :fire (fire-battery state (:battery command))
    :click (click-fire state (:x command) (:y command))
    (throw (ex-info (str "unsupported command: " (:type command))
                    {:command command}))))

(defn- spawn-fireball-at
  "Allocate and attach a expanding fireball centered at x,y."
  [state x y]
  (let [[fid state] (next-entity-id state)
        fireball (missiles/make-fireball fid x y)]
    (update state :fireballs (fnil conj []) fireball)))

(defn- spawn-fireball-from-missile
  [state missile]
  (spawn-fireball-at state (:x1 missile) (:y1 missile)))
(defn- tick-defensive-missiles
  [state dt]
  (reduce (fn [s missile]
            (let [result (missiles/advance-defensive missile dt)]
              (if (missiles/arrived? result)
                (spawn-fireball-from-missile s missile)
                (update s :defensive-missiles (fnil conj []) result))))
          (assoc state :defensive-missiles [])
          (defensive-missiles state)))

(defn- tick-fireballs
  [state dt]
  (reduce (fn [s fireball]
            (let [result (missiles/advance-fireball fireball dt)]
              (if (= missiles/expired result)
                s
                (update s :fireballs (fnil conj []) result))))
          (assoc state :fireballs [])
          (fireballs state)))

(defn- target-hit-by-fireball?
  [target fireballs]
  (some #(missiles/point-in-fireball? % (:x target) (:y target)) fireballs))

(defn- destroy-targets-in-fireballs
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

(defn- assoc-long
  [state k v]
  (assoc state k (long v)))

(defn set-bonus-city-threshold
  "Test/setup helper: configure the score interval for bonus city awards."
  [state threshold]
  (assoc-long state :bonus-city-threshold threshold))

(defn set-bonus-city-reserve
  "Test/setup helper: set bonus cities currently held in reserve."
  [state n]
  (assoc-long state :bonus-cities n))

(defn- lowest-destroyed-city-id
  [state]
  (->> (cities state)
       (remove :alive?)
       (map :id)
       sort
       first))

(defn apply-bonus-cities-from-reserve
  "Place reserve cities onto destroyed slots while living cities stay under max."
  [state]
  (loop [s state]
    (let [id (when (and (pos? (bonus-cities s))
                        (< (count (living-cities s)) world/city-count))
               (lowest-destroyed-city-id s))]
      (if id
        (recur (-> s
                   (update-city id cities/restore)
                   (update :bonus-cities dec)))
        s))))

(defn- sync-bonus-cities-from-score
  "Award reserve cities for newly crossed score thresholds and place if room."
  [state]
  (let [threshold (bonus-city-threshold state)
        already (long (or (:bonus-cities-awarded state) initial-bonus-cities-awarded))
        new-awards (scoring/new-bonus-city-awards (score state) threshold already)
        earned (scoring/thresholds-crossed (score state) threshold)]
    (if (pos? new-awards)
      (-> state
          (assoc :bonus-cities-awarded earned)
          (update :bonus-cities (fnil + initial-bonus-cities) new-awards)
          (update :bonus-city-earned-events
                  (fnil + initial-bonus-city-earned-events) new-awards)
          apply-bonus-cities-from-reserve)
      state)))
(defn- add-score
  [state points]
  (-> state
      (update :score (fnil + initial-score) (long points))
      sync-bonus-cities-from-score))

(defn set-score
  "Test/setup helper: set absolute score and process bonus city thresholds."
  [state score-value]
  (-> state
      (assoc :score (long score-value))
      sync-bonus-cities-from-score))

(defn- destroy-enemy-by-fireball
  [state]
  (-> state
      (add-score (scoring/enemy-kill-points (multiplier state)))
      (assoc :last-enemy-fate :fireball)))

(defn- spawn-impact-fireball
  "Visual/game blast at the impact point (ground strike)."
  [state enemy]
  (spawn-fireball-at state (:x1 enemy) (:y1 enemy)))

(defn- resolve-enemy-impact
  [state enemy]
  (-> state
      (impact-target enemy)
      (spawn-impact-fireball enemy)
      (assoc :last-enemy-fate :impact)))

(defn- keep-flying-enemy
  [state enemy]
  (update state :enemy-missiles (fnil conj []) enemy))

(defn- progress-of
  [enemy-or-result]
  (if (missiles/arrived? enemy-or-result)
    1.0
    (double (:progress enemy-or-result 0.0))))

(defn- index-of-id
  "Portable index lookup for .cljc (avoids java.util.List/.indexOf)."
  [xs x]
  (first (keep-indexed (fn [i v] (when (= v x) i)) xs)))

(defn- mirv-child-target-ids
  "Prefer starting at preferred city id, then cycle living cities for variety."
  [state preferred-city-id n]
  (let [living (mapv :id (living-cities state))]
    (if (seq living)
      (let [idx (or (index-of-id living preferred-city-id) 0)
            ordered (vec (concat (subvec living idx) (subvec living 0 idx)))]
        (vec (take n (cycle ordered))))
      [])))

(defn- split-mirv-parent
  "Remove parent (already not re-queued) and spawn child warheads at split point."
  [state parent]
  (let [split-p (double (:split-progress parent 0.5))
        at (missiles/position-at-progress parent split-p)
        n (long (:child-count parent 0))
        targets (mirv-child-target-ids state (:target-id parent) n)
        origin {:x (:x at) :y (:y at)}]
    (reduce (fn [s city-id]
              (let [c (city s city-id)]
                (if c
                  (spawn-enemy-at s origin {:x (:x c) :y (:y c)} :city city-id
                                  {:enemy-kind enemy-kind-mirv-child})
                  s)))
            state
            targets)))

(defn- should-split-mirv?
  [enemy result]
  (and (mirv-parent? enemy)
       (>= (progress-of result)
           (double (:split-progress enemy 1.0)))))

(defn- resolve-advanced-enemy
  "Apply MIRV split, impact, fireball kill, or continued flight for an advanced enemy."
  [state enemy result fireballs]
  (cond
    (should-split-mirv? enemy result)
    (split-mirv-parent state enemy)

    (missiles/arrived? result)
    (resolve-enemy-impact state enemy)

    (enemy-hit-by-fireball? result fireballs)
    (destroy-enemy-by-fireball state)

    :else
    (keep-flying-enemy state result)))

(defn- tick-one-enemy
  [state enemy dt fireballs]
  (if (enemy-hit-by-fireball? enemy fireballs)
    (destroy-enemy-by-fireball state)
    (resolve-advanced-enemy state enemy
                            (missiles/advance-enemy enemy dt)
                            fireballs)))

(defn- tick-enemy-missiles
  [state dt]
  (let [fbs (fireballs state)]
    (reduce (fn [s enemy]
              (tick-one-enemy s enemy dt fbs))
            (assoc state :enemy-missiles [])
            (enemy-missiles state))))

(defn- wave-ready-to-complete?
  [state]
  (boolean
   (and (:wave-had-enemies? state)
        (not (:wave-complete? state))
        (empty? (enemy-missiles state)))))

(defn- unused-defensive-missiles
  "Sum of remaining ammo on non-destroyed batteries (before rearm)."
  [state]
  (->> (batteries state)
       (remove :destroyed?)
       (map #(long (or (:missiles %) 0)))
       (reduce + 0)))

(defn- award-wave-end-bonuses
  "Unused missiles and surviving cities × multiplier for the completing wave."
  [state]
  (add-score state
             (scoring/wave-end-points
              (unused-defensive-missiles state)
              (count (living-cities state))
              (multiplier state))))

(defn- mark-wave-complete
  [state]
  (-> state
      award-wave-end-bonuses
      apply-bonus-cities-from-reserve
      (assoc :wave-complete? wave-flag-on
             :wave-had-enemies? wave-starts-with-enemies?)
      (update :wave (fnil inc waves/initial-wave))))

(defn- maybe-complete-wave
  "When all active wave enemies are gone, mark the wave complete and advance."
  [state]
  (if (wave-ready-to-complete? state) (mark-wave-complete state) state))

(defn- transform-living-battery
  [battery f]
  (if (:destroyed? battery) battery (f battery)))

(defn- map-living-batteries
  [state f]
  (update state :batteries
          (fn [bs]
            (mapv #(transform-living-battery % f) bs))))

(defn rearm-surviving-batteries
  "Refill non-destroyed batteries to full ammo."
  [state]
  (map-living-batteries state #(batteries/set-ammo % waves/full-ammo)))

(defn- wave-targets-for
  [living n]
  (if (seq living) (take n (cycle living)) []))

(defn set-wave-enemies-active
  "Test helper: replace in-flight enemies with n scheduled wave enemies."
  [state n]
  (let [active? (pos? n)
        state (assoc state
                     :enemy-missiles []
                     :wave-complete? wave-starts-complete?
                     :wave-had-enemies? active?)
        living (mapv :id (living-cities state))
        targets (vec (wave-targets-for living n))
        width (playfield-width state)]
    (reduce (fn [s [i city-id]]
              (spawn-enemy-targeting-city-from
               s (waves/sky-origin-x width i n) 0 city-id))
            state
            (map-indexed vector targets))))

(defn set-non-destroyed-battery-ammo
  "Test helper: set ammo on every non-destroyed battery."
  [state ammo]
  (map-living-batteries state #(batteries/set-ammo % ammo)))

(defn wave-schedule-metrics
  [wave-number]
  (waves/schedule-metrics wave-number))

(defn wave-mirv-count
  [wave-number]
  (waves/mirv-count wave-number))

(defn harder-wave?
  [low-metrics high-metrics]
  (waves/harder? low-metrics high-metrics))

(defn set-wave
  "Test helper: jump to a wave number without auto-completing."
  [state wave-number]
  (assoc state
         :wave wave-number
         :wave-complete? wave-starts-complete?
         :wave-had-enemies? wave-starts-with-enemies?
         :enemy-missiles []))

(defn start-next-wave
  "Begin the next wave: rearm survivors; wave number already advanced on complete."
  [state]
  (-> state
      (assoc :wave-complete? wave-starts-complete?
             :wave-had-enemies? wave-starts-with-enemies?)
      (rearm-surviving-batteries)))

(defn tick
  "Advance simulation by dt seconds (clamped). Returns {:state s :events [...]}."
  [state dt]
  (let [applied (missiles/clamp-dt dt)
        state (-> state
                  (assoc :last-applied-dt applied)
                  (update :sim-time (fnil + 0.0) applied)
                  (tick-defensive-missiles applied)
                  (tick-fireballs applied)
                  (destroy-targets-in-fireballs)
                  (tick-enemy-missiles applied)
                  (maybe-complete-wave))]
    {:state state :events []}))

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-07-24T14:10:47.966666-05:00", :module-hash "991160295", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 7, :hash "-1338775330"} {:id "def/initial-score", :kind "def", :line 9, :end-line 9, :hash "485183318"} {:id "def/initial-entity-id", :kind "def", :line 10, :end-line 10, :hash "-2006662704"} {:id "def/wave-flag-off", :kind "def", :line 11, :end-line 11, :hash "1734145513"} {:id "def/wave-flag-on", :kind "def", :line 12, :end-line 12, :hash "1660732832"} {:id "def/wave-starts-complete?", :kind "def", :line 13, :end-line 13, :hash "1259204240"} {:id "def/wave-starts-with-enemies?", :kind "def", :line 14, :end-line 14, :hash "929188796"} {:id "def/clamp-lo", :kind "def", :line 15, :end-line 15, :hash "-224595111"} {:id "def/default-crosshair", :kind "def", :line 16, :end-line 16, :hash "-249046571"} {:id "def/target-starts-destroyed?", :kind "def", :line 17, :end-line 17, :hash "224311611"} {:id "defn-/clamp", :kind "defn-", :line 19, :end-line 21, :hash "1680610514"} {:id "defn-/clamp-point", :kind "defn-", :line 23, :end-line 26, :hash "-1550073030"} {:id "defn-/center-crosshair", :kind "defn-", :line 28, :end-line 30, :hash "502710703"} {:id "defn-/reclamp-crosshair", :kind "defn-", :line 32, :end-line 35, :hash "-495207193"} {:id "defn-/update-battery", :kind "defn-", :line 37, :end-line 39, :hash "-630735181"} {:id "defn-/next-entity-id", :kind "defn-", :line 41, :end-line 44, :hash "-611923035"} {:id "defn-/no-events", :kind "defn-", :line 46, :end-line 48, :hash "652168329"} {:id "defn/new-game", :kind "defn", :line 50, :end-line 68, :hash "1405822368"} {:id "defn/resize", :kind "defn", :line 70, :end-line 77, :hash "-1344415357"} {:id "defn/playfield-width", :kind "defn", :line 79, :end-line 81, :hash "-1043537513"} {:id "defn/playfield-height", :kind "defn", :line 83, :end-line 85, :hash "344252362"} {:id "defn/cities", :kind "defn", :line 87, :end-line 89, :hash "1240083502"} {:id "defn/living-cities", :kind "defn", :line 91, :end-line 93, :hash "416648848"} {:id "defn/batteries", :kind "defn", :line 95, :end-line 97, :hash "206298614"} {:id "defn/battery", :kind "defn", :line 99, :end-line 101, :hash "-1555624967"} {:id "defn/on-ground?", :kind "defn", :line 103, :end-line 106, :hash "1609612027"} {:id "defn/city-on-ground?", :kind "defn", :line 108, :end-line 110, :hash "-1878088970"} {:id "defn/crosshair", :kind "defn", :line 112, :end-line 114, :hash "-2027795649"} {:id "defn/score", :kind "defn", :line 116, :end-line 118, :hash "-1700557235"} {:id "defn/wave", :kind "defn", :line 120, :end-line 122, :hash "1109090166"} {:id "defn/wave-complete?", :kind "defn", :line 124, :end-line 126, :hash "-334236383"} {:id "defn/hud", :kind "defn", :line 128, :end-line 132, :hash "1267084367"} {:id "defn/defensive-missiles", :kind "defn", :line 134, :end-line 136, :hash "-1457861839"} {:id "defn/fireballs", :kind "defn", :line 138, :end-line 140, :hash "-47675919"} {:id "defn/enemy-missiles", :kind "defn", :line 142, :end-line 144, :hash "-1649887754"} {:id "defn/destroyable-targets", :kind "defn", :line 146, :end-line 148, :hash "-2146081921"} {:id "defn/last-enemy-fate", :kind "defn", :line 150, :end-line 152, :hash "-1164295963"} {:id "defn/city", :kind "defn", :line 154, :end-line 156, :hash "417115868"} {:id "defn/living-city?", :kind "defn", :line 158, :end-line 160, :hash "808122796"} {:id "defn/sim-time", :kind "defn", :line 162, :end-line 164, :hash "1526499425"} {:id "defn/last-applied-dt", :kind "defn", :line 166, :end-line 168, :hash "-1011651673"} {:id "defn/max-fireball-radius", :kind "defn", :line 170, :end-line 172, :hash "421742428"} {:id "defn/set-battery-ammo", :kind "defn", :line 174, :end-line 177, :hash "975933252"} {:id "defn/destroy-battery", :kind "defn", :line 179, :end-line 182, :hash "674766162"} {:id "defn/add-destroyable-target", :kind "defn", :line 184, :end-line 189, :hash "-1701043486"} {:id "defn-/update-city", :kind "defn-", :line 191, :end-line 193, :hash "-2016100813"} {:id "defn/destroy-city", :kind "defn", :line 195, :end-line 197, :hash "1888198826"} {:id "defn-/enemy-speed-for-state", :kind "defn-", :line 199, :end-line 201, :hash "-677140217"} {:id "defn/spawn-enemy-at", :kind "defn", :line 203, :end-line 213, :hash "-191047273"} {:id "defn/spawn-enemy-targeting-city-from", :kind "defn", :line 215, :end-line 224, :hash "297620600"} {:id "defn/spawn-enemy-targeting-city", :kind "defn", :line 226, :end-line 231, :hash "1574498502"} {:id "defn/spawn-enemy-targeting-battery-from", :kind "defn", :line 233, :end-line 242, :hash "-765503325"} {:id "defn/spawn-enemy-targeting-battery", :kind "defn", :line 244, :end-line 249, :hash "1391767096"} {:id "defn/spawn-enemies-targeting-distinct-cities", :kind "defn", :line 251, :end-line 255, :hash "314430177"} {:id "defn/add-static-fireball", :kind "defn", :line 257, :end-line 262, :hash "2053229248"} {:id "defn/route-enemy-through-point", :kind "defn", :line 264, :end-line 278, :hash "-439948960"} {:id "defn-/impact-target", :kind "defn-", :line 280, :end-line 285, :hash "-984684299"} {:id "defn-/enemy-hit-by-fireball?", :kind "defn-", :line 287, :end-line 289, :hash "-583076826"} {:id "defn-/fire-battery", :kind "defn-", :line 291, :end-line 302, :hash "-618779090"} {:id "defn-/aim", :kind "defn-", :line 304, :end-line 310, :hash "242968114"} {:id "defn/click-zone", :kind "defn", :line 312, :end-line 315, :hash "943238228"} {:id "defn/click-fallback-order", :kind "defn", :line 317, :end-line 320, :hash "-1791151582"} {:id "defn-/click-fire", :kind "defn-", :line 322, :end-line 332, :hash "-533006603"} {:id "defn/handle", :kind "defn", :line 334, :end-line 342, :hash "-1723942109"} {:id "defn-/spawn-fireball-at", :kind "defn-", :line 344, :end-line 349, :hash "-1922366979"} {:id "defn-/spawn-fireball-from-missile", :kind "defn-", :line 351, :end-line 353, :hash "1839324960"} {:id "defn-/tick-defensive-missiles", :kind "defn-", :line 354, :end-line 362, :hash "465906604"} {:id "defn-/tick-fireballs", :kind "defn-", :line 364, :end-line 372, :hash "-1794535937"} {:id "defn-/target-hit-by-fireball?", :kind "defn-", :line 374, :end-line 376, :hash "-18018455"} {:id "defn-/destroy-targets-in-fireballs", :kind "defn-", :line 378, :end-line 388, :hash "-1920073096"} {:id "defn-/destroy-enemy-by-fireball", :kind "defn-", :line 390, :end-line 392, :hash "650263139"} {:id "defn-/spawn-impact-fireball", :kind "defn-", :line 394, :end-line 397, :hash "-2084493934"} {:id "defn-/resolve-enemy-impact", :kind "defn-", :line 398, :end-line 403, :hash "1944987463"} {:id "defn-/keep-flying-enemy", :kind "defn-", :line 405, :end-line 407, :hash "-1439807545"} {:id "defn-/tick-one-enemy", :kind "defn-", :line 409, :end-line 425, :hash "56096190"} {:id "defn-/tick-enemy-missiles", :kind "defn-", :line 427, :end-line 433, :hash "-1658169989"} {:id "defn-/wave-ready-to-complete?", :kind "defn-", :line 435, :end-line 440, :hash "-1319953137"} {:id "defn-/mark-wave-complete", :kind "defn-", :line 442, :end-line 447, :hash "746013251"} {:id "defn-/maybe-complete-wave", :kind "defn-", :line 449, :end-line 452, :hash "1290373418"} {:id "defn-/transform-living-battery", :kind "defn-", :line 454, :end-line 456, :hash "-703267492"} {:id "defn-/map-living-batteries", :kind "defn-", :line 458, :end-line 462, :hash "-1924229694"} {:id "defn/rearm-surviving-batteries", :kind "defn", :line 464, :end-line 467, :hash "1499444922"} {:id "defn-/wave-targets-for", :kind "defn-", :line 469, :end-line 471, :hash "-1532070143"} {:id "defn/set-wave-enemies-active", :kind "defn", :line 473, :end-line 488, :hash "-439255699"} {:id "defn/set-non-destroyed-battery-ammo", :kind "defn", :line 490, :end-line 493, :hash "510144529"} {:id "defn/wave-schedule-metrics", :kind "defn", :line 495, :end-line 497, :hash "-550911174"} {:id "defn/harder-wave?", :kind "defn", :line 499, :end-line 501, :hash "-1219120849"} {:id "defn/set-wave", :kind "defn", :line 503, :end-line 510, :hash "1005128065"} {:id "defn/start-next-wave", :kind "defn", :line 512, :end-line 518, :hash "98352319"} {:id "defn/tick", :kind "defn", :line 520, :end-line 532, :hash "-1833206829"}]}
;; clj-mutate-manifest-end

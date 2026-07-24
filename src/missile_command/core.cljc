(ns missile-command.core
  (:require [missile-command.batteries :as batteries]
            [missile-command.cities :as cities]
            [missile-command.flyers :as flyers]
            [missile-command.game-end :as game-end]
            [missile-command.input :as input]
            [missile-command.missiles :as missiles]
            [missile-command.scoring :as scoring]
            [missile-command.screens :as screens]
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
(def screen-title screens/title)
(def screen-playing screens/playing)
(def screen-the-end screens/the-end)
(def end-message-text game-end/message-text)
(def title-game-name screens/title-game-name)
(def title-start-affordance screens/title-start-affordance)
(def end-fireball-expand-seconds missiles/fireball-expand-seconds)
(def end-fireball-contract-seconds missiles/fireball-contract-seconds)

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
          :screen screen-title
          :title-game-name title-game-name
          :title-start-affordance title-start-affordance
          :end-message nil
          :end-fireball nil
          :final-score nil
          :end-message-reveal 0.0
          :crosshair (center-crosshair width height)
          :defensive-missiles []
          :fireballs []
          :enemy-missiles []
          :flyers []
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

(defn screen
  [state]
  (screens/of state))

(defn title?
  [state]
  (screens/title? state))

(defn playing?
  [state]
  (screens/playing? state))

(defn the-end?
  "True when the run has entered THE END."
  [state]
  (screens/the-end? state))

(defn title-game-name-of
  [state]
  (screens/title-game-name-of state))

(defn title-shows-start-affordance?
  [state]
  (screens/title-shows-start-affordance? state))

(defn start-game
  "Leave title (or any shell) and begin a fresh playing run at current size."
  [state]
  (let [w (playfield-width state)
        h (playfield-height state)]
    (-> (new-game {:width w :height h})
        (assoc :screen screen-playing))))

(defn confirm-end-screen
  "Confirm THE END when no high-score entry is required; return to title."
  [state]
  (if (the-end? state)
    (let [w (playfield-width state)
          h (playfield-height state)]
      (new-game {:width w :height h}))
    state))

(defn end-message
  [state]
  (:end-message state))

(defn final-score
  "Score frozen at THE END, else current score."
  [state]
  (long (or (:final-score state) (score state))))

(defn end-fireball
  [state]
  (:end-fireball state))

(defn hud
  "Minimal HUD projection for hosts and tests."
  [state]
  {:wave (wave state)
   :score (score state)
   :multiplier (multiplier state)
   :bonus-cities (bonus-cities state)
   :screen (screen state)
   :the-end? (the-end? state)
   :end-message (end-message state)
   :title-game-name (title-game-name-of state)})

(defn defensive-missiles
  [state]
  (or (:defensive-missiles state) []))

(defn fireballs
  [state]
  (or (:fireballs state) []))

(defn enemy-missiles
  [state]
  (or (:enemy-missiles state) []))

(defn flyers
  [state]
  (or (:flyers state) []))

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
(def enemy-kind-smart :smart)

;; Edge band for smart-bomb evasion: outer ring of the blast (ratio of radius).
(def smart-bomb-edge-inner-factor 0.625)
(def ^:private smart-not-yet-evaded false)
(def smart-bomb-evade-clearance 8.0)

(defn- mirv-parent?
  [enemy]
  (= enemy-kind-mirv (:enemy-kind enemy)))

(defn- mirv-child?
  [enemy]
  (= enemy-kind-mirv-child (:enemy-kind enemy)))

(defn- smart-bomb?
  [enemy]
  (= enemy-kind-smart (:enemy-kind enemy)))

(defn mirv-parents
  [state]
  (filterv mirv-parent? (enemy-missiles state)))

(defn mirv-children
  [state]
  (filterv mirv-child? (enemy-missiles state)))

(defn smart-bombs
  [state]
  (filterv smart-bomb? (enemy-missiles state)))

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

(defn spawn-smart-bomb-targeting-city
  "Spawn a smart bomb toward a city (can evade edge-of-blast fireballs once)."
  [state city-id]
  (let [c (city state city-id)]
    (when-not c
      (throw (ex-info (str "unknown city " city-id) {:city-id city-id})))
    (spawn-enemy-at state
                    {:x (:x c) :y 0}
                    {:x (:x c) :y (:y c)}
                    :city city-id
                    {:enemy-kind enemy-kind-smart
                     :smart-evaded? smart-not-yet-evaded})))

(defn spawn-flyer
  "Spawn a bomber or satellite traversing from start to end at speed."
  [state flyer-kind start-x start-y end-x end-y speed]
  (let [[fid state] (next-entity-id state)
        flyer (flyers/make fid flyer-kind start-x start-y end-x end-y speed)]
    (-> state
        (update :flyers (fnil conj []) flyer)
        (assoc :wave-had-enemies? wave-flag-on
               :wave-complete? wave-starts-complete?))))

(defn set-flyer-drops
  "Configure drop events on the first flyer (path progress + targets)."
  [state drops]
  (update state :flyers
          (fn [fs]
            (if (seq fs)
              (assoc (vec fs) 0 (assoc (first fs) :drops (vec drops) :drops-fired #{}))
              (vec fs)))))

(defn set-flyer-drops-toward-living-cities
  "First flyer drops `drop-count` missiles at `drop-progress` toward living cities."
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
  "First flyer drops one missile at progress toward a city."
  [state city-id drop-progress]
  (set-flyer-drops state
                   [{:id 0
                     :at-progress (double drop-progress)
                     :target [:city city-id]}]))

(defn flyers-of-kind
  [state flyer-kind]
  (filterv #(= (keyword flyer-kind) (:kind %)) (flyers state)))

(defn add-static-fireball
  "Test/setup helper: place a fixed-radius fireball."
  [state x y radius]
  (let [[fid state] (next-entity-id state)
        fb (missiles/make-static-fireball fid x y radius)]
    (update state :fireballs (fnil conj []) fb)))

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

(defn route-first-smart-bomb-through-point
  "Retarget the first smart bomb so its path starts at the given point."
  [state x y]
  (update state :enemy-missiles
          (fn [ms]
            (if-let [idx (first-enemy-index ms smart-bomb?)]
              (retarget-enemy-at-index ms idx x y)
              (vec ms)))))

(defn route-smart-bomb-centered-in-fireball
  "Place the smart bomb path through the fireball center (well-centered kill)."
  [state fb-x fb-y _center-limit]
  (route-first-smart-bomb-through-point state fb-x fb-y))

(defn route-smart-bomb-edge-band-in-fireball
  "Place the smart bomb path through the edge band of the fireball (evade once)."
  [state fb-x fb-y edge-inner radius]
  (let [mid (/ (+ (double edge-inner) (double radius)) 2.0)
        ;; Offset east of center so approach is in the edge ring only.
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

(defn- first-mirv-child-index
  [enemies]
  (first-enemy-index enemies mirv-child?))

(defn route-first-mirv-child-through-point
  "Retarget the first MIRV child so its path starts at the given point."
  [state x y]
  (update state :enemy-missiles
          (fn [ms]
            (if-let [idx (first-mirv-child-index ms)]
              (retarget-enemy-at-index ms idx x y)
              (vec ms)))))

(defn- impact-target
  [state enemy]
  (case (:target-kind enemy)
    :city (destroy-city state (:target-id enemy))
    :battery (destroy-battery state (:target-id enemy))
    state))

(defn- enemy-hit-by-fireball?
  [enemy fireballs]
  (some #(missiles/point-in-fireball? % (:x enemy) (:y enemy)) fireballs))

(defn- distance-to-fireball
  [enemy fireball]
  (let [dx (- (double (:x enemy)) (double (:x fireball)))
        dy (- (double (:y enemy)) (double (:y fireball)))]
    (Math/sqrt (+ (* dx dx) (* dy dy)))))

(defn- first-touching-fireball
  [enemy fireballs]
  (first (filter #(missiles/point-in-fireball? % (:x enemy) (:y enemy)) fireballs)))

(defn- smart-bomb-edge-band?
  "True when distance is outside the lethal core but still inside the blast."
  [d radius]
  (let [edge-inner (* (double radius) smart-bomb-edge-inner-factor)]
    (and (> d edge-inner) (<= d (double radius)))))

(defn- evade-smart-bomb
  "Steer clear of the fireball once; keep original target."
  [enemy fireball]
  (let [fx (double (:x fireball))
        fy (double (:y fireball))
        r (double (:radius fireball))
        ex (double (:x enemy))
        ey (double (:y enemy))
        dx (- ex fx)
        dy (- ey fy)
        dist (max 1.0e-6 (Math/sqrt (+ (* dx dx) (* dy dy))))
        clear (+ r smart-bomb-evade-clearance)
        nx (+ fx (* dx (/ clear dist)))
        ny (+ fy (* dy (/ clear dist)))
        retargeted (missiles/make-enemy (:id enemy)
                                        {:x nx :y ny}
                                        {:x (:x1 enemy) :y (:y1 enemy)}
                                        (:speed enemy)
                                        (:target-kind enemy)
                                        (:target-id enemy))]
    (merge retargeted
           (enemy-attrs-to-preserve enemy)
           {:enemy-kind enemy-kind-smart
            :smart-evaded? true
            :smart-evaded-fireball-id (:id fireball)})))

(defn- fire-battery
  [state battery-id]
  (if-not (playing? state)
    (no-events state)
    (let [bat (battery state battery-id)]
      (if-not (batteries/can-fire? bat)
        (no-events state)
        (let [[missile-id state] (next-entity-id state)
              missile (missiles/make-defensive missile-id battery-id bat
                                               (crosshair state))]
          {:state (-> state
                      (update :defensive-missiles (fnil conj []) missile)
                      (update-battery battery-id batteries/spend-ammo))
           :events [{:type :sfx/launch :battery battery-id}]})))))

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

(defn- handle-click
  [state x y]
  (cond
    (title? state) (no-events (start-game state))
    (the-end? state) (no-events (confirm-end-screen state))
    :else (click-fire state x y)))

(defn- unsupported-command
  [command]
  (throw (ex-info (str "unsupported command: " (:type command))
                  {:command command})))

(def ^:private command-handlers
  {:aim (fn [state cmd] (aim state (:x cmd) (:y cmd)))
   :fire (fn [state cmd] (fire-battery state (:battery cmd)))
   :click (fn [state cmd] (handle-click state (:x cmd) (:y cmd)))
   :start (fn [state _] (no-events (start-game state)))
   :confirm (fn [state _] (no-events (confirm-end-screen state)))})

(defn handle
  "Apply a player command. Returns {:state s :events [...]}."
  [state command]
  (if-let [handler (get command-handlers (:type command))]
    (handler state command)
    (unsupported-command command)))

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

(defn- make-end-fireball
  [state fireball-id]
  (game-end/make-fireball fireball-id
                          (playfield-width state)
                          (playfield-height state)
                          end-fireball-expand-seconds
                          end-fireball-contract-seconds))

(defn- update-end-message-reveal
  [state]
  (assoc state :end-message-reveal
         (game-end/reveal-fraction (end-fireball state))))

(defn- enter-the-end
  [state]
  (let [[fid state] (next-entity-id state)
        fb (make-end-fireball state fid)]
    (-> state
        (assoc :screen screen-the-end
               :end-message end-message-text
               :final-score (score state)
               :end-fireball fb
               :enemy-missiles []
               :flyers []
               :defensive-missiles [])
        update-end-message-reveal)))

(defn evaluate-game-over
  "Apply reserve restores; enter THE END when no living cities and no reserve."
  [state]
  (if (the-end? state)
    state
    (let [restored (apply-bonus-cities-from-reserve state)]
      (if (game-end/should-enter? (count (living-cities restored))
                                  (bonus-cities restored))
        (enter-the-end restored)
        restored))))

(defn end-fireball-centered?
  [state]
  (game-end/fireball-centered? (end-fireball state)
                               (playfield-width state)
                               (playfield-height state)))

(defn end-fireball-fills-playfield?
  [state]
  (game-end/fireball-fills-playfield? (end-fireball state)))

(defn end-message-layout
  "Glyph bounds for THE END: a square matching the max end-fireball diameter."
  [state]
  (game-end/message-layout (end-fireball state)))

(defn end-message-fills-max-expanse?
  [state]
  (game-end/message-fills-max-expanse? (end-fireball state)))

(defn end-message-centered?
  [state]
  (game-end/message-centered? (end-message-layout state)
                              (playfield-width state)
                              (playfield-height state)))

(defn end-message-visibility-clipped?
  "Letters are only drawn inside the end fireball disk."
  [state]
  (boolean (and (the-end? state) (end-fireball state))))

(defn end-message-point-visible?
  "A glyph point is visible only when inside the current end fireball disk."
  [state x y]
  (game-end/point-visible? (end-fireball state) x y))

(defn end-message-reveal
  "0..1 fraction of the message revealed as the end fireball expands."
  [state]
  (double (or (:end-message-reveal state) 0.0)))

(defn- tick-end-fireball
  [state dt]
  (if-let [fb (end-fireball state)]
    (let [result (missiles/advance-fireball fb dt)]
      (if (= missiles/expired result)
        (update-end-message-reveal (assoc state :end-fireball
                                          (assoc fb
                                                 :age (missiles/fireball-lifetime fb)
                                                 :radius 0.0)))
        (update-end-message-reveal (assoc state :end-fireball result))))
    state))

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
  [state enemy]
  (-> state
      (add-score (scoring/enemy-kill-points
                  (if (smart-bomb? enemy) :smart :ballistic)
                  (multiplier state)))
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

(defn- resolve-fireball-contact
  "Destroy, evade (smart bombs once), or ignore contact with a fireball."
  [state enemy fireballs]
  (if-let [fb (first-touching-fireball enemy fireballs)]
    (if (and (smart-bomb? enemy)
             (not (:smart-evaded? enemy))
             (smart-bomb-edge-band? (distance-to-fireball enemy fb)
                                    (:radius fb)))
      (keep-flying-enemy state (evade-smart-bomb enemy fb))
      (destroy-enemy-by-fireball state enemy))
    (keep-flying-enemy state enemy)))

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
  "Apply MIRV split, impact, fireball kill/evade, or continued flight."
  [state enemy result fireballs]
  (cond
    (should-split-mirv? enemy result)
    (split-mirv-parent state enemy)

    (missiles/arrived? result)
    (resolve-enemy-impact state enemy)

    (enemy-hit-by-fireball? result fireballs)
    (resolve-fireball-contact state result fireballs)

    :else
    (keep-flying-enemy state result)))

(defn- tick-one-enemy
  [state enemy dt fireballs]
  (if (enemy-hit-by-fireball? enemy fireballs)
    (resolve-fireball-contact state enemy fireballs)
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

(defn- destroy-flyer-by-fireball
  [state]
  (-> state
      (add-score (scoring/flyer-kill-points (multiplier state)))
      (assoc :last-enemy-fate :fireball
             :last-flyer-fate :fireball)))

(defn- apply-flyer-drops
  [state flyer]
  (let [drops (flyers/pending-drops flyer (:progress flyer))]
    (if (seq drops)
      (let [state (reduce
                   (fn [s drop]
                     (let [at (flyers/position-at flyer (:at-progress drop))
                           [kind id] (:target drop)
                           c (when (= kind :city) (city s id))]
                       (if (and (= kind :city) c)
                         (spawn-enemy-at s at {:x (:x c) :y (:y c)} :city id
                                         {:dropped-from-flyer? true})
                         s)))
                   state
                   drops)
            flyer' (update flyer :drops-fired
                           (fnil into #{}) (map :id drops))]
        [state flyer'])
      [state flyer])))

(defn- keep-flying-flyer
  [state flyer]
  (update state :flyers (fnil conj []) flyer))

(defn- tick-one-flyer
  [state flyer dt fireballs]
  (if (flyers/hit-by-fireball? flyer fireballs)
    (destroy-flyer-by-fireball state)
    (let [result (flyers/advance flyer dt)]
      (cond
        (= :left result)
        state

        (flyers/hit-by-fireball? result fireballs)
        (destroy-flyer-by-fireball state)

        :else
        (let [[s flyer'] (apply-flyer-drops state result)]
          (keep-flying-flyer s flyer'))))))

(defn- tick-flyers
  [state dt]
  (let [fbs (fireballs state)]
    (reduce (fn [s flyer]
              (tick-one-flyer s flyer dt fbs))
            (assoc state :flyers [])
            (flyers state))))

(defn- wave-ready-to-complete?
  [state]
  (boolean
   (and (:wave-had-enemies? state)
        (not (:wave-complete? state))
        (empty? (enemy-missiles state))
        (empty? (flyers state)))))

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

(defn- non-destroyed-batteries
  [state]
  (filterv (complement :destroyed?) (batteries state)))

(defn- wave-target-pool
  "Eligible wave targets: living cities then non-destroyed batteries."
  [state]
  (waves/target-pool (mapv :id (living-cities state))
                     (mapv :id (non-destroyed-batteries state))))

(defn- spawn-wave-enemy
  "Spawn one wave enemy toward a city or battery from a sky origin."
  [state origin-x target-spec]
  (let [[kind id] target-spec]
    (case kind
      :city (spawn-enemy-targeting-city-from state origin-x 0 id)
      :battery (spawn-enemy-targeting-battery-from state origin-x 0 id)
      state)))

(defn spawn-wave-enemy-targeting-battery
  "Spawn a single wave-style enemy aimed at a battery from a sky origin."
  [state battery-id]
  (spawn-wave-enemy state
                    (waves/sky-origin-x (playfield-width state) 0 1)
                    [:battery battery-id]))

(defn set-wave-enemies-active
  "Replace in-flight enemies with n scheduled wave enemies.
  Targets cycle living cities and non-destroyed batteries."
  [state n]
  (let [active? (pos? n)
        state (assoc state
                     :enemy-missiles []
                     :wave-complete? wave-starts-complete?
                     :wave-had-enemies? active?)
        targets (waves/cycle-targets (wave-target-pool state) n)
        width (playfield-width state)]
    (reduce (fn [s [i target-spec]]
              (spawn-wave-enemy
               s (waves/sky-origin-x width i n) target-spec))
            state
            (map-indexed vector targets))))

(defn set-non-destroyed-battery-ammo
  "Test helper: set ammo on every non-destroyed battery."
  [state ammo]
  (map-living-batteries state #(batteries/set-ammo % ammo)))

(def wave-schedule-metrics waves/schedule-metrics)
(def wave-mirv-count waves/mirv-count)
(def wave-smart-bomb-count waves/smart-bomb-count)
(def wave-bomber-count waves/bomber-count)
(def wave-satellite-count waves/satellite-count)
(def harder-wave? waves/harder?)

(defn set-wave
  "Test helper: jump to a wave number without auto-completing."
  [state wave-number]
  (assoc state
         :wave wave-number
         :wave-complete? wave-starts-complete?
         :wave-had-enemies? wave-starts-with-enemies?
         :enemy-missiles []
         :flyers []))

(defn start-next-wave
  "Begin the next wave: rearm survivors; wave number already advanced on complete."
  [state]
  (-> state
      (assoc :wave-complete? wave-starts-complete?
             :wave-had-enemies? wave-starts-with-enemies?)
      (rearm-surviving-batteries)))

(defn- advance-clock
  [state applied]
  (-> state
      (assoc :last-applied-dt applied)
      (update :sim-time (fnil + 0.0) applied)))

(defn tick
  "Advance simulation by dt seconds (clamped). Returns {:state s :events [...]}.
  Title is idle (clock only); THE END only expands the end fireball."
  [state dt]
  (let [applied (missiles/clamp-dt dt)]
    (cond
      (title? state)
      {:state (advance-clock state applied) :events []}

      (the-end? state)
      {:state (-> state
                  (advance-clock applied)
                  (tick-end-fireball applied))
       :events []}

      :else
      (let [state (-> state
                      (advance-clock applied)
                      (tick-defensive-missiles applied)
                      (tick-fireballs applied)
                      (destroy-targets-in-fireballs)
                      (tick-enemy-missiles applied)
                      (tick-flyers applied)
                      (maybe-complete-wave)
                      (evaluate-game-over))]
        {:state state :events []}))))

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-07-24T14:42:57.127703-05:00", :module-hash "-474664051", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 9, :hash "-1709413254"} {:id "def/initial-score", :kind "def", :line 10, :end-line 10, :hash "485183318"} {:id "def/initial-entity-id", :kind "def", :line 11, :end-line 11, :hash "-2006662704"} {:id "def/initial-bonus-cities", :kind "def", :line 12, :end-line 12, :hash "774282307"} {:id "def/initial-bonus-cities-awarded", :kind "def", :line 13, :end-line 13, :hash "-135107397"} {:id "def/initial-bonus-city-earned-events", :kind "def", :line 14, :end-line 14, :hash "-446479005"} {:id "def/wave-flag-off", :kind "def", :line 15, :end-line 15, :hash "1734145513"} {:id "def/wave-flag-on", :kind "def", :line 16, :end-line 16, :hash "1660732832"} {:id "def/wave-starts-complete?", :kind "def", :line 17, :end-line 17, :hash "1259204240"} {:id "def/wave-starts-with-enemies?", :kind "def", :line 18, :end-line 18, :hash "929188796"} {:id "def/clamp-lo", :kind "def", :line 19, :end-line 19, :hash "-224595111"} {:id "def/default-crosshair", :kind "def", :line 20, :end-line 20, :hash "-249046571"} {:id "def/target-starts-destroyed?", :kind "def", :line 21, :end-line 21, :hash "224311611"} {:id "defn-/clamp", :kind "defn-", :line 23, :end-line 25, :hash "1680610514"} {:id "defn-/clamp-point", :kind "defn-", :line 27, :end-line 30, :hash "-1550073030"} {:id "defn-/center-crosshair", :kind "defn-", :line 32, :end-line 34, :hash "502710703"} {:id "defn-/reclamp-crosshair", :kind "defn-", :line 36, :end-line 39, :hash "-495207193"} {:id "defn-/update-battery", :kind "defn-", :line 41, :end-line 43, :hash "-630735181"} {:id "defn-/next-entity-id", :kind "defn-", :line 45, :end-line 48, :hash "-611923035"} {:id "defn-/no-events", :kind "defn-", :line 50, :end-line 52, :hash "652168329"} {:id "defn/new-game", :kind "defn", :line 54, :end-line 77, :hash "-519428917"} {:id "defn/resize", :kind "defn", :line 79, :end-line 86, :hash "-1344415357"} {:id "defn/playfield-width", :kind "defn", :line 88, :end-line 90, :hash "-1043537513"} {:id "defn/playfield-height", :kind "defn", :line 92, :end-line 94, :hash "344252362"} {:id "defn/cities", :kind "defn", :line 96, :end-line 98, :hash "1240083502"} {:id "defn/living-cities", :kind "defn", :line 100, :end-line 102, :hash "416648848"} {:id "defn/batteries", :kind "defn", :line 104, :end-line 106, :hash "206298614"} {:id "defn/battery", :kind "defn", :line 108, :end-line 110, :hash "-1555624967"} {:id "defn/on-ground?", :kind "defn", :line 112, :end-line 115, :hash "1609612027"} {:id "defn/city-on-ground?", :kind "defn", :line 117, :end-line 119, :hash "-1878088970"} {:id "defn/crosshair", :kind "defn", :line 121, :end-line 123, :hash "-2027795649"} {:id "defn/score", :kind "defn", :line 125, :end-line 127, :hash "-1700557235"} {:id "defn/wave", :kind "defn", :line 129, :end-line 131, :hash "1109090166"} {:id "defn/multiplier", :kind "defn", :line 133, :end-line 136, :hash "-1249467982"} {:id "defn-/long-state", :kind "defn-", :line 138, :end-line 140, :hash "-1384016045"} {:id "defn/bonus-cities", :kind "defn", :line 142, :end-line 145, :hash "1351687248"} {:id "defn/bonus-city-threshold", :kind "defn", :line 147, :end-line 149, :hash "1726572985"} {:id "defn/bonus-city-earned-events", :kind "defn", :line 151, :end-line 154, :hash "-2024093832"} {:id "defn/wave-complete?", :kind "defn", :line 156, :end-line 158, :hash "-334236383"} {:id "defn/hud", :kind "defn", :line 160, :end-line 166, :hash "1957882040"} {:id "defn/defensive-missiles", :kind "defn", :line 168, :end-line 170, :hash "-1457861839"} {:id "defn/fireballs", :kind "defn", :line 172, :end-line 174, :hash "-47675919"} {:id "defn/enemy-missiles", :kind "defn", :line 176, :end-line 178, :hash "-1649887754"} {:id "defn/flyers", :kind "defn", :line 180, :end-line 182, :hash "-195685942"} {:id "defn/destroyable-targets", :kind "defn", :line 184, :end-line 186, :hash "-2146081921"} {:id "defn/last-enemy-fate", :kind "defn", :line 188, :end-line 190, :hash "-1164295963"} {:id "defn/city", :kind "defn", :line 192, :end-line 194, :hash "417115868"} {:id "defn/living-city?", :kind "defn", :line 196, :end-line 198, :hash "808122796"} {:id "defn/sim-time", :kind "defn", :line 200, :end-line 202, :hash "1526499425"} {:id "defn/last-applied-dt", :kind "defn", :line 204, :end-line 206, :hash "-1011651673"} {:id "defn/max-fireball-radius", :kind "defn", :line 208, :end-line 210, :hash "421742428"} {:id "defn/set-battery-ammo", :kind "defn", :line 212, :end-line 215, :hash "975933252"} {:id "defn/destroy-battery", :kind "defn", :line 217, :end-line 220, :hash "674766162"} {:id "defn/add-destroyable-target", :kind "defn", :line 222, :end-line 227, :hash "-1701043486"} {:id "defn-/update-city", :kind "defn-", :line 229, :end-line 231, :hash "-2016100813"} {:id "defn/destroy-city", :kind "defn", :line 233, :end-line 235, :hash "1888198826"} {:id "defn-/enemy-speed-for-state", :kind "defn-", :line 237, :end-line 239, :hash "-677140217"} {:id "def/enemy-kind-ballistic", :kind "def", :line 241, :end-line 241, :hash "844796837"} {:id "def/enemy-kind-mirv", :kind "def", :line 242, :end-line 242, :hash "-903061239"} {:id "def/enemy-kind-mirv-child", :kind "def", :line 243, :end-line 243, :hash "53130372"} {:id "def/enemy-kind-smart", :kind "def", :line 244, :end-line 244, :hash "246438905"} {:id "def/smart-bomb-edge-inner-factor", :kind "def", :line 247, :end-line 247, :hash "-1342792927"} {:id "def/smart-not-yet-evaded", :kind "def", :line 248, :end-line 248, :hash "101087391"} {:id "def/smart-bomb-evade-clearance", :kind "def", :line 249, :end-line 249, :hash "-755204528"} {:id "defn-/mirv-parent?", :kind "defn-", :line 251, :end-line 253, :hash "763145358"} {:id "defn-/mirv-child?", :kind "defn-", :line 255, :end-line 257, :hash "519586696"} {:id "defn-/smart-bomb?", :kind "defn-", :line 259, :end-line 261, :hash "-490539965"} {:id "defn/mirv-parents", :kind "defn", :line 263, :end-line 265, :hash "-1734114206"} {:id "defn/mirv-children", :kind "defn", :line 267, :end-line 269, :hash "1045363527"} {:id "defn/smart-bombs", :kind "defn", :line 271, :end-line 273, :hash "1657617417"} {:id "defn/spawn-enemy-at", :kind "defn", :line 275, :end-line 290, :hash "-2030994445"} {:id "defn/spawn-enemy-targeting-city-from", :kind "defn", :line 292, :end-line 301, :hash "297620600"} {:id "defn/spawn-enemy-targeting-city", :kind "defn", :line 303, :end-line 308, :hash "1574498502"} {:id "defn/spawn-enemy-targeting-battery-from", :kind "defn", :line 310, :end-line 319, :hash "-765503325"} {:id "defn/spawn-enemy-targeting-battery", :kind "defn", :line 321, :end-line 326, :hash "1391767096"} {:id "defn/spawn-enemies-targeting-distinct-cities", :kind "defn", :line 328, :end-line 332, :hash "314430177"} {:id "defn/spawn-mirv-targeting-city", :kind "defn", :line 334, :end-line 346, :hash "-266412817"} {:id "defn/spawn-smart-bomb-targeting-city", :kind "defn", :line 348, :end-line 359, :hash "-976059765"} {:id "defn/spawn-flyer", :kind "defn", :line 361, :end-line 369, :hash "2100696208"} {:id "defn/set-flyer-drops", :kind "defn", :line 371, :end-line 378, :hash "-2098771675"} {:id "defn/set-flyer-drops-toward-living-cities", :kind "defn", :line 380, :end-line 393, :hash "-1546667140"} {:id "defn/set-flyer-drop-targeting-city", :kind "defn", :line 395, :end-line 401, :hash "-362325883"} {:id "defn/flyers-of-kind", :kind "defn", :line 403, :end-line 405, :hash "2117257319"} {:id "defn/add-static-fireball", :kind "defn", :line 407, :end-line 412, :hash "2053229248"} {:id "defn-/enemy-attrs-to-preserve", :kind "defn-", :line 414, :end-line 417, :hash "-111533279"} {:id "defn-/retarget-enemy-from", :kind "defn-", :line 419, :end-line 428, :hash "-550286216"} {:id "defn-/first-enemy-index", :kind "defn-", :line 430, :end-line 432, :hash "-1572668638"} {:id "defn-/retarget-enemy-at-index", :kind "defn-", :line 434, :end-line 436, :hash "1058188477"} {:id "defn/route-first-smart-bomb-through-point", :kind "defn", :line 438, :end-line 445, :hash "-155725677"} {:id "defn/route-smart-bomb-centered-in-fireball", :kind "defn", :line 447, :end-line 450, :hash "-541649650"} {:id "defn/route-smart-bomb-edge-band-in-fireball", :kind "defn", :line 452, :end-line 459, :hash "1354861353"} {:id "defn/route-flyer-through-point", :kind "defn", :line 461, :end-line 475, :hash "-723558539"} {:id "defn/route-enemy-through-point", :kind "defn", :line 477, :end-line 484, :hash "1758214460"} {:id "defn-/first-mirv-child-index", :kind "defn-", :line 486, :end-line 488, :hash "-357091384"} {:id "defn/route-first-mirv-child-through-point", :kind "defn", :line 490, :end-line 497, :hash "1808906776"} {:id "defn-/impact-target", :kind "defn-", :line 499, :end-line 504, :hash "-984684299"} {:id "defn-/enemy-hit-by-fireball?", :kind "defn-", :line 506, :end-line 508, :hash "-387864824"} {:id "defn-/distance-to-fireball", :kind "defn-", :line 510, :end-line 514, :hash "-1214701948"} {:id "defn-/first-touching-fireball", :kind "defn-", :line 516, :end-line 518, :hash "717444682"} {:id "defn-/smart-bomb-edge-band?", :kind "defn-", :line 520, :end-line 524, :hash "885202764"} {:id "defn-/evade-smart-bomb", :kind "defn-", :line 526, :end-line 550, :hash "1355113499"} {:id "defn-/fire-battery", :kind "defn-", :line 552, :end-line 563, :hash "-618779090"} {:id "defn-/aim", :kind "defn-", :line 565, :end-line 571, :hash "242968114"} {:id "defn/click-zone", :kind "defn", :line 573, :end-line 576, :hash "943238228"} {:id "defn/click-fallback-order", :kind "defn", :line 578, :end-line 581, :hash "-1791151582"} {:id "defn-/click-fire", :kind "defn-", :line 583, :end-line 593, :hash "1088598870"} {:id "defn/handle", :kind "defn", :line 595, :end-line 603, :hash "-1723942109"} {:id "defn-/spawn-fireball-at", :kind "defn-", :line 605, :end-line 610, :hash "-1922366979"} {:id "defn-/spawn-fireball-from-missile", :kind "defn-", :line 612, :end-line 614, :hash "1839324960"} {:id "defn-/tick-defensive-missiles", :kind "defn-", :line 615, :end-line 623, :hash "465906604"} {:id "defn-/tick-fireballs", :kind "defn-", :line 625, :end-line 633, :hash "-1794535937"} {:id "defn-/target-hit-by-fireball?", :kind "defn-", :line 635, :end-line 637, :hash "1356179508"} {:id "defn-/destroy-targets-in-fireballs", :kind "defn-", :line 639, :end-line 649, :hash "-1920073096"} {:id "defn-/assoc-long", :kind "defn-", :line 651, :end-line 653, :hash "-1607523900"} {:id "defn/set-bonus-city-threshold", :kind "defn", :line 655, :end-line 658, :hash "633568299"} {:id "defn/set-bonus-city-reserve", :kind "defn", :line 660, :end-line 663, :hash "301818582"} {:id "defn-/lowest-destroyed-city-id", :kind "defn-", :line 665, :end-line 671, :hash "-2041789500"} {:id "defn/apply-bonus-cities-from-reserve", :kind "defn", :line 673, :end-line 684, :hash "927622616"} {:id "defn-/sync-bonus-cities-from-score", :kind "defn-", :line 686, :end-line 700, :hash "593228589"} {:id "defn-/add-score", :kind "defn-", :line 702, :end-line 706, :hash "-269277404"} {:id "defn/set-score", :kind "defn", :line 708, :end-line 713, :hash "2036835626"} {:id "defn-/destroy-enemy-by-fireball", :kind "defn-", :line 715, :end-line 721, :hash "-1165575634"} {:id "defn-/spawn-impact-fireball", :kind "defn-", :line 723, :end-line 726, :hash "-2084493934"} {:id "defn-/resolve-enemy-impact", :kind "defn-", :line 728, :end-line 733, :hash "1944987463"} {:id "defn-/keep-flying-enemy", :kind "defn-", :line 735, :end-line 737, :hash "-1439807545"} {:id "defn-/resolve-fireball-contact", :kind "defn-", :line 739, :end-line 749, :hash "632440842"} {:id "defn-/progress-of", :kind "defn-", :line 751, :end-line 755, :hash "1779378488"} {:id "defn-/index-of-id", :kind "defn-", :line 757, :end-line 760, :hash "-934326406"} {:id "defn-/mirv-child-target-ids", :kind "defn-", :line 762, :end-line 770, :hash "-194964239"} {:id "defn-/split-mirv-parent", :kind "defn-", :line 772, :end-line 787, :hash "1014154875"} {:id "defn-/should-split-mirv?", :kind "defn-", :line 789, :end-line 793, :hash "-1686921459"} {:id "defn-/resolve-advanced-enemy", :kind "defn-", :line 795, :end-line 809, :hash "1088768601"} {:id "defn-/tick-one-enemy", :kind "defn-", :line 811, :end-line 817, :hash "2100624266"} {:id "defn-/tick-enemy-missiles", :kind "defn-", :line 819, :end-line 825, :hash "-1658169989"} {:id "defn-/destroy-flyer-by-fireball", :kind "defn-", :line 827, :end-line 832, :hash "1452981331"} {:id "defn-/apply-flyer-drops", :kind "defn-", :line 834, :end-line 852, :hash "1084438316"} {:id "defn-/keep-flying-flyer", :kind "defn-", :line 854, :end-line 856, :hash "1225384403"} {:id "defn-/tick-one-flyer", :kind "defn-", :line 858, :end-line 872, :hash "1709281062"} {:id "defn-/tick-flyers", :kind "defn-", :line 874, :end-line 880, :hash "73639010"} {:id "defn-/wave-ready-to-complete?", :kind "defn-", :line 882, :end-line 888, :hash "1949930133"} {:id "defn-/unused-defensive-missiles", :kind "defn-", :line 890, :end-line 896, :hash "52651543"} {:id "defn-/award-wave-end-bonuses", :kind "defn-", :line 898, :end-line 905, :hash "-1067549351"} {:id "defn-/mark-wave-complete", :kind "defn-", :line 907, :end-line 914, :hash "2011566928"} {:id "defn-/maybe-complete-wave", :kind "defn-", :line 916, :end-line 919, :hash "1290373418"} {:id "defn-/transform-living-battery", :kind "defn-", :line 921, :end-line 923, :hash "-703267492"} {:id "defn-/map-living-batteries", :kind "defn-", :line 925, :end-line 929, :hash "1661747933"} {:id "defn/rearm-surviving-batteries", :kind "defn", :line 931, :end-line 934, :hash "352805794"} {:id "defn-/wave-targets-for", :kind "defn-", :line 936, :end-line 938, :hash "-1532070143"} {:id "defn/set-wave-enemies-active", :kind "defn", :line 940, :end-line 955, :hash "-439255699"} {:id "defn/set-non-destroyed-battery-ammo", :kind "defn", :line 957, :end-line 960, :hash "-1800722181"} {:id "defn/wave-schedule-metrics", :kind "defn", :line 962, :end-line 964, :hash "-550911174"} {:id "defn/wave-mirv-count", :kind "defn", :line 966, :end-line 968, :hash "-4136486"} {:id "defn/wave-smart-bomb-count", :kind "defn", :line 970, :end-line 972, :hash "948455901"} {:id "defn/wave-bomber-count", :kind "defn", :line 974, :end-line 976, :hash "830978937"} {:id "defn/wave-satellite-count", :kind "defn", :line 978, :end-line 980, :hash "2089323577"} {:id "defn/harder-wave?", :kind "defn", :line 982, :end-line 984, :hash "-1219120849"} {:id "defn/set-wave", :kind "defn", :line 986, :end-line 994, :hash "1048551934"} {:id "defn/start-next-wave", :kind "defn", :line 996, :end-line 1002, :hash "98352319"} {:id "defn/tick", :kind "defn", :line 1004, :end-line 1017, :hash "979263788"}]}
;; clj-mutate-manifest-end

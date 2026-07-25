(ns missile-command.core
  (:require [missile-command.batteries :as batteries]
            [missile-command.cities :as cities]
            [missile-command.flyers :as flyers]
            [missile-command.game-end :as game-end]
            [missile-command.hud :as hud]
            [missile-command.input :as input]
            [missile-command.missiles :as missiles]
            [missile-command.scoring :as scoring]
            [missile-command.screens :as screens]
            [missile-command.high-scores :as high-scores]
            [missile-command.options :as options]
            [missile-command.sfx :as sfx]
            [missile-command.wave-banner :as wave-banner]
            [missile-command.wave-schedule :as wave-schedule]
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
(def screen-paused screens/paused)
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

(def sfx-events sfx/events)
(def sfx-emitted? sfx/emitted?)

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
          :high-scores []
          :high-score-capacity high-scores/default-capacity
          :pending-high-score nil
          :submitted-high-score-initials nil
          :options options/default-options
          :sfx-events []
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

(def screen screens/of)
(def title? screens/title?)
(def playing? screens/playing?)
(def paused? screens/paused?)
(def the-end? screens/the-end?)
(def title-game-name-of screens/title-game-name-of)
(def title-shows-start-affordance? screens/title-shows-start-affordance?)

(def high-score-table high-scores/table)
(def high-score-capacity high-scores/capacity)
(def pending-high-score high-scores/pending)
(def submitted-high-score-initials high-scores/submitted-initials)
(def high-score-entry? high-scores/entry-screen?)
(def high-scores-view? high-scores/view-screen?)
(def set-high-score-capacity high-scores/set-capacity)
(def add-high-score-entry high-scores/add-entry)
(def open-high-scores high-scores/open-view)
(def close-high-scores high-scores/close-view)

(defn pause-game
  "Enter paused from playing only; ignore on title/end/already paused."
  [state]
  (if (playing? state)
    (assoc state :screen screen-paused)
    state))

(defn resume-game
  "Return from paused to playing; no-op otherwise."
  [state]
  (if (paused? state)
    (assoc state :screen screen-playing)
    state))

(defn- blank-shell
  "New game shell at the same playfield size as source."
  [source]
  (new-game {:width (playfield-width source)
             :height (playfield-height source)}))

(def game-options options/of)
(def mute? options/mute-state?)
(def difficulty options/difficulty-of)
(def options? options/screen?)
(def open-options options/open)
(def leave-options options/leave)
(def set-mute options/set-mute-state)
(def set-difficulty options/set-difficulty-state)
(def bind-fire-key options/bind-fire-key-state)
(def fire-key-includes? options/fire-key-includes-state?)
(def pause-key-includes? options/pause-key-includes-state?)
(def wave-banner? wave-banner/screen?)
(def wave-banner wave-banner/of)
(def wave-banner-text wave-banner/text)
(def wave-banner-announced-wave wave-banner/announced-wave)
(def wave-banner-phase wave-banner/phase)
(def wave-banner-text-position wave-banner/text-position)
(def wave-banner-distance-to-center wave-banner/distance-to-center)

(defn export-settings
  "Serializable high scores and options for host persistence."
  [state]
  {:options (game-options state)
   :high-scores (high-score-table state)
   :high-score-capacity (high-score-capacity state)})

(defn import-settings
  "Restore high scores and options onto a shell state (e.g. after host restart)."
  [state settings]
  (let [settings (or settings {})]
    (assoc state
           :options (or (:options settings) options/default-options)
           :high-scores (vec (or (:high-scores settings) []))
           :high-score-capacity
           (long (or (:high-score-capacity settings)
                     high-scores/default-capacity)))))


(defn- apply-shell
  "High-score shell transition that also carries options."
  [shell-state source]
  (options/carry shell-state source))
(defn start-game
  "Leave title (or any shell) and begin a fresh playing run at current size."
  [state]
  (-> (high-scores/start-playing (blank-shell state) state)
      (apply-shell state)))

(defn final-score
  "Score frozen at THE END, else current score."
  [state]
  (long (or (:final-score state) (score state))))

(defn confirm-end-screen
  "After THE END: open initials entry if score qualifies, else return to title."
  [state]
  (apply-shell
   (high-scores/confirm-end state
                            (the-end? state)
                            (final-score state)
                            (blank-shell state))
   state))

(defn submit-high-score-initials
  "Insert pending score with initials, then return to title."
  [state initials]
  (apply-shell
   (high-scores/submit-entry state
                             (high-score-entry? state)
                             (long (or (pending-high-score state) (final-score state)))
                             initials
                             (blank-shell state))
   state))

(defn end-message
  [state]
  (:end-message state))

(defn end-fireball
  [state]
  (:end-fireball state))

(defn hud
  "In-game HUD projection: score, wave, multiplier, ammo, cities, reserve.
  Present during playing and paused; not required on title."
  [state]
  (hud/projection state))

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
  "Mark a battery destroyed; emit sfx when newly destroyed."
  [state battery-id]
  (let [bat (battery state battery-id)]
    (sfx/maybe-emit (update-battery state battery-id batteries/destroy)
                    (and bat (not (:destroyed? bat)))
                    :sfx/battery-destroyed)))

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
  (let [c (city state city-id)]
    (sfx/maybe-emit (update-city state city-id cities/destroy)
                    (and c (:alive? c))
                    :sfx/city-destroyed)))

(defn- enemy-speed-for-state
  "Enemy missile speed for the current wave, scaled by difficulty."
  [state]
  (:enemy-speed (waves/schedule-metrics (wave state) (difficulty state))))

(def enemy-kind-ballistic :ballistic)
(def enemy-kind-mirv :mirv)
(def enemy-kind-mirv-child :mirv-child)
(def enemy-kind-smart :smart)

;; Edge band for smart-bomb evasion: outer ring of the blast (ratio of radius).
;; Core (d <= factor*r) is lethal; outer ring dodges once. Wider than a thin rim
;; so near-misses are readable in play.
(def smart-bomb-edge-inner-factor 0.45)
(def ^:private smart-not-yet-evaded false)
(def smart-bomb-evade-clearance 12.0)

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
                                               (crosshair state))
              remaining (dec (long (:missiles bat)))
              events (sfx/launch-events battery-id remaining)
              state (-> state
                        (update :defensive-missiles (fnil conj []) missile)
                        (update-battery battery-id batteries/spend-ammo)
                        (sfx/emit-many events))]
          {:state state :events events})))))

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

(defn- click-noop-shell?
  "Screens where a click must not fire (pause, scores, options)."
  [state]
  (or (paused? state)
      (high-score-entry? state)
      (high-scores-view? state)
      (options? state)))

(defn- handle-click
  [state x y]
  (cond
    (title? state) (no-events (start-game state))
    (the-end? state) (no-events (confirm-end-screen state))
    (click-noop-shell? state) (no-events state)
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
   :confirm (fn [state _] (no-events (confirm-end-screen state)))
   :pause (fn [state _] (no-events (pause-game state)))
   :resume (fn [state _] (no-events (resume-game state)))
   :open-high-scores (fn [state _] (no-events (open-high-scores state)))
   :close-high-scores (fn [state _] (no-events (close-high-scores state)))
   :submit-high-score
   (fn [state cmd] (no-events (submit-high-score-initials state (:initials cmd))))
   :open-options (fn [state _] (no-events (open-options state)))
   :leave-options (fn [state _] (no-events (leave-options state)))
   :set-mute (fn [state cmd] (no-events (set-mute state (:mute cmd))))
   :set-difficulty (fn [state cmd] (no-events (set-difficulty state (:difficulty cmd))))
   :bind-fire-key (fn [state cmd]
                    (no-events (bind-fire-key state (:battery cmd) (:key cmd))))
   :key (fn [state cmd]
          (if-let [battery-id (options/key->battery (options/of state) (:key cmd))]
            (fire-battery state battery-id)
            (no-events state)))})

(defn handle
  "Apply a player command. Returns {:state s :events [...]}."
  [state command]
  (if-let [handler (get command-handlers (:type command))]
    (handler state command)
    (unsupported-command command)))
(defn press-key
  "Apply a remappable key: fire mapped battery when playing, else no-op result."
  [state key]
  (handle state {:type :key :key key}))

(defn- spawn-fireball-at
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
        update-end-message-reveal
        (sfx/emit :sfx/the-end))))

(defn evaluate-game-over
  "Enter THE END when no living cities and no reserve. Does not place reserve."
  [state]
  (if (the-end? state)
    state
    (if (game-end/should-enter? (count (living-cities state))
                                (bonus-cities state))
      (enter-the-end state)
      state)))

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
  "Award reserve cities for newly crossed score thresholds (place only at wave end)."
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
          (sfx/emit :sfx/bonus-city))
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
      (assoc :last-enemy-fate :fireball)
      (sfx/emit :sfx/intercepted)))

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
             :last-flyer-fate :fireball)
      (sfx/emit :sfx/intercepted)))

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

(defn- maybe-complete-wave
  "When all active wave enemies are gone, mark complete, announce next wave banner."
  [state]
  (if (wave-ready-to-complete? state)
    (-> state
        award-wave-end-bonuses
        apply-bonus-cities-from-reserve
        (assoc :wave-complete? wave-flag-on
               :wave-had-enemies? wave-starts-with-enemies?)
        (update :wave (fnil inc waves/initial-wave))
        (#(wave-banner/enter % (wave %)))
        (sfx/emit :sfx/wave))
    state))


(defn- transform-living-battery
  [battery f]
  (if (:destroyed? battery) battery (f battery)))

(defn- map-living-batteries
  [state f]
  (update state :batteries
          (fn [bs]
            (mapv #(transform-living-battery % f) bs))))

(defn rearm-surviving-batteries
  "Restore every battery: clear destroyed and refill to full ammo.
  (Name kept for call-site compatibility; destroyed bases come back each wave.)"
  [state]
  (update state :batteries
          (fn [bs]
            (mapv #(batteries/restore % waves/full-ammo) (or bs [])))))

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

(defn activate-wave-schedule
  "Spawn the full current-wave schedule: ballistics, MIRVs, smart bombs, flyers.
  Hosts call this when the sky is empty and play should continue."
  [state]
  (wave-schedule/activate
   state
   {:wave wave
    :living-cities living-cities
    :city city
    :playfield-width playfield-width
    :playfield-height playfield-height
    :set-wave-enemies-active set-wave-enemies-active
    :spawn-enemy-at spawn-enemy-at
    :spawn-smart-bomb-targeting-city spawn-smart-bomb-targeting-city
    :spawn-flyer spawn-flyer
    :enemy-kind-mirv enemy-kind-mirv}))

(defn set-non-destroyed-battery-ammo
  "Test helper: set ammo on every non-destroyed battery."
  [state ammo]
  (map-living-batteries state #(batteries/set-ammo % ammo)))

(def wave-schedule-metrics waves/schedule-metrics)
(def wave-schedule-metrics-for waves/schedule-metrics-for-state)

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

(defn- clear-combat-entities
  "Remove in-flight combat visuals between waves (defensive missiles + fireballs).
  Does not restore cities (bonus cities handle that)."
  [state]
  (assoc state
         :defensive-missiles []
         :fireballs []
         :destroyable-targets []))

(defn start-next-wave
  "Begin the next wave: restore and rearm all batteries; clear leftover
  fireballs/missiles; leave banner."
  [state]
  (-> state
      wave-banner/clear
      clear-combat-entities
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
  Playing runs combat; wave-banner animates then resumes; THE END expands the
  end fireball; paused freezes; other shells advance the clock only."
  [state dt]
  (let [applied (missiles/clamp-dt dt)
        wrap (fn [s]
               {:state (sfx/maybe-title-warning s (title? s))
                :events []})]
    (cond
      (paused? state)
      (wrap (assoc state :last-applied-dt 0.0))

      (wave-banner? state)
      ;; Keep combat fireballs/missiles animating during the banner so they do
      ;; not pop out of existence; start-next-wave clears leftovers afterward.
      (wrap (-> state
                (advance-clock applied)
                (tick-defensive-missiles applied)
                (tick-fireballs applied)
                (wave-banner/tick applied start-next-wave)))

      (the-end? state)
      (wrap (-> state
                (advance-clock applied)
                (tick-end-fireball applied)))

      (playing? state)
      (wrap (-> state
                (advance-clock applied)
                (tick-defensive-missiles applied)
                (tick-fireballs applied)
                (destroy-targets-in-fireballs)
                (tick-enemy-missiles applied)
                (tick-flyers applied)
                (maybe-complete-wave)
                (evaluate-game-over)))

      :else
      ;; title, high-score-entry, high-scores view, options
      (wrap (advance-clock state applied)))))

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-07-25T10:05:15.964153-05:00", :module-hash "-194027955", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 17, :hash "-1082337446"} {:id "def/initial-score", :kind "def", :line 18, :end-line 18, :hash "485183318"} {:id "def/initial-entity-id", :kind "def", :line 19, :end-line 19, :hash "-2006662704"} {:id "def/initial-bonus-cities", :kind "def", :line 20, :end-line 20, :hash "774282307"} {:id "def/initial-bonus-cities-awarded", :kind "def", :line 21, :end-line 21, :hash "-135107397"} {:id "def/initial-bonus-city-earned-events", :kind "def", :line 22, :end-line 22, :hash "-446479005"} {:id "def/wave-flag-off", :kind "def", :line 23, :end-line 23, :hash "1734145513"} {:id "def/wave-flag-on", :kind "def", :line 24, :end-line 24, :hash "1660732832"} {:id "def/wave-starts-complete?", :kind "def", :line 25, :end-line 25, :hash "1259204240"} {:id "def/wave-starts-with-enemies?", :kind "def", :line 26, :end-line 26, :hash "929188796"} {:id "def/clamp-lo", :kind "def", :line 27, :end-line 27, :hash "-224595111"} {:id "def/default-crosshair", :kind "def", :line 28, :end-line 28, :hash "-249046571"} {:id "def/target-starts-destroyed?", :kind "def", :line 29, :end-line 29, :hash "224311611"} {:id "def/screen-title", :kind "def", :line 30, :end-line 30, :hash "1092741116"} {:id "def/screen-playing", :kind "def", :line 31, :end-line 31, :hash "1649702326"} {:id "def/screen-paused", :kind "def", :line 32, :end-line 32, :hash "-1395420898"} {:id "def/screen-the-end", :kind "def", :line 33, :end-line 33, :hash "184874292"} {:id "def/end-message-text", :kind "def", :line 34, :end-line 34, :hash "-1724984215"} {:id "def/title-game-name", :kind "def", :line 35, :end-line 35, :hash "-1224113475"} {:id "def/title-start-affordance", :kind "def", :line 36, :end-line 36, :hash "1586942227"} {:id "def/end-fireball-expand-seconds", :kind "def", :line 37, :end-line 37, :hash "1938981448"} {:id "def/end-fireball-contract-seconds", :kind "def", :line 38, :end-line 38, :hash "-273647692"} {:id "defn-/clamp", :kind "defn-", :line 40, :end-line 42, :hash "1680610514"} {:id "defn-/clamp-point", :kind "defn-", :line 44, :end-line 47, :hash "-1550073030"} {:id "defn-/center-crosshair", :kind "defn-", :line 49, :end-line 51, :hash "502710703"} {:id "defn-/reclamp-crosshair", :kind "defn-", :line 53, :end-line 56, :hash "-495207193"} {:id "defn-/update-battery", :kind "defn-", :line 58, :end-line 60, :hash "-630735181"} {:id "defn-/next-entity-id", :kind "defn-", :line 62, :end-line 65, :hash "-611923035"} {:id "defn-/no-events", :kind "defn-", :line 67, :end-line 69, :hash "652168329"} {:id "def/sfx-events", :kind "def", :line 71, :end-line 71, :hash "525363444"} {:id "def/sfx-emitted?", :kind "def", :line 72, :end-line 72, :hash "-1793854788"} {:id "defn/new-game", :kind "defn", :line 74, :end-line 110, :hash "1699967064"} {:id "defn/resize", :kind "defn", :line 112, :end-line 119, :hash "-1344415357"} {:id "defn/playfield-width", :kind "defn", :line 121, :end-line 123, :hash "-1043537513"} {:id "defn/playfield-height", :kind "defn", :line 125, :end-line 127, :hash "344252362"} {:id "defn/cities", :kind "defn", :line 129, :end-line 131, :hash "1240083502"} {:id "defn/living-cities", :kind "defn", :line 133, :end-line 135, :hash "416648848"} {:id "defn/batteries", :kind "defn", :line 137, :end-line 139, :hash "206298614"} {:id "defn/battery", :kind "defn", :line 141, :end-line 143, :hash "-1555624967"} {:id "defn/on-ground?", :kind "defn", :line 145, :end-line 148, :hash "1609612027"} {:id "defn/city-on-ground?", :kind "defn", :line 150, :end-line 152, :hash "-1878088970"} {:id "defn/crosshair", :kind "defn", :line 154, :end-line 156, :hash "-2027795649"} {:id "defn/score", :kind "defn", :line 158, :end-line 160, :hash "-1700557235"} {:id "defn/wave", :kind "defn", :line 162, :end-line 164, :hash "1109090166"} {:id "defn/multiplier", :kind "defn", :line 166, :end-line 169, :hash "-1249467982"} {:id "defn-/long-state", :kind "defn-", :line 171, :end-line 173, :hash "-1384016045"} {:id "defn/bonus-cities", :kind "defn", :line 175, :end-line 178, :hash "1351687248"} {:id "defn/bonus-city-threshold", :kind "defn", :line 180, :end-line 182, :hash "1726572985"} {:id "defn/bonus-city-earned-events", :kind "defn", :line 184, :end-line 187, :hash "-2024093832"} {:id "defn/wave-complete?", :kind "defn", :line 189, :end-line 191, :hash "-334236383"} {:id "def/screen", :kind "def", :line 193, :end-line 193, :hash "681051221"} {:id "def/title?", :kind "def", :line 194, :end-line 194, :hash "-413853504"} {:id "def/playing?", :kind "def", :line 195, :end-line 195, :hash "410765386"} {:id "def/paused?", :kind "def", :line 196, :end-line 196, :hash "665268154"} {:id "def/the-end?", :kind "def", :line 197, :end-line 197, :hash "486194475"} {:id "def/title-game-name-of", :kind "def", :line 198, :end-line 198, :hash "1444622798"} {:id "def/title-shows-start-affordance?", :kind "def", :line 199, :end-line 199, :hash "763145159"} {:id "def/high-score-table", :kind "def", :line 201, :end-line 201, :hash "1163853287"} {:id "def/high-score-capacity", :kind "def", :line 202, :end-line 202, :hash "-1128697050"} {:id "def/pending-high-score", :kind "def", :line 203, :end-line 203, :hash "2019144034"} {:id "def/submitted-high-score-initials", :kind "def", :line 204, :end-line 204, :hash "1796003601"} {:id "def/high-score-entry?", :kind "def", :line 205, :end-line 205, :hash "985452078"} {:id "def/high-scores-view?", :kind "def", :line 206, :end-line 206, :hash "336415907"} {:id "def/set-high-score-capacity", :kind "def", :line 207, :end-line 207, :hash "1210250449"} {:id "def/add-high-score-entry", :kind "def", :line 208, :end-line 208, :hash "785373078"} {:id "def/open-high-scores", :kind "def", :line 209, :end-line 209, :hash "-1768638500"} {:id "def/close-high-scores", :kind "def", :line 210, :end-line 210, :hash "-800419625"} {:id "defn/pause-game", :kind "defn", :line 212, :end-line 217, :hash "1793185663"} {:id "defn/resume-game", :kind "defn", :line 219, :end-line 224, :hash "1617711604"} {:id "defn-/blank-shell", :kind "defn-", :line 226, :end-line 230, :hash "533787309"} {:id "def/game-options", :kind "def", :line 232, :end-line 232, :hash "1685212516"} {:id "def/mute?", :kind "def", :line 233, :end-line 233, :hash "573004342"} {:id "def/difficulty", :kind "def", :line 234, :end-line 234, :hash "247818244"} {:id "def/options?", :kind "def", :line 235, :end-line 235, :hash "857221898"} {:id "def/open-options", :kind "def", :line 236, :end-line 236, :hash "-1773224624"} {:id "def/leave-options", :kind "def", :line 237, :end-line 237, :hash "-1553054960"} {:id "def/set-mute", :kind "def", :line 238, :end-line 238, :hash "961239161"} {:id "def/set-difficulty", :kind "def", :line 239, :end-line 239, :hash "-1413206900"} {:id "def/bind-fire-key", :kind "def", :line 240, :end-line 240, :hash "-86622152"} {:id "def/fire-key-includes?", :kind "def", :line 241, :end-line 241, :hash "109857506"} {:id "def/pause-key-includes?", :kind "def", :line 242, :end-line 242, :hash "-1954048295"} {:id "def/wave-banner?", :kind "def", :line 243, :end-line 243, :hash "1130113897"} {:id "def/wave-banner", :kind "def", :line 244, :end-line 244, :hash "911252070"} {:id "def/wave-banner-text", :kind "def", :line 245, :end-line 245, :hash "-1812837808"} {:id "def/wave-banner-announced-wave", :kind "def", :line 246, :end-line 246, :hash "549833261"} {:id "def/wave-banner-phase", :kind "def", :line 247, :end-line 247, :hash "-1207534753"} {:id "def/wave-banner-text-position", :kind "def", :line 248, :end-line 248, :hash "1533588800"} {:id "def/wave-banner-distance-to-center", :kind "def", :line 249, :end-line 249, :hash "368380193"} {:id "defn/export-settings", :kind "defn", :line 251, :end-line 256, :hash "-1585521700"} {:id "defn/import-settings", :kind "defn", :line 258, :end-line 267, :hash "221337773"} {:id "defn-/apply-shell", :kind "defn-", :line 270, :end-line 273, :hash "-1543202908"} {:id "defn/start-game", :kind "defn", :line 274, :end-line 278, :hash "321312616"} {:id "defn/final-score", :kind "defn", :line 280, :end-line 283, :hash "-2124677376"} {:id "defn/confirm-end-screen", :kind "defn", :line 285, :end-line 293, :hash "-1073333732"} {:id "defn/submit-high-score-initials", :kind "defn", :line 295, :end-line 304, :hash "-243607349"} {:id "defn/end-message", :kind "defn", :line 306, :end-line 308, :hash "1667840292"} {:id "defn/end-fireball", :kind "defn", :line 310, :end-line 312, :hash "603568745"} {:id "defn/hud", :kind "defn", :line 314, :end-line 318, :hash "-1986360486"} {:id "defn/defensive-missiles", :kind "defn", :line 320, :end-line 322, :hash "-1457861839"} {:id "defn/fireballs", :kind "defn", :line 324, :end-line 326, :hash "-47675919"} {:id "defn/enemy-missiles", :kind "defn", :line 328, :end-line 330, :hash "-1649887754"} {:id "defn/flyers", :kind "defn", :line 332, :end-line 334, :hash "-195685942"} {:id "defn/destroyable-targets", :kind "defn", :line 336, :end-line 338, :hash "-2146081921"} {:id "defn/last-enemy-fate", :kind "defn", :line 340, :end-line 342, :hash "-1164295963"} {:id "defn/city", :kind "defn", :line 344, :end-line 346, :hash "417115868"} {:id "defn/living-city?", :kind "defn", :line 348, :end-line 350, :hash "808122796"} {:id "defn/sim-time", :kind "defn", :line 352, :end-line 354, :hash "1526499425"} {:id "defn/last-applied-dt", :kind "defn", :line 356, :end-line 358, :hash "-1011651673"} {:id "defn/max-fireball-radius", :kind "defn", :line 360, :end-line 362, :hash "421742428"} {:id "defn/set-battery-ammo", :kind "defn", :line 364, :end-line 367, :hash "975933252"} {:id "defn/destroy-battery", :kind "defn", :line 369, :end-line 375, :hash "1155956327"} {:id "defn/add-destroyable-target", :kind "defn", :line 377, :end-line 382, :hash "-1701043486"} {:id "defn-/update-city", :kind "defn-", :line 384, :end-line 386, :hash "-2016100813"} {:id "defn/destroy-city", :kind "defn", :line 388, :end-line 393, :hash "-1181664448"} {:id "defn-/enemy-speed-for-state", :kind "defn-", :line 395, :end-line 398, :hash "104260835"} {:id "def/enemy-kind-ballistic", :kind "def", :line 400, :end-line 400, :hash "844796837"} {:id "def/enemy-kind-mirv", :kind "def", :line 401, :end-line 401, :hash "-903061239"} {:id "def/enemy-kind-mirv-child", :kind "def", :line 402, :end-line 402, :hash "53130372"} {:id "def/enemy-kind-smart", :kind "def", :line 403, :end-line 403, :hash "246438905"} {:id "def/smart-bomb-edge-inner-factor", :kind "def", :line 408, :end-line 408, :hash "1336091741"} {:id "def/smart-not-yet-evaded", :kind "def", :line 409, :end-line 409, :hash "101087391"} {:id "def/smart-bomb-evade-clearance", :kind "def", :line 410, :end-line 410, :hash "-1155691061"} {:id "defn-/mirv-parent?", :kind "defn-", :line 412, :end-line 414, :hash "763145358"} {:id "defn-/mirv-child?", :kind "defn-", :line 416, :end-line 418, :hash "519586696"} {:id "defn-/smart-bomb?", :kind "defn-", :line 420, :end-line 422, :hash "-490539965"} {:id "defn/mirv-parents", :kind "defn", :line 424, :end-line 426, :hash "-1734114206"} {:id "defn/mirv-children", :kind "defn", :line 428, :end-line 430, :hash "1045363527"} {:id "defn/smart-bombs", :kind "defn", :line 432, :end-line 434, :hash "1657617417"} {:id "defn/spawn-enemy-at", :kind "defn", :line 436, :end-line 451, :hash "-2030994445"} {:id "defn/spawn-enemy-targeting-city-from", :kind "defn", :line 453, :end-line 462, :hash "297620600"} {:id "defn/spawn-enemy-targeting-city", :kind "defn", :line 464, :end-line 469, :hash "1574498502"} {:id "defn/spawn-enemy-targeting-battery-from", :kind "defn", :line 471, :end-line 480, :hash "-765503325"} {:id "defn/spawn-enemy-targeting-battery", :kind "defn", :line 482, :end-line 487, :hash "1391767096"} {:id "defn/spawn-enemies-targeting-distinct-cities", :kind "defn", :line 489, :end-line 493, :hash "314430177"} {:id "defn/spawn-mirv-targeting-city", :kind "defn", :line 495, :end-line 507, :hash "-266412817"} {:id "defn/spawn-smart-bomb-targeting-city", :kind "defn", :line 509, :end-line 520, :hash "-976059765"} {:id "defn/spawn-flyer", :kind "defn", :line 522, :end-line 530, :hash "2100696208"} {:id "defn/set-flyer-drops", :kind "defn", :line 532, :end-line 539, :hash "-2098771675"} {:id "defn/set-flyer-drops-toward-living-cities", :kind "defn", :line 541, :end-line 554, :hash "-1546667140"} {:id "defn/set-flyer-drop-targeting-city", :kind "defn", :line 556, :end-line 562, :hash "-362325883"} {:id "defn/flyers-of-kind", :kind "defn", :line 564, :end-line 566, :hash "2117257319"} {:id "defn/add-static-fireball", :kind "defn", :line 568, :end-line 573, :hash "2053229248"} {:id "defn-/enemy-attrs-to-preserve", :kind "defn-", :line 575, :end-line 578, :hash "-111533279"} {:id "defn-/retarget-enemy-from", :kind "defn-", :line 580, :end-line 589, :hash "-550286216"} {:id "defn-/first-enemy-index", :kind "defn-", :line 591, :end-line 593, :hash "-1572668638"} {:id "defn-/retarget-enemy-at-index", :kind "defn-", :line 595, :end-line 597, :hash "1058188477"} {:id "defn/route-first-smart-bomb-through-point", :kind "defn", :line 599, :end-line 606, :hash "-155725677"} {:id "defn/route-smart-bomb-centered-in-fireball", :kind "defn", :line 608, :end-line 611, :hash "-541649650"} {:id "defn/route-smart-bomb-edge-band-in-fireball", :kind "defn", :line 613, :end-line 620, :hash "1354861353"} {:id "defn/route-flyer-through-point", :kind "defn", :line 622, :end-line 636, :hash "-723558539"} {:id "defn/route-enemy-through-point", :kind "defn", :line 638, :end-line 645, :hash "1758214460"} {:id "defn-/first-mirv-child-index", :kind "defn-", :line 647, :end-line 649, :hash "-357091384"} {:id "defn/route-first-mirv-child-through-point", :kind "defn", :line 651, :end-line 658, :hash "1808906776"} {:id "defn-/impact-target", :kind "defn-", :line 660, :end-line 665, :hash "-984684299"} {:id "defn-/enemy-hit-by-fireball?", :kind "defn-", :line 667, :end-line 669, :hash "-387864824"} {:id "defn-/distance-to-fireball", :kind "defn-", :line 671, :end-line 675, :hash "-1214701948"} {:id "defn-/first-touching-fireball", :kind "defn-", :line 677, :end-line 679, :hash "717444682"} {:id "defn-/smart-bomb-edge-band?", :kind "defn-", :line 681, :end-line 685, :hash "885202764"} {:id "defn-/evade-smart-bomb", :kind "defn-", :line 687, :end-line 711, :hash "1355113499"} {:id "defn-/fire-battery", :kind "defn-", :line 713, :end-line 729, :hash "1591374319"} {:id "defn-/aim", :kind "defn-", :line 731, :end-line 737, :hash "242968114"} {:id "defn/click-zone", :kind "defn", :line 739, :end-line 742, :hash "943238228"} {:id "defn/click-fallback-order", :kind "defn", :line 744, :end-line 747, :hash "-1791151582"} {:id "defn-/click-fire", :kind "defn-", :line 749, :end-line 759, :hash "1088598870"} {:id "defn-/click-noop-shell?", :kind "defn-", :line 761, :end-line 767, :hash "626310851"} {:id "defn-/handle-click", :kind "defn-", :line 769, :end-line 775, :hash "987185200"} {:id "defn-/unsupported-command", :kind "defn-", :line 777, :end-line 780, :hash "585518571"} {:id "def/command-handlers", :kind "def", :line 782, :end-line 803, :hash "-2121453946"} {:id "defn/handle", :kind "defn", :line 805, :end-line 810, :hash "1696801717"} {:id "defn/press-key", :kind "defn", :line 811, :end-line 814, :hash "1450190204"} {:id "defn-/spawn-fireball-at", :kind "defn-", :line 816, :end-line 824, :hash "1365986550"} {:id "defn-/spawn-fireball-from-missile", :kind "defn-", :line 826, :end-line 828, :hash "1839324960"} {:id "defn-/tick-defensive-missiles", :kind "defn-", :line 830, :end-line 838, :hash "465906604"} {:id "defn-/tick-fireballs", :kind "defn-", :line 840, :end-line 848, :hash "-1794535937"} {:id "defn-/target-hit-by-fireball?", :kind "defn-", :line 850, :end-line 852, :hash "1356179508"} {:id "defn-/destroy-targets-in-fireballs", :kind "defn-", :line 854, :end-line 864, :hash "-1920073096"} {:id "defn-/assoc-long", :kind "defn-", :line 866, :end-line 868, :hash "-1607523900"} {:id "defn/set-bonus-city-threshold", :kind "defn", :line 870, :end-line 873, :hash "633568299"} {:id "defn/set-bonus-city-reserve", :kind "defn", :line 875, :end-line 878, :hash "301818582"} {:id "defn-/lowest-destroyed-city-id", :kind "defn-", :line 880, :end-line 886, :hash "-2041789500"} {:id "defn/apply-bonus-cities-from-reserve", :kind "defn", :line 888, :end-line 899, :hash "927622616"} {:id "defn-/make-end-fireball", :kind "defn-", :line 901, :end-line 907, :hash "-918471848"} {:id "defn-/update-end-message-reveal", :kind "defn-", :line 909, :end-line 912, :hash "1654790648"} {:id "defn-/enter-the-end", :kind "defn-", :line 914, :end-line 927, :hash "-68727569"} {:id "defn/evaluate-game-over", :kind "defn", :line 929, :end-line 937, :hash "1952984408"} {:id "defn/end-fireball-centered?", :kind "defn", :line 939, :end-line 943, :hash "-1673190026"} {:id "defn/end-fireball-fills-playfield?", :kind "defn", :line 945, :end-line 947, :hash "496208359"} {:id "defn/end-message-layout", :kind "defn", :line 949, :end-line 952, :hash "-2147217889"} {:id "defn/end-message-fills-max-expanse?", :kind "defn", :line 954, :end-line 956, :hash "1595237781"} {:id "defn/end-message-centered?", :kind "defn", :line 958, :end-line 962, :hash "-447969280"} {:id "defn/end-message-visibility-clipped?", :kind "defn", :line 964, :end-line 967, :hash "-1616346772"} {:id "defn/end-message-point-visible?", :kind "defn", :line 969, :end-line 972, :hash "-703026949"} {:id "defn/end-message-reveal", :kind "defn", :line 974, :end-line 977, :hash "-172598567"} {:id "defn-/tick-end-fireball", :kind "defn-", :line 979, :end-line 989, :hash "473583625"} {:id "defn-/sync-bonus-cities-from-score", :kind "defn-", :line 991, :end-line 1005, :hash "-1764739627"} {:id "defn-/add-score", :kind "defn-", :line 1007, :end-line 1011, :hash "-269277404"} {:id "defn/set-score", :kind "defn", :line 1013, :end-line 1018, :hash "2036835626"} {:id "defn-/destroy-enemy-by-fireball", :kind "defn-", :line 1020, :end-line 1027, :hash "-172702724"} {:id "defn-/spawn-impact-fireball", :kind "defn-", :line 1029, :end-line 1032, :hash "-2084493934"} {:id "defn-/resolve-enemy-impact", :kind "defn-", :line 1034, :end-line 1039, :hash "1944987463"} {:id "defn-/keep-flying-enemy", :kind "defn-", :line 1041, :end-line 1043, :hash "-1439807545"} {:id "defn-/resolve-fireball-contact", :kind "defn-", :line 1045, :end-line 1055, :hash "632440842"} {:id "defn-/progress-of", :kind "defn-", :line 1057, :end-line 1061, :hash "1779378488"} {:id "defn-/index-of-id", :kind "defn-", :line 1063, :end-line 1066, :hash "-934326406"} {:id "defn-/mirv-child-target-ids", :kind "defn-", :line 1068, :end-line 1076, :hash "-194964239"} {:id "defn-/split-mirv-parent", :kind "defn-", :line 1078, :end-line 1093, :hash "1014154875"} {:id "defn-/should-split-mirv?", :kind "defn-", :line 1095, :end-line 1099, :hash "-1686921459"} {:id "defn-/resolve-advanced-enemy", :kind "defn-", :line 1101, :end-line 1115, :hash "1088768601"} {:id "defn-/tick-one-enemy", :kind "defn-", :line 1117, :end-line 1123, :hash "2100624266"} {:id "defn-/tick-enemy-missiles", :kind "defn-", :line 1125, :end-line 1131, :hash "-1658169989"} {:id "defn-/destroy-flyer-by-fireball", :kind "defn-", :line 1133, :end-line 1139, :hash "-1739016762"} {:id "defn-/apply-flyer-drops", :kind "defn-", :line 1141, :end-line 1159, :hash "1084438316"} {:id "defn-/keep-flying-flyer", :kind "defn-", :line 1161, :end-line 1163, :hash "1225384403"} {:id "defn-/tick-one-flyer", :kind "defn-", :line 1165, :end-line 1179, :hash "1709281062"} {:id "defn-/tick-flyers", :kind "defn-", :line 1181, :end-line 1187, :hash "73639010"} {:id "defn-/wave-ready-to-complete?", :kind "defn-", :line 1189, :end-line 1195, :hash "1949930133"} {:id "defn-/unused-defensive-missiles", :kind "defn-", :line 1197, :end-line 1203, :hash "52651543"} {:id "defn-/award-wave-end-bonuses", :kind "defn-", :line 1205, :end-line 1212, :hash "-1067549351"} {:id "defn-/maybe-complete-wave", :kind "defn-", :line 1214, :end-line 1226, :hash "-929749798"} {:id "defn-/transform-living-battery", :kind "defn-", :line 1229, :end-line 1231, :hash "-703267492"} {:id "defn-/map-living-batteries", :kind "defn-", :line 1233, :end-line 1237, :hash "493344184"} {:id "defn/rearm-surviving-batteries", :kind "defn", :line 1239, :end-line 1245, :hash "-41122009"} {:id "defn-/non-destroyed-batteries", :kind "defn-", :line 1247, :end-line 1249, :hash "-2142188193"} {:id "defn-/wave-target-pool", :kind "defn-", :line 1251, :end-line 1255, :hash "1662828947"} {:id "defn-/spawn-wave-enemy", :kind "defn-", :line 1257, :end-line 1264, :hash "833630058"} {:id "defn/spawn-wave-enemy-targeting-battery", :kind "defn", :line 1266, :end-line 1271, :hash "-458735756"} {:id "defn/set-wave-enemies-active", :kind "defn", :line 1273, :end-line 1288, :hash "-1117220171"} {:id "defn/activate-wave-schedule", :kind "defn", :line 1290, :end-line 1305, :hash "249748825"} {:id "defn/set-non-destroyed-battery-ammo", :kind "defn", :line 1307, :end-line 1310, :hash "479283075"} {:id "def/wave-schedule-metrics", :kind "def", :line 1312, :end-line 1312, :hash "-426249483"} {:id "def/wave-schedule-metrics-for", :kind "def", :line 1313, :end-line 1313, :hash "-2119268773"} {:id "def/wave-mirv-count", :kind "def", :line 1315, :end-line 1315, :hash "746006295"} {:id "def/wave-smart-bomb-count", :kind "def", :line 1316, :end-line 1316, :hash "-664512608"} {:id "def/wave-bomber-count", :kind "def", :line 1317, :end-line 1317, :hash "1023037642"} {:id "def/wave-satellite-count", :kind "def", :line 1318, :end-line 1318, :hash "-1625262900"} {:id "def/harder-wave?", :kind "def", :line 1319, :end-line 1319, :hash "-498526476"} {:id "defn/set-wave", :kind "defn", :line 1320, :end-line 1328, :hash "1048551934"} {:id "defn-/clear-combat-entities", :kind "defn-", :line 1330, :end-line 1337, :hash "1598105619"} {:id "defn/start-next-wave", :kind "defn", :line 1339, :end-line 1348, :hash "1475864965"} {:id "defn-/advance-clock", :kind "defn-", :line 1350, :end-line 1354, :hash "2082435033"} {:id "defn/tick", :kind "defn", :line 1356, :end-line 1396, :hash "-2046658823"}]}
;; clj-mutate-manifest-end

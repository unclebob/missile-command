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


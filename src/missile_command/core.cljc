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

(defn- handle-click
  [state x y]
  (cond
    (title? state) (no-events (start-game state))
    (the-end? state) (no-events (confirm-end-screen state))
    (paused? state) (no-events state)
    (high-score-entry? state) (no-events state)
    (high-scores-view? state) (no-events state)
    (options? state) (no-events state)
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

;; Defaults for scheduled advanced enemies (arcade-style mid-descent split).
(def default-mirv-child-count 3)
(def default-mirv-split-progress 0.5)
(def default-flyer-speed 100.0)
(def default-flyer-altitude-fraction 0.18)
(def default-flyer-drop-count 3)
;; First drop progress; later drops are staggered evenly toward this upper bound.
(def default-flyer-drop-progress-start 0.25)
(def default-flyer-drop-progress-end 0.75)

(defn- living-city-ids
  [state]
  (mapv :id (living-cities state)))

(defn- cycle-living-city-ids
  "Take n living city ids, cycling when fewer cities than n."
  [state n]
  (let [ids (living-city-ids state)]
    (if (seq ids)
      (vec (take (long n) (cycle ids)))
      [])))

(defn- spawn-wave-mirvs
  "Spawn n MIRV parents toward living cities from distributed sky origins."
  [state n]
  (let [city-ids (cycle-living-city-ids state n)
        width (playfield-width state)
        total (count city-ids)]
    (reduce (fn [s [i city-id]]
              (let [c (city s city-id)]
                (if c
                  (spawn-enemy-at s
                                  {:x (waves/sky-origin-x width i total) :y 0}
                                  {:x (:x c) :y (:y c)}
                                  :city city-id
                                  {:enemy-kind enemy-kind-mirv
                                   :child-count default-mirv-child-count
                                   :split-progress default-mirv-split-progress})
                  s)))
            state
            (map-indexed vector city-ids))))

(defn- spawn-wave-smart-bombs
  "Spawn n smart bombs toward living cities."
  [state n]
  (reduce (fn [s city-id]
            (spawn-smart-bomb-targeting-city s city-id))
          state
          (cycle-living-city-ids state n)))

(defn- flyer-drop-progresses
  "Stagger n drop times between start and end so bombs fall one after another."
  [n]
  (let [n (long n)
        lo default-flyer-drop-progress-start
        hi default-flyer-drop-progress-end]
    (if (<= n 1)
      [lo]
      (mapv (fn [i]
              (+ lo (* (- hi lo) (/ (double i) (double (dec n))))))
            (range n)))))

(defn- configure-last-flyer-drops
  "Attach staggered drop schedule to the most recently spawned flyer."
  [state drop-count]
  (update state :flyers
          (fn [fs]
            (if (seq fs)
              (let [fs (vec fs)
                    idx (dec (count fs))
                    ids (living-city-ids state)
                    n (long drop-count)
                    targets (if (seq ids)
                              (vec (take n (cycle ids)))
                              [])
                    progresses (flyer-drop-progresses (count targets))
                    drops (mapv (fn [j city-id at]
                                  {:id j
                                   :at-progress (double at)
                                   :target [:city city-id]})
                                (range (count targets))
                                targets
                                progresses)]
                (assoc fs idx (assoc (nth fs idx)
                                     :drops drops
                                     :drops-fired #{})))
              (vec fs)))))

(defn- spawn-wave-flyer
  "Spawn one bomber or satellite crossing the upper sky with city-bound drops."
  [state flyer-kind]
  (let [w (double (playfield-width state))
        h (double (playfield-height state))
        y (* h default-flyer-altitude-fraction)]
    (-> state
        (spawn-flyer flyer-kind 0.0 y w y default-flyer-speed)
        (configure-last-flyer-drops default-flyer-drop-count))))

(defn activate-wave-schedule
  "Spawn the full current-wave schedule: ballistics, MIRVs, smart bombs, flyers.
  Hosts call this when the sky is empty and play should continue."
  [state]
  (let [m (waves/schedule-metrics-for-state state (wave state))
        ballistic (long (:enemy-count m 0))
        mirvs (long (:mirv-count m 0))
        smarts (long (:smart-bomb-count m 0))
        bombers (long (:bomber-count m 0))
        sats (long (:satellite-count m 0))]
    (cond-> (-> state
                (assoc :flyers [])
                (set-wave-enemies-active ballistic))
      (pos? mirvs) (spawn-wave-mirvs mirvs)
      (pos? smarts) (spawn-wave-smart-bombs smarts)
      (pos? bombers) (spawn-wave-flyer :bomber)
      (pos? sats) (spawn-wave-flyer :satellite))))

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

(defn- maybe-title-warning
  "Play warning once each time the title screen is entered."
  [state]
  (if (title? state)
    (if (:title-warning-played? state)
      state
      (-> state
          (assoc :title-warning-played? true)
          (sfx/emit :sfx/warning)))
    (dissoc state :title-warning-played?)))

(defn tick
  "Advance simulation by dt seconds (clamped). Returns {:state s :events [...]}.
  Playing runs combat; wave-banner animates then resumes; THE END expands the
  end fireball; paused freezes; other shells advance the clock only."
  [state dt]
  (let [applied (missiles/clamp-dt dt)
        wrap (fn [s] {:state (maybe-title-warning s) :events []})]
    (cond
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

      (paused? state)
      (wrap (assoc state :last-applied-dt 0.0))

      :else
      ;; title, high-score-entry, high-scores view, options
      (wrap (advance-clock state applied)))))

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-07-24T16:27:07.038804-05:00", :module-hash "-720837596", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 16, :hash "1255117971"} {:id "def/initial-score", :kind "def", :line 17, :end-line 17, :hash "485183318"} {:id "def/initial-entity-id", :kind "def", :line 18, :end-line 18, :hash "-2006662704"} {:id "def/initial-bonus-cities", :kind "def", :line 19, :end-line 19, :hash "774282307"} {:id "def/initial-bonus-cities-awarded", :kind "def", :line 20, :end-line 20, :hash "-135107397"} {:id "def/initial-bonus-city-earned-events", :kind "def", :line 21, :end-line 21, :hash "-446479005"} {:id "def/wave-flag-off", :kind "def", :line 22, :end-line 22, :hash "1734145513"} {:id "def/wave-flag-on", :kind "def", :line 23, :end-line 23, :hash "1660732832"} {:id "def/wave-starts-complete?", :kind "def", :line 24, :end-line 24, :hash "1259204240"} {:id "def/wave-starts-with-enemies?", :kind "def", :line 25, :end-line 25, :hash "929188796"} {:id "def/clamp-lo", :kind "def", :line 26, :end-line 26, :hash "-224595111"} {:id "def/default-crosshair", :kind "def", :line 27, :end-line 27, :hash "-249046571"} {:id "def/target-starts-destroyed?", :kind "def", :line 28, :end-line 28, :hash "224311611"} {:id "def/screen-title", :kind "def", :line 29, :end-line 29, :hash "1092741116"} {:id "def/screen-playing", :kind "def", :line 30, :end-line 30, :hash "1649702326"} {:id "def/screen-paused", :kind "def", :line 31, :end-line 31, :hash "-1395420898"} {:id "def/screen-the-end", :kind "def", :line 32, :end-line 32, :hash "184874292"} {:id "def/end-message-text", :kind "def", :line 33, :end-line 33, :hash "-1724984215"} {:id "def/title-game-name", :kind "def", :line 34, :end-line 34, :hash "-1224113475"} {:id "def/title-start-affordance", :kind "def", :line 35, :end-line 35, :hash "1586942227"} {:id "def/end-fireball-expand-seconds", :kind "def", :line 36, :end-line 36, :hash "1938981448"} {:id "def/end-fireball-contract-seconds", :kind "def", :line 37, :end-line 37, :hash "-273647692"} {:id "defn-/clamp", :kind "defn-", :line 39, :end-line 41, :hash "1680610514"} {:id "defn-/clamp-point", :kind "defn-", :line 43, :end-line 46, :hash "-1550073030"} {:id "defn-/center-crosshair", :kind "defn-", :line 48, :end-line 50, :hash "502710703"} {:id "defn-/reclamp-crosshair", :kind "defn-", :line 52, :end-line 55, :hash "-495207193"} {:id "defn-/update-battery", :kind "defn-", :line 57, :end-line 59, :hash "-630735181"} {:id "defn-/next-entity-id", :kind "defn-", :line 61, :end-line 64, :hash "-611923035"} {:id "defn-/no-events", :kind "defn-", :line 66, :end-line 68, :hash "652168329"} {:id "def/sfx-events", :kind "def", :line 70, :end-line 70, :hash "525363444"} {:id "def/sfx-emitted?", :kind "def", :line 71, :end-line 71, :hash "-1793854788"} {:id "defn/new-game", :kind "defn", :line 73, :end-line 109, :hash "1699967064"} {:id "defn/resize", :kind "defn", :line 111, :end-line 118, :hash "-1344415357"} {:id "defn/playfield-width", :kind "defn", :line 120, :end-line 122, :hash "-1043537513"} {:id "defn/playfield-height", :kind "defn", :line 124, :end-line 126, :hash "344252362"} {:id "defn/cities", :kind "defn", :line 128, :end-line 130, :hash "1240083502"} {:id "defn/living-cities", :kind "defn", :line 132, :end-line 134, :hash "416648848"} {:id "defn/batteries", :kind "defn", :line 136, :end-line 138, :hash "206298614"} {:id "defn/battery", :kind "defn", :line 140, :end-line 142, :hash "-1555624967"} {:id "defn/on-ground?", :kind "defn", :line 144, :end-line 147, :hash "1609612027"} {:id "defn/city-on-ground?", :kind "defn", :line 149, :end-line 151, :hash "-1878088970"} {:id "defn/crosshair", :kind "defn", :line 153, :end-line 155, :hash "-2027795649"} {:id "defn/score", :kind "defn", :line 157, :end-line 159, :hash "-1700557235"} {:id "defn/wave", :kind "defn", :line 161, :end-line 163, :hash "1109090166"} {:id "defn/multiplier", :kind "defn", :line 165, :end-line 168, :hash "-1249467982"} {:id "defn-/long-state", :kind "defn-", :line 170, :end-line 172, :hash "-1384016045"} {:id "defn/bonus-cities", :kind "defn", :line 174, :end-line 177, :hash "1351687248"} {:id "defn/bonus-city-threshold", :kind "defn", :line 179, :end-line 181, :hash "1726572985"} {:id "defn/bonus-city-earned-events", :kind "defn", :line 183, :end-line 186, :hash "-2024093832"} {:id "defn/wave-complete?", :kind "defn", :line 188, :end-line 190, :hash "-334236383"} {:id "def/screen", :kind "def", :line 192, :end-line 192, :hash "681051221"} {:id "def/title?", :kind "def", :line 193, :end-line 193, :hash "-413853504"} {:id "def/playing?", :kind "def", :line 194, :end-line 194, :hash "410765386"} {:id "def/paused?", :kind "def", :line 195, :end-line 195, :hash "665268154"} {:id "def/the-end?", :kind "def", :line 196, :end-line 196, :hash "486194475"} {:id "def/title-game-name-of", :kind "def", :line 197, :end-line 197, :hash "1444622798"} {:id "def/title-shows-start-affordance?", :kind "def", :line 198, :end-line 198, :hash "763145159"} {:id "def/high-score-table", :kind "def", :line 200, :end-line 200, :hash "1163853287"} {:id "def/high-score-capacity", :kind "def", :line 201, :end-line 201, :hash "-1128697050"} {:id "def/pending-high-score", :kind "def", :line 202, :end-line 202, :hash "2019144034"} {:id "def/submitted-high-score-initials", :kind "def", :line 203, :end-line 203, :hash "1796003601"} {:id "def/high-score-entry?", :kind "def", :line 204, :end-line 204, :hash "985452078"} {:id "def/high-scores-view?", :kind "def", :line 205, :end-line 205, :hash "336415907"} {:id "def/set-high-score-capacity", :kind "def", :line 206, :end-line 206, :hash "1210250449"} {:id "def/add-high-score-entry", :kind "def", :line 207, :end-line 207, :hash "785373078"} {:id "def/open-high-scores", :kind "def", :line 208, :end-line 208, :hash "-1768638500"} {:id "def/close-high-scores", :kind "def", :line 209, :end-line 209, :hash "-800419625"} {:id "defn/pause-game", :kind "defn", :line 211, :end-line 216, :hash "1793185663"} {:id "defn/resume-game", :kind "defn", :line 218, :end-line 223, :hash "1617711604"} {:id "defn-/blank-shell", :kind "defn-", :line 225, :end-line 229, :hash "533787309"} {:id "def/game-options", :kind "def", :line 231, :end-line 231, :hash "1685212516"} {:id "def/mute?", :kind "def", :line 232, :end-line 232, :hash "573004342"} {:id "def/difficulty", :kind "def", :line 233, :end-line 233, :hash "247818244"} {:id "def/options?", :kind "def", :line 234, :end-line 234, :hash "857221898"} {:id "def/open-options", :kind "def", :line 235, :end-line 235, :hash "-1773224624"} {:id "def/leave-options", :kind "def", :line 236, :end-line 236, :hash "-1553054960"} {:id "def/set-mute", :kind "def", :line 237, :end-line 237, :hash "961239161"} {:id "def/set-difficulty", :kind "def", :line 238, :end-line 238, :hash "-1413206900"} {:id "def/bind-fire-key", :kind "def", :line 239, :end-line 239, :hash "-86622152"} {:id "def/fire-key-includes?", :kind "def", :line 240, :end-line 240, :hash "109857506"} {:id "def/pause-key-includes?", :kind "def", :line 241, :end-line 241, :hash "-1954048295"} {:id "def/wave-banner?", :kind "def", :line 242, :end-line 242, :hash "1130113897"} {:id "def/wave-banner", :kind "def", :line 243, :end-line 243, :hash "911252070"} {:id "def/wave-banner-text", :kind "def", :line 244, :end-line 244, :hash "-1812837808"} {:id "def/wave-banner-announced-wave", :kind "def", :line 245, :end-line 245, :hash "549833261"} {:id "def/wave-banner-phase", :kind "def", :line 246, :end-line 246, :hash "-1207534753"} {:id "def/wave-banner-text-position", :kind "def", :line 247, :end-line 247, :hash "1533588800"} {:id "def/wave-banner-distance-to-center", :kind "def", :line 248, :end-line 248, :hash "368380193"} {:id "defn/export-settings", :kind "defn", :line 250, :end-line 255, :hash "-1585521700"} {:id "defn/import-settings", :kind "defn", :line 257, :end-line 266, :hash "221337773"} {:id "defn-/apply-shell", :kind "defn-", :line 269, :end-line 272, :hash "-1543202908"} {:id "defn/start-game", :kind "defn", :line 273, :end-line 277, :hash "321312616"} {:id "defn/final-score", :kind "defn", :line 279, :end-line 282, :hash "-2124677376"} {:id "defn/confirm-end-screen", :kind "defn", :line 284, :end-line 292, :hash "-1073333732"} {:id "defn/submit-high-score-initials", :kind "defn", :line 294, :end-line 303, :hash "-243607349"} {:id "defn/end-message", :kind "defn", :line 305, :end-line 307, :hash "1667840292"} {:id "defn/end-fireball", :kind "defn", :line 309, :end-line 311, :hash "603568745"} {:id "defn/hud", :kind "defn", :line 313, :end-line 317, :hash "-1986360486"} {:id "defn/defensive-missiles", :kind "defn", :line 319, :end-line 321, :hash "-1457861839"} {:id "defn/fireballs", :kind "defn", :line 323, :end-line 325, :hash "-47675919"} {:id "defn/enemy-missiles", :kind "defn", :line 327, :end-line 329, :hash "-1649887754"} {:id "defn/flyers", :kind "defn", :line 331, :end-line 333, :hash "-195685942"} {:id "defn/destroyable-targets", :kind "defn", :line 335, :end-line 337, :hash "-2146081921"} {:id "defn/last-enemy-fate", :kind "defn", :line 339, :end-line 341, :hash "-1164295963"} {:id "defn/city", :kind "defn", :line 343, :end-line 345, :hash "417115868"} {:id "defn/living-city?", :kind "defn", :line 347, :end-line 349, :hash "808122796"} {:id "defn/sim-time", :kind "defn", :line 351, :end-line 353, :hash "1526499425"} {:id "defn/last-applied-dt", :kind "defn", :line 355, :end-line 357, :hash "-1011651673"} {:id "defn/max-fireball-radius", :kind "defn", :line 359, :end-line 361, :hash "421742428"} {:id "defn/set-battery-ammo", :kind "defn", :line 363, :end-line 366, :hash "975933252"} {:id "defn/destroy-battery", :kind "defn", :line 368, :end-line 374, :hash "1155956327"} {:id "defn/add-destroyable-target", :kind "defn", :line 376, :end-line 381, :hash "-1701043486"} {:id "defn-/update-city", :kind "defn-", :line 383, :end-line 385, :hash "-2016100813"} {:id "defn/destroy-city", :kind "defn", :line 387, :end-line 392, :hash "-1181664448"} {:id "defn-/enemy-speed-for-state", :kind "defn-", :line 394, :end-line 397, :hash "104260835"} {:id "def/enemy-kind-ballistic", :kind "def", :line 399, :end-line 399, :hash "844796837"} {:id "def/enemy-kind-mirv", :kind "def", :line 400, :end-line 400, :hash "-903061239"} {:id "def/enemy-kind-mirv-child", :kind "def", :line 401, :end-line 401, :hash "53130372"} {:id "def/enemy-kind-smart", :kind "def", :line 402, :end-line 402, :hash "246438905"} {:id "def/smart-bomb-edge-inner-factor", :kind "def", :line 405, :end-line 405, :hash "-1342792927"} {:id "def/smart-not-yet-evaded", :kind "def", :line 406, :end-line 406, :hash "101087391"} {:id "def/smart-bomb-evade-clearance", :kind "def", :line 407, :end-line 407, :hash "-755204528"} {:id "defn-/mirv-parent?", :kind "defn-", :line 409, :end-line 411, :hash "763145358"} {:id "defn-/mirv-child?", :kind "defn-", :line 413, :end-line 415, :hash "519586696"} {:id "defn-/smart-bomb?", :kind "defn-", :line 417, :end-line 419, :hash "-490539965"} {:id "defn/mirv-parents", :kind "defn", :line 421, :end-line 423, :hash "-1734114206"} {:id "defn/mirv-children", :kind "defn", :line 425, :end-line 427, :hash "1045363527"} {:id "defn/smart-bombs", :kind "defn", :line 429, :end-line 431, :hash "1657617417"} {:id "defn/spawn-enemy-at", :kind "defn", :line 433, :end-line 448, :hash "-2030994445"} {:id "defn/spawn-enemy-targeting-city-from", :kind "defn", :line 450, :end-line 459, :hash "297620600"} {:id "defn/spawn-enemy-targeting-city", :kind "defn", :line 461, :end-line 466, :hash "1574498502"} {:id "defn/spawn-enemy-targeting-battery-from", :kind "defn", :line 468, :end-line 477, :hash "-765503325"} {:id "defn/spawn-enemy-targeting-battery", :kind "defn", :line 479, :end-line 484, :hash "1391767096"} {:id "defn/spawn-enemies-targeting-distinct-cities", :kind "defn", :line 486, :end-line 490, :hash "314430177"} {:id "defn/spawn-mirv-targeting-city", :kind "defn", :line 492, :end-line 504, :hash "-266412817"} {:id "defn/spawn-smart-bomb-targeting-city", :kind "defn", :line 506, :end-line 517, :hash "-976059765"} {:id "defn/spawn-flyer", :kind "defn", :line 519, :end-line 527, :hash "2100696208"} {:id "defn/set-flyer-drops", :kind "defn", :line 529, :end-line 536, :hash "-2098771675"} {:id "defn/set-flyer-drops-toward-living-cities", :kind "defn", :line 538, :end-line 551, :hash "-1546667140"} {:id "defn/set-flyer-drop-targeting-city", :kind "defn", :line 553, :end-line 559, :hash "-362325883"} {:id "defn/flyers-of-kind", :kind "defn", :line 561, :end-line 563, :hash "2117257319"} {:id "defn/add-static-fireball", :kind "defn", :line 565, :end-line 570, :hash "2053229248"} {:id "defn-/enemy-attrs-to-preserve", :kind "defn-", :line 572, :end-line 575, :hash "-111533279"} {:id "defn-/retarget-enemy-from", :kind "defn-", :line 577, :end-line 586, :hash "-550286216"} {:id "defn-/first-enemy-index", :kind "defn-", :line 588, :end-line 590, :hash "-1572668638"} {:id "defn-/retarget-enemy-at-index", :kind "defn-", :line 592, :end-line 594, :hash "1058188477"} {:id "defn/route-first-smart-bomb-through-point", :kind "defn", :line 596, :end-line 603, :hash "-155725677"} {:id "defn/route-smart-bomb-centered-in-fireball", :kind "defn", :line 605, :end-line 608, :hash "-541649650"} {:id "defn/route-smart-bomb-edge-band-in-fireball", :kind "defn", :line 610, :end-line 617, :hash "1354861353"} {:id "defn/route-flyer-through-point", :kind "defn", :line 619, :end-line 633, :hash "-723558539"} {:id "defn/route-enemy-through-point", :kind "defn", :line 635, :end-line 642, :hash "1758214460"} {:id "defn-/first-mirv-child-index", :kind "defn-", :line 644, :end-line 646, :hash "-357091384"} {:id "defn/route-first-mirv-child-through-point", :kind "defn", :line 648, :end-line 655, :hash "1808906776"} {:id "defn-/impact-target", :kind "defn-", :line 657, :end-line 662, :hash "-984684299"} {:id "defn-/enemy-hit-by-fireball?", :kind "defn-", :line 664, :end-line 666, :hash "-387864824"} {:id "defn-/distance-to-fireball", :kind "defn-", :line 668, :end-line 672, :hash "-1214701948"} {:id "defn-/first-touching-fireball", :kind "defn-", :line 674, :end-line 676, :hash "717444682"} {:id "defn-/smart-bomb-edge-band?", :kind "defn-", :line 678, :end-line 682, :hash "885202764"} {:id "defn-/evade-smart-bomb", :kind "defn-", :line 684, :end-line 708, :hash "1355113499"} {:id "defn-/fire-battery", :kind "defn-", :line 710, :end-line 726, :hash "1591374319"} {:id "defn-/aim", :kind "defn-", :line 728, :end-line 734, :hash "242968114"} {:id "defn/click-zone", :kind "defn", :line 736, :end-line 739, :hash "943238228"} {:id "defn/click-fallback-order", :kind "defn", :line 741, :end-line 744, :hash "-1791151582"} {:id "defn-/click-fire", :kind "defn-", :line 746, :end-line 756, :hash "1088598870"} {:id "defn-/handle-click", :kind "defn-", :line 758, :end-line 764, :hash "-938668576"} {:id "defn-/unsupported-command", :kind "defn-", :line 766, :end-line 769, :hash "585518571"} {:id "def/command-handlers", :kind "def", :line 771, :end-line 792, :hash "-2121453946"} {:id "defn/handle", :kind "defn", :line 794, :end-line 799, :hash "1696801717"} {:id "defn/press-key", :kind "defn", :line 800, :end-line 803, :hash "1450190204"} {:id "defn-/spawn-fireball-at", :kind "defn-", :line 805, :end-line 810, :hash "-1922366979"} {:id "defn-/spawn-fireball-from-missile", :kind "defn-", :line 812, :end-line 814, :hash "1839324960"} {:id "defn-/tick-defensive-missiles", :kind "defn-", :line 816, :end-line 824, :hash "465906604"} {:id "defn-/tick-fireballs", :kind "defn-", :line 826, :end-line 834, :hash "-1794535937"} {:id "defn-/target-hit-by-fireball?", :kind "defn-", :line 836, :end-line 838, :hash "1356179508"} {:id "defn-/destroy-targets-in-fireballs", :kind "defn-", :line 840, :end-line 850, :hash "-1920073096"} {:id "defn-/assoc-long", :kind "defn-", :line 852, :end-line 854, :hash "-1607523900"} {:id "defn/set-bonus-city-threshold", :kind "defn", :line 856, :end-line 859, :hash "633568299"} {:id "defn/set-bonus-city-reserve", :kind "defn", :line 861, :end-line 864, :hash "301818582"} {:id "defn-/lowest-destroyed-city-id", :kind "defn-", :line 866, :end-line 872, :hash "-2041789500"} {:id "defn/apply-bonus-cities-from-reserve", :kind "defn", :line 874, :end-line 885, :hash "927622616"} {:id "defn-/make-end-fireball", :kind "defn-", :line 887, :end-line 893, :hash "-918471848"} {:id "defn-/update-end-message-reveal", :kind "defn-", :line 895, :end-line 898, :hash "1654790648"} {:id "defn-/enter-the-end", :kind "defn-", :line 900, :end-line 913, :hash "-68727569"} {:id "defn/evaluate-game-over", :kind "defn", :line 915, :end-line 924, :hash "-1819788070"} {:id "defn/end-fireball-centered?", :kind "defn", :line 926, :end-line 930, :hash "-1673190026"} {:id "defn/end-fireball-fills-playfield?", :kind "defn", :line 932, :end-line 934, :hash "496208359"} {:id "defn/end-message-layout", :kind "defn", :line 936, :end-line 939, :hash "-2147217889"} {:id "defn/end-message-fills-max-expanse?", :kind "defn", :line 941, :end-line 943, :hash "1595237781"} {:id "defn/end-message-centered?", :kind "defn", :line 945, :end-line 949, :hash "-447969280"} {:id "defn/end-message-visibility-clipped?", :kind "defn", :line 951, :end-line 954, :hash "-1616346772"} {:id "defn/end-message-point-visible?", :kind "defn", :line 956, :end-line 959, :hash "-703026949"} {:id "defn/end-message-reveal", :kind "defn", :line 961, :end-line 964, :hash "-172598567"} {:id "defn-/tick-end-fireball", :kind "defn-", :line 966, :end-line 976, :hash "473583625"} {:id "defn-/sync-bonus-cities-from-score", :kind "defn-", :line 978, :end-line 993, :hash "1147675266"} {:id "defn-/add-score", :kind "defn-", :line 995, :end-line 999, :hash "-269277404"} {:id "defn/set-score", :kind "defn", :line 1001, :end-line 1006, :hash "2036835626"} {:id "defn-/destroy-enemy-by-fireball", :kind "defn-", :line 1008, :end-line 1015, :hash "1144411196"} {:id "defn-/spawn-impact-fireball", :kind "defn-", :line 1017, :end-line 1020, :hash "-2084493934"} {:id "defn-/resolve-enemy-impact", :kind "defn-", :line 1022, :end-line 1027, :hash "1944987463"} {:id "defn-/keep-flying-enemy", :kind "defn-", :line 1029, :end-line 1031, :hash "-1439807545"} {:id "defn-/resolve-fireball-contact", :kind "defn-", :line 1033, :end-line 1043, :hash "632440842"} {:id "defn-/progress-of", :kind "defn-", :line 1045, :end-line 1049, :hash "1779378488"} {:id "defn-/index-of-id", :kind "defn-", :line 1051, :end-line 1054, :hash "-934326406"} {:id "defn-/mirv-child-target-ids", :kind "defn-", :line 1056, :end-line 1064, :hash "-194964239"} {:id "defn-/split-mirv-parent", :kind "defn-", :line 1066, :end-line 1081, :hash "1014154875"} {:id "defn-/should-split-mirv?", :kind "defn-", :line 1083, :end-line 1087, :hash "-1686921459"} {:id "defn-/resolve-advanced-enemy", :kind "defn-", :line 1089, :end-line 1103, :hash "1088768601"} {:id "defn-/tick-one-enemy", :kind "defn-", :line 1105, :end-line 1111, :hash "2100624266"} {:id "defn-/tick-enemy-missiles", :kind "defn-", :line 1113, :end-line 1119, :hash "-1658169989"} {:id "defn-/destroy-flyer-by-fireball", :kind "defn-", :line 1121, :end-line 1126, :hash "1452981331"} {:id "defn-/apply-flyer-drops", :kind "defn-", :line 1128, :end-line 1146, :hash "1084438316"} {:id "defn-/keep-flying-flyer", :kind "defn-", :line 1148, :end-line 1150, :hash "1225384403"} {:id "defn-/tick-one-flyer", :kind "defn-", :line 1152, :end-line 1166, :hash "1709281062"} {:id "defn-/tick-flyers", :kind "defn-", :line 1168, :end-line 1174, :hash "73639010"} {:id "defn-/wave-ready-to-complete?", :kind "defn-", :line 1176, :end-line 1182, :hash "1949930133"} {:id "defn-/unused-defensive-missiles", :kind "defn-", :line 1184, :end-line 1190, :hash "52651543"} {:id "defn-/award-wave-end-bonuses", :kind "defn-", :line 1192, :end-line 1199, :hash "-1067549351"} {:id "defn-/maybe-complete-wave", :kind "defn-", :line 1201, :end-line 1213, :hash "761711827"} {:id "defn-/transform-living-battery", :kind "defn-", :line 1216, :end-line 1218, :hash "-703267492"} {:id "defn-/map-living-batteries", :kind "defn-", :line 1220, :end-line 1224, :hash "493344184"} {:id "defn/rearm-surviving-batteries", :kind "defn", :line 1226, :end-line 1229, :hash "297237121"} {:id "defn-/non-destroyed-batteries", :kind "defn-", :line 1231, :end-line 1233, :hash "-2142188193"} {:id "defn-/wave-target-pool", :kind "defn-", :line 1235, :end-line 1239, :hash "1662828947"} {:id "defn-/spawn-wave-enemy", :kind "defn-", :line 1241, :end-line 1248, :hash "833630058"} {:id "defn/spawn-wave-enemy-targeting-battery", :kind "defn", :line 1250, :end-line 1255, :hash "-458735756"} {:id "defn/set-wave-enemies-active", :kind "defn", :line 1257, :end-line 1272, :hash "-1117220171"} {:id "defn/set-non-destroyed-battery-ammo", :kind "defn", :line 1274, :end-line 1277, :hash "479283075"} {:id "def/wave-schedule-metrics", :kind "def", :line 1279, :end-line 1279, :hash "-426249483"} {:id "def/wave-schedule-metrics-for", :kind "def", :line 1280, :end-line 1280, :hash "-2119268773"} {:id "def/wave-mirv-count", :kind "def", :line 1282, :end-line 1282, :hash "746006295"} {:id "def/wave-smart-bomb-count", :kind "def", :line 1283, :end-line 1283, :hash "-664512608"} {:id "def/wave-bomber-count", :kind "def", :line 1284, :end-line 1284, :hash "1023037642"} {:id "def/wave-satellite-count", :kind "def", :line 1285, :end-line 1285, :hash "-1625262900"} {:id "def/harder-wave?", :kind "def", :line 1286, :end-line 1286, :hash "-498526476"} {:id "defn/set-wave", :kind "defn", :line 1287, :end-line 1295, :hash "1048551934"} {:id "defn/start-next-wave", :kind "defn", :line 1297, :end-line 1304, :hash "1758294105"} {:id "defn-/advance-clock", :kind "defn-", :line 1306, :end-line 1310, :hash "2082435033"} {:id "defn/tick", :kind "defn", :line 1312, :end-line 1349, :hash "557869910"}]}
;; clj-mutate-manifest-end

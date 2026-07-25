(ns missile-command.core
  (:require [missile-command.batteries :as batteries]
            [missile-command.bonus-cities :as bc]
            [missile-command.cities :as cities]
            [missile-command.combat :as combat]
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
            [missile-command.wave-lifecycle :as wave-lifecycle]
            [missile-command.wave-schedule :as wave-schedule]
            [missile-command.waves :as waves]
            [missile-command.world :as world]))
(def initial-score 0)
(def initial-entity-id 0)
(def initial-bonus-cities bc/initial-reserve)
(def initial-bonus-cities-awarded bc/initial-awarded)
(def initial-bonus-city-earned-events bc/initial-earned-events)
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
(def sfx-take-new sfx/take-new)
(def sfx-truncate-to sfx/truncate-to)
(def sfx-drain sfx/drain)
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

(def bonus-cities
  "Bonus cities held in reserve (not yet placed on the playfield)."
  bc/reserve)

(def bonus-city-threshold bc/threshold)

(def bonus-city-earned-events
  "How many threshold-crossing awards have been recorded this run."
  bc/earned-events)

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
(def wave-banner-subtitle wave-banner/subtitle)
(def wave-banner-bonus-city? wave-banner/bonus-city?)
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
                    {:x (waves/random-sky-origin-x (playfield-width state)) :y 0}
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
  "Allocate and attach an expanding fireball centered at x,y."
  [state x y]
  (combat/spawn-fireball-at state x y))

(defn- assoc-long
  [state k v]
  (assoc state k (long v)))

(def set-bonus-city-threshold bc/set-threshold)
(def set-bonus-city-reserve bc/set-reserve)
(def apply-bonus-cities-from-reserve bc/apply-from-reserve)

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

(defn- add-score
  [state points]
  (-> state
      (update :score (fnil + initial-score) (long points))
      bc/sync-from-score))

(defn set-score
  "Test/setup helper: set absolute score and process bonus city thresholds."
  [state score-value]
  (-> state
      (assoc :score (long score-value))
      bc/sync-from-score))

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

(defn- maybe-complete-wave
  "When the last attack of the wave is cleared, mark complete and show banner.
  Banner includes Bonus City when a city was restored from reserve this wave."
  [state]
  (wave-lifecycle/complete-wave
   state
   {:apply-bonus-fn apply-bonus-cities-from-reserve
    :add-score-fn add-score
    :living-city-count (count (living-cities state))
    :multiplier (multiplier state)
    :wave-flag-on wave-flag-on
    :wave-starts-with-enemies? wave-starts-with-enemies?}))


(defn rearm-surviving-batteries
  "Restore every battery: clear destroyed and refill to full ammo.
  (Name kept for call-site compatibility; destroyed bases come back each wave.)"
  [state]
  (wave-lifecycle/rearm-all-batteries state))

(defn spawn-wave-enemy-targeting-battery
  "Spawn a single wave-style enemy aimed at a battery from a sky origin."
  [state battery-id]
  (spawn-enemy-targeting-battery-from
   state
   (waves/sky-origin-x (playfield-width state) 0 1)
   0
   battery-id))

(defn set-wave-enemies-active
  "Replace in-flight enemies with n scheduled wave enemies.
  Targets cycle living cities and non-destroyed batteries."
  [state n]
  (wave-schedule/set-enemies-active
   state n
   {:living-cities living-cities
    :batteries-living (fn [s] (batteries/living (batteries s)))
    :playfield-width playfield-width
    :spawn-city-from spawn-enemy-targeting-city-from
    :spawn-battery-from spawn-enemy-targeting-battery-from
    :wave-starts-complete? wave-starts-complete?}))

;; Defaults re-exported for unit tests / hosts.
(def default-mirv-child-count wave-schedule/default-mirv-child-count)
(def default-mirv-split-progress wave-schedule/default-mirv-split-progress)
(def default-flyer-speed wave-schedule/default-flyer-speed)
(def default-flyer-altitude-fraction wave-schedule/default-flyer-altitude-fraction)
(def default-flyer-drop-count wave-schedule/default-flyer-drop-count)
(def default-flyer-drop-progress-start wave-schedule/default-flyer-drop-progress-start)
(def default-flyer-drop-progress-end wave-schedule/default-flyer-drop-progress-end)

(defn- wave-schedule-hooks
  []
  {:wave wave
   :living-cities living-cities
   :city city
   :playfield-width playfield-width
   :playfield-height playfield-height
   :set-wave-enemies-active set-wave-enemies-active
   :spawn-enemy-at spawn-enemy-at
   :spawn-smart-bomb-targeting-city spawn-smart-bomb-targeting-city
   :spawn-flyer spawn-flyer
   :enemy-kind-mirv enemy-kind-mirv})

(defn begin-wave-attack
  "Begin attack k (1-based): a full salvo of ballistics; specials on the last."
  [state k]
  (wave-schedule/begin-attack state k (wave-schedule-hooks)))

(def start-wave-attack begin-wave-attack)

(defn activate-wave-schedule
  "Start attack 1 of the current wave (a 3-missile salvo). Attacks 2 and 3
  begin only after the previous attack is fully cleared."
  [state]
  (begin-wave-attack state 1))

(defn- maybe-advance-wave-attack
  "When the current attack is cleared and more remain, start the next attack."
  [state]
  (wave-schedule/maybe-advance-attack state begin-wave-attack))

(defn- ensure-wave-attack-started
  "Start attack 1 when sky is clear, no attack is active, and wave incomplete."
  [state]
  (wave-schedule/ensure-attack-started state activate-wave-schedule))

(defn set-non-destroyed-battery-ammo
  "Test helper: set ammo on every non-destroyed battery."
  [state ammo]
  (update state :batteries #(batteries/set-living-ammo % ammo)))

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
  (wave-lifecycle/set-wave state wave-number
                           wave-starts-complete?
                           wave-starts-with-enemies?))

(defn start-next-wave
  "Begin the next wave: restore and rearm all batteries; clear leftover
  fireballs/missiles; leave banner."
  [state]
  (wave-lifecycle/start-next-wave state
                                  wave-starts-complete?
                                  wave-starts-with-enemies?))

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
      (playing? state)
      (wrap (-> state
                (advance-clock applied)
                (combat/tick-defensive-phase applied)
                (tick-enemy-missiles applied)
                (tick-flyers applied)
                (maybe-advance-wave-attack)
                (maybe-complete-wave)
                (ensure-wave-attack-started)
                (evaluate-game-over)))

      (wave-banner? state)
      ;; Keep combat fireballs/missiles animating during the banner so they do
      ;; not pop out of existence; start-next-wave clears leftovers afterward.
      ;; Skip destroyable-target phase: banner only animates existing combat FX.
      (wrap (-> state
                (advance-clock applied)
                (combat/tick-defensive applied)
                (combat/tick-fireballs applied)
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
;; {:version 1, :tested-at "2026-07-25T11:12:10.021761-05:00", :module-hash "-1855630584", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 20, :hash "1391860203"} {:id "def/initial-score", :kind "def", :line 21, :end-line 21, :hash "485183318"} {:id "def/initial-entity-id", :kind "def", :line 22, :end-line 22, :hash "-2006662704"} {:id "def/initial-bonus-cities", :kind "def", :line 23, :end-line 23, :hash "1599320953"} {:id "def/initial-bonus-cities-awarded", :kind "def", :line 24, :end-line 24, :hash "1570127145"} {:id "def/initial-bonus-city-earned-events", :kind "def", :line 25, :end-line 25, :hash "1714792540"} {:id "def/wave-flag-off", :kind "def", :line 26, :end-line 26, :hash "1734145513"} {:id "def/wave-flag-on", :kind "def", :line 27, :end-line 27, :hash "1660732832"} {:id "def/wave-starts-complete?", :kind "def", :line 28, :end-line 28, :hash "1259204240"} {:id "def/wave-starts-with-enemies?", :kind "def", :line 29, :end-line 29, :hash "929188796"} {:id "def/clamp-lo", :kind "def", :line 30, :end-line 30, :hash "-224595111"} {:id "def/default-crosshair", :kind "def", :line 31, :end-line 31, :hash "-249046571"} {:id "def/target-starts-destroyed?", :kind "def", :line 32, :end-line 32, :hash "224311611"} {:id "def/screen-title", :kind "def", :line 33, :end-line 33, :hash "1092741116"} {:id "def/screen-playing", :kind "def", :line 34, :end-line 34, :hash "1649702326"} {:id "def/screen-paused", :kind "def", :line 35, :end-line 35, :hash "-1395420898"} {:id "def/screen-the-end", :kind "def", :line 36, :end-line 36, :hash "184874292"} {:id "def/end-message-text", :kind "def", :line 37, :end-line 37, :hash "-1724984215"} {:id "def/title-game-name", :kind "def", :line 38, :end-line 38, :hash "-1224113475"} {:id "def/title-start-affordance", :kind "def", :line 39, :end-line 39, :hash "1586942227"} {:id "def/end-fireball-expand-seconds", :kind "def", :line 40, :end-line 40, :hash "1938981448"} {:id "def/end-fireball-contract-seconds", :kind "def", :line 41, :end-line 41, :hash "-273647692"} {:id "defn-/clamp", :kind "defn-", :line 43, :end-line 45, :hash "1680610514"} {:id "defn-/clamp-point", :kind "defn-", :line 47, :end-line 50, :hash "-1550073030"} {:id "defn-/center-crosshair", :kind "defn-", :line 52, :end-line 54, :hash "502710703"} {:id "defn-/reclamp-crosshair", :kind "defn-", :line 56, :end-line 59, :hash "-495207193"} {:id "defn-/update-battery", :kind "defn-", :line 61, :end-line 63, :hash "-630735181"} {:id "defn-/next-entity-id", :kind "defn-", :line 65, :end-line 68, :hash "-611923035"} {:id "defn-/no-events", :kind "defn-", :line 70, :end-line 72, :hash "652168329"} {:id "def/sfx-events", :kind "def", :line 74, :end-line 74, :hash "525363444"} {:id "def/sfx-take-new", :kind "def", :line 75, :end-line 75, :hash "-2043808376"} {:id "def/sfx-truncate-to", :kind "def", :line 76, :end-line 76, :hash "1968803966"} {:id "def/sfx-drain", :kind "def", :line 77, :end-line 77, :hash "-485131970"} {:id "def/sfx-emitted?", :kind "def", :line 78, :end-line 78, :hash "-1793854788"} {:id "defn/new-game", :kind "defn", :line 80, :end-line 116, :hash "1699967064"} {:id "defn/resize", :kind "defn", :line 118, :end-line 125, :hash "-1344415357"} {:id "defn/playfield-width", :kind "defn", :line 127, :end-line 129, :hash "-1043537513"} {:id "defn/playfield-height", :kind "defn", :line 131, :end-line 133, :hash "344252362"} {:id "defn/cities", :kind "defn", :line 135, :end-line 137, :hash "1240083502"} {:id "defn/living-cities", :kind "defn", :line 139, :end-line 141, :hash "416648848"} {:id "defn/batteries", :kind "defn", :line 143, :end-line 145, :hash "206298614"} {:id "defn/battery", :kind "defn", :line 147, :end-line 149, :hash "-1555624967"} {:id "defn/on-ground?", :kind "defn", :line 151, :end-line 154, :hash "1609612027"} {:id "defn/city-on-ground?", :kind "defn", :line 156, :end-line 158, :hash "-1878088970"} {:id "defn/crosshair", :kind "defn", :line 160, :end-line 162, :hash "-2027795649"} {:id "defn/score", :kind "defn", :line 164, :end-line 166, :hash "-1700557235"} {:id "defn/wave", :kind "defn", :line 168, :end-line 170, :hash "1109090166"} {:id "defn/multiplier", :kind "defn", :line 172, :end-line 175, :hash "-1249467982"} {:id "defn-/long-state", :kind "defn-", :line 177, :end-line 179, :hash "-1384016045"} {:id "def/bonus-cities", :kind "def", :line 181, :end-line 183, :hash "-145856508"} {:id "def/bonus-city-threshold", :kind "def", :line 185, :end-line 185, :hash "-1608074869"} {:id "def/bonus-city-earned-events", :kind "def", :line 187, :end-line 189, :hash "992122868"} {:id "defn/wave-complete?", :kind "defn", :line 191, :end-line 193, :hash "-334236383"} {:id "def/screen", :kind "def", :line 195, :end-line 195, :hash "681051221"} {:id "def/title?", :kind "def", :line 196, :end-line 196, :hash "-413853504"} {:id "def/playing?", :kind "def", :line 197, :end-line 197, :hash "410765386"} {:id "def/paused?", :kind "def", :line 198, :end-line 198, :hash "665268154"} {:id "def/the-end?", :kind "def", :line 199, :end-line 199, :hash "486194475"} {:id "def/title-game-name-of", :kind "def", :line 200, :end-line 200, :hash "1444622798"} {:id "def/title-shows-start-affordance?", :kind "def", :line 201, :end-line 201, :hash "763145159"} {:id "def/high-score-table", :kind "def", :line 203, :end-line 203, :hash "1163853287"} {:id "def/high-score-capacity", :kind "def", :line 204, :end-line 204, :hash "-1128697050"} {:id "def/pending-high-score", :kind "def", :line 205, :end-line 205, :hash "2019144034"} {:id "def/submitted-high-score-initials", :kind "def", :line 206, :end-line 206, :hash "1796003601"} {:id "def/high-score-entry?", :kind "def", :line 207, :end-line 207, :hash "985452078"} {:id "def/high-scores-view?", :kind "def", :line 208, :end-line 208, :hash "336415907"} {:id "def/set-high-score-capacity", :kind "def", :line 209, :end-line 209, :hash "1210250449"} {:id "def/add-high-score-entry", :kind "def", :line 210, :end-line 210, :hash "785373078"} {:id "def/open-high-scores", :kind "def", :line 211, :end-line 211, :hash "-1768638500"} {:id "def/close-high-scores", :kind "def", :line 212, :end-line 212, :hash "-800419625"} {:id "defn/pause-game", :kind "defn", :line 214, :end-line 219, :hash "1793185663"} {:id "defn/resume-game", :kind "defn", :line 221, :end-line 226, :hash "1617711604"} {:id "defn-/blank-shell", :kind "defn-", :line 228, :end-line 232, :hash "533787309"} {:id "def/game-options", :kind "def", :line 234, :end-line 234, :hash "1685212516"} {:id "def/mute?", :kind "def", :line 235, :end-line 235, :hash "573004342"} {:id "def/difficulty", :kind "def", :line 236, :end-line 236, :hash "247818244"} {:id "def/options?", :kind "def", :line 237, :end-line 237, :hash "857221898"} {:id "def/open-options", :kind "def", :line 238, :end-line 238, :hash "-1773224624"} {:id "def/leave-options", :kind "def", :line 239, :end-line 239, :hash "-1553054960"} {:id "def/set-mute", :kind "def", :line 240, :end-line 240, :hash "961239161"} {:id "def/set-difficulty", :kind "def", :line 241, :end-line 241, :hash "-1413206900"} {:id "def/bind-fire-key", :kind "def", :line 242, :end-line 242, :hash "-86622152"} {:id "def/fire-key-includes?", :kind "def", :line 243, :end-line 243, :hash "109857506"} {:id "def/pause-key-includes?", :kind "def", :line 244, :end-line 244, :hash "-1954048295"} {:id "def/wave-banner?", :kind "def", :line 245, :end-line 245, :hash "1130113897"} {:id "def/wave-banner", :kind "def", :line 246, :end-line 246, :hash "911252070"} {:id "def/wave-banner-text", :kind "def", :line 247, :end-line 247, :hash "-1812837808"} {:id "def/wave-banner-subtitle", :kind "def", :line 248, :end-line 248, :hash "764045309"} {:id "def/wave-banner-bonus-city?", :kind "def", :line 249, :end-line 249, :hash "-1008641310"} {:id "def/wave-banner-announced-wave", :kind "def", :line 250, :end-line 250, :hash "549833261"} {:id "def/wave-banner-phase", :kind "def", :line 251, :end-line 251, :hash "-1207534753"} {:id "def/wave-banner-text-position", :kind "def", :line 252, :end-line 252, :hash "1533588800"} {:id "def/wave-banner-distance-to-center", :kind "def", :line 253, :end-line 253, :hash "368380193"} {:id "defn/export-settings", :kind "defn", :line 255, :end-line 260, :hash "-1585521700"} {:id "defn/import-settings", :kind "defn", :line 262, :end-line 271, :hash "221337773"} {:id "defn-/apply-shell", :kind "defn-", :line 274, :end-line 277, :hash "-1543202908"} {:id "defn/start-game", :kind "defn", :line 278, :end-line 282, :hash "321312616"} {:id "defn/final-score", :kind "defn", :line 284, :end-line 287, :hash "-2124677376"} {:id "defn/confirm-end-screen", :kind "defn", :line 289, :end-line 297, :hash "-1073333732"} {:id "defn/submit-high-score-initials", :kind "defn", :line 299, :end-line 308, :hash "-243607349"} {:id "defn/end-message", :kind "defn", :line 310, :end-line 312, :hash "1667840292"} {:id "defn/end-fireball", :kind "defn", :line 314, :end-line 316, :hash "603568745"} {:id "defn/hud", :kind "defn", :line 318, :end-line 322, :hash "-1986360486"} {:id "defn/defensive-missiles", :kind "defn", :line 324, :end-line 326, :hash "-1457861839"} {:id "defn/fireballs", :kind "defn", :line 328, :end-line 330, :hash "-47675919"} {:id "defn/enemy-missiles", :kind "defn", :line 332, :end-line 334, :hash "-1649887754"} {:id "defn/flyers", :kind "defn", :line 336, :end-line 338, :hash "-195685942"} {:id "defn/destroyable-targets", :kind "defn", :line 340, :end-line 342, :hash "-2146081921"} {:id "defn/last-enemy-fate", :kind "defn", :line 344, :end-line 346, :hash "-1164295963"} {:id "defn/city", :kind "defn", :line 348, :end-line 350, :hash "417115868"} {:id "defn/living-city?", :kind "defn", :line 352, :end-line 354, :hash "808122796"} {:id "defn/sim-time", :kind "defn", :line 356, :end-line 358, :hash "1526499425"} {:id "defn/last-applied-dt", :kind "defn", :line 360, :end-line 362, :hash "-1011651673"} {:id "defn/max-fireball-radius", :kind "defn", :line 364, :end-line 366, :hash "421742428"} {:id "defn/set-battery-ammo", :kind "defn", :line 368, :end-line 371, :hash "975933252"} {:id "defn/destroy-battery", :kind "defn", :line 373, :end-line 379, :hash "1155956327"} {:id "defn/add-destroyable-target", :kind "defn", :line 381, :end-line 386, :hash "-1701043486"} {:id "defn-/update-city", :kind "defn-", :line 388, :end-line 390, :hash "-2016100813"} {:id "defn/destroy-city", :kind "defn", :line 392, :end-line 397, :hash "-1181664448"} {:id "defn-/enemy-speed-for-state", :kind "defn-", :line 399, :end-line 402, :hash "104260835"} {:id "def/enemy-kind-ballistic", :kind "def", :line 404, :end-line 404, :hash "844796837"} {:id "def/enemy-kind-mirv", :kind "def", :line 405, :end-line 405, :hash "-903061239"} {:id "def/enemy-kind-mirv-child", :kind "def", :line 406, :end-line 406, :hash "53130372"} {:id "def/enemy-kind-smart", :kind "def", :line 407, :end-line 407, :hash "246438905"} {:id "def/smart-bomb-edge-inner-factor", :kind "def", :line 412, :end-line 412, :hash "1336091741"} {:id "def/smart-not-yet-evaded", :kind "def", :line 413, :end-line 413, :hash "101087391"} {:id "def/smart-bomb-evade-clearance", :kind "def", :line 414, :end-line 414, :hash "-1155691061"} {:id "defn-/mirv-parent?", :kind "defn-", :line 416, :end-line 418, :hash "763145358"} {:id "defn-/mirv-child?", :kind "defn-", :line 420, :end-line 422, :hash "519586696"} {:id "defn-/smart-bomb?", :kind "defn-", :line 424, :end-line 426, :hash "-490539965"} {:id "defn/mirv-parents", :kind "defn", :line 428, :end-line 430, :hash "-1734114206"} {:id "defn/mirv-children", :kind "defn", :line 432, :end-line 434, :hash "1045363527"} {:id "defn/smart-bombs", :kind "defn", :line 436, :end-line 438, :hash "1657617417"} {:id "defn/spawn-enemy-at", :kind "defn", :line 440, :end-line 455, :hash "-2030994445"} {:id "defn/spawn-enemy-targeting-city-from", :kind "defn", :line 457, :end-line 466, :hash "297620600"} {:id "defn/spawn-enemy-targeting-city", :kind "defn", :line 468, :end-line 473, :hash "1574498502"} {:id "defn/spawn-enemy-targeting-battery-from", :kind "defn", :line 475, :end-line 484, :hash "-765503325"} {:id "defn/spawn-enemy-targeting-battery", :kind "defn", :line 486, :end-line 491, :hash "1391767096"} {:id "defn/spawn-enemies-targeting-distinct-cities", :kind "defn", :line 493, :end-line 497, :hash "314430177"} {:id "defn/spawn-mirv-targeting-city", :kind "defn", :line 499, :end-line 511, :hash "-266412817"} {:id "defn/spawn-smart-bomb-targeting-city", :kind "defn", :line 513, :end-line 524, :hash "-988761888"} {:id "defn/spawn-flyer", :kind "defn", :line 526, :end-line 534, :hash "2100696208"} {:id "defn/set-flyer-drops", :kind "defn", :line 536, :end-line 543, :hash "-2098771675"} {:id "defn/set-flyer-drops-toward-living-cities", :kind "defn", :line 545, :end-line 558, :hash "-1546667140"} {:id "defn/set-flyer-drop-targeting-city", :kind "defn", :line 560, :end-line 566, :hash "-362325883"} {:id "defn/flyers-of-kind", :kind "defn", :line 568, :end-line 570, :hash "2117257319"} {:id "defn/add-static-fireball", :kind "defn", :line 572, :end-line 577, :hash "2053229248"} {:id "defn-/enemy-attrs-to-preserve", :kind "defn-", :line 579, :end-line 582, :hash "-111533279"} {:id "defn-/retarget-enemy-from", :kind "defn-", :line 584, :end-line 593, :hash "-550286216"} {:id "defn-/first-enemy-index", :kind "defn-", :line 595, :end-line 597, :hash "-1572668638"} {:id "defn-/retarget-enemy-at-index", :kind "defn-", :line 599, :end-line 601, :hash "1058188477"} {:id "defn/route-first-smart-bomb-through-point", :kind "defn", :line 603, :end-line 610, :hash "-155725677"} {:id "defn/route-smart-bomb-centered-in-fireball", :kind "defn", :line 612, :end-line 615, :hash "-541649650"} {:id "defn/route-smart-bomb-edge-band-in-fireball", :kind "defn", :line 617, :end-line 624, :hash "1354861353"} {:id "defn/route-flyer-through-point", :kind "defn", :line 626, :end-line 640, :hash "-723558539"} {:id "defn/route-enemy-through-point", :kind "defn", :line 642, :end-line 649, :hash "1758214460"} {:id "defn-/first-mirv-child-index", :kind "defn-", :line 651, :end-line 653, :hash "-357091384"} {:id "defn/route-first-mirv-child-through-point", :kind "defn", :line 655, :end-line 662, :hash "1808906776"} {:id "defn-/impact-target", :kind "defn-", :line 664, :end-line 669, :hash "-984684299"} {:id "defn-/enemy-hit-by-fireball?", :kind "defn-", :line 671, :end-line 673, :hash "-387864824"} {:id "defn-/distance-to-fireball", :kind "defn-", :line 675, :end-line 679, :hash "-1214701948"} {:id "defn-/first-touching-fireball", :kind "defn-", :line 681, :end-line 683, :hash "717444682"} {:id "defn-/smart-bomb-edge-band?", :kind "defn-", :line 685, :end-line 689, :hash "885202764"} {:id "defn-/evade-smart-bomb", :kind "defn-", :line 691, :end-line 715, :hash "1355113499"} {:id "defn-/fire-battery", :kind "defn-", :line 717, :end-line 733, :hash "1591374319"} {:id "defn-/aim", :kind "defn-", :line 735, :end-line 741, :hash "242968114"} {:id "defn/click-zone", :kind "defn", :line 743, :end-line 746, :hash "943238228"} {:id "defn/click-fallback-order", :kind "defn", :line 748, :end-line 751, :hash "-1791151582"} {:id "defn-/click-fire", :kind "defn-", :line 753, :end-line 763, :hash "1088598870"} {:id "defn-/click-noop-shell?", :kind "defn-", :line 765, :end-line 771, :hash "626310851"} {:id "defn-/handle-click", :kind "defn-", :line 773, :end-line 779, :hash "987185200"} {:id "defn-/unsupported-command", :kind "defn-", :line 781, :end-line 784, :hash "585518571"} {:id "def/command-handlers", :kind "def", :line 786, :end-line 807, :hash "-2121453946"} {:id "defn/handle", :kind "defn", :line 809, :end-line 814, :hash "1696801717"} {:id "defn/press-key", :kind "defn", :line 815, :end-line 818, :hash "1450190204"} {:id "defn-/spawn-fireball-at", :kind "defn-", :line 820, :end-line 823, :hash "-474153351"} {:id "defn-/assoc-long", :kind "defn-", :line 825, :end-line 827, :hash "-1607523900"} {:id "def/set-bonus-city-threshold", :kind "def", :line 829, :end-line 829, :hash "-1065553678"} {:id "def/set-bonus-city-reserve", :kind "def", :line 830, :end-line 830, :hash "-399484399"} {:id "def/apply-bonus-cities-from-reserve", :kind "def", :line 831, :end-line 831, :hash "-849731520"} {:id "defn-/make-end-fireball", :kind "defn-", :line 833, :end-line 839, :hash "-918471848"} {:id "defn-/update-end-message-reveal", :kind "defn-", :line 841, :end-line 844, :hash "1654790648"} {:id "defn-/enter-the-end", :kind "defn-", :line 846, :end-line 859, :hash "-68727569"} {:id "defn/evaluate-game-over", :kind "defn", :line 861, :end-line 869, :hash "1952984408"} {:id "defn/end-fireball-centered?", :kind "defn", :line 871, :end-line 875, :hash "-1673190026"} {:id "defn/end-fireball-fills-playfield?", :kind "defn", :line 877, :end-line 879, :hash "496208359"} {:id "defn/end-message-layout", :kind "defn", :line 881, :end-line 884, :hash "-2147217889"} {:id "defn/end-message-fills-max-expanse?", :kind "defn", :line 886, :end-line 888, :hash "1595237781"} {:id "defn/end-message-centered?", :kind "defn", :line 890, :end-line 894, :hash "-447969280"} {:id "defn/end-message-visibility-clipped?", :kind "defn", :line 896, :end-line 899, :hash "-1616346772"} {:id "defn/end-message-point-visible?", :kind "defn", :line 901, :end-line 904, :hash "-703026949"} {:id "defn/end-message-reveal", :kind "defn", :line 906, :end-line 909, :hash "-172598567"} {:id "defn-/tick-end-fireball", :kind "defn-", :line 911, :end-line 921, :hash "473583625"} {:id "defn-/add-score", :kind "defn-", :line 923, :end-line 927, :hash "-765578423"} {:id "defn/set-score", :kind "defn", :line 929, :end-line 934, :hash "-416014500"} {:id "defn-/destroy-enemy-by-fireball", :kind "defn-", :line 936, :end-line 943, :hash "-172702724"} {:id "defn-/spawn-impact-fireball", :kind "defn-", :line 945, :end-line 948, :hash "-2084493934"} {:id "defn-/resolve-enemy-impact", :kind "defn-", :line 950, :end-line 955, :hash "1944987463"} {:id "defn-/keep-flying-enemy", :kind "defn-", :line 957, :end-line 959, :hash "-1439807545"} {:id "defn-/resolve-fireball-contact", :kind "defn-", :line 961, :end-line 971, :hash "632440842"} {:id "defn-/progress-of", :kind "defn-", :line 973, :end-line 977, :hash "1779378488"} {:id "defn-/index-of-id", :kind "defn-", :line 979, :end-line 982, :hash "-934326406"} {:id "defn-/mirv-child-target-ids", :kind "defn-", :line 984, :end-line 992, :hash "-194964239"} {:id "defn-/split-mirv-parent", :kind "defn-", :line 994, :end-line 1009, :hash "1014154875"} {:id "defn-/should-split-mirv?", :kind "defn-", :line 1011, :end-line 1015, :hash "-1686921459"} {:id "defn-/resolve-advanced-enemy", :kind "defn-", :line 1017, :end-line 1031, :hash "1088768601"} {:id "defn-/tick-one-enemy", :kind "defn-", :line 1033, :end-line 1039, :hash "2100624266"} {:id "defn-/tick-enemy-missiles", :kind "defn-", :line 1041, :end-line 1047, :hash "-1658169989"} {:id "defn-/destroy-flyer-by-fireball", :kind "defn-", :line 1049, :end-line 1055, :hash "-1739016762"} {:id "defn-/apply-flyer-drops", :kind "defn-", :line 1057, :end-line 1075, :hash "1084438316"} {:id "defn-/keep-flying-flyer", :kind "defn-", :line 1077, :end-line 1079, :hash "1225384403"} {:id "defn-/tick-one-flyer", :kind "defn-", :line 1081, :end-line 1095, :hash "1709281062"} {:id "defn-/tick-flyers", :kind "defn-", :line 1097, :end-line 1103, :hash "73639010"} {:id "defn-/maybe-complete-wave", :kind "defn-", :line 1105, :end-line 1116, :hash "-1564082991"} {:id "defn/rearm-surviving-batteries", :kind "defn", :line 1119, :end-line 1123, :hash "361044815"} {:id "defn/spawn-wave-enemy-targeting-battery", :kind "defn", :line 1125, :end-line 1132, :hash "1810574750"} {:id "defn/set-wave-enemies-active", :kind "defn", :line 1134, :end-line 1145, :hash "-1444118340"} {:id "def/default-mirv-child-count", :kind "def", :line 1148, :end-line 1148, :hash "37539110"} {:id "def/default-mirv-split-progress", :kind "def", :line 1149, :end-line 1149, :hash "-1830047123"} {:id "def/default-flyer-speed", :kind "def", :line 1150, :end-line 1150, :hash "974542101"} {:id "def/default-flyer-altitude-fraction", :kind "def", :line 1151, :end-line 1151, :hash "-78571882"} {:id "def/default-flyer-drop-count", :kind "def", :line 1152, :end-line 1152, :hash "1025270707"} {:id "def/default-flyer-drop-progress-start", :kind "def", :line 1153, :end-line 1153, :hash "2130099899"} {:id "def/default-flyer-drop-progress-end", :kind "def", :line 1154, :end-line 1154, :hash "-570587514"} {:id "defn-/wave-schedule-hooks", :kind "defn-", :line 1156, :end-line 1167, :hash "-256215380"} {:id "defn/begin-wave-attack", :kind "defn", :line 1169, :end-line 1172, :hash "-1577210980"} {:id "def/start-wave-attack", :kind "def", :line 1174, :end-line 1174, :hash "1999025037"} {:id "defn/activate-wave-schedule", :kind "defn", :line 1176, :end-line 1180, :hash "616848354"} {:id "defn-/maybe-advance-wave-attack", :kind "defn-", :line 1182, :end-line 1185, :hash "-257542288"} {:id "defn-/ensure-wave-attack-started", :kind "defn-", :line 1187, :end-line 1190, :hash "905838658"} {:id "defn/set-non-destroyed-battery-ammo", :kind "defn", :line 1192, :end-line 1195, :hash "275837595"} {:id "def/wave-schedule-metrics", :kind "def", :line 1197, :end-line 1197, :hash "-426249483"} {:id "def/wave-schedule-metrics-for", :kind "def", :line 1198, :end-line 1198, :hash "-2119268773"} {:id "def/wave-mirv-count", :kind "def", :line 1200, :end-line 1200, :hash "746006295"} {:id "def/wave-smart-bomb-count", :kind "def", :line 1201, :end-line 1201, :hash "-664512608"} {:id "def/wave-bomber-count", :kind "def", :line 1202, :end-line 1202, :hash "1023037642"} {:id "def/wave-satellite-count", :kind "def", :line 1203, :end-line 1203, :hash "-1625262900"} {:id "def/harder-wave?", :kind "def", :line 1204, :end-line 1204, :hash "-498526476"} {:id "defn/set-wave", :kind "defn", :line 1205, :end-line 1210, :hash "1918037621"} {:id "defn/start-next-wave", :kind "defn", :line 1212, :end-line 1218, :hash "-1628649690"} {:id "defn-/advance-clock", :kind "defn-", :line 1220, :end-line 1224, :hash "2082435033"} {:id "defn/tick", :kind "defn", :line 1226, :end-line 1267, :hash "1148358010"}]}
;; clj-mutate-manifest-end

(ns missile-command.core
  (:require [missile-command.batteries :as batteries]
            [missile-command.cities :as cities]
            [missile-command.flyers :as flyers]
            [missile-command.game-end :as game-end]
            [missile-command.input :as input]
            [missile-command.missiles :as missiles]
            [missile-command.scoring :as scoring]
            [missile-command.high-scores :as high-scores]
            [missile-command.options :as options]
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
(def screen-paused :paused)
(def screen-the-end screens/the-end)
(def screen-high-score-entry :high-score-entry)
(def screen-high-scores :high-scores)
(def screen-options :options)
(def end-message-text game-end/message-text)
(def wrong-end-message-text game-end/wrong-message-text)
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

(defn- emit-sfx
  "Append an SFX event to the state's cumulative log."
  [state type]
  (update state :sfx-events (fnil conj []) {:type type}))

(defn sfx-events
  [state]
  (vec (or (:sfx-events state) [])))

(defn sfx-emitted?
  "True when an event of the given type (keyword or sfx/... string) was logged."
  [state type]
  (let [t (if (keyword? type)
            type
            (keyword type))]
    (boolean (some #(= t (:type %)) (sfx-events state)))))

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

(defn screen
  [state]
  (screens/of state))

(defn title?
  [state]
  (screens/title? state))

(defn playing?
  [state]
  (screens/playing? state))

(defn paused?
  [state]
  (= screen-paused (screen state)))

(defn the-end?
  "True when the run has entered THE END."
  [state]
  (screens/the-end? state))

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

(defn title-game-name-of
  [state]
  (screens/title-game-name-of state))

(defn title-shows-start-affordance?
  [state]
  (screens/title-shows-start-affordance? state))

(defn high-score-table
  [state]
  (vec (or (:high-scores state) [])))

(defn high-score-capacity
  [state]
  (long (or (:high-score-capacity state) high-scores/default-capacity)))

(defn pending-high-score
  [state]
  (:pending-high-score state))

(defn submitted-high-score-initials
  [state]
  (:submitted-high-score-initials state))

(defn high-score-entry?
  [state]
  (= screen-high-score-entry (screen state)))

(defn high-scores-view?
  [state]
  (= screen-high-scores (screen state)))

(defn game-options
  [state]
  (or (:options state) options/default-options))

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

(defn mute?
  [state]
  (options/mute? (game-options state)))

(defn difficulty
  [state]
  (options/difficulty (game-options state)))

(defn options?
  [state]
  (= screen-options (screen state)))

(defn- carry-high-scores
  "Copy high-score table from source onto target (threadable: target first)."
  [target source]
  (assoc target
         :high-scores (high-score-table source)
         :high-score-capacity (high-score-capacity source)
         :pending-high-score nil
         :submitted-high-score-initials
         (:submitted-high-score-initials source)))

(defn- carry-options
  "Copy player options from source onto target (threadable: target first)."
  [target source]
  (assoc target :options (game-options source)))

(defn- carry-shell
  "Preserve high scores and options across shell transitions."
  [target source]
  (-> target
      (carry-high-scores source)
      (carry-options source)))

(defn set-high-score-capacity
  [state capacity]
  (assoc state :high-score-capacity (long capacity)))

(defn add-high-score-entry
  "Seed or append a table entry without changing screen."
  [state initials score]
  (update state :high-scores
          (fn [entries]
            (high-scores/insert (or entries [])
                                (high-score-capacity state)
                                initials
                                score))))

(defn open-options
  "Open options from the title screen."
  [state]
  (if (title? state)
    (assoc state :screen screen-options)
    state))

(defn leave-options
  "Return from options to title."
  [state]
  (if (options? state)
    (assoc state :screen screen-title)
    state))

(defn set-mute
  [state mute-value]
  (assoc state :options
         (options/set-mute (game-options state) mute-value)))

(defn set-difficulty
  [state difficulty]
  (assoc state :options
         (options/set-difficulty (game-options state) difficulty)))

(defn bind-fire-key
  [state battery-id key]
  (assoc state :options
         (options/bind-fire-key (game-options state) battery-id key)))

(defn fire-key-includes?
  [state battery-id key]
  (options/fire-key-includes? (game-options state) battery-id key))

(defn pause-key-includes?
  [state key]
  (options/pause-key-includes? (game-options state) key))

(defn start-game
  "Leave title (or any shell) and begin a fresh playing run at current size."
  [state]
  (let [w (playfield-width state)
        h (playfield-height state)]
    (-> (new-game {:width w :height h})
        (carry-shell state)
        (assoc :screen screen-playing
               :submitted-high-score-initials nil))))

(defn final-score
  "Score frozen at THE END, else current score."
  [state]
  (long (or (:final-score state) (score state))))

(defn confirm-end-screen
  "After THE END: open initials entry if score qualifies, else return to title."
  [state]
  (if-not (the-end? state)
    state
    (let [score (final-score state)
          table (high-score-table state)
          cap (high-score-capacity state)]
      (if (high-scores/qualifies? table cap score)
        (assoc state
               :screen screen-high-score-entry
               :pending-high-score score)
        (let [w (playfield-width state)
              h (playfield-height state)]
          (-> (new-game {:width w :height h})
              (carry-shell state)
              (assoc :screen screen-title
                     :submitted-high-score-initials nil)))))))

(defn submit-high-score-initials
  "Insert pending score with initials, then return to title."
  [state initials]
  (if-not (high-score-entry? state)
    state
    (let [score (long (or (pending-high-score state) (final-score state)))
          norm (high-scores/normalize-initials initials)
          table (high-scores/insert (high-score-table state)
                                    (high-score-capacity state)
                                    norm
                                    score)
          w (playfield-width state)
          h (playfield-height state)]
      (-> (new-game {:width w :height h})
          (carry-shell (assoc state :high-scores table))
          (assoc :screen screen-title
                 :pending-high-score nil
                 :submitted-high-score-initials norm)))))

(defn open-high-scores
  "View high-score table from title."
  [state]
  (if (title? state)
    (assoc state :screen screen-high-scores)
    state))

(defn close-high-scores
  "Return from high-scores view to title."
  [state]
  (if (high-scores-view? state)
    (assoc state :screen screen-title)
    state))

(defn end-message
  [state]
  (:end-message state))

(defn end-fireball
  [state]
  (:end-fireball state))

(defn- battery-ammo
  [state battery-id]
  (long (or (:missiles (battery state battery-id)) 0)))

(defn hud
  "In-game HUD projection: score, wave, multiplier, ammo, cities, reserve.
  Present during playing and paused; not required on title."
  [state]
  (let [playing-or-paused? (or (playing? state) (paused? state))]
    {:wave (wave state)
     :score (score state)
     :multiplier (multiplier state)
     :bonus-cities (bonus-cities state)
     :living-cities (count (living-cities state))
     :left-ammo (battery-ammo state :left)
     :center-ammo (battery-ammo state :center)
     :right-ammo (battery-ammo state :right)
     :ammo {:left (battery-ammo state :left)
            :center (battery-ammo state :center)
            :right (battery-ammo state :right)}
     :full-playing-hud? playing-or-paused?
     :screen (screen state)
     :the-end? (the-end? state)
     :end-message (end-message state)
     :title-game-name (title-game-name-of state)}))

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
  (let [bat (battery state battery-id)
        state (update-battery state battery-id batteries/destroy)]
    (if (and bat (not (:destroyed? bat)))
      (emit-sfx state :sfx/battery-destroyed)
      state)))

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
  (let [c (city state city-id)
        state (update-city state city-id cities/destroy)]
    (if (and c (:alive? c))
      (emit-sfx state :sfx/city-destroyed)
      state)))

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
                                               (crosshair state))
              remaining (dec (long (:missiles bat)))
              events (cond-> [{:type :sfx/launch :battery battery-id}]
                       (= 1 remaining)
                       (conj {:type :sfx/low-ammo :battery battery-id}))
              state (-> state
                        (update :defensive-missiles (fnil conj []) missile)
                        (update-battery battery-id batteries/spend-ammo)
                        (update :sfx-events (fnil into []) events))]
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
  (case (:type command)
    :aim (aim state (:x command) (:y command))
    :fire (fire-battery state (:battery command))
    :click (cond
             (title? state) (no-events (start-game state))
             (the-end? state) (no-events (confirm-end-screen state))
             (paused? state) (no-events state)
             :else (click-fire state (:x command) (:y command)))
    :start (no-events (start-game state))
    :confirm (no-events (confirm-end-screen state))
    :pause (no-events (pause-game state))
    :resume (no-events (resume-game state))
    :open-high-scores (no-events (open-high-scores state))
    :close-high-scores (no-events (close-high-scores state))
    :submit-high-score
    (no-events (submit-high-score-initials state (:initials command)))
    :open-options (no-events (open-options state))
    :leave-options (no-events (leave-options state))
    :set-mute (no-events (set-mute state (:mute command)))
    :set-difficulty (no-events (set-difficulty state (:difficulty command)))
    :bind-fire-key (no-events (bind-fire-key state (:battery command) (:key command)))
    :key (if-let [battery-id (options/key->battery (game-options state) (:key command))]
           (fire-battery state battery-id)
           (no-events state))
    (throw (ex-info (str "unsupported command: " (:type command))
                    {:command command}))))

(defn press-key
  "Apply a remappable key: fire mapped battery when playing, else no-op result."
  [state key]
  (handle state {:type :key :key key}))

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
        update-end-message-reveal
        (emit-sfx :sfx/the-end))))

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
          (emit-sfx :sfx/bonus-city)
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
      (assoc :last-enemy-fate :fireball)
      (emit-sfx :sfx/explosion)))

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
      (update :wave (fnil inc waves/initial-wave))
      (emit-sfx :sfx/wave-clear)))

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

(defn wave-schedule-metrics
  ([wave-number]
   (waves/schedule-metrics wave-number))
  ([wave-number difficulty]
   (waves/schedule-metrics wave-number difficulty)))

(defn wave-schedule-metrics-for
  "Wave schedule metrics using the state's difficulty preset."
  [state wave-number]
  (waves/schedule-metrics wave-number (difficulty state)))

(defn wave-mirv-count
  [wave-number]
  (waves/mirv-count wave-number))

(defn wave-smart-bomb-count
  [wave-number]
  (waves/smart-bomb-count wave-number))

(defn wave-bomber-count
  [wave-number]
  (waves/bomber-count wave-number))

(defn wave-satellite-count
  [wave-number]
  (waves/satellite-count wave-number))

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
      (paused? state)
      {:state (assoc state :last-applied-dt 0.0)
       :events []}

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
;; {:version 1, :tested-at "2026-07-24T15:48:47.136783-05:00", :module-hash "522555037", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 11, :hash "-1635290608"} {:id "def/initial-score", :kind "def", :line 12, :end-line 12, :hash "485183318"} {:id "def/initial-entity-id", :kind "def", :line 13, :end-line 13, :hash "-2006662704"} {:id "def/initial-bonus-cities", :kind "def", :line 14, :end-line 14, :hash "774282307"} {:id "def/initial-bonus-cities-awarded", :kind "def", :line 15, :end-line 15, :hash "-135107397"} {:id "def/initial-bonus-city-earned-events", :kind "def", :line 16, :end-line 16, :hash "-446479005"} {:id "def/wave-flag-off", :kind "def", :line 17, :end-line 17, :hash "1734145513"} {:id "def/wave-flag-on", :kind "def", :line 18, :end-line 18, :hash "1660732832"} {:id "def/wave-starts-complete?", :kind "def", :line 19, :end-line 19, :hash "1259204240"} {:id "def/wave-starts-with-enemies?", :kind "def", :line 20, :end-line 20, :hash "929188796"} {:id "def/clamp-lo", :kind "def", :line 21, :end-line 21, :hash "-224595111"} {:id "def/default-crosshair", :kind "def", :line 22, :end-line 22, :hash "-249046571"} {:id "def/target-starts-destroyed?", :kind "def", :line 23, :end-line 23, :hash "224311611"} {:id "def/screen-title", :kind "def", :line 24, :end-line 24, :hash "1092741116"} {:id "def/screen-playing", :kind "def", :line 25, :end-line 25, :hash "1649702326"} {:id "def/screen-the-end", :kind "def", :line 26, :end-line 26, :hash "184874292"} {:id "def/end-message-text", :kind "def", :line 27, :end-line 27, :hash "-1724984215"} {:id "def/title-game-name", :kind "def", :line 28, :end-line 28, :hash "-1224113475"} {:id "def/title-start-affordance", :kind "def", :line 29, :end-line 29, :hash "1586942227"} {:id "def/end-fireball-expand-seconds", :kind "def", :line 30, :end-line 30, :hash "1938981448"} {:id "def/end-fireball-contract-seconds", :kind "def", :line 31, :end-line 31, :hash "-273647692"} {:id "defn-/clamp", :kind "defn-", :line 33, :end-line 35, :hash "1680610514"} {:id "defn-/clamp-point", :kind "defn-", :line 37, :end-line 40, :hash "-1550073030"} {:id "defn-/center-crosshair", :kind "defn-", :line 42, :end-line 44, :hash "502710703"} {:id "defn-/reclamp-crosshair", :kind "defn-", :line 46, :end-line 49, :hash "-495207193"} {:id "defn-/update-battery", :kind "defn-", :line 51, :end-line 53, :hash "-630735181"} {:id "defn-/next-entity-id", :kind "defn-", :line 55, :end-line 58, :hash "-611923035"} {:id "defn-/no-events", :kind "defn-", :line 60, :end-line 62, :hash "652168329"} {:id "defn/new-game", :kind "defn", :line 64, :end-line 94, :hash "-623775965"} {:id "defn/resize", :kind "defn", :line 96, :end-line 103, :hash "-1344415357"} {:id "defn/playfield-width", :kind "defn", :line 105, :end-line 107, :hash "-1043537513"} {:id "defn/playfield-height", :kind "defn", :line 109, :end-line 111, :hash "344252362"} {:id "defn/cities", :kind "defn", :line 113, :end-line 115, :hash "1240083502"} {:id "defn/living-cities", :kind "defn", :line 117, :end-line 119, :hash "416648848"} {:id "defn/batteries", :kind "defn", :line 121, :end-line 123, :hash "206298614"} {:id "defn/battery", :kind "defn", :line 125, :end-line 127, :hash "-1555624967"} {:id "defn/on-ground?", :kind "defn", :line 129, :end-line 132, :hash "1609612027"} {:id "defn/city-on-ground?", :kind "defn", :line 134, :end-line 136, :hash "-1878088970"} {:id "defn/crosshair", :kind "defn", :line 138, :end-line 140, :hash "-2027795649"} {:id "defn/score", :kind "defn", :line 142, :end-line 144, :hash "-1700557235"} {:id "defn/wave", :kind "defn", :line 146, :end-line 148, :hash "1109090166"} {:id "defn/multiplier", :kind "defn", :line 150, :end-line 153, :hash "-1249467982"} {:id "defn-/long-state", :kind "defn-", :line 155, :end-line 157, :hash "-1384016045"} {:id "defn/bonus-cities", :kind "defn", :line 159, :end-line 162, :hash "1351687248"} {:id "defn/bonus-city-threshold", :kind "defn", :line 164, :end-line 166, :hash "1726572985"} {:id "defn/bonus-city-earned-events", :kind "defn", :line 168, :end-line 171, :hash "-2024093832"} {:id "defn/wave-complete?", :kind "defn", :line 173, :end-line 175, :hash "-334236383"} {:id "defn/screen", :kind "defn", :line 177, :end-line 179, :hash "399838058"} {:id "defn/title?", :kind "defn", :line 181, :end-line 183, :hash "-1329422669"} {:id "defn/playing?", :kind "defn", :line 185, :end-line 187, :hash "1277576876"} {:id "defn/the-end?", :kind "defn", :line 189, :end-line 192, :hash "-1222345228"} {:id "defn/title-game-name-of", :kind "defn", :line 194, :end-line 196, :hash "1414623275"} {:id "defn/title-shows-start-affordance?", :kind "defn", :line 198, :end-line 200, :hash "-1236749138"} {:id "defn/start-game", :kind "defn", :line 202, :end-line 208, :hash "-189167581"} {:id "defn/confirm-end-screen", :kind "defn", :line 210, :end-line 217, :hash "98010422"} {:id "defn/end-message", :kind "defn", :line 219, :end-line 221, :hash "1667840292"} {:id "defn/final-score", :kind "defn", :line 223, :end-line 226, :hash "-2124677376"} {:id "defn/end-fireball", :kind "defn", :line 228, :end-line 230, :hash "603568745"} {:id "defn/hud", :kind "defn", :line 232, :end-line 242, :hash "1774880997"} {:id "defn/defensive-missiles", :kind "defn", :line 244, :end-line 246, :hash "-1457861839"} {:id "defn/fireballs", :kind "defn", :line 248, :end-line 250, :hash "-47675919"} {:id "defn/enemy-missiles", :kind "defn", :line 252, :end-line 254, :hash "-1649887754"} {:id "defn/flyers", :kind "defn", :line 256, :end-line 258, :hash "-195685942"} {:id "defn/destroyable-targets", :kind "defn", :line 260, :end-line 262, :hash "-2146081921"} {:id "defn/last-enemy-fate", :kind "defn", :line 264, :end-line 266, :hash "-1164295963"} {:id "defn/city", :kind "defn", :line 268, :end-line 270, :hash "417115868"} {:id "defn/living-city?", :kind "defn", :line 272, :end-line 274, :hash "808122796"} {:id "defn/sim-time", :kind "defn", :line 276, :end-line 278, :hash "1526499425"} {:id "defn/last-applied-dt", :kind "defn", :line 280, :end-line 282, :hash "-1011651673"} {:id "defn/max-fireball-radius", :kind "defn", :line 284, :end-line 286, :hash "421742428"} {:id "defn/set-battery-ammo", :kind "defn", :line 288, :end-line 291, :hash "975933252"} {:id "defn/destroy-battery", :kind "defn", :line 293, :end-line 296, :hash "674766162"} {:id "defn/add-destroyable-target", :kind "defn", :line 298, :end-line 303, :hash "-1701043486"} {:id "defn-/update-city", :kind "defn-", :line 305, :end-line 307, :hash "-2016100813"} {:id "defn/destroy-city", :kind "defn", :line 309, :end-line 311, :hash "1888198826"} {:id "defn-/enemy-speed-for-state", :kind "defn-", :line 313, :end-line 315, :hash "-677140217"} {:id "def/enemy-kind-ballistic", :kind "def", :line 317, :end-line 317, :hash "844796837"} {:id "def/enemy-kind-mirv", :kind "def", :line 318, :end-line 318, :hash "-903061239"} {:id "def/enemy-kind-mirv-child", :kind "def", :line 319, :end-line 319, :hash "53130372"} {:id "def/enemy-kind-smart", :kind "def", :line 320, :end-line 320, :hash "246438905"} {:id "def/smart-bomb-edge-inner-factor", :kind "def", :line 323, :end-line 323, :hash "-1342792927"} {:id "def/smart-not-yet-evaded", :kind "def", :line 324, :end-line 324, :hash "101087391"} {:id "def/smart-bomb-evade-clearance", :kind "def", :line 325, :end-line 325, :hash "-755204528"} {:id "defn-/mirv-parent?", :kind "defn-", :line 327, :end-line 329, :hash "763145358"} {:id "defn-/mirv-child?", :kind "defn-", :line 331, :end-line 333, :hash "519586696"} {:id "defn-/smart-bomb?", :kind "defn-", :line 335, :end-line 337, :hash "-490539965"} {:id "defn/mirv-parents", :kind "defn", :line 339, :end-line 341, :hash "-1734114206"} {:id "defn/mirv-children", :kind "defn", :line 343, :end-line 345, :hash "1045363527"} {:id "defn/smart-bombs", :kind "defn", :line 347, :end-line 349, :hash "1657617417"} {:id "defn/spawn-enemy-at", :kind "defn", :line 351, :end-line 366, :hash "-2030994445"} {:id "defn/spawn-enemy-targeting-city-from", :kind "defn", :line 368, :end-line 377, :hash "297620600"} {:id "defn/spawn-enemy-targeting-city", :kind "defn", :line 379, :end-line 384, :hash "1574498502"} {:id "defn/spawn-enemy-targeting-battery-from", :kind "defn", :line 386, :end-line 395, :hash "-765503325"} {:id "defn/spawn-enemy-targeting-battery", :kind "defn", :line 397, :end-line 402, :hash "1391767096"} {:id "defn/spawn-enemies-targeting-distinct-cities", :kind "defn", :line 404, :end-line 408, :hash "314430177"} {:id "defn/spawn-mirv-targeting-city", :kind "defn", :line 410, :end-line 422, :hash "-266412817"} {:id "defn/spawn-smart-bomb-targeting-city", :kind "defn", :line 424, :end-line 435, :hash "-976059765"} {:id "defn/spawn-flyer", :kind "defn", :line 437, :end-line 445, :hash "2100696208"} {:id "defn/set-flyer-drops", :kind "defn", :line 447, :end-line 454, :hash "-2098771675"} {:id "defn/set-flyer-drops-toward-living-cities", :kind "defn", :line 456, :end-line 469, :hash "-1546667140"} {:id "defn/set-flyer-drop-targeting-city", :kind "defn", :line 471, :end-line 477, :hash "-362325883"} {:id "defn/flyers-of-kind", :kind "defn", :line 479, :end-line 481, :hash "2117257319"} {:id "defn/add-static-fireball", :kind "defn", :line 483, :end-line 488, :hash "2053229248"} {:id "defn-/enemy-attrs-to-preserve", :kind "defn-", :line 490, :end-line 493, :hash "-111533279"} {:id "defn-/retarget-enemy-from", :kind "defn-", :line 495, :end-line 504, :hash "-550286216"} {:id "defn-/first-enemy-index", :kind "defn-", :line 506, :end-line 508, :hash "-1572668638"} {:id "defn-/retarget-enemy-at-index", :kind "defn-", :line 510, :end-line 512, :hash "1058188477"} {:id "defn/route-first-smart-bomb-through-point", :kind "defn", :line 514, :end-line 521, :hash "-155725677"} {:id "defn/route-smart-bomb-centered-in-fireball", :kind "defn", :line 523, :end-line 526, :hash "-541649650"} {:id "defn/route-smart-bomb-edge-band-in-fireball", :kind "defn", :line 528, :end-line 535, :hash "1354861353"} {:id "defn/route-flyer-through-point", :kind "defn", :line 537, :end-line 551, :hash "-723558539"} {:id "defn/route-enemy-through-point", :kind "defn", :line 553, :end-line 560, :hash "1758214460"} {:id "defn-/first-mirv-child-index", :kind "defn-", :line 562, :end-line 564, :hash "-357091384"} {:id "defn/route-first-mirv-child-through-point", :kind "defn", :line 566, :end-line 573, :hash "1808906776"} {:id "defn-/impact-target", :kind "defn-", :line 575, :end-line 580, :hash "-984684299"} {:id "defn-/enemy-hit-by-fireball?", :kind "defn-", :line 582, :end-line 584, :hash "-387864824"} {:id "defn-/distance-to-fireball", :kind "defn-", :line 586, :end-line 590, :hash "-1214701948"} {:id "defn-/first-touching-fireball", :kind "defn-", :line 592, :end-line 594, :hash "717444682"} {:id "defn-/smart-bomb-edge-band?", :kind "defn-", :line 596, :end-line 600, :hash "885202764"} {:id "defn-/evade-smart-bomb", :kind "defn-", :line 602, :end-line 626, :hash "1355113499"} {:id "defn-/fire-battery", :kind "defn-", :line 628, :end-line 641, :hash "1750447281"} {:id "defn-/aim", :kind "defn-", :line 643, :end-line 649, :hash "242968114"} {:id "defn/click-zone", :kind "defn", :line 651, :end-line 654, :hash "943238228"} {:id "defn/click-fallback-order", :kind "defn", :line 656, :end-line 659, :hash "-1791151582"} {:id "defn-/click-fire", :kind "defn-", :line 661, :end-line 671, :hash "1088598870"} {:id "defn-/handle-click", :kind "defn-", :line 673, :end-line 678, :hash "-1052456845"} {:id "defn-/unsupported-command", :kind "defn-", :line 680, :end-line 683, :hash "585518571"} {:id "def/command-handlers", :kind "def", :line 685, :end-line 690, :hash "136640635"} {:id "defn/handle", :kind "defn", :line 692, :end-line 697, :hash "1696801717"} {:id "defn-/spawn-fireball-at", :kind "defn-", :line 699, :end-line 704, :hash "-1922366979"} {:id "defn-/spawn-fireball-from-missile", :kind "defn-", :line 706, :end-line 708, :hash "1839324960"} {:id "defn-/tick-defensive-missiles", :kind "defn-", :line 710, :end-line 718, :hash "465906604"} {:id "defn-/tick-fireballs", :kind "defn-", :line 720, :end-line 728, :hash "-1794535937"} {:id "defn-/target-hit-by-fireball?", :kind "defn-", :line 730, :end-line 732, :hash "1356179508"} {:id "defn-/destroy-targets-in-fireballs", :kind "defn-", :line 734, :end-line 744, :hash "-1920073096"} {:id "defn-/assoc-long", :kind "defn-", :line 746, :end-line 748, :hash "-1607523900"} {:id "defn/set-bonus-city-threshold", :kind "defn", :line 750, :end-line 753, :hash "633568299"} {:id "defn/set-bonus-city-reserve", :kind "defn", :line 755, :end-line 758, :hash "301818582"} {:id "defn-/lowest-destroyed-city-id", :kind "defn-", :line 760, :end-line 766, :hash "-2041789500"} {:id "defn/apply-bonus-cities-from-reserve", :kind "defn", :line 768, :end-line 779, :hash "927622616"} {:id "defn-/make-end-fireball", :kind "defn-", :line 781, :end-line 787, :hash "-918471848"} {:id "defn-/update-end-message-reveal", :kind "defn-", :line 789, :end-line 792, :hash "1654790648"} {:id "defn-/enter-the-end", :kind "defn-", :line 794, :end-line 806, :hash "675084751"} {:id "defn/evaluate-game-over", :kind "defn", :line 808, :end-line 817, :hash "-1819788070"} {:id "defn/end-fireball-centered?", :kind "defn", :line 819, :end-line 823, :hash "-1673190026"} {:id "defn/end-fireball-fills-playfield?", :kind "defn", :line 825, :end-line 827, :hash "496208359"} {:id "defn/end-message-layout", :kind "defn", :line 829, :end-line 832, :hash "-2147217889"} {:id "defn/end-message-fills-max-expanse?", :kind "defn", :line 834, :end-line 836, :hash "1595237781"} {:id "defn/end-message-centered?", :kind "defn", :line 838, :end-line 842, :hash "-447969280"} {:id "defn/end-message-visibility-clipped?", :kind "defn", :line 844, :end-line 847, :hash "-1616346772"} {:id "defn/end-message-point-visible?", :kind "defn", :line 849, :end-line 852, :hash "-703026949"} {:id "defn/end-message-reveal", :kind "defn", :line 854, :end-line 857, :hash "-172598567"} {:id "defn-/tick-end-fireball", :kind "defn-", :line 859, :end-line 869, :hash "473583625"} {:id "defn-/sync-bonus-cities-from-score", :kind "defn-", :line 871, :end-line 885, :hash "593228589"} {:id "defn-/add-score", :kind "defn-", :line 887, :end-line 891, :hash "-269277404"} {:id "defn/set-score", :kind "defn", :line 893, :end-line 898, :hash "2036835626"} {:id "defn-/destroy-enemy-by-fireball", :kind "defn-", :line 900, :end-line 906, :hash "-1165575634"} {:id "defn-/spawn-impact-fireball", :kind "defn-", :line 908, :end-line 911, :hash "-2084493934"} {:id "defn-/resolve-enemy-impact", :kind "defn-", :line 913, :end-line 918, :hash "1944987463"} {:id "defn-/keep-flying-enemy", :kind "defn-", :line 920, :end-line 922, :hash "-1439807545"} {:id "defn-/resolve-fireball-contact", :kind "defn-", :line 924, :end-line 934, :hash "632440842"} {:id "defn-/progress-of", :kind "defn-", :line 936, :end-line 940, :hash "1779378488"} {:id "defn-/index-of-id", :kind "defn-", :line 942, :end-line 945, :hash "-934326406"} {:id "defn-/mirv-child-target-ids", :kind "defn-", :line 947, :end-line 955, :hash "-194964239"} {:id "defn-/split-mirv-parent", :kind "defn-", :line 957, :end-line 972, :hash "1014154875"} {:id "defn-/should-split-mirv?", :kind "defn-", :line 974, :end-line 978, :hash "-1686921459"} {:id "defn-/resolve-advanced-enemy", :kind "defn-", :line 980, :end-line 994, :hash "1088768601"} {:id "defn-/tick-one-enemy", :kind "defn-", :line 996, :end-line 1002, :hash "2100624266"} {:id "defn-/tick-enemy-missiles", :kind "defn-", :line 1004, :end-line 1010, :hash "-1658169989"} {:id "defn-/destroy-flyer-by-fireball", :kind "defn-", :line 1012, :end-line 1017, :hash "1452981331"} {:id "defn-/apply-flyer-drops", :kind "defn-", :line 1019, :end-line 1037, :hash "1084438316"} {:id "defn-/keep-flying-flyer", :kind "defn-", :line 1039, :end-line 1041, :hash "1225384403"} {:id "defn-/tick-one-flyer", :kind "defn-", :line 1043, :end-line 1057, :hash "1709281062"} {:id "defn-/tick-flyers", :kind "defn-", :line 1059, :end-line 1065, :hash "73639010"} {:id "defn-/wave-ready-to-complete?", :kind "defn-", :line 1067, :end-line 1073, :hash "1949930133"} {:id "defn-/unused-defensive-missiles", :kind "defn-", :line 1075, :end-line 1081, :hash "52651543"} {:id "defn-/award-wave-end-bonuses", :kind "defn-", :line 1083, :end-line 1090, :hash "-1067549351"} {:id "defn-/mark-wave-complete", :kind "defn-", :line 1092, :end-line 1099, :hash "2011566928"} {:id "defn-/maybe-complete-wave", :kind "defn-", :line 1101, :end-line 1104, :hash "1290373418"} {:id "defn-/transform-living-battery", :kind "defn-", :line 1106, :end-line 1108, :hash "-703267492"} {:id "defn-/map-living-batteries", :kind "defn-", :line 1110, :end-line 1114, :hash "1661747933"} {:id "defn/rearm-surviving-batteries", :kind "defn", :line 1116, :end-line 1119, :hash "352805794"} {:id "defn-/non-destroyed-batteries", :kind "defn-", :line 1121, :end-line 1123, :hash "-2142188193"} {:id "defn-/wave-target-pool", :kind "defn-", :line 1125, :end-line 1129, :hash "1662828947"} {:id "defn-/spawn-wave-enemy", :kind "defn-", :line 1131, :end-line 1138, :hash "833630058"} {:id "defn/spawn-wave-enemy-targeting-battery", :kind "defn", :line 1140, :end-line 1145, :hash "-458735756"} {:id "defn/set-wave-enemies-active", :kind "defn", :line 1147, :end-line 1162, :hash "-1117220171"} {:id "defn/set-non-destroyed-battery-ammo", :kind "defn", :line 1164, :end-line 1167, :hash "-1800722181"} {:id "def/wave-schedule-metrics", :kind "def", :line 1169, :end-line 1169, :hash "-426249483"} {:id "def/wave-mirv-count", :kind "def", :line 1170, :end-line 1170, :hash "746006295"} {:id "def/wave-smart-bomb-count", :kind "def", :line 1171, :end-line 1171, :hash "-664512608"} {:id "def/wave-bomber-count", :kind "def", :line 1172, :end-line 1172, :hash "1023037642"} {:id "def/wave-satellite-count", :kind "def", :line 1173, :end-line 1173, :hash "-1625262900"} {:id "def/harder-wave?", :kind "def", :line 1174, :end-line 1174, :hash "-498526476"} {:id "defn/set-wave", :kind "defn", :line 1176, :end-line 1184, :hash "1048551934"} {:id "defn/start-next-wave", :kind "defn", :line 1186, :end-line 1192, :hash "98352319"} {:id "defn-/advance-clock", :kind "defn-", :line 1194, :end-line 1198, :hash "2082435033"} {:id "defn/tick", :kind "defn", :line 1200, :end-line 1225, :hash "323455717"}]}
;; clj-mutate-manifest-end

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
            [missile-command.rng :as rng]
            [missile-command.sfx :as sfx]
            [missile-command.shell :as shell]
            [missile-command.testing :as testing]
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

(def pause-game shell/pause-game)
(def resume-game shell/resume-game)

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

(def export-settings shell/export-settings)
(def import-settings shell/import-settings)

(defn start-game
  "Leave title (or any shell) and begin a fresh playing run at current size."
  [state]
  (shell/start-game state blank-shell))

(defn final-score
  "Score frozen at THE END, else current score."
  [state]
  (long (or (:final-score state) (score state))))

(defn confirm-end-screen
  "After THE END: open initials entry if score qualifies, else return to title."
  [state]
  (shell/confirm-end-screen state
                            (the-end? state)
                            (final-score state)
                            blank-shell))

(defn submit-high-score-initials
  "Insert pending score with initials, then return to title."
  [state initials]
  (shell/submit-high-score-initials
   state
   (high-score-entry? state)
   (long (or (pending-high-score state) (final-score state)))
   initials
   blank-shell))

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

(def add-destroyable-target testing/add-destroyable-target)

(defn- update-city
  [state city-id f]
  (update state :cities #(cities/update-city % city-id f)))

(defn destroy-city
  [state city-id]
  (let [c (city state city-id)]
    (sfx/maybe-emit (update-city state city-id cities/destroy)
                    (and c (:alive? c))
                    :sfx/city-destroyed)))

(def enemy-kind-ballistic combat/enemy-kind-ballistic)
(def enemy-kind-mirv combat/enemy-kind-mirv)
(def enemy-kind-mirv-child combat/enemy-kind-mirv-child)
(def enemy-kind-smart combat/enemy-kind-smart)
(def smart-bomb-edge-inner-factor combat/smart-bomb-edge-inner-factor)
(def smart-bomb-evade-clearance combat/smart-bomb-evade-clearance)

(def mirv-parents combat/mirv-parents)
(def mirv-children combat/mirv-children)
(def smart-bombs combat/smart-bombs)

(defn spawn-enemy-at
  "Spawn an enemy missile from origin toward a target point.
  Optional attrs merge onto the missile (e.g. MIRV fields)."
  ([state origin target target-kind target-id]
   (combat/spawn-enemy-at state origin target target-kind target-id nil))
  ([state origin target target-kind target-id attrs]
   (combat/spawn-enemy-at state origin target target-kind target-id attrs)))

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
    (let [width (playfield-width state)
          [ox state] (rng/next-sky-origin-x state width)]
      (spawn-enemy-at state
                      {:x ox :y 0}
                      {:x (:x c) :y (:y c)}
                      :city city-id
                      {:enemy-kind enemy-kind-smart
                       :smart-evaded? false}))))

(defn with-rng-seed
  "Attach a seedable RNG for deterministic sky origins (QA / tests)."
  [state seed]
  (rng/with-seed state seed))

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

(def add-static-fireball testing/add-static-fireball)
(def route-first-smart-bomb-through-point testing/route-first-smart-bomb-through-point)
(def route-smart-bomb-centered-in-fireball testing/route-smart-bomb-centered-in-fireball)
(def route-smart-bomb-edge-band-in-fireball testing/route-smart-bomb-edge-band-in-fireball)
(def route-flyer-through-point testing/route-flyer-through-point)
(def route-enemy-through-point testing/route-enemy-through-point)
(def route-first-mirv-child-through-point testing/route-first-mirv-child-through-point)

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
                (combat/tick-playing-combat applied)
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
;; {:version 1, :tested-at "2026-07-25T12:04:51.959316-05:00", :module-hash "1237747817", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 23, :hash "-1764520185"} {:id "def/initial-score", :kind "def", :line 24, :end-line 24, :hash "485183318"} {:id "def/initial-entity-id", :kind "def", :line 25, :end-line 25, :hash "-2006662704"} {:id "def/initial-bonus-cities", :kind "def", :line 26, :end-line 26, :hash "1599320953"} {:id "def/initial-bonus-cities-awarded", :kind "def", :line 27, :end-line 27, :hash "1570127145"} {:id "def/initial-bonus-city-earned-events", :kind "def", :line 28, :end-line 28, :hash "1714792540"} {:id "def/wave-flag-off", :kind "def", :line 29, :end-line 29, :hash "1734145513"} {:id "def/wave-flag-on", :kind "def", :line 30, :end-line 30, :hash "1660732832"} {:id "def/wave-starts-complete?", :kind "def", :line 31, :end-line 31, :hash "1259204240"} {:id "def/wave-starts-with-enemies?", :kind "def", :line 32, :end-line 32, :hash "929188796"} {:id "def/clamp-lo", :kind "def", :line 33, :end-line 33, :hash "-224595111"} {:id "def/default-crosshair", :kind "def", :line 34, :end-line 34, :hash "-249046571"} {:id "def/target-starts-destroyed?", :kind "def", :line 35, :end-line 35, :hash "224311611"} {:id "def/screen-title", :kind "def", :line 36, :end-line 36, :hash "1092741116"} {:id "def/screen-playing", :kind "def", :line 37, :end-line 37, :hash "1649702326"} {:id "def/screen-paused", :kind "def", :line 38, :end-line 38, :hash "-1395420898"} {:id "def/screen-the-end", :kind "def", :line 39, :end-line 39, :hash "184874292"} {:id "def/end-message-text", :kind "def", :line 40, :end-line 40, :hash "-1724984215"} {:id "def/title-game-name", :kind "def", :line 41, :end-line 41, :hash "-1224113475"} {:id "def/title-start-affordance", :kind "def", :line 42, :end-line 42, :hash "1586942227"} {:id "def/end-fireball-expand-seconds", :kind "def", :line 43, :end-line 43, :hash "1938981448"} {:id "def/end-fireball-contract-seconds", :kind "def", :line 44, :end-line 44, :hash "-273647692"} {:id "defn-/clamp", :kind "defn-", :line 46, :end-line 48, :hash "1680610514"} {:id "defn-/clamp-point", :kind "defn-", :line 50, :end-line 53, :hash "-1550073030"} {:id "defn-/center-crosshair", :kind "defn-", :line 55, :end-line 57, :hash "502710703"} {:id "defn-/reclamp-crosshair", :kind "defn-", :line 59, :end-line 62, :hash "-495207193"} {:id "defn-/update-battery", :kind "defn-", :line 64, :end-line 66, :hash "-630735181"} {:id "defn-/next-entity-id", :kind "defn-", :line 68, :end-line 71, :hash "-611923035"} {:id "defn-/no-events", :kind "defn-", :line 73, :end-line 75, :hash "652168329"} {:id "def/sfx-events", :kind "def", :line 77, :end-line 77, :hash "525363444"} {:id "def/sfx-take-new", :kind "def", :line 78, :end-line 78, :hash "-2043808376"} {:id "def/sfx-truncate-to", :kind "def", :line 79, :end-line 79, :hash "1968803966"} {:id "def/sfx-drain", :kind "def", :line 80, :end-line 80, :hash "-485131970"} {:id "def/sfx-emitted?", :kind "def", :line 81, :end-line 81, :hash "-1793854788"} {:id "defn/new-game", :kind "defn", :line 83, :end-line 119, :hash "1699967064"} {:id "defn/resize", :kind "defn", :line 121, :end-line 128, :hash "-1344415357"} {:id "defn/playfield-width", :kind "defn", :line 130, :end-line 132, :hash "-1043537513"} {:id "defn/playfield-height", :kind "defn", :line 134, :end-line 136, :hash "344252362"} {:id "defn/cities", :kind "defn", :line 138, :end-line 140, :hash "1240083502"} {:id "defn/living-cities", :kind "defn", :line 142, :end-line 144, :hash "416648848"} {:id "defn/batteries", :kind "defn", :line 146, :end-line 148, :hash "206298614"} {:id "defn/battery", :kind "defn", :line 150, :end-line 152, :hash "-1555624967"} {:id "defn/on-ground?", :kind "defn", :line 154, :end-line 157, :hash "1609612027"} {:id "defn/city-on-ground?", :kind "defn", :line 159, :end-line 161, :hash "-1878088970"} {:id "defn/crosshair", :kind "defn", :line 163, :end-line 165, :hash "-2027795649"} {:id "defn/score", :kind "defn", :line 167, :end-line 169, :hash "-1700557235"} {:id "defn/wave", :kind "defn", :line 171, :end-line 173, :hash "1109090166"} {:id "defn/multiplier", :kind "defn", :line 175, :end-line 178, :hash "-1249467982"} {:id "def/bonus-cities", :kind "def", :line 180, :end-line 182, :hash "-145856508"} {:id "def/bonus-city-threshold", :kind "def", :line 184, :end-line 184, :hash "-1608074869"} {:id "def/bonus-city-earned-events", :kind "def", :line 186, :end-line 188, :hash "992122868"} {:id "defn/wave-complete?", :kind "defn", :line 190, :end-line 192, :hash "-334236383"} {:id "def/screen", :kind "def", :line 194, :end-line 194, :hash "681051221"} {:id "def/title?", :kind "def", :line 195, :end-line 195, :hash "-413853504"} {:id "def/playing?", :kind "def", :line 196, :end-line 196, :hash "410765386"} {:id "def/paused?", :kind "def", :line 197, :end-line 197, :hash "665268154"} {:id "def/the-end?", :kind "def", :line 198, :end-line 198, :hash "486194475"} {:id "def/title-game-name-of", :kind "def", :line 199, :end-line 199, :hash "1444622798"} {:id "def/title-shows-start-affordance?", :kind "def", :line 200, :end-line 200, :hash "763145159"} {:id "def/high-score-table", :kind "def", :line 202, :end-line 202, :hash "1163853287"} {:id "def/high-score-capacity", :kind "def", :line 203, :end-line 203, :hash "-1128697050"} {:id "def/pending-high-score", :kind "def", :line 204, :end-line 204, :hash "2019144034"} {:id "def/submitted-high-score-initials", :kind "def", :line 205, :end-line 205, :hash "1796003601"} {:id "def/high-score-entry?", :kind "def", :line 206, :end-line 206, :hash "985452078"} {:id "def/high-scores-view?", :kind "def", :line 207, :end-line 207, :hash "336415907"} {:id "def/set-high-score-capacity", :kind "def", :line 208, :end-line 208, :hash "1210250449"} {:id "def/add-high-score-entry", :kind "def", :line 209, :end-line 209, :hash "785373078"} {:id "def/open-high-scores", :kind "def", :line 210, :end-line 210, :hash "-1768638500"} {:id "def/close-high-scores", :kind "def", :line 211, :end-line 211, :hash "-800419625"} {:id "def/pause-game", :kind "def", :line 213, :end-line 213, :hash "1983515257"} {:id "def/resume-game", :kind "def", :line 214, :end-line 214, :hash "-914102412"} {:id "defn-/blank-shell", :kind "defn-", :line 216, :end-line 220, :hash "533787309"} {:id "def/game-options", :kind "def", :line 222, :end-line 222, :hash "1685212516"} {:id "def/mute?", :kind "def", :line 223, :end-line 223, :hash "573004342"} {:id "def/difficulty", :kind "def", :line 224, :end-line 224, :hash "247818244"} {:id "def/options?", :kind "def", :line 225, :end-line 225, :hash "857221898"} {:id "def/open-options", :kind "def", :line 226, :end-line 226, :hash "-1773224624"} {:id "def/leave-options", :kind "def", :line 227, :end-line 227, :hash "-1553054960"} {:id "def/set-mute", :kind "def", :line 228, :end-line 228, :hash "961239161"} {:id "def/set-difficulty", :kind "def", :line 229, :end-line 229, :hash "-1413206900"} {:id "def/bind-fire-key", :kind "def", :line 230, :end-line 230, :hash "-86622152"} {:id "def/fire-key-includes?", :kind "def", :line 231, :end-line 231, :hash "109857506"} {:id "def/pause-key-includes?", :kind "def", :line 232, :end-line 232, :hash "-1954048295"} {:id "def/wave-banner?", :kind "def", :line 233, :end-line 233, :hash "1130113897"} {:id "def/wave-banner", :kind "def", :line 234, :end-line 234, :hash "911252070"} {:id "def/wave-banner-text", :kind "def", :line 235, :end-line 235, :hash "-1812837808"} {:id "def/wave-banner-subtitle", :kind "def", :line 236, :end-line 236, :hash "764045309"} {:id "def/wave-banner-bonus-city?", :kind "def", :line 237, :end-line 237, :hash "-1008641310"} {:id "def/wave-banner-announced-wave", :kind "def", :line 238, :end-line 238, :hash "549833261"} {:id "def/wave-banner-phase", :kind "def", :line 239, :end-line 239, :hash "-1207534753"} {:id "def/wave-banner-text-position", :kind "def", :line 240, :end-line 240, :hash "1533588800"} {:id "def/wave-banner-distance-to-center", :kind "def", :line 241, :end-line 241, :hash "368380193"} {:id "def/export-settings", :kind "def", :line 243, :end-line 243, :hash "799368708"} {:id "def/import-settings", :kind "def", :line 244, :end-line 244, :hash "1563108461"} {:id "defn/start-game", :kind "defn", :line 246, :end-line 249, :hash "-622402712"} {:id "defn/final-score", :kind "defn", :line 251, :end-line 254, :hash "-2124677376"} {:id "defn/confirm-end-screen", :kind "defn", :line 256, :end-line 262, :hash "2048563930"} {:id "defn/submit-high-score-initials", :kind "defn", :line 264, :end-line 272, :hash "-864689966"} {:id "defn/end-message", :kind "defn", :line 274, :end-line 276, :hash "1667840292"} {:id "defn/end-fireball", :kind "defn", :line 278, :end-line 280, :hash "603568745"} {:id "defn/hud", :kind "defn", :line 282, :end-line 286, :hash "-1986360486"} {:id "defn/defensive-missiles", :kind "defn", :line 288, :end-line 290, :hash "-1457861839"} {:id "defn/fireballs", :kind "defn", :line 292, :end-line 294, :hash "-47675919"} {:id "defn/enemy-missiles", :kind "defn", :line 296, :end-line 298, :hash "-1649887754"} {:id "defn/flyers", :kind "defn", :line 300, :end-line 302, :hash "-195685942"} {:id "defn/destroyable-targets", :kind "defn", :line 304, :end-line 306, :hash "-2146081921"} {:id "defn/last-enemy-fate", :kind "defn", :line 308, :end-line 310, :hash "-1164295963"} {:id "defn/city", :kind "defn", :line 312, :end-line 314, :hash "417115868"} {:id "defn/living-city?", :kind "defn", :line 316, :end-line 318, :hash "808122796"} {:id "defn/sim-time", :kind "defn", :line 320, :end-line 322, :hash "1526499425"} {:id "defn/last-applied-dt", :kind "defn", :line 324, :end-line 326, :hash "-1011651673"} {:id "defn/max-fireball-radius", :kind "defn", :line 328, :end-line 330, :hash "421742428"} {:id "defn/set-battery-ammo", :kind "defn", :line 332, :end-line 335, :hash "975933252"} {:id "defn/destroy-battery", :kind "defn", :line 337, :end-line 343, :hash "1155956327"} {:id "def/add-destroyable-target", :kind "def", :line 345, :end-line 345, :hash "1900650860"} {:id "defn-/update-city", :kind "defn-", :line 347, :end-line 349, :hash "-2016100813"} {:id "defn/destroy-city", :kind "defn", :line 351, :end-line 356, :hash "-1181664448"} {:id "def/enemy-kind-ballistic", :kind "def", :line 358, :end-line 358, :hash "296076667"} {:id "def/enemy-kind-mirv", :kind "def", :line 359, :end-line 359, :hash "-1043521157"} {:id "def/enemy-kind-mirv-child", :kind "def", :line 360, :end-line 360, :hash "-2101498292"} {:id "def/enemy-kind-smart", :kind "def", :line 361, :end-line 361, :hash "22178418"} {:id "def/smart-bomb-edge-inner-factor", :kind "def", :line 362, :end-line 362, :hash "702668191"} {:id "def/smart-bomb-evade-clearance", :kind "def", :line 363, :end-line 363, :hash "-225229347"} {:id "def/mirv-parents", :kind "def", :line 365, :end-line 365, :hash "-1589899942"} {:id "def/mirv-children", :kind "def", :line 366, :end-line 366, :hash "-1014328737"} {:id "def/smart-bombs", :kind "def", :line 367, :end-line 367, :hash "-1620458263"} {:id "defn/spawn-enemy-at", :kind "defn", :line 369, :end-line 375, :hash "-1104711034"} {:id "defn/spawn-enemy-targeting-city-from", :kind "defn", :line 377, :end-line 386, :hash "297620600"} {:id "defn/spawn-enemy-targeting-city", :kind "defn", :line 388, :end-line 393, :hash "1574498502"} {:id "defn/spawn-enemy-targeting-battery-from", :kind "defn", :line 395, :end-line 404, :hash "-765503325"} {:id "defn/spawn-enemy-targeting-battery", :kind "defn", :line 406, :end-line 411, :hash "1391767096"} {:id "defn/spawn-enemies-targeting-distinct-cities", :kind "defn", :line 413, :end-line 417, :hash "314430177"} {:id "defn/spawn-mirv-targeting-city", :kind "defn", :line 419, :end-line 431, :hash "-266412817"} {:id "defn/spawn-smart-bomb-targeting-city", :kind "defn", :line 433, :end-line 446, :hash "1761593103"} {:id "defn/with-rng-seed", :kind "defn", :line 448, :end-line 451, :hash "-930910007"} {:id "defn/spawn-flyer", :kind "defn", :line 453, :end-line 461, :hash "2100696208"} {:id "defn/set-flyer-drops", :kind "defn", :line 463, :end-line 470, :hash "-2098771675"} {:id "defn/set-flyer-drops-toward-living-cities", :kind "defn", :line 472, :end-line 485, :hash "-1546667140"} {:id "defn/set-flyer-drop-targeting-city", :kind "defn", :line 487, :end-line 493, :hash "-362325883"} {:id "defn/flyers-of-kind", :kind "defn", :line 495, :end-line 497, :hash "2117257319"} {:id "def/add-static-fireball", :kind "def", :line 499, :end-line 499, :hash "-79639594"} {:id "def/route-first-smart-bomb-through-point", :kind "def", :line 500, :end-line 500, :hash "2106244581"} {:id "def/route-smart-bomb-centered-in-fireball", :kind "def", :line 501, :end-line 501, :hash "-990696242"} {:id "def/route-smart-bomb-edge-band-in-fireball", :kind "def", :line 502, :end-line 502, :hash "508133687"} {:id "def/route-flyer-through-point", :kind "def", :line 503, :end-line 503, :hash "-1713445099"} {:id "def/route-enemy-through-point", :kind "def", :line 504, :end-line 504, :hash "1422275024"} {:id "def/route-first-mirv-child-through-point", :kind "def", :line 505, :end-line 505, :hash "-1540414387"} {:id "defn-/fire-battery", :kind "defn-", :line 507, :end-line 523, :hash "1591374319"} {:id "defn-/aim", :kind "defn-", :line 525, :end-line 531, :hash "242968114"} {:id "defn/click-zone", :kind "defn", :line 533, :end-line 536, :hash "943238228"} {:id "defn/click-fallback-order", :kind "defn", :line 538, :end-line 541, :hash "-1791151582"} {:id "defn-/click-fire", :kind "defn-", :line 543, :end-line 553, :hash "-533006603"} {:id "defn-/click-noop-shell?", :kind "defn-", :line 555, :end-line 561, :hash "626310851"} {:id "defn-/handle-click", :kind "defn-", :line 563, :end-line 569, :hash "987185200"} {:id "defn-/unsupported-command", :kind "defn-", :line 571, :end-line 574, :hash "585518571"} {:id "def/command-handlers", :kind "def", :line 576, :end-line 597, :hash "-2121453946"} {:id "defn/handle", :kind "defn", :line 599, :end-line 604, :hash "1696801717"} {:id "defn/press-key", :kind "defn", :line 605, :end-line 608, :hash "1450190204"} {:id "def/set-bonus-city-threshold", :kind "def", :line 610, :end-line 610, :hash "-1065553678"} {:id "def/set-bonus-city-reserve", :kind "def", :line 611, :end-line 611, :hash "-399484399"} {:id "def/apply-bonus-cities-from-reserve", :kind "def", :line 612, :end-line 612, :hash "-849731520"} {:id "defn-/make-end-fireball", :kind "defn-", :line 614, :end-line 620, :hash "-918471848"} {:id "defn-/update-end-message-reveal", :kind "defn-", :line 622, :end-line 625, :hash "1654790648"} {:id "defn-/enter-the-end", :kind "defn-", :line 627, :end-line 640, :hash "-68727569"} {:id "defn/evaluate-game-over", :kind "defn", :line 642, :end-line 650, :hash "1952984408"} {:id "defn/end-fireball-centered?", :kind "defn", :line 652, :end-line 656, :hash "-1673190026"} {:id "defn/end-fireball-fills-playfield?", :kind "defn", :line 658, :end-line 660, :hash "496208359"} {:id "defn/end-message-layout", :kind "defn", :line 662, :end-line 665, :hash "-2147217889"} {:id "defn/end-message-fills-max-expanse?", :kind "defn", :line 667, :end-line 669, :hash "1595237781"} {:id "defn/end-message-centered?", :kind "defn", :line 671, :end-line 675, :hash "-447969280"} {:id "defn/end-message-visibility-clipped?", :kind "defn", :line 677, :end-line 680, :hash "-1616346772"} {:id "defn/end-message-point-visible?", :kind "defn", :line 682, :end-line 685, :hash "-703026949"} {:id "defn/end-message-reveal", :kind "defn", :line 687, :end-line 690, :hash "-172598567"} {:id "defn-/tick-end-fireball", :kind "defn-", :line 692, :end-line 702, :hash "473583625"} {:id "defn-/add-score", :kind "defn-", :line 704, :end-line 708, :hash "-765578423"} {:id "defn/set-score", :kind "defn", :line 710, :end-line 715, :hash "-416014500"} {:id "defn-/maybe-complete-wave", :kind "defn-", :line 717, :end-line 728, :hash "-1564082991"} {:id "defn/rearm-surviving-batteries", :kind "defn", :line 731, :end-line 735, :hash "361044815"} {:id "defn/spawn-wave-enemy-targeting-battery", :kind "defn", :line 737, :end-line 744, :hash "1810574750"} {:id "defn/set-wave-enemies-active", :kind "defn", :line 746, :end-line 757, :hash "-1444118340"} {:id "def/default-mirv-child-count", :kind "def", :line 760, :end-line 760, :hash "37539110"} {:id "def/default-mirv-split-progress", :kind "def", :line 761, :end-line 761, :hash "-1830047123"} {:id "def/default-flyer-speed", :kind "def", :line 762, :end-line 762, :hash "974542101"} {:id "def/default-flyer-altitude-fraction", :kind "def", :line 763, :end-line 763, :hash "-78571882"} {:id "def/default-flyer-drop-count", :kind "def", :line 764, :end-line 764, :hash "1025270707"} {:id "def/default-flyer-drop-progress-start", :kind "def", :line 765, :end-line 765, :hash "2130099899"} {:id "def/default-flyer-drop-progress-end", :kind "def", :line 766, :end-line 766, :hash "-570587514"} {:id "defn-/wave-schedule-hooks", :kind "defn-", :line 768, :end-line 779, :hash "-256215380"} {:id "defn/begin-wave-attack", :kind "defn", :line 781, :end-line 784, :hash "-1577210980"} {:id "def/start-wave-attack", :kind "def", :line 786, :end-line 786, :hash "1999025037"} {:id "defn/activate-wave-schedule", :kind "defn", :line 788, :end-line 792, :hash "616848354"} {:id "defn-/maybe-advance-wave-attack", :kind "defn-", :line 794, :end-line 797, :hash "-257542288"} {:id "defn-/ensure-wave-attack-started", :kind "defn-", :line 799, :end-line 802, :hash "905838658"} {:id "defn/set-non-destroyed-battery-ammo", :kind "defn", :line 804, :end-line 807, :hash "1075327706"} {:id "def/wave-schedule-metrics", :kind "def", :line 809, :end-line 809, :hash "-426249483"} {:id "def/wave-schedule-metrics-for", :kind "def", :line 810, :end-line 810, :hash "-2119268773"} {:id "def/wave-mirv-count", :kind "def", :line 812, :end-line 812, :hash "746006295"} {:id "def/wave-smart-bomb-count", :kind "def", :line 813, :end-line 813, :hash "-664512608"} {:id "def/wave-bomber-count", :kind "def", :line 814, :end-line 814, :hash "1023037642"} {:id "def/wave-satellite-count", :kind "def", :line 815, :end-line 815, :hash "-1625262900"} {:id "def/harder-wave?", :kind "def", :line 816, :end-line 816, :hash "-498526476"} {:id "defn/set-wave", :kind "defn", :line 817, :end-line 822, :hash "1918037621"} {:id "defn/start-next-wave", :kind "defn", :line 824, :end-line 830, :hash "-1628649690"} {:id "defn-/advance-clock", :kind "defn-", :line 832, :end-line 836, :hash "2082435033"} {:id "defn/tick", :kind "defn", :line 838, :end-line 877, :hash "946977361"}]}
;; clj-mutate-manifest-end

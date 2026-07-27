(ns missile-command.core
  "Host-facing game facade.

  Stable production entry points are construction (`new-game`, `resize`),
  command/simulation entry (`handle`, `tick`), and read projections used by
  hosts, renderers, persistence, audio, and telemetry. State staging helpers
  used by specs, acceptance, and QA scenarios live in `missile-command.testing`;
  compatibility wrappers remain here until existing call sites are migrated."
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
(def sfx-take-new-with-cursor sfx/take-new-with-cursor)
(def sfx-truncate-to sfx/truncate-to)
(def sfx-drain sfx/drain)
(def sfx-emitted? sfx/emitted?)
(def sfx-event-type-name sfx/event-type-name)

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
  ([state initials]
   (submit-high-score-initials state initials nil nil))
  ([state initials display-name public-code]
  (shell/submit-high-score-initials
   state
   (high-score-entry? state)
   (long (or (pending-high-score state) (final-score state)))
   initials
   display-name
   public-code
   blank-shell)))

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

(def set-battery-ammo
  "Compatibility wrapper for testing/setup. Prefer missile-command.testing."
  testing/set-battery-ammo)

(def destroy-battery
  "Compatibility wrapper for testing/setup. Prefer missile-command.testing."
  testing/destroy-battery)

(def add-destroyable-target testing/add-destroyable-target)

(def destroy-city
  "Compatibility wrapper for testing/setup. Prefer missile-command.testing."
  testing/destroy-city)

(def enemy-kind-ballistic combat/enemy-kind-ballistic)
(def enemy-kind-mirv combat/enemy-kind-mirv)
(def enemy-kind-mirv-child combat/enemy-kind-mirv-child)
(def enemy-kind-smart combat/enemy-kind-smart)
(def smart-bomb-edge-inner-factor combat/smart-bomb-edge-inner-factor)
(def smart-bomb-evade-clearance combat/smart-bomb-evade-clearance)

(def mirv-parents combat/mirv-parents)
(def mirv-children combat/mirv-children)
(def smart-bombs combat/smart-bombs)

(def spawn-enemy-at
  "Compatibility wrapper for testing/setup. Prefer missile-command.testing."
  testing/spawn-enemy-at)

(def spawn-enemy-targeting-city-from
  "Compatibility wrapper for testing/setup. Prefer missile-command.testing."
  testing/spawn-enemy-targeting-city-from)

(def spawn-enemy-targeting-city
  "Compatibility wrapper for testing/setup. Prefer missile-command.testing."
  testing/spawn-enemy-targeting-city)

(def spawn-enemy-targeting-battery-from
  "Compatibility wrapper for testing/setup. Prefer missile-command.testing."
  testing/spawn-enemy-targeting-battery-from)

(def spawn-enemy-targeting-battery
  "Compatibility wrapper for testing/setup. Prefer missile-command.testing."
  testing/spawn-enemy-targeting-battery)

(def spawn-enemies-targeting-distinct-cities
  "Compatibility wrapper for testing/setup. Prefer missile-command.testing."
  testing/spawn-enemies-targeting-distinct-cities)

(def spawn-mirv-targeting-city
  "Compatibility wrapper for testing/setup. Prefer missile-command.testing."
  testing/spawn-mirv-targeting-city)

(def spawn-smart-bomb-targeting-city
  "Compatibility wrapper for testing/setup. Prefer missile-command.testing."
  testing/spawn-smart-bomb-targeting-city)

(def with-rng-seed
  "Compatibility wrapper for testing/setup. Prefer missile-command.testing."
  testing/with-rng-seed)

(def spawn-flyer
  "Compatibility wrapper for testing/setup. Prefer missile-command.testing."
  testing/spawn-flyer)

(def set-flyer-drops
  "Compatibility wrapper for testing/setup. Prefer missile-command.testing."
  testing/set-flyer-drops)

(def set-flyer-drops-toward-living-cities
  "Compatibility wrapper for testing/setup. Prefer missile-command.testing."
  testing/set-flyer-drops-toward-living-cities)

(def set-flyer-drop-targeting-city
  "Compatibility wrapper for testing/setup. Prefer missile-command.testing."
  testing/set-flyer-drop-targeting-city)

(def flyers-of-kind
  "Compatibility wrapper for testing/setup. Prefer missile-command.testing."
  testing/flyers-of-kind)

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
   (fn [state cmd] (no-events (submit-high-score-initials state
                                                          (:initials cmd)
                                                          (:display-name cmd)
                                                          (:public-code cmd))))
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
  "Apply a player command.
  Returns {:state s :events [...]} where :events is non-SFX command feedback
  used by telemetry; hosts play audio from :sfx-events with an SFX cursor."
  [state command]
  (if-let [handler (get command-handlers (:type command))]
    (handler state command)
    (unsupported-command command)))
(defn press-key
  "Apply a remappable key: fire mapped battery when playing, else no-op result."
  [state key]
  (handle state {:type :key :key key}))

(def set-bonus-city-threshold
  "Compatibility wrapper for testing/setup. Prefer missile-command.testing."
  testing/set-bonus-city-threshold)
(def set-bonus-city-reserve
  "Compatibility wrapper for testing/setup. Prefer missile-command.testing."
  testing/set-bonus-city-reserve)
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

(def set-score
  "Compatibility wrapper for testing/setup. Prefer missile-command.testing."
  testing/set-score)

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

(def spawn-wave-enemy-targeting-battery
  "Compatibility wrapper for testing/setup. Prefer missile-command.testing."
  testing/spawn-wave-enemy-targeting-battery)

(def set-wave-enemies-active
  "Compatibility wrapper for testing/setup. Prefer missile-command.testing."
  testing/set-wave-enemies-active)

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

(def begin-wave-attack
  "Compatibility wrapper for testing/setup. Prefer missile-command.testing."
  testing/begin-wave-attack)

(def start-wave-attack
  "Compatibility wrapper for testing/setup. Prefer missile-command.testing."
  testing/start-wave-attack)

(def activate-wave-schedule
  "Compatibility wrapper for testing/setup. Prefer missile-command.testing."
  testing/activate-wave-schedule)

(defn- maybe-advance-wave-attack
  "When the current attack is cleared and more remain, start the next attack."
  [state]
  (wave-schedule/maybe-advance-attack state begin-wave-attack))

(defn- ensure-wave-attack-started
  "Start attack 1 when sky is clear, no attack is active, and wave incomplete."
  [state]
  (wave-schedule/ensure-attack-started state activate-wave-schedule))

(def set-non-destroyed-battery-ammo
  "Compatibility wrapper for testing/setup. Prefer missile-command.testing."
  testing/set-non-destroyed-battery-ammo)

(def wave-schedule-metrics waves/schedule-metrics)
(def wave-schedule-metrics-for waves/schedule-metrics-for-state)

(def wave-mirv-count waves/mirv-count)
(def wave-smart-bomb-count waves/smart-bomb-count)
(def wave-bomber-count waves/bomber-count)
(def wave-satellite-count waves/satellite-count)
(def harder-wave? waves/harder?)
(def set-wave
  "Compatibility wrapper for testing/setup. Prefer missile-command.testing."
  testing/set-wave)

(def start-next-wave
  "Compatibility wrapper for testing/setup. Prefer missile-command.testing."
  testing/start-next-wave)

(defn- advance-clock
  [state applied]
  (-> state
      (assoc :last-applied-dt applied)
      (update :sim-time (fnil + 0.0) applied)))

(defn tick
  "Advance simulation by dt seconds (clamped).
  Returns {:state s :events [...]} where :events is non-SFX command feedback.
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
;; {:version 1, :tested-at "2026-07-27T13:34:01.508329-05:00", :module-hash "-666467434", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 30, :hash "254432893"} {:id "def/initial-score", :kind "def", :line 31, :end-line 31, :hash "485183318"} {:id "def/initial-entity-id", :kind "def", :line 32, :end-line 32, :hash "-2006662704"} {:id "def/initial-bonus-cities", :kind "def", :line 33, :end-line 33, :hash "1599320953"} {:id "def/initial-bonus-cities-awarded", :kind "def", :line 34, :end-line 34, :hash "1570127145"} {:id "def/initial-bonus-city-earned-events", :kind "def", :line 35, :end-line 35, :hash "1714792540"} {:id "def/wave-flag-off", :kind "def", :line 36, :end-line 36, :hash "1734145513"} {:id "def/wave-flag-on", :kind "def", :line 37, :end-line 37, :hash "1660732832"} {:id "def/wave-starts-complete?", :kind "def", :line 38, :end-line 38, :hash "1259204240"} {:id "def/wave-starts-with-enemies?", :kind "def", :line 39, :end-line 39, :hash "929188796"} {:id "def/clamp-lo", :kind "def", :line 40, :end-line 40, :hash "-224595111"} {:id "def/default-crosshair", :kind "def", :line 41, :end-line 41, :hash "-249046571"} {:id "def/target-starts-destroyed?", :kind "def", :line 42, :end-line 42, :hash "224311611"} {:id "def/screen-title", :kind "def", :line 43, :end-line 43, :hash "1092741116"} {:id "def/screen-playing", :kind "def", :line 44, :end-line 44, :hash "1649702326"} {:id "def/screen-paused", :kind "def", :line 45, :end-line 45, :hash "-1395420898"} {:id "def/screen-the-end", :kind "def", :line 46, :end-line 46, :hash "184874292"} {:id "def/end-message-text", :kind "def", :line 47, :end-line 47, :hash "-1724984215"} {:id "def/title-game-name", :kind "def", :line 48, :end-line 48, :hash "-1224113475"} {:id "def/title-start-affordance", :kind "def", :line 49, :end-line 49, :hash "1586942227"} {:id "def/end-fireball-expand-seconds", :kind "def", :line 50, :end-line 50, :hash "1938981448"} {:id "def/end-fireball-contract-seconds", :kind "def", :line 51, :end-line 51, :hash "-273647692"} {:id "defn-/clamp", :kind "defn-", :line 53, :end-line 55, :hash "1680610514"} {:id "defn-/clamp-point", :kind "defn-", :line 57, :end-line 60, :hash "-1550073030"} {:id "defn-/center-crosshair", :kind "defn-", :line 62, :end-line 64, :hash "502710703"} {:id "defn-/reclamp-crosshair", :kind "defn-", :line 66, :end-line 69, :hash "-495207193"} {:id "defn-/update-battery", :kind "defn-", :line 71, :end-line 73, :hash "-630735181"} {:id "defn-/next-entity-id", :kind "defn-", :line 75, :end-line 78, :hash "-611923035"} {:id "defn-/no-events", :kind "defn-", :line 80, :end-line 82, :hash "652168329"} {:id "def/sfx-events", :kind "def", :line 84, :end-line 84, :hash "525363444"} {:id "def/sfx-take-new", :kind "def", :line 85, :end-line 85, :hash "-2043808376"} {:id "def/sfx-take-new-with-cursor", :kind "def", :line 86, :end-line 86, :hash "-74193920"} {:id "def/sfx-truncate-to", :kind "def", :line 87, :end-line 87, :hash "1968803966"} {:id "def/sfx-drain", :kind "def", :line 88, :end-line 88, :hash "-485131970"} {:id "def/sfx-emitted?", :kind "def", :line 89, :end-line 89, :hash "-1793854788"} {:id "def/sfx-event-type-name", :kind "def", :line 90, :end-line 90, :hash "1796224615"} {:id "defn/new-game", :kind "defn", :line 92, :end-line 128, :hash "1699967064"} {:id "defn/resize", :kind "defn", :line 130, :end-line 137, :hash "-1344415357"} {:id "defn/playfield-width", :kind "defn", :line 139, :end-line 141, :hash "-1043537513"} {:id "defn/playfield-height", :kind "defn", :line 143, :end-line 145, :hash "344252362"} {:id "defn/cities", :kind "defn", :line 147, :end-line 149, :hash "1240083502"} {:id "defn/living-cities", :kind "defn", :line 151, :end-line 153, :hash "416648848"} {:id "defn/batteries", :kind "defn", :line 155, :end-line 157, :hash "206298614"} {:id "defn/battery", :kind "defn", :line 159, :end-line 161, :hash "-1555624967"} {:id "defn/on-ground?", :kind "defn", :line 163, :end-line 166, :hash "1609612027"} {:id "defn/city-on-ground?", :kind "defn", :line 168, :end-line 170, :hash "-1878088970"} {:id "defn/crosshair", :kind "defn", :line 172, :end-line 174, :hash "-2027795649"} {:id "defn/score", :kind "defn", :line 176, :end-line 178, :hash "-1700557235"} {:id "defn/wave", :kind "defn", :line 180, :end-line 182, :hash "1109090166"} {:id "defn/multiplier", :kind "defn", :line 184, :end-line 187, :hash "-1249467982"} {:id "def/bonus-cities", :kind "def", :line 189, :end-line 191, :hash "-145856508"} {:id "def/bonus-city-threshold", :kind "def", :line 193, :end-line 193, :hash "-1608074869"} {:id "def/bonus-city-earned-events", :kind "def", :line 195, :end-line 197, :hash "992122868"} {:id "defn/wave-complete?", :kind "defn", :line 199, :end-line 201, :hash "-334236383"} {:id "def/screen", :kind "def", :line 203, :end-line 203, :hash "681051221"} {:id "def/title?", :kind "def", :line 204, :end-line 204, :hash "-413853504"} {:id "def/playing?", :kind "def", :line 205, :end-line 205, :hash "410765386"} {:id "def/paused?", :kind "def", :line 206, :end-line 206, :hash "665268154"} {:id "def/the-end?", :kind "def", :line 207, :end-line 207, :hash "486194475"} {:id "def/title-game-name-of", :kind "def", :line 208, :end-line 208, :hash "1444622798"} {:id "def/title-shows-start-affordance?", :kind "def", :line 209, :end-line 209, :hash "763145159"} {:id "def/high-score-table", :kind "def", :line 211, :end-line 211, :hash "1163853287"} {:id "def/high-score-capacity", :kind "def", :line 212, :end-line 212, :hash "-1128697050"} {:id "def/pending-high-score", :kind "def", :line 213, :end-line 213, :hash "2019144034"} {:id "def/submitted-high-score-initials", :kind "def", :line 214, :end-line 214, :hash "1796003601"} {:id "def/high-score-entry?", :kind "def", :line 215, :end-line 215, :hash "985452078"} {:id "def/high-scores-view?", :kind "def", :line 216, :end-line 216, :hash "336415907"} {:id "def/set-high-score-capacity", :kind "def", :line 217, :end-line 217, :hash "1210250449"} {:id "def/add-high-score-entry", :kind "def", :line 218, :end-line 218, :hash "785373078"} {:id "def/open-high-scores", :kind "def", :line 219, :end-line 219, :hash "-1768638500"} {:id "def/close-high-scores", :kind "def", :line 220, :end-line 220, :hash "-800419625"} {:id "def/pause-game", :kind "def", :line 222, :end-line 222, :hash "1983515257"} {:id "def/resume-game", :kind "def", :line 223, :end-line 223, :hash "-914102412"} {:id "defn-/blank-shell", :kind "defn-", :line 225, :end-line 229, :hash "533787309"} {:id "def/game-options", :kind "def", :line 231, :end-line 231, :hash "1685212516"} {:id "def/mute?", :kind "def", :line 232, :end-line 232, :hash "573004342"} {:id "def/difficulty", :kind "def", :line 233, :end-line 233, :hash "247818244"} {:id "def/options?", :kind "def", :line 234, :end-line 234, :hash "857221898"} {:id "def/open-options", :kind "def", :line 235, :end-line 235, :hash "-1773224624"} {:id "def/leave-options", :kind "def", :line 236, :end-line 236, :hash "-1553054960"} {:id "def/set-mute", :kind "def", :line 237, :end-line 237, :hash "961239161"} {:id "def/set-difficulty", :kind "def", :line 238, :end-line 238, :hash "-1413206900"} {:id "def/bind-fire-key", :kind "def", :line 239, :end-line 239, :hash "-86622152"} {:id "def/fire-key-includes?", :kind "def", :line 240, :end-line 240, :hash "109857506"} {:id "def/pause-key-includes?", :kind "def", :line 241, :end-line 241, :hash "-1954048295"} {:id "def/wave-banner?", :kind "def", :line 242, :end-line 242, :hash "1130113897"} {:id "def/wave-banner", :kind "def", :line 243, :end-line 243, :hash "911252070"} {:id "def/wave-banner-text", :kind "def", :line 244, :end-line 244, :hash "-1812837808"} {:id "def/wave-banner-subtitle", :kind "def", :line 245, :end-line 245, :hash "764045309"} {:id "def/wave-banner-bonus-city?", :kind "def", :line 246, :end-line 246, :hash "-1008641310"} {:id "def/wave-banner-announced-wave", :kind "def", :line 247, :end-line 247, :hash "549833261"} {:id "def/wave-banner-phase", :kind "def", :line 248, :end-line 248, :hash "-1207534753"} {:id "def/wave-banner-text-position", :kind "def", :line 249, :end-line 249, :hash "1533588800"} {:id "def/wave-banner-distance-to-center", :kind "def", :line 250, :end-line 250, :hash "368380193"} {:id "def/export-settings", :kind "def", :line 252, :end-line 252, :hash "799368708"} {:id "def/import-settings", :kind "def", :line 253, :end-line 253, :hash "1563108461"} {:id "defn/start-game", :kind "defn", :line 255, :end-line 258, :hash "-622402712"} {:id "defn/final-score", :kind "defn", :line 260, :end-line 263, :hash "-2124677376"} {:id "defn/confirm-end-screen", :kind "defn", :line 265, :end-line 271, :hash "2048563930"} {:id "defn/submit-high-score-initials", :kind "defn", :line 273, :end-line 285, :hash "-618447068"} {:id "defn/end-message", :kind "defn", :line 287, :end-line 289, :hash "1667840292"} {:id "defn/end-fireball", :kind "defn", :line 291, :end-line 293, :hash "603568745"} {:id "defn/hud", :kind "defn", :line 295, :end-line 299, :hash "-1986360486"} {:id "defn/defensive-missiles", :kind "defn", :line 301, :end-line 303, :hash "-1457861839"} {:id "defn/fireballs", :kind "defn", :line 305, :end-line 307, :hash "-47675919"} {:id "defn/enemy-missiles", :kind "defn", :line 309, :end-line 311, :hash "-1649887754"} {:id "defn/flyers", :kind "defn", :line 313, :end-line 315, :hash "-195685942"} {:id "defn/destroyable-targets", :kind "defn", :line 317, :end-line 319, :hash "-2146081921"} {:id "defn/last-enemy-fate", :kind "defn", :line 321, :end-line 323, :hash "-1164295963"} {:id "defn/city", :kind "defn", :line 325, :end-line 327, :hash "417115868"} {:id "defn/living-city?", :kind "defn", :line 329, :end-line 331, :hash "808122796"} {:id "defn/sim-time", :kind "defn", :line 333, :end-line 335, :hash "1526499425"} {:id "defn/last-applied-dt", :kind "defn", :line 337, :end-line 339, :hash "-1011651673"} {:id "defn/max-fireball-radius", :kind "defn", :line 341, :end-line 343, :hash "421742428"} {:id "def/set-battery-ammo", :kind "def", :line 345, :end-line 347, :hash "1552017328"} {:id "def/destroy-battery", :kind "def", :line 349, :end-line 351, :hash "-348104190"} {:id "def/add-destroyable-target", :kind "def", :line 353, :end-line 353, :hash "1900650860"} {:id "def/destroy-city", :kind "def", :line 355, :end-line 357, :hash "1170297697"} {:id "def/enemy-kind-ballistic", :kind "def", :line 359, :end-line 359, :hash "296076667"} {:id "def/enemy-kind-mirv", :kind "def", :line 360, :end-line 360, :hash "-1043521157"} {:id "def/enemy-kind-mirv-child", :kind "def", :line 361, :end-line 361, :hash "-2101498292"} {:id "def/enemy-kind-smart", :kind "def", :line 362, :end-line 362, :hash "22178418"} {:id "def/smart-bomb-edge-inner-factor", :kind "def", :line 363, :end-line 363, :hash "702668191"} {:id "def/smart-bomb-evade-clearance", :kind "def", :line 364, :end-line 364, :hash "-225229347"} {:id "def/mirv-parents", :kind "def", :line 366, :end-line 366, :hash "-1589899942"} {:id "def/mirv-children", :kind "def", :line 367, :end-line 367, :hash "-1014328737"} {:id "def/smart-bombs", :kind "def", :line 368, :end-line 368, :hash "-1620458263"} {:id "def/spawn-enemy-at", :kind "def", :line 370, :end-line 372, :hash "182517950"} {:id "def/spawn-enemy-targeting-city-from", :kind "def", :line 374, :end-line 376, :hash "-693853895"} {:id "def/spawn-enemy-targeting-city", :kind "def", :line 378, :end-line 380, :hash "-225813059"} {:id "def/spawn-enemy-targeting-battery-from", :kind "def", :line 382, :end-line 384, :hash "-1441168235"} {:id "def/spawn-enemy-targeting-battery", :kind "def", :line 386, :end-line 388, :hash "-2055273288"} {:id "def/spawn-enemies-targeting-distinct-cities", :kind "def", :line 390, :end-line 392, :hash "237344399"} {:id "def/spawn-mirv-targeting-city", :kind "def", :line 394, :end-line 396, :hash "-1517853975"} {:id "def/spawn-smart-bomb-targeting-city", :kind "def", :line 398, :end-line 400, :hash "-875997980"} {:id "def/with-rng-seed", :kind "def", :line 402, :end-line 404, :hash "-586988259"} {:id "def/spawn-flyer", :kind "def", :line 406, :end-line 408, :hash "262633878"} {:id "def/set-flyer-drops", :kind "def", :line 410, :end-line 412, :hash "-1444328243"} {:id "def/set-flyer-drops-toward-living-cities", :kind "def", :line 414, :end-line 416, :hash "-1372786239"} {:id "def/set-flyer-drop-targeting-city", :kind "def", :line 418, :end-line 420, :hash "-828157691"} {:id "def/flyers-of-kind", :kind "def", :line 422, :end-line 424, :hash "-156283593"} {:id "def/add-static-fireball", :kind "def", :line 426, :end-line 426, :hash "-79639594"} {:id "def/route-first-smart-bomb-through-point", :kind "def", :line 427, :end-line 427, :hash "2106244581"} {:id "def/route-smart-bomb-centered-in-fireball", :kind "def", :line 428, :end-line 428, :hash "-990696242"} {:id "def/route-smart-bomb-edge-band-in-fireball", :kind "def", :line 429, :end-line 429, :hash "508133687"} {:id "def/route-flyer-through-point", :kind "def", :line 430, :end-line 430, :hash "-1713445099"} {:id "def/route-enemy-through-point", :kind "def", :line 431, :end-line 431, :hash "1422275024"} {:id "def/route-first-mirv-child-through-point", :kind "def", :line 432, :end-line 432, :hash "-1540414387"} {:id "defn-/fire-battery", :kind "defn-", :line 434, :end-line 450, :hash "1591374319"} {:id "defn-/aim", :kind "defn-", :line 452, :end-line 458, :hash "242968114"} {:id "defn/click-zone", :kind "defn", :line 460, :end-line 463, :hash "943238228"} {:id "defn/click-fallback-order", :kind "defn", :line 465, :end-line 468, :hash "-1791151582"} {:id "defn-/click-fire", :kind "defn-", :line 470, :end-line 480, :hash "1162408842"} {:id "defn-/click-noop-shell?", :kind "defn-", :line 482, :end-line 488, :hash "626310851"} {:id "defn-/handle-click", :kind "defn-", :line 490, :end-line 496, :hash "987185200"} {:id "defn-/unsupported-command", :kind "defn-", :line 498, :end-line 501, :hash "585518571"} {:id "def/command-handlers", :kind "def", :line 503, :end-line 527, :hash "-183840043"} {:id "defn/handle", :kind "defn", :line 529, :end-line 536, :hash "-1206972263"} {:id "defn/press-key", :kind "defn", :line 537, :end-line 540, :hash "1450190204"} {:id "def/set-bonus-city-threshold", :kind "def", :line 542, :end-line 544, :hash "2084873560"} {:id "def/set-bonus-city-reserve", :kind "def", :line 545, :end-line 547, :hash "349676912"} {:id "def/apply-bonus-cities-from-reserve", :kind "def", :line 548, :end-line 548, :hash "-849731520"} {:id "defn-/make-end-fireball", :kind "defn-", :line 550, :end-line 556, :hash "-918471848"} {:id "defn-/update-end-message-reveal", :kind "defn-", :line 558, :end-line 561, :hash "1654790648"} {:id "defn-/enter-the-end", :kind "defn-", :line 563, :end-line 576, :hash "-68727569"} {:id "defn/evaluate-game-over", :kind "defn", :line 578, :end-line 586, :hash "1952984408"} {:id "defn/end-fireball-centered?", :kind "defn", :line 588, :end-line 592, :hash "-1673190026"} {:id "defn/end-fireball-fills-playfield?", :kind "defn", :line 594, :end-line 596, :hash "496208359"} {:id "defn/end-message-layout", :kind "defn", :line 598, :end-line 601, :hash "-2147217889"} {:id "defn/end-message-fills-max-expanse?", :kind "defn", :line 603, :end-line 605, :hash "1595237781"} {:id "defn/end-message-centered?", :kind "defn", :line 607, :end-line 611, :hash "-447969280"} {:id "defn/end-message-visibility-clipped?", :kind "defn", :line 613, :end-line 616, :hash "-1616346772"} {:id "defn/end-message-point-visible?", :kind "defn", :line 618, :end-line 621, :hash "-703026949"} {:id "defn/end-message-reveal", :kind "defn", :line 623, :end-line 626, :hash "-172598567"} {:id "defn-/tick-end-fireball", :kind "defn-", :line 628, :end-line 638, :hash "473583625"} {:id "defn-/add-score", :kind "defn-", :line 640, :end-line 644, :hash "-765578423"} {:id "def/set-score", :kind "def", :line 646, :end-line 648, :hash "-1968310892"} {:id "defn-/maybe-complete-wave", :kind "defn-", :line 650, :end-line 661, :hash "-1564082991"} {:id "defn/rearm-surviving-batteries", :kind "defn", :line 664, :end-line 668, :hash "361044815"} {:id "def/spawn-wave-enemy-targeting-battery", :kind "def", :line 670, :end-line 672, :hash "-2053584172"} {:id "def/set-wave-enemies-active", :kind "def", :line 674, :end-line 676, :hash "280705968"} {:id "def/default-mirv-child-count", :kind "def", :line 679, :end-line 679, :hash "37539110"} {:id "def/default-mirv-split-progress", :kind "def", :line 680, :end-line 680, :hash "-1830047123"} {:id "def/default-flyer-speed", :kind "def", :line 681, :end-line 681, :hash "974542101"} {:id "def/default-flyer-altitude-fraction", :kind "def", :line 682, :end-line 682, :hash "-78571882"} {:id "def/default-flyer-drop-count", :kind "def", :line 683, :end-line 683, :hash "1025270707"} {:id "def/default-flyer-drop-progress-start", :kind "def", :line 684, :end-line 684, :hash "2130099899"} {:id "def/default-flyer-drop-progress-end", :kind "def", :line 685, :end-line 685, :hash "-570587514"} {:id "defn-/wave-schedule-hooks", :kind "defn-", :line 687, :end-line 698, :hash "-256215380"} {:id "def/begin-wave-attack", :kind "def", :line 700, :end-line 702, :hash "-1510525227"} {:id "def/start-wave-attack", :kind "def", :line 704, :end-line 706, :hash "1691837253"} {:id "def/activate-wave-schedule", :kind "def", :line 708, :end-line 710, :hash "1151720027"} {:id "defn-/maybe-advance-wave-attack", :kind "defn-", :line 712, :end-line 715, :hash "-257542288"} {:id "defn-/ensure-wave-attack-started", :kind "defn-", :line 717, :end-line 720, :hash "905838658"} {:id "def/set-non-destroyed-battery-ammo", :kind "def", :line 722, :end-line 724, :hash "-282673075"} {:id "def/wave-schedule-metrics", :kind "def", :line 726, :end-line 726, :hash "-426249483"} {:id "def/wave-schedule-metrics-for", :kind "def", :line 727, :end-line 727, :hash "-2119268773"} {:id "def/wave-mirv-count", :kind "def", :line 729, :end-line 729, :hash "746006295"} {:id "def/wave-smart-bomb-count", :kind "def", :line 730, :end-line 730, :hash "-664512608"} {:id "def/wave-bomber-count", :kind "def", :line 731, :end-line 731, :hash "1023037642"} {:id "def/wave-satellite-count", :kind "def", :line 732, :end-line 732, :hash "-1625262900"} {:id "def/harder-wave?", :kind "def", :line 733, :end-line 733, :hash "-498526476"} {:id "def/set-wave", :kind "def", :line 734, :end-line 736, :hash "520461696"} {:id "def/start-next-wave", :kind "def", :line 738, :end-line 740, :hash "-1430135305"} {:id "defn-/advance-clock", :kind "defn-", :line 742, :end-line 746, :hash "2082435033"} {:id "defn/tick", :kind "defn", :line 748, :end-line 788, :hash "1482130198"}]}
;; clj-mutate-manifest-end

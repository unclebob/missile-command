(ns missile-command.jvm.sketch
  "Quil sketch: route mouse/keyboard UI events into pure core commands."
  (:require [quil.core :as q]
            [quil.middleware :as m]
            [quil.applet :as applet]
            [clojure.string :as str]
            [missile-command.core :as core]
            [missile-command.missiles :as missiles]
            [missile-command.jvm.input :as input]
            [missile-command.jvm.persist :as persist]
            [missile-command.jvm.render :as render]
            [missile-command.jvm.window :as window]))

(def default-width 800)
(def default-height 600)

(defonce launch-options
  (atom {:qa-telemetry? false
         :qa-speed 1.0
         :destroy-batteries []
         :qa-events nil
         :qa-scenario nil
         :qa-targets []
         :qa-enemies []
         :qa-fireballs []
         :scores-file nil
         :launch-anchor nil
         :restore-focus-app nil}))

(defonce pending-qa-events (atom []))
(defonce fireball-phases (atom {}))
(defonce last-frame-ms (atom nil))
(defonce initials-draft (atom ""))
(defonce sfx-emitted-count (atom 0))

(defn configure!
  [opts]
  (reset! launch-options (select-keys opts [:qa-telemetry?
                                            :qa-speed
                                            :destroy-batteries
                                            :qa-events
                                            :qa-scenario
                                            :qa-targets
                                            :qa-enemies
                                            :qa-fireballs
                                            :scores-file
                                            :launch-anchor
                                            :restore-focus-app]))
  (reset! pending-qa-events
          (if-let [path (:qa-events opts)]
            (input/load-qa-events path)
            []))
  (reset! fireball-phases {})
  (reset! last-frame-ms nil)
  (reset! initials-draft "")
  (reset! sfx-emitted-count 0))

(defn- settings-path
  "QA --scores-file overrides host settings path; else MC_SETTINGS_PATH/default."
  []
  (or (:scores-file @launch-options)
      (persist/default-settings-path)))

(defn- emit!
  [line]
  (when (:qa-telemetry? @launch-options)
    (println line)
    (flush)))

(defn- emit-sim!
  [state]
  (emit! (input/format-sim-telemetry-line
          (assoc state :initials-draft @initials-draft))))

(defn- persist-settings!
  [state]
  (try
    (persist/save-settings! state (settings-path))
    (catch Exception e
      (binding [*out* *err*]
        (println "settings save failed:" (.getMessage e)))))
  state)

(defn- load-persisted
  [state]
  (persist/load-into state (settings-path)))

(defn- emit-telemetry-fire!
  [result]
  (emit! (input/format-telemetry-line result)))

(defn- emit-fireball-phases!
  [state]
  (let [[events next-map] (input/detect-fireball-phase-events
                           @fireball-phases
                           (core/fireballs state))]
    (reset! fireball-phases next-map)
    (doseq [e events]
      (emit! (input/format-fireball-phase-line state (:fireball e) (:phase e))))))

(defn- emit-new-sfx!
  "Emit qa-sfx lines for newly logged core events; honor mute for played=."
  [prev-state state]
  (let [prev (count (or (:sfx-events prev-state) []))
        all (or (:sfx-events state) [])
        fresh (drop prev all)
        muted? (core/mute? state)]
    (doseq [e fresh]
      (let [kw (:type e)
            t (if (namespace kw)
                (str (namespace kw) "/" (name kw))
                (name kw))]
        (emit! (str "qa-sfx type=" t
                    " played=" (if muted? "false" "true")
                    " mute=" muted?))))
    (reset! sfx-emitted-count (count all))))

(defn- apply-handle
  [state command]
  (let [result (core/handle state command)
        state' (:state result)]
    (when (#{:fire :click :key} (:type command))
      (emit-telemetry-fire! result))
    (emit-new-sfx! state state')
    (when (and (= :submit-high-score (:type command))
               (not (core/high-score-entry? state'))
               (core/high-score-entry? state))
      (reset! initials-draft "")
      (persist-settings! state'))
    (when (#{:set-mute :set-difficulty :bind-fire-key :leave-options} (:type command))
      (persist-settings! state'))
    state'))

(defn- apply-destroy-options
  [state]
  (reduce (fn [s id]
            (core/destroy-battery s id))
          state
          (:destroy-batteries @launch-options)))

(defn- apply-qa-targets
  [state]
  (reduce (fn [s {:keys [x y]}]
            (core/add-destroyable-target s x y))
          state
          (:qa-targets @launch-options)))

(defn- apply-enemy-spec
  [state {:keys [kind id]}]
  (case kind
    :city (core/spawn-enemy-targeting-city state id)
    :battery (core/spawn-enemy-targeting-battery state id)
    state))

(defn- apply-qa-enemies
  [state]
  (reduce apply-enemy-spec state (:qa-enemies @launch-options)))

(defn- apply-qa-fireballs
  [state]
  (reduce (fn [s {:keys [x y radius]}]
            (core/add-static-fireball s x y radius))
          state
          (:qa-fireballs @launch-options)))

(defn- configure-display!
  []
  (try
    (let [surface (.getSurface (applet/current-applet))
          anchor (:launch-anchor @launch-options)
          prev (:restore-focus-app @launch-options)]
      (window/place-on-launch-screen! surface (q/width) (q/height) anchor prev))
    (catch Exception e
      (binding [*out* *err*]
        (println "window placement skipped:" (.getMessage e))))))

(defn- apply-qa-scenario
  [state]
  (if-let [path (:qa-scenario @launch-options)]
    (input/apply-scenario state (input/load-scenario-edn path))
    state))

(defn- spawn-scheduled-wave-enemies
  "Launch the current wave's scheduled enemy count (cities only)."
  [state]
  (let [n (long (:enemy-count (core/wave-schedule-metrics-for state (core/wave state))))]
    (if (pos? n)
      (core/set-wave-enemies-active state n)
      state)))

(defn- ensure-wave-enemies
  "If no enemies are in flight, spawn this wave's schedule (normal play)."
  [state]
  (if (seq (core/enemy-missiles state))
    state
    (spawn-scheduled-wave-enemies state)))

(defn setup
  []
  (q/frame-rate 60)
  (q/no-cursor)
  (configure-display!)
  (reset! last-frame-ms (System/currentTimeMillis))
  (let [state (-> (core/new-game {:width (q/width) :height (q/height)})
                  load-persisted
                  apply-destroy-options
                  apply-qa-scenario
                  apply-qa-targets
                  apply-qa-enemies
                  apply-qa-fireballs
                  ;; Scenario may stage zero cities → enter THE END even from title.
                  core/evaluate-game-over)
        ;; Only spawn wave attacks when already playing (not on title/end).
        state (if (core/playing? state)
                (ensure-wave-enemies state)
                state)]
    (when (:qa-telemetry? @launch-options)
      (emit-sim! state)
      ;; Surface SFX already logged during setup (e.g. THE END on zero cities).
      (emit-new-sfx! (assoc state :sfx-events []) state))
    state))

(defn- frame-dt-seconds
  "Wall-clock seconds since last frame, clamped. Multiplied by :qa-speed."
  []
  (let [now (System/currentTimeMillis)
        prev (or @last-frame-ms now)
        raw (/ (double (- now prev)) 1000.0)
        wall (max 0.0 (min raw 0.25))
        speed (double (or (:qa-speed @launch-options) 1.0))]
    (reset! last-frame-ms now)
    (* wall speed)))

(defn- advance-one-step
  "One core tick + rearm/spawn on wave complete. Returns [state completed?]."
  [state dt]
  (let [result (core/tick state dt)
        state' (:state result)
        completed? (and (core/wave-complete? state')
                        (not (core/wave-complete? state)))
        ;; Continuous play: rearm survivors and launch next wave attacks.
        state' (if completed?
                 (-> state'
                     core/start-next-wave
                     ensure-wave-enemies)
                 state')]
    [state' completed?]))

(defn- tick-state
  "Advance sim by wall-dt * qa-speed, substepping at missiles/max-dt."
  [state]
  (let [budget (frame-dt-seconds)
        wave-before (core/wave state)
        screen-before (core/screen state)
        step-max (double missiles/max-dt)
        [state' completed-any?]
        (loop [s state
               remaining budget
               completed-any? false]
          (if (<= remaining 1.0e-12)
            [s completed-any?]
            (let [step (min remaining step-max)
                  [s' completed?] (advance-one-step s step)]
              (recur s' (- remaining step) (or completed-any? completed?)))))
        active? (or (seq (core/fireballs state'))
                    (seq (core/enemy-missiles state'))
                    (seq (core/destroyable-targets state')))
        wave-changed? (not= wave-before (core/wave state'))
        screen-changed? (not= screen-before (core/screen state'))
        ;; THE END screen-fill fireball lives on :end-fireball, not :fireballs.
        end-sequence? (core/the-end? state')]
    (emit-fireball-phases! state')
    (emit-new-sfx! state state')
    (when (and (:qa-telemetry? @launch-options)
               (or active? completed-any? wave-changed? end-sequence?
                   screen-changed?))
      (emit-sim! state'))
    (when (and (:qa-telemetry? @launch-options)
               (core/last-enemy-fate state')
               (empty? (core/enemy-missiles state'))
               (empty? (core/fireballs state'))
               (not= (:last-emitted-fate @launch-options)
                     (core/last-enemy-fate state')))
      (emit-sim! state')
      (swap! launch-options assoc :last-emitted-fate (core/last-enemy-fate state')))
    state'))

(defn- toggle-pause
  "Pause while playing, resume while paused; otherwise leave state alone."
  [state]
  (cond
    (core/playing? state) (apply-handle state {:type :pause})
    (core/paused? state) (apply-handle state {:type :resume})
    :else state))

(defn- drain-one-qa-event
  [state]
  (let [events @pending-qa-events]
    (if (empty? events)
      state
      (let [ev (first events)]
        (case (:type ev)
          :wait
          (let [until (or (:until-ms ev)
                          (+ (System/currentTimeMillis)
                             (long (* 1000.0 (double (:seconds ev))))))]
            (if (nil? (:until-ms ev))
              (do
                (reset! pending-qa-events
                        (vec (cons (assoc ev :until-ms until) (rest events))))
                state)
              (if (>= (System/currentTimeMillis) until)
                (do (reset! pending-qa-events (vec (rest events))) state)
                state)))

          :quit
          (do (reset! pending-qa-events [])
              (persist-settings! state)
              (q/exit)
              state)

          (do
            (reset! pending-qa-events (vec (rest events)))
            (case (:type ev)
              :click (let [s (apply-handle state (input/click-command (:x ev) (:y ev)))
                           s (if (and (core/playing? s) (not (core/playing? state)))
                               (-> s apply-destroy-options ensure-wave-enemies)
                               s)]
                       (when (and (:qa-telemetry? @launch-options)
                                  (not= (core/screen state) (core/screen s)))
                         (emit-sim! s))
                       s)
              :aim (apply-handle state (input/aim-command (:x ev) (:y ev)))
              :start (let [s (apply-handle state {:type :start})
                           s (cond-> s
                               (core/playing? s) apply-destroy-options
                               ;; Skip auto wave if QA will stage enemies next.
                               (and (core/playing? s)
                                    (empty? (:qa-enemies @launch-options))
                                    (not-any? #(= :enemy (:type %))
                                              @pending-qa-events))
                               ensure-wave-enemies)]
                       (when (:qa-telemetry? @launch-options)
                         (emit-sim! s))
                       s)
              :confirm (let [s (apply-handle state {:type :confirm})]
                         (when (:qa-telemetry? @launch-options)
                           (emit-sim! s))
                         s)
              :pause (let [s (apply-handle state {:type :pause})]
                       (when (:qa-telemetry? @launch-options)
                         (emit-sim! s))
                       s)
              :resume (let [s (apply-handle state {:type :resume})]
                        (when (:qa-telemetry? @launch-options)
                          (emit-sim! s))
                        s)
              :open-high-scores
              (let [s (apply-handle state {:type :open-high-scores})]
                (when (:qa-telemetry? @launch-options) (emit-sim! s))
                s)
              :close-high-scores
              (let [s (apply-handle state {:type :close-high-scores})]
                (when (:qa-telemetry? @launch-options) (emit-sim! s))
                s)
              :submit-high-score
              (let [s (apply-handle state {:type :submit-high-score
                                           :initials (:initials ev)})]
                (when (:qa-telemetry? @launch-options) (emit-sim! s))
                s)
              :open-options
              (let [s (apply-handle state {:type :open-options})]
                (when (:qa-telemetry? @launch-options) (emit-sim! s))
                s)
              :leave-options
              (let [s (apply-handle state {:type :leave-options})]
                (when (:qa-telemetry? @launch-options) (emit-sim! s))
                s)
              :set-mute
              (let [s (apply-handle state {:type :set-mute :mute (:mute ev)})]
                (when (:qa-telemetry? @launch-options) (emit-sim! s))
                s)
              :set-difficulty
              (let [s (apply-handle state {:type :set-difficulty
                                           :difficulty (:difficulty ev)})]
                (when (:qa-telemetry? @launch-options) (emit-sim! s))
                s)
              :bind-fire-key
              (let [s (apply-handle state {:type :bind-fire-key
                                           :battery (:battery ev)
                                           :key (:key ev)})]
                (when (:qa-telemetry? @launch-options) (emit-sim! s))
                s)
              :key (let [ch (:ch ev)
                         key-name (str/lower-case (str ch))
                         s (apply-handle state {:type :key :key key-name})
                         fired? (not= (count (core/defensive-missiles s))
                                      (count (core/defensive-missiles state)))]
                     (cond
                       fired? s
                       (or (= \p ch) (= \P ch)
                           (core/pause-key-includes? state key-name))
                       (toggle-pause state)
                       (or (= \o ch) (= \O ch))
                       (cond
                         (core/title? state)
                         (let [s2 (apply-handle state {:type :open-options})]
                           (when (:qa-telemetry? @launch-options) (emit-sim! s2))
                           s2)
                         (core/options? state)
                         (let [s2 (apply-handle state {:type :leave-options})]
                           (when (:qa-telemetry? @launch-options) (emit-sim! s2))
                           s2)
                         :else state)
                       (or (= \h ch) (= \H ch))
                       (cond
                         (core/title? state)
                         (let [s2 (apply-handle state {:type :open-high-scores})]
                           (when (:qa-telemetry? @launch-options) (emit-sim! s2))
                           s2)
                         (core/high-scores-view? state)
                         (let [s2 (apply-handle state {:type :close-high-scores})]
                           (when (:qa-telemetry? @launch-options) (emit-sim! s2))
                           s2)
                         :else state)
                       (or (= \newline ch) (= \return ch))
                       (cond
                         (core/title? state)
                         (ensure-wave-enemies (apply-handle state {:type :start}))
                         (core/the-end? state) (apply-handle state {:type :confirm})
                         (core/high-score-entry? state)
                         (let [draft @initials-draft
                               s2 (if (seq draft)
                                    (apply-handle state {:type :submit-high-score
                                                         :initials draft})
                                    state)]
                           (when (:qa-telemetry? @launch-options) (emit-sim! s2))
                           s2)
                         :else state)
                       :else s))
              :enemy (apply-enemy-spec state (:spec ev))
              :fireball (let [{:keys [x y radius]} (:spec ev)]
                          (core/add-static-fireball state x y radius))
              state)))))))

(defn update-state
  [state]
  (let [scripted? (seq @pending-qa-events)
        state (-> state
                  (input/resize-if-needed (q/width) (q/height)
                                          core/resize core/playfield-width core/playfield-height)
                  tick-state)]
    (if scripted?
      (drain-one-qa-event state)
      (-> state
          (as-> s (apply-handle s (input/aim-command (q/mouse-x) (q/mouse-y))))
          drain-one-qa-event))))

(defn draw
  [state]
  (render/draw-world! state @initials-draft)
  (render/crosshair-at! (q/mouse-x) (q/mouse-y)))

(defn mouse-moved
  [state _event]
  (apply-handle state (input/aim-command (q/mouse-x) (q/mouse-y))))

(defn mouse-dragged
  [state event]
  (mouse-moved state event))

(defn- left-button?
  [event]
  (let [b (or (:button event) (q/mouse-button))]
    (or (nil? b) (= b :left) (= b 37) (= (str b) "left"))))

(defn mouse-pressed
  [state event]
  (if (left-button? event)
    (apply-handle state (input/click-command (q/mouse-x) (q/mouse-y)))
    state))

(defn- initials-char?
  [ch]
  (and ch (re-matches #"[A-Za-z0-9]" (str ch))))

(defn- append-initials-draft!
  [ch]
  (let [c (str/upper-case (str ch))
        cur @initials-draft]
    (when (< (count cur) 3)
      (reset! initials-draft (str cur c)))))

(defn key-pressed
  [state _event]
  (let [ch (q/raw-key)
        key-name (when ch (str/lower-case (str ch)))]
    (cond
      (input/escape-key? ch)
      (cond
        (or (core/playing? state) (core/paused? state))
        (toggle-pause state)
        (core/high-scores-view? state)
        (apply-handle state {:type :close-high-scores})
        (core/options? state)
        (apply-handle state {:type :leave-options})
        (core/high-score-entry? state)
        state
        :else
        (do (persist-settings! state) (q/exit) state))

      (and (core/options? state) (or (= \m ch) (= \M ch)))
      (apply-handle state {:type :set-mute :mute (not (core/mute? state))})

      (and (core/options? state) (= \1 ch))
      (apply-handle state {:type :set-difficulty :difficulty "easy"})

      (and (core/options? state) (= \2 ch))
      (apply-handle state {:type :set-difficulty :difficulty "normal"})

      (and (core/options? state) (= \3 ch))
      (apply-handle state {:type :set-difficulty :difficulty "arcade"})

      (or (= \o ch) (= \O ch))
      (cond
        (core/title? state) (apply-handle state {:type :open-options})
        (core/options? state) (apply-handle state {:type :leave-options})
        :else state)

      (or (= \p ch) (= \P ch)
          (and key-name (core/pause-key-includes? state key-name)))
      (toggle-pause state)

      (or (= \h ch) (= \H ch))
      (cond
        (core/title? state)
        (apply-handle state {:type :open-high-scores})
        (core/high-scores-view? state)
        (apply-handle state {:type :close-high-scores})
        :else state)

      (or (= \newline ch) (= \return ch) (= (int 10) (int ch)))
      (cond
        (core/title? state)
        (ensure-wave-enemies (apply-handle state {:type :start}))
        (core/the-end? state) (apply-handle state {:type :confirm})
        (core/high-score-entry? state)
        (let [draft @initials-draft]
          (if (seq draft)
            (apply-handle state {:type :submit-high-score :initials draft})
            state))
        :else state)

      (and (core/high-score-entry? state) (initials-char? ch))
      (do (append-initials-draft! ch) state)

      (and (core/high-score-entry? state)
           (or (= \backspace ch) (= (char 8) ch)))
      (do (reset! initials-draft
                  (let [d @initials-draft]
                    (if (seq d) (subs d 0 (dec (count d))) d)))
          state)

      (and key-name (core/playing? state))
      (apply-handle state {:type :key :key key-name})

      :else state)))

(defn run-sketch!
  ([]
   (run-sketch! default-width default-height))
  ([width height]
   (q/sketch
    :title "Missile Command"
    :size [width height]
    ;; java2d → AWT Frame so setAutoRequestFocus(false) can prevent focus steal
    :renderer :java2d
    :setup setup
    :update update-state
    :draw draw
    :mouse-moved mouse-moved
    :mouse-dragged mouse-dragged
    :mouse-pressed mouse-pressed
    :key-pressed key-pressed
    :middleware [m/fun-mode]
    :features [:resizable]
    :settings #(q/smooth 2))))

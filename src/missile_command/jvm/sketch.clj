(ns missile-command.jvm.sketch
  "Quil sketch: route mouse/keyboard UI events into pure core commands."
  (:require [quil.core :as q]
            [quil.middleware :as m]
            [quil.applet :as applet]
            [clojure.string :as str]
            [missile-command.core :as core]
            [missile-command.host-input :as host-input]
            [missile-command.missiles :as missiles]
            [missile-command.testing :as testing]
            [missile-command.jvm.input :as input]
            [missile-command.jvm.audio :as audio]
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
         :no-keyfocus? false
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
                                            :no-keyfocus?
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

(defn- no-keyfocus-qa?
  []
  (and (:qa-telemetry? @launch-options)
       (:no-keyfocus? @launch-options)))

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
  "Play new SFX clips and emit qa-sfx lines; honor mute for playback.
  Uses sfx-take-new (cursor = previous log length)."
  [prev-state state]
  (when (and (core/title? prev-state) (core/playing? state))
    (audio/stop-title!))
  (let [prev (count (core/sfx-events prev-state))
        fresh (core/sfx-take-new state prev)
        muted? (core/mute? state)]
    (audio/play-events! fresh muted?)
    (doseq [e fresh]
      (let [kw (:type e)
            t (if (namespace kw)
                (str (namespace kw) "/" (name kw))
                (name kw))]
        (emit! (str "qa-sfx type=" t
                    " played=" (if muted? "false" "true")
                    " mute=" muted?))))
    (reset! sfx-emitted-count (count (core/sfx-events state)))))

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
            (testing/destroy-battery s id))
          state
          (:destroy-batteries @launch-options)))

(defn- apply-qa-targets
  [state]
  (reduce (fn [s {:keys [x y]}]
            (testing/add-destroyable-target s x y))
          state
          (:qa-targets @launch-options)))

(defn- apply-enemy-spec
  [state {:keys [kind id]}]
  (case kind
    :city (testing/spawn-enemy-targeting-city state id)
    :battery (testing/spawn-enemy-targeting-battery state id)
    state))

(defn- apply-qa-enemies
  [state]
  (reduce apply-enemy-spec state (:qa-enemies @launch-options)))

(defn- apply-qa-fireballs
  [state]
  (reduce (fn [s {:keys [x y radius]}]
            (testing/add-static-fireball s x y radius))
          state
          (:qa-fireballs @launch-options)))

(defn- configure-display!
  []
  (try
    (let [surface (.getSurface (applet/current-applet))
          anchor (:launch-anchor @launch-options)
          prev (:restore-focus-app @launch-options)
          no-keyfocus? (and (:qa-telemetry? @launch-options)
                            (:no-keyfocus? @launch-options))]
      (window/place-on-launch-screen! surface (q/width) (q/height) anchor prev no-keyfocus?))
    (catch Exception e
      (binding [*out* *err*]
        (println "window placement skipped:" (.getMessage e))))))

(defn- apply-qa-scenario
  [state]
  (if-let [path (:qa-scenario @launch-options)]
    (input/apply-scenario state (input/load-scenario-edn path))
    state))

(defn setup
  []
  (q/frame-rate 60)
  (q/no-cursor)
  (configure-display!)
  (audio/warm!)
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
        ]
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
  "One core tick. Core starts attack 1 when the sky is empty."
  [state dt]
  (let [was-banner? (core/wave-banner? state)
        result (core/tick state dt)
        state' (:state result)
        banner-finished? (and was-banner? (core/playing? state'))]
    [state' banner-finished?]))

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

(defn- apply-input-intent
  [state intent]
  (cond
    (:command intent)
    (let [state' (apply-handle state (:command intent))]
      (when-let [draft (:draft intent)]
        (reset! initials-draft draft))
      state')

    (contains? intent :draft)
    (do (reset! initials-draft (:draft intent)) state)

    :else state))

(defn- normalized-key-event
  ([ch]
   (normalized-key-event ch false false))
  ([ch enter?]
   (normalized-key-event ch enter? false))
  ([ch enter? backspace?]
   {:ch ch
    :key-name (when ch (str/lower-case (str ch)))
    :escape? (input/escape-key? ch)
    :enter? enter?
    :backspace? backspace?}))

(defn- toggle-pause
  "Pause while playing, resume while paused; otherwise leave state alone."
  [state]
  (apply-input-intent state (host-input/key-intent state @initials-draft
                                                  {:ch \p :key-name "p"})))

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
                               (apply-destroy-options s)
                               s)]
                       (when (and (:qa-telemetry? @launch-options)
                                  (not= (core/screen state) (core/screen s)))
                         (emit-sim! s))
                       s)
              :aim (apply-handle state (input/aim-command (:x ev) (:y ev)))
              :start (let [s (apply-handle state {:type :start})
                           s (cond-> s
                               (core/playing? s) apply-destroy-options)]
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
                         (apply-handle state {:type :start})
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
                          (testing/add-static-fireball state x y radius))
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
  (if (no-keyfocus-qa?)
    state
    (apply-handle state (input/aim-command (q/mouse-x) (q/mouse-y)))))

(defn mouse-dragged
  [state event]
  (mouse-moved state event))

(defn- left-button?
  [event]
  (let [b (or (:button event) (q/mouse-button))]
    (or (nil? b) (= b :left) (= b 37) (= (str b) "left"))))

(defn mouse-pressed
  [state event]
  (if (or (no-keyfocus-qa?)
          (not (left-button? event)))
    state
    (apply-handle state (input/click-command (q/mouse-x) (q/mouse-y)))))

(defn key-pressed
  [state _event]
  (if (no-keyfocus-qa?)
    state
    (let [ch (q/raw-key)
          enter? (or (= \newline ch) (= \return ch) (= (int 10) (int ch)))
          backspace? (or (= \backspace ch) (= (char 8) ch))
          event (normalized-key-event ch enter? backspace?)
          intent (host-input/key-intent state @initials-draft event)]
      (if intent
        (apply-input-intent state intent)
        (if (:escape? event)
          (do (persist-settings! state) (q/exit) state)
          state)))))

(defn run-sketch!
  ([]
   (run-sketch! default-width default-height))
  ([width height]
   (let [opts [:title "Missile Command"
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
               :settings #(q/smooth 2)]
         no-keyfocus? (and (:qa-telemetry? @launch-options)
                           (:no-keyfocus? @launch-options))]
     (if no-keyfocus?
       (with-redefs-fn {(intern 'quil.applet '-showSurface)
                        (fn [this]
                          (window/show-non-focusable-surface! (.getSurface this)))}
         #(apply q/sketch opts))
       (apply q/sketch opts)))))

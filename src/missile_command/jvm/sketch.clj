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
            [missile-command.jvm.frame :as frame]
            [missile-command.jvm.persist :as persist]
            [missile-command.jvm.qa-runner :as qa-runner]
            [missile-command.jvm.render :as render]
            [missile-command.jvm.telemetry-emitter :as telemetry-emitter]
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

(defn- no-keyfocus-qa?
  []
  (and (:qa-telemetry? @launch-options)
       (:no-keyfocus? @launch-options)))

(defn- telemetry-context
  []
  {:launch-options launch-options
   :initials-draft initials-draft
   :fireball-phases fireball-phases
   :sfx-emitted-count sfx-emitted-count
   :stop-title! audio/stop-title!
   :play-events! audio/play-events!})

(defn- emit-sim!
  [state]
  (telemetry-emitter/emit-sim! (telemetry-context) state))

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
  (telemetry-emitter/emit-telemetry-fire! (telemetry-context) result))

(defn- emit-fireball-phases!
  [state]
  (telemetry-emitter/emit-fireball-phases! (telemetry-context) state))

(defn- emit-new-sfx!
  [prev-state state]
  (telemetry-emitter/emit-new-sfx! (telemetry-context) prev-state state))

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
  []
  (frame/next-dt! last-frame-ms @launch-options))

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
        [state' completed-any?] (frame/advance-substeps state budget missiles/max-dt advance-one-step)
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
  (qa-runner/drain-one-event
   {:pending-events pending-qa-events
    :now-ms #(System/currentTimeMillis)
    :qa-telemetry? (:qa-telemetry? @launch-options)
    :emit-sim! emit-sim!
    :persist-settings! persist-settings!
    :exit! q/exit
    :apply-handle apply-handle
    :apply-destroy-options apply-destroy-options
    :apply-enemy-spec apply-enemy-spec
    :toggle-pause toggle-pause
    :initials-draft initials-draft}
   state))

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

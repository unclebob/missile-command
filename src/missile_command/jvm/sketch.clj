(ns missile-command.jvm.sketch
  "Quil sketch: route mouse/keyboard UI events into pure core commands."
  (:require [quil.core :as q]
            [quil.middleware :as m]
            [quil.applet :as applet]
            [clojure.string :as str]
            [missile-command.core :as core]
            [missile-command.host-input :as host-input]
            [missile-command.missiles :as missiles]
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

(defn- telemetry-context
  []
  {:launch-options launch-options
   :initials-draft initials-draft
   :fireball-phases fireball-phases
   :sfx-emitted-count sfx-emitted-count
   :stop-title! audio/stop-title!
   :play-events! audio/play-events!})

(defn- no-keyfocus-qa?
  []
  (and (:qa-telemetry? @launch-options)
       (:no-keyfocus? @launch-options)))

(defn- real-input-enabled?
  []
  (not (no-keyfocus-qa?)))

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
  (input/apply-destroy-batteries state (:destroy-batteries @launch-options)))

(defn- apply-qa-targets
  [state]
  (input/apply-qa-targets state (:qa-targets @launch-options)))

(defn- apply-enemy-spec
  [state spec]
  (input/apply-enemy-spec state spec))

(defn- apply-qa-enemies
  [state]
  (input/apply-qa-enemies state (:qa-enemies @launch-options)))

(defn- apply-qa-fireballs
  [state]
  (input/apply-qa-fireballs state (:qa-fireballs @launch-options)))

(defn- configure-display!
  []
  (try
    (let [surface (.getSurface (applet/current-applet))
          anchor (:launch-anchor @launch-options)
          prev (:restore-focus-app @launch-options)]
      (window/place-on-launch-screen! surface (q/width) (q/height) anchor prev (no-keyfocus-qa?)))
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
    :now-ns #(System/nanoTime)
    :qa-telemetry? (:qa-telemetry? @launch-options)
    :emit-sim! emit-sim!
    :persist-settings! persist-settings!
    :exit! q/exit
    :apply-handle apply-handle
    :apply-destroy-options apply-destroy-options
    :apply-enemy-spec apply-enemy-spec
    :apply-qa-fireballs apply-qa-fireballs
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
      (cond-> state
        (real-input-enabled?)
        (apply-handle (input/aim-command (q/mouse-x) (q/mouse-y)))

        true
        drain-one-qa-event))))
(defn draw
  [state]
  (render/draw-world! state @initials-draft)
  (render/crosshair-at! (q/mouse-x) (q/mouse-y)))

(defn mouse-moved
  [state _event]
  (if (real-input-enabled?)
    (apply-handle state (input/aim-command (q/mouse-x) (q/mouse-y)))
    state))

(defn mouse-dragged
  [state event]
  (mouse-moved state event))

(defn- left-button?
  [event]
  (let [b (or (:button event) (q/mouse-button))]
    (or (nil? b) (= b :left) (= b 37) (= (str b) "left"))))

(defn mouse-pressed
  [state event]
  (if (and (real-input-enabled?)
           (left-button? event))
    (apply-handle state (input/click-command (q/mouse-x) (q/mouse-y)))
    state))

(defn key-pressed
  [state _event]
  (if (real-input-enabled?)
    (let [ch (q/raw-key)
          enter? (or (= \newline ch) (= \return ch) (= (int 10) (int ch)))
          backspace? (or (= \backspace ch) (= (char 8) ch))
          event (normalized-key-event ch enter? backspace?)
          intent (host-input/key-intent state @initials-draft event)]
      (if intent
        (apply-input-intent state intent)
        (if (:escape? event)
          (do (persist-settings! state) (q/exit) state)
          state)))
    state))

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
         no-keyfocus? (no-keyfocus-qa?)]
     (if no-keyfocus?
       (with-redefs-fn {(intern 'quil.applet '-showSurface)
                        (fn [this]
                          (window/show-non-focusable-surface! (.getSurface this)))}
         #(apply q/sketch opts))
       (apply q/sketch opts)))))

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-07-26T10:19:10.415433-05:00", :module-hash "-2118659332", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 13, :hash "-1263449719"} {:id "def/default-width", :kind "def", :line 15, :end-line 15, :hash "1515114879"} {:id "def/default-height", :kind "def", :line 16, :end-line 16, :hash "1673066894"} {:id "form/3/defonce", :kind "defonce", :line 18, :end-line 30, :hash "-1716031864"} {:id "form/4/defonce", :kind "defonce", :line 32, :end-line 32, :hash "1002191581"} {:id "form/5/defonce", :kind "defonce", :line 33, :end-line 33, :hash "-1026770644"} {:id "form/6/defonce", :kind "defonce", :line 34, :end-line 34, :hash "836696795"} {:id "form/7/defonce", :kind "defonce", :line 35, :end-line 35, :hash "490469823"} {:id "form/8/defonce", :kind "defonce", :line 36, :end-line 36, :hash "-829465550"} {:id "defn/configure!", :kind "defn", :line 38, :end-line 59, :hash "-913584225"} {:id "defn-/settings-path", :kind "defn-", :line 61, :end-line 65, :hash "-1863195585"} {:id "defn-/emit!", :kind "defn-", :line 67, :end-line 71, :hash "-332396237"} {:id "defn-/no-keyfocus-qa?", :kind "defn-", :line 73, :end-line 76, :hash "-1856740227"} {:id "defn-/real-input-enabled?", :kind "defn-", :line 78, :end-line 80, :hash "220575733"} {:id "defn-/emit-sim!", :kind "defn-", :line 82, :end-line 85, :hash "1660367962"} {:id "defn-/persist-settings!", :kind "defn-", :line 87, :end-line 94, :hash "338512157"} {:id "defn-/load-persisted", :kind "defn-", :line 96, :end-line 98, :hash "-1022612185"} {:id "defn-/emit-telemetry-fire!", :kind "defn-", :line 100, :end-line 102, :hash "-1450278322"} {:id "defn-/emit-fireball-phases!", :kind "defn-", :line 104, :end-line 111, :hash "-2091449435"} {:id "defn-/emit-new-sfx!", :kind "defn-", :line 113, :end-line 131, :hash "-43333562"} {:id "defn-/apply-handle", :kind "defn-", :line 133, :end-line 147, :hash "-1526758363"} {:id "defn-/apply-destroy-options", :kind "defn-", :line 149, :end-line 151, :hash "-681480682"} {:id "defn-/apply-qa-targets", :kind "defn-", :line 153, :end-line 155, :hash "148229047"} {:id "defn-/apply-enemy-spec", :kind "defn-", :line 157, :end-line 159, :hash "-719432365"} {:id "defn-/apply-qa-enemies", :kind "defn-", :line 161, :end-line 163, :hash "20336924"} {:id "defn-/apply-qa-fireballs", :kind "defn-", :line 165, :end-line 167, :hash "917410177"} {:id "defn-/configure-display!", :kind "defn-", :line 169, :end-line 178, :hash "237377933"} {:id "defn-/apply-qa-scenario", :kind "defn-", :line 180, :end-line 184, :hash "781354933"} {:id "defn/setup", :kind "defn", :line 186, :end-line 207, :hash "33615506"} {:id "defn-/frame-dt-seconds", :kind "defn-", :line 209, :end-line 218, :hash "901947249"} {:id "defn-/advance-one-step", :kind "defn-", :line 220, :end-line 227, :hash "67813870"} {:id "defn-/tick-state", :kind "defn-", :line 229, :end-line 266, :hash "1935649145"} {:id "defn-/toggle-pause", :kind "defn-", :line 268, :end-line 274, :hash "243918724"} {:id "defn-/drain-one-qa-event", :kind "defn-", :line 276, :end-line 420, :hash "2026920595"} {:id "defn/update-state", :kind "defn", :line 422, :end-line 436, :hash "-426389492"} {:id "defn/draw", :kind "defn", :line 437, :end-line 440, :hash "171649855"} {:id "defn/mouse-moved", :kind "defn", :line 442, :end-line 446, :hash "-1860317437"} {:id "defn/mouse-dragged", :kind "defn", :line 448, :end-line 450, :hash "-440374218"} {:id "defn-/left-button?", :kind "defn-", :line 452, :end-line 455, :hash "143503345"} {:id "defn/mouse-pressed", :kind "defn", :line 457, :end-line 462, :hash "1887518453"} {:id "defn-/initials-char?", :kind "defn-", :line 464, :end-line 466, :hash "-1078115175"} {:id "defn-/append-initials-draft!", :kind "defn-", :line 468, :end-line 473, :hash "-400509074"} {:id "defn/key-pressed", :kind "defn", :line 475, :end-line 550, :hash "1306562840"} {:id "defn/run-sketch!", :kind "defn", :line 552, :end-line 576, :hash "1780402941"}]}
;; clj-mutate-manifest-end

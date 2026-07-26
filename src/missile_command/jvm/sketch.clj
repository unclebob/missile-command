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

(defn- no-keyfocus-qa?
  []
  (and (:qa-telemetry? @launch-options)
       (:no-keyfocus? @launch-options)))

(defn- real-input-enabled?
  []
  (not (no-keyfocus-qa?)))

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

(defn- enter-key?
  [ch]
  (or (= \newline ch)
      (= \return ch)
      (when ch
        (= (int 10) (int ch)))))

(defn- backspace-key?
  [ch]
  (or (= \backspace ch)
      (when ch
        (= (char 8) ch))))

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
    :apply-qa-fireballs input/apply-qa-fireballs
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
          enter? (enter-key? ch)
          backspace? (backspace-key? ch)
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
;; {:version 1, :tested-at "2026-07-26T10:39:38.305767-05:00", :module-hash "1145684545", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 17, :hash "1776301454"} {:id "def/default-width", :kind "def", :line 19, :end-line 19, :hash "1515114879"} {:id "def/default-height", :kind "def", :line 20, :end-line 20, :hash "1673066894"} {:id "form/3/defonce", :kind "defonce", :line 22, :end-line 34, :hash "-1716031864"} {:id "form/4/defonce", :kind "defonce", :line 36, :end-line 36, :hash "1002191581"} {:id "form/5/defonce", :kind "defonce", :line 37, :end-line 37, :hash "-1026770644"} {:id "form/6/defonce", :kind "defonce", :line 38, :end-line 38, :hash "836696795"} {:id "form/7/defonce", :kind "defonce", :line 39, :end-line 39, :hash "490469823"} {:id "form/8/defonce", :kind "defonce", :line 40, :end-line 40, :hash "-829465550"} {:id "defn/configure!", :kind "defn", :line 42, :end-line 63, :hash "-913584225"} {:id "defn-/settings-path", :kind "defn-", :line 65, :end-line 69, :hash "-1863195585"} {:id "defn-/no-keyfocus-qa?", :kind "defn-", :line 71, :end-line 74, :hash "-1856740227"} {:id "defn-/real-input-enabled?", :kind "defn-", :line 76, :end-line 78, :hash "220575733"} {:id "defn-/telemetry-context", :kind "defn-", :line 80, :end-line 87, :hash "-942230843"} {:id "defn-/emit-sim!", :kind "defn-", :line 89, :end-line 91, :hash "1900707031"} {:id "defn-/persist-settings!", :kind "defn-", :line 93, :end-line 100, :hash "338512157"} {:id "defn-/load-persisted", :kind "defn-", :line 102, :end-line 104, :hash "-1022612185"} {:id "defn-/emit-telemetry-fire!", :kind "defn-", :line 106, :end-line 108, :hash "1119768835"} {:id "defn-/emit-fireball-phases!", :kind "defn-", :line 110, :end-line 112, :hash "-20715274"} {:id "defn-/emit-new-sfx!", :kind "defn-", :line 114, :end-line 116, :hash "887994917"} {:id "defn-/apply-handle", :kind "defn-", :line 118, :end-line 132, :hash "-1526758363"} {:id "defn-/apply-destroy-options", :kind "defn-", :line 134, :end-line 136, :hash "-681480682"} {:id "defn-/apply-qa-targets", :kind "defn-", :line 138, :end-line 140, :hash "148229047"} {:id "defn-/apply-enemy-spec", :kind "defn-", :line 142, :end-line 144, :hash "-719432365"} {:id "defn-/apply-qa-enemies", :kind "defn-", :line 146, :end-line 148, :hash "20336924"} {:id "defn-/apply-qa-fireballs", :kind "defn-", :line 150, :end-line 152, :hash "917410177"} {:id "defn-/configure-display!", :kind "defn-", :line 154, :end-line 163, :hash "237377933"} {:id "defn-/apply-qa-scenario", :kind "defn-", :line 165, :end-line 169, :hash "781354933"} {:id "defn/setup", :kind "defn", :line 171, :end-line 192, :hash "33615506"} {:id "defn-/frame-dt-seconds", :kind "defn-", :line 194, :end-line 196, :hash "1711218174"} {:id "defn-/advance-one-step", :kind "defn-", :line 198, :end-line 205, :hash "67813870"} {:id "defn-/tick-state", :kind "defn-", :line 207, :end-line 235, :hash "-472559871"} {:id "defn-/apply-input-intent", :kind "defn-", :line 237, :end-line 249, :hash "-1199831732"} {:id "defn-/normalized-key-event", :kind "defn-", :line 251, :end-line 261, :hash "-152574294"} {:id "defn-/enter-key?", :kind "defn-", :line 263, :end-line 268, :hash "-256199460"} {:id "defn-/backspace-key?", :kind "defn-", :line 270, :end-line 274, :hash "-890259396"} {:id "defn-/toggle-pause", :kind "defn-", :line 276, :end-line 280, :hash "586986183"} {:id "defn-/drain-one-qa-event", :kind "defn-", :line 282, :end-line 297, :hash "1001076716"} {:id "defn/update-state", :kind "defn", :line 299, :end-line 313, :hash "-426389492"} {:id "defn/draw", :kind "defn", :line 314, :end-line 317, :hash "171649855"} {:id "defn/mouse-moved", :kind "defn", :line 319, :end-line 323, :hash "-1860317437"} {:id "defn/mouse-dragged", :kind "defn", :line 325, :end-line 327, :hash "-440374218"} {:id "defn-/left-button?", :kind "defn-", :line 329, :end-line 332, :hash "143503345"} {:id "defn/mouse-pressed", :kind "defn", :line 334, :end-line 339, :hash "1887518453"} {:id "defn/key-pressed", :kind "defn", :line 341, :end-line 354, :hash "1146766385"} {:id "defn/run-sketch!", :kind "defn", :line 356, :end-line 380, :hash "1780402941"}]}
;; clj-mutate-manifest-end

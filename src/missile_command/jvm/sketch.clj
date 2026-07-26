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

(defn- real-input-enabled?
  []
  (not (no-keyfocus-qa?)))

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
                          (input/apply-qa-fireballs
                           state
                           [{:x x :y y :radius radius}]))
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
;; {:version 1, :tested-at "2026-07-26T10:19:10.415433-05:00", :module-hash "-2118659332", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 13, :hash "-1263449719"} {:id "def/default-width", :kind "def", :line 15, :end-line 15, :hash "1515114879"} {:id "def/default-height", :kind "def", :line 16, :end-line 16, :hash "1673066894"} {:id "form/3/defonce", :kind "defonce", :line 18, :end-line 30, :hash "-1716031864"} {:id "form/4/defonce", :kind "defonce", :line 32, :end-line 32, :hash "1002191581"} {:id "form/5/defonce", :kind "defonce", :line 33, :end-line 33, :hash "-1026770644"} {:id "form/6/defonce", :kind "defonce", :line 34, :end-line 34, :hash "836696795"} {:id "form/7/defonce", :kind "defonce", :line 35, :end-line 35, :hash "490469823"} {:id "form/8/defonce", :kind "defonce", :line 36, :end-line 36, :hash "-829465550"} {:id "defn/configure!", :kind "defn", :line 38, :end-line 59, :hash "-913584225"} {:id "defn-/settings-path", :kind "defn-", :line 61, :end-line 65, :hash "-1863195585"} {:id "defn-/emit!", :kind "defn-", :line 67, :end-line 71, :hash "-332396237"} {:id "defn-/no-keyfocus-qa?", :kind "defn-", :line 73, :end-line 76, :hash "-1856740227"} {:id "defn-/real-input-enabled?", :kind "defn-", :line 78, :end-line 80, :hash "220575733"} {:id "defn-/emit-sim!", :kind "defn-", :line 82, :end-line 85, :hash "1660367962"} {:id "defn-/persist-settings!", :kind "defn-", :line 87, :end-line 94, :hash "338512157"} {:id "defn-/load-persisted", :kind "defn-", :line 96, :end-line 98, :hash "-1022612185"} {:id "defn-/emit-telemetry-fire!", :kind "defn-", :line 100, :end-line 102, :hash "-1450278322"} {:id "defn-/emit-fireball-phases!", :kind "defn-", :line 104, :end-line 111, :hash "-2091449435"} {:id "defn-/emit-new-sfx!", :kind "defn-", :line 113, :end-line 131, :hash "-43333562"} {:id "defn-/apply-handle", :kind "defn-", :line 133, :end-line 147, :hash "-1526758363"} {:id "defn-/apply-destroy-options", :kind "defn-", :line 149, :end-line 151, :hash "-681480682"} {:id "defn-/apply-qa-targets", :kind "defn-", :line 153, :end-line 155, :hash "148229047"} {:id "defn-/apply-enemy-spec", :kind "defn-", :line 157, :end-line 159, :hash "-719432365"} {:id "defn-/apply-qa-enemies", :kind "defn-", :line 161, :end-line 163, :hash "20336924"} {:id "defn-/apply-qa-fireballs", :kind "defn-", :line 165, :end-line 167, :hash "917410177"} {:id "defn-/configure-display!", :kind "defn-", :line 169, :end-line 178, :hash "237377933"} {:id "defn-/apply-qa-scenario", :kind "defn-", :line 180, :end-line 184, :hash "781354933"} {:id "defn/setup", :kind "defn", :line 186, :end-line 207, :hash "33615506"} {:id "defn-/frame-dt-seconds", :kind "defn-", :line 209, :end-line 218, :hash "901947249"} {:id "defn-/advance-one-step", :kind "defn-", :line 220, :end-line 227, :hash "67813870"} {:id "defn-/tick-state", :kind "defn-", :line 229, :end-line 266, :hash "1935649145"} {:id "defn-/toggle-pause", :kind "defn-", :line 268, :end-line 274, :hash "243918724"} {:id "defn-/drain-one-qa-event", :kind "defn-", :line 276, :end-line 420, :hash "2026920595"} {:id "defn/update-state", :kind "defn", :line 422, :end-line 436, :hash "-426389492"} {:id "defn/draw", :kind "defn", :line 437, :end-line 440, :hash "171649855"} {:id "defn/mouse-moved", :kind "defn", :line 442, :end-line 446, :hash "-1860317437"} {:id "defn/mouse-dragged", :kind "defn", :line 448, :end-line 450, :hash "-440374218"} {:id "defn-/left-button?", :kind "defn-", :line 452, :end-line 455, :hash "143503345"} {:id "defn/mouse-pressed", :kind "defn", :line 457, :end-line 462, :hash "1887518453"} {:id "defn-/initials-char?", :kind "defn-", :line 464, :end-line 466, :hash "-1078115175"} {:id "defn-/append-initials-draft!", :kind "defn-", :line 468, :end-line 473, :hash "-400509074"} {:id "defn/key-pressed", :kind "defn", :line 475, :end-line 550, :hash "1306562840"} {:id "defn/run-sketch!", :kind "defn", :line 552, :end-line 576, :hash "1780402941"}]}
;; clj-mutate-manifest-end

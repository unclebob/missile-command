(ns missile-command.jvm.sketch
  "Quil sketch: route mouse/keyboard UI events into pure core commands."
  (:require [quil.core :as q]
            [quil.middleware :as m]
            [quil.applet :as applet]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [missile-command.core :as core]
            [missile-command.global-scores :as global]
            [missile-command.host-input :as host-input]
            [missile-command.missiles :as missiles]
            [missile-command.jvm.audio :as audio]
            [missile-command.jvm.frame :as frame]
            [missile-command.jvm.global-scores :as global-client]
            [missile-command.jvm.input :as input]
            [missile-command.jvm.persist :as persist]
            [missile-command.jvm.qa-runner :as qa-runner]
            [missile-command.jvm.render :as render]
            [missile-command.jvm.telemetry-emitter :as telemetry-emitter]
            [missile-command.jvm.window :as window]
            [missile-command.world :as world])
  (:import [javax.swing JOptionPane]))

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
         :leaderboard-url nil
         :leaderboard-name nil
         :player-name nil
         :no-global-scores? false
         :launch-anchor nil
         :restore-focus-app nil}))

(defonce pending-qa-events (atom []))
(defonce fireball-phases (atom {}))
(defonce last-frame-ms (atom nil))
(defonce initials-draft (atom ""))
(defonce sfx-emitted-count (atom 0))
(defonce global-scores (atom global/empty-state))
(defonce high-scores-opened-ms (atom 0))
(defonce local-player-code (atom nil))
(defonce name-prompt-open? (atom false))

(declare settings-path)
(declare escape-key?)

(defn- new-player-code
  []
  (-> (str (java.util.UUID/randomUUID))
      (str/replace #"-" "")
      (subs 0 6)
      str/upper-case))

(defn- load-local-player-code
  []
  (:local-player-code (persist/load-settings (settings-path))))

(defn- save-local-player-code!
  [code]
  (let [path (settings-path)
        file (io/file path)
        settings (or (persist/load-settings path) {})]
    (io/make-parents file)
    (spit file (pr-str (assoc settings :local-player-code code))))
  code)

(defn- ensure-local-player-code!
  []
  (or @local-player-code
      (let [code (or (load-local-player-code) (new-player-code))]
        (reset! local-player-code code)
        (save-local-player-code! code))))

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
                                            :leaderboard-url
                                            :leaderboard-name
                                            :player-name
                                            :no-global-scores?
                                            :launch-anchor
                                            :restore-focus-app]))
  (reset! pending-qa-events
          (if-let [path (:qa-events opts)]
            (input/load-qa-events path)
            []))
  (reset! fireball-phases {})
  (reset! last-frame-ms nil)
  (reset! initials-draft "")
  (reset! sfx-emitted-count 0)
  (reset! global-scores (global-client/initial-state opts))
  (reset! local-player-code nil)
  (reset! name-prompt-open? false)
  (global-client/fetch-leaderboard! global-scores)
  (reset! high-scores-opened-ms 0))

(defn- settings-path
  "QA --scores-file overrides host settings path; else MC_SETTINGS_PATH/default."
  []
  (or (:scores-file @launch-options)
      (persist/default-settings-path)))

(defn- no-keyfocus-qa?
  []
  (and (:qa-telemetry? @launch-options)
       (:no-keyfocus? @launch-options)))

(defn- fitted-playfield-size
  [width height]
  (world/fit-playfield-size width height))

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
  (let [command (cond-> command
                  (= :submit-high-score (:type command))
                  (assoc :public-code (ensure-local-player-code!)
                         :display-name (or (:display-name command)
                                           (:initials command))
                         :created-at (.toString (java.time.Instant/now))))
        result (core/handle state command)
        state' (:state result)]
    (when (#{:fire :click :key} (:type command))
      (emit-telemetry-fire! result))
    (emit-new-sfx! state state')
    (when (and (= :submit-high-score (:type command))
               (not (core/high-score-entry? state'))
               (core/high-score-entry? state))
      (global-client/submit-score! (settings-path)
                                   global-scores
                                   state
                                   (or (core/submitted-high-score-initials state')
                                       (:initials command))
                                   (:display-name command))
      (reset! initials-draft "")
      (persist-settings! state'))
    (when (and (= :open-high-scores (:type command))
               (core/high-scores-view? state'))
      (reset! high-scores-opened-ms (System/currentTimeMillis))
      (global-client/fetch-leaderboard! global-scores))
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

(defn- close-game!
  []
  (audio/stop-all!)
  (try (q/exit) (catch Exception _))
  (shutdown-agents)
  (System/exit 0))

(defn- play-screen?
  [state]
  (core/playing? state))

(defn- apply-cursor-policy!
  [state]
  (if (play-screen? state)
    (q/no-cursor)
    (q/cursor)))

(defn- configure-display!
  []
  (try
    (let [surface (.getSurface (applet/current-applet))
          anchor (:launch-anchor @launch-options)
          prev (:restore-focus-app @launch-options)]
      (window/install-exit-on-close! surface close-game!)
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
  (q/frame-rate 30)
  (q/cursor)
  (configure-display!)
  (audio/warm!)
  (reset! last-frame-ms (System/currentTimeMillis))
  (let [{:keys [width height]} (fitted-playfield-size (q/width) (q/height))
        state (-> (core/new-game {:width width :height height})
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

(defn- usable-player-name
  [raw]
  (let [name (str/trim (str (or raw "")))]
    (if (seq name) name "PLAYER")))

(defn- prompt-player-name!
  [state]
  (let [score (or (core/pending-high-score state) (core/final-score state) 0)
        default-name (usable-player-name
                      (or (:player-name @launch-options)
                          @initials-draft))
        response (JOptionPane/showInputDialog nil
                                             (str "High score: " score "\nPlayer name:")
                                             default-name)]
    (usable-player-name response)))

(defn- resolve-high-score-entry
  [state]
  (if (and (core/high-score-entry? state)
           (not (:qa-telemetry? @launch-options))
           (not @name-prompt-open?))
    (try
      (reset! name-prompt-open? true)
      (let [name (prompt-player-name! state)]
        (apply-handle state {:type :submit-high-score
                             :initials name
                             :display-name name}))
      (finally
        (reset! name-prompt-open? false)))
    state))

(defn- normalized-key-event
  ([ch]
   (normalized-key-event ch false false))
  ([ch enter?]
   (normalized-key-event ch enter? false))
  ([ch enter? backspace?]
   {:ch ch
    :key-name (when ch (str/lower-case (str ch)))
    :escape? (escape-key? ch)
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

(defn- escape-key?
  [ch]
  (or (input/escape-key? ch)
      (try (= 27 (q/key-code))
           (catch Exception _ false))))

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
    :exit! close-game!
    :apply-handle apply-handle
    :apply-destroy-options apply-destroy-options
    :apply-enemy-spec apply-enemy-spec
    :apply-qa-fireballs input/apply-qa-fireballs
    :apply-scenario input/apply-scenario
    :load-scenario-edn input/load-scenario-edn
    :load-persisted load-persisted
    :new-game #(let [{:keys [width height]} (fitted-playfield-size (q/width) (q/height))]
                 (core/new-game {:width width :height height}))
    :toggle-pause toggle-pause
    :initials-draft initials-draft}
   state))

(defn update-state
  [state]
  (let [scripted? (seq @pending-qa-events)
        {:keys [width height]} (fitted-playfield-size (q/width) (q/height))
        state (-> state
                  (input/resize-if-needed width height
                                          core/resize core/playfield-width core/playfield-height)
                  tick-state)]
    (-> (if scripted?
          (drain-one-qa-event state)
          (cond-> state
            (real-input-enabled?)
            (apply-handle (input/aim-command (q/mouse-x) (q/mouse-y)))

            true
            drain-one-qa-event))
        resolve-high-score-entry)))

(defn draw
  [state]
  (apply-cursor-policy! state)
  (render/draw-world!
   (global/attach state @global-scores @high-scores-opened-ms (System/currentTimeMillis))
   @initials-draft)
  (when (play-screen? state)
    (render/crosshair-at! (q/mouse-x) (q/mouse-y))))

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
          (do (persist-settings! state) (close-game!) state)
          state)))
    state))

(defn run-sketch!
  ([]
   (run-sketch! default-width default-height))
  ([width height]
   (let [{fitted-width :width fitted-height :height} (fitted-playfield-size width height)
         opts [:title "Missile Command"
               :size [fitted-width fitted-height]
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
;; {:version 1, :tested-at "2026-07-27T13:34:29.681149-05:00", :module-hash "587116372", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 20, :hash "-1947467185"} {:id "def/default-width", :kind "def", :line 22, :end-line 22, :hash "1515114879"} {:id "def/default-height", :kind "def", :line 23, :end-line 23, :hash "1673066894"} {:id "form/3/defonce", :kind "defonce", :line 25, :end-line 41, :hash "-1614428306"} {:id "form/4/defonce", :kind "defonce", :line 43, :end-line 43, :hash "1002191581"} {:id "form/5/defonce", :kind "defonce", :line 44, :end-line 44, :hash "-1026770644"} {:id "form/6/defonce", :kind "defonce", :line 45, :end-line 45, :hash "836696795"} {:id "form/7/defonce", :kind "defonce", :line 46, :end-line 46, :hash "490469823"} {:id "form/8/defonce", :kind "defonce", :line 47, :end-line 47, :hash "-829465550"} {:id "form/9/defonce", :kind "defonce", :line 48, :end-line 48, :hash "1631541882"} {:id "form/10/defonce", :kind "defonce", :line 49, :end-line 49, :hash "961660822"} {:id "form/11/defonce", :kind "defonce", :line 50, :end-line 50, :hash "-358982386"} {:id "form/12/declare", :kind "declare", :line 52, :end-line 52, :hash "-1191293058"} {:id "form/13/declare", :kind "declare", :line 53, :end-line 53, :hash "496562229"} {:id "defn-/new-player-code", :kind "defn-", :line 55, :end-line 60, :hash "1174320082"} {:id "defn-/load-local-player-code", :kind "defn-", :line 62, :end-line 64, :hash "-1889298289"} {:id "defn-/save-local-player-code!", :kind "defn-", :line 66, :end-line 73, :hash "-181129980"} {:id "defn-/ensure-local-player-code!", :kind "defn-", :line 75, :end-line 80, :hash "-617627048"} {:id "defn/configure!", :kind "defn", :line 82, :end-line 110, :hash "2008295933"} {:id "defn-/settings-path", :kind "defn-", :line 112, :end-line 116, :hash "-1863195585"} {:id "defn-/no-keyfocus-qa?", :kind "defn-", :line 118, :end-line 121, :hash "-1856740227"} {:id "defn-/real-input-enabled?", :kind "defn-", :line 123, :end-line 125, :hash "220575733"} {:id "defn-/telemetry-context", :kind "defn-", :line 127, :end-line 134, :hash "-942230843"} {:id "defn-/emit-sim!", :kind "defn-", :line 136, :end-line 138, :hash "1900707031"} {:id "defn-/persist-settings!", :kind "defn-", :line 140, :end-line 147, :hash "338512157"} {:id "defn-/load-persisted", :kind "defn-", :line 149, :end-line 151, :hash "-1022612185"} {:id "defn-/emit-telemetry-fire!", :kind "defn-", :line 153, :end-line 155, :hash "1119768835"} {:id "defn-/emit-fireball-phases!", :kind "defn-", :line 157, :end-line 159, :hash "-20715274"} {:id "defn-/emit-new-sfx!", :kind "defn-", :line 161, :end-line 163, :hash "887994917"} {:id "defn-/apply-handle", :kind "defn-", :line 165, :end-line 194, :hash "1709300442"} {:id "defn-/apply-destroy-options", :kind "defn-", :line 196, :end-line 198, :hash "-681480682"} {:id "defn-/apply-qa-targets", :kind "defn-", :line 200, :end-line 202, :hash "148229047"} {:id "defn-/apply-enemy-spec", :kind "defn-", :line 204, :end-line 206, :hash "-719432365"} {:id "defn-/apply-qa-enemies", :kind "defn-", :line 208, :end-line 210, :hash "20336924"} {:id "defn-/apply-qa-fireballs", :kind "defn-", :line 212, :end-line 214, :hash "917410177"} {:id "defn-/configure-display!", :kind "defn-", :line 216, :end-line 225, :hash "237377933"} {:id "defn-/apply-qa-scenario", :kind "defn-", :line 227, :end-line 231, :hash "781354933"} {:id "defn/setup", :kind "defn", :line 233, :end-line 254, :hash "33615506"} {:id "defn-/frame-dt-seconds", :kind "defn-", :line 256, :end-line 258, :hash "1711218174"} {:id "defn-/advance-one-step", :kind "defn-", :line 260, :end-line 267, :hash "67813870"} {:id "defn-/tick-state", :kind "defn-", :line 269, :end-line 297, :hash "-472559871"} {:id "defn-/apply-input-intent", :kind "defn-", :line 299, :end-line 311, :hash "-1199831732"} {:id "defn-/normalized-key-event", :kind "defn-", :line 313, :end-line 323, :hash "-1633199008"} {:id "defn-/enter-key?", :kind "defn-", :line 325, :end-line 330, :hash "-256199460"} {:id "defn-/backspace-key?", :kind "defn-", :line 332, :end-line 336, :hash "-890259396"} {:id "defn-/escape-key?", :kind "defn-", :line 338, :end-line 342, :hash "368632330"} {:id "defn-/toggle-pause", :kind "defn-", :line 344, :end-line 348, :hash "586986183"} {:id "defn-/drain-one-qa-event", :kind "defn-", :line 350, :end-line 369, :hash "174371611"} {:id "defn/update-state", :kind "defn", :line 371, :end-line 385, :hash "-426389492"} {:id "defn/draw", :kind "defn", :line 386, :end-line 391, :hash "1765440392"} {:id "defn/mouse-moved", :kind "defn", :line 393, :end-line 397, :hash "-1860317437"} {:id "defn/mouse-dragged", :kind "defn", :line 399, :end-line 401, :hash "-440374218"} {:id "defn-/left-button?", :kind "defn-", :line 403, :end-line 406, :hash "143503345"} {:id "defn/mouse-pressed", :kind "defn", :line 408, :end-line 413, :hash "1887518453"} {:id "defn/key-pressed", :kind "defn", :line 415, :end-line 428, :hash "1146766385"} {:id "defn/run-sketch!", :kind "defn", :line 430, :end-line 454, :hash "1780402941"}]}
;; clj-mutate-manifest-end

(ns missile-command.browser.main
  "ClojureScript Quil browser host entrypoint."
  (:require [clojure.string :as str]
            [quil.core :as q :include-macros true]
            [quil.middleware :as m]
            [missile-command.core :as core]
            [missile-command.host-input :as host-input]
            [missile-command.browser.persist :as persist]
            [missile-command.browser.render :as render]
            [missile-command.browser.audio :as audio]))

(def default-width 800)
(def default-height 600)
;; Cap canvas size so full-screen retina doesn't thrash the sim/draw loop.
(def max-canvas-edge 1280)

(defonce initials-draft (atom ""))

(defn- canvas-size
  []
  (let [w (or (.-innerWidth js/window) default-width)
        h (or (.-innerHeight js/window) default-height)
        scale (min 1.0
                   (/ (double max-canvas-edge) (double (max w 1)))
                   (/ (double max-canvas-edge) (double (max h 1))))]
    [(max 320 (long (* w scale)))
     (max 240 (long (* h scale)))]))

(defn- maybe-resize
  "Resize playfield only when the canvas size actually changed."
  [state]
  (let [w (q/width)
        h (q/height)]
    (if (or (not= w (core/playfield-width state))
            (not= h (core/playfield-height state)))
      (core/resize state w h)
      state)))

(defn- play-new-sfx!
  "Play SFX appended since prev-state (official contract: sfx-take-new)."
  [prev-state state]
  (when (and (core/title? prev-state) (core/playing? state))
    (audio/stop-title!))
  (let [prev (count (core/sfx-events prev-state))
        fresh (core/sfx-take-new state prev)]
    (audio/play-events! fresh (core/mute? state)))
  ;; After unlock, retry title music if core already emitted :sfx/warning.
  (when (and (core/title? state) @audio/unlocked?)
    (audio/ensure-title! (core/mute? state))))

(defn- apply-handle
  "Apply a core command; persist settings when options/scores change."
  [state command]
  (let [result (core/handle state command)
        state' (:state result)]
    (play-new-sfx! state state')
    (when (and (= :submit-high-score (:type command))
               (core/high-score-entry? state)
               (not (core/high-score-entry? state')))
      (reset! initials-draft "")
      (persist/save-settings! state'))
    (when (#{:set-mute :set-difficulty :bind-fire-key :leave-options} (:type command))
      (persist/save-settings! state'))
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

(defn- key-name
  [ch]
  (when ch (str/lower-case (str ch))))

(defn- backspace-key?
  "Quil/p5 may report backspace as char or key-code 8."
  [ch]
  (or (try (= 8 (q/key-code)) (catch :default _ false))
      (= "Backspace" (str ch))
      (= 8 (when (number? ch) ch))
      (= \u0008 ch)))

(defn- enter-key?
  [ch]
  (or (= \newline ch)
      (= \return ch)
      (try (#{10 13} (q/key-code)) (catch :default _ false))))

(defn- focus-canvas!
  "Browser keyboard events need the canvas focused."
  []
  (try
    (when-let [c (.querySelector js/document "canvas")]
      (.setAttribute c "tabindex" "0")
      (.style.setProperty (.-style c) "outline" "none")
      (.focus c))
    (catch :default _ nil)))

(defn setup
  []
  (q/frame-rate 60)
  (q/no-cursor)
  ;; Avoid 2× retina pixel density cost in p5/Quil.
  (try (q/pixel-density 1) (catch :default _))
  (audio/warm!)
  (reset! initials-draft "")
  (js/setTimeout focus-canvas! 0)
  (let [[w h] (canvas-size)]
    (q/resize-sketch w h)
    (-> (core/new-game {:width w :height h})
        persist/load-into)))

(defn update-state
  [state]
  (let [state (maybe-resize state)
        ;; Only aim while playing/paused; shell screens skip aim work.
        state (if (or (core/playing? state) (core/paused? state))
                (:state (core/handle state {:type :aim
                                            :x (q/mouse-x)
                                            :y (q/mouse-y)}))
                state)
        ;; Fixed step keeps sim stable in the browser; wall-clock lag only drops FPS.
        ticked (:state (core/tick state (/ 1.0 60.0)))]
    (play-new-sfx! state ticked)
    ticked))

(defn draw
  [state]
  (render/draw-world! state @initials-draft)
  (when (or (core/playing? state) (core/paused? state))
    (render/crosshair-at! (q/mouse-x) (q/mouse-y))))

(defn mouse-pressed
  [state _]
  (focus-canvas!)
  (let [first-unlock? (not @audio/unlocked?)]
    (audio/unlock!)
    ;; First click on title only unlocks audio + starts title warning (browsers
    ;; block autoplay). Second click (or Enter) starts the game.
    (if (and (core/title? state) first-unlock?)
      (do (audio/ensure-title! (core/mute? state))
          state)
      (apply-handle state {:type :click :x (q/mouse-x) :y (q/mouse-y)}))))

(defn- escape-key?
  [ch]
  (or (try (= 27 (q/key-code)) (catch :default _ false))
      (= "Escape" (str ch))
      (= 27 (when (number? ch) ch))))

(defn key-pressed
  [state _]
  (audio/unlock!)
  (when (core/title? state)
    (audio/ensure-title! (core/mute? state)))
  (let [ch (q/raw-key)
        event {:ch ch
               :key-name (key-name ch)
               :escape? (escape-key? ch)
               :enter? (enter-key? ch)
               :backspace? (backspace-key? ch)}]
    (if-let [intent (host-input/key-intent state @initials-draft event)]
      (apply-input-intent state intent)
      state)))

(defn ^:export run
  []
  (q/defsketch missile-command-browser
    :host "missile-command"
    :title "Missile Command"
    :size [default-width default-height]
    :setup setup
    :update update-state
    :draw draw
    :mouse-pressed mouse-pressed
    :key-pressed key-pressed
    :middleware [m/fun-mode]))

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-07-26T10:25:44.507363-05:00", :module-hash "-593069136", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 10, :hash "-775129158"} {:id "def/default-width", :kind "def", :line 12, :end-line 12, :hash "1515114879"} {:id "def/default-height", :kind "def", :line 13, :end-line 13, :hash "1673066894"} {:id "def/max-canvas-edge", :kind "def", :line 15, :end-line 15, :hash "980421588"} {:id "form/4/defonce", :kind "defonce", :line 17, :end-line 17, :hash "490469823"} {:id "defn-/canvas-size", :kind "defn-", :line 19, :end-line 27, :hash "2041325195"} {:id "defn-/maybe-resize", :kind "defn-", :line 29, :end-line 37, :hash "138955454"} {:id "defn-/play-new-sfx!", :kind "defn-", :line 39, :end-line 49, :hash "-621287917"} {:id "defn-/apply-handle", :kind "defn-", :line 51, :end-line 64, :hash "-969854057"} {:id "defn-/apply-input-intent", :kind "defn-", :line 66, :end-line 78, :hash "-1199831732"} {:id "defn-/key-name", :kind "defn-", :line 80, :end-line 82, :hash "1959061477"} {:id "defn-/backspace-key?", :kind "defn-", :line 84, :end-line 90, :hash "1076867633"} {:id "defn-/enter-key?", :kind "defn-", :line 92, :end-line 96, :hash "1257294141"} {:id "defn-/focus-canvas!", :kind "defn-", :line 98, :end-line 106, :hash "1762094652"} {:id "defn/setup", :kind "defn", :line 108, :end-line 120, :hash "-1953546766"} {:id "defn/update-state", :kind "defn", :line 122, :end-line 134, :hash "-1909946871"} {:id "defn/draw", :kind "defn", :line 136, :end-line 140, :hash "505697670"} {:id "defn/mouse-pressed", :kind "defn", :line 142, :end-line 152, :hash "2096012436"} {:id "defn-/escape-key?", :kind "defn-", :line 154, :end-line 158, :hash "-1047378806"} {:id "defn/key-pressed", :kind "defn", :line 160, :end-line 173, :hash "914901487"} {:id "defn/run", :kind "defn", :line 175, :end-line 186, :hash "-146256283"}]}
;; clj-mutate-manifest-end

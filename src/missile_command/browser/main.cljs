(ns missile-command.browser.main
  "ClojureScript Quil browser host entrypoint."
  (:require [clojure.string :as str]
            [quil.core :as q :include-macros true]
            [quil.middleware :as m]
            [missile-command.core :as core]
            [missile-command.global-scores :as global]
            [missile-command.host-input :as host-input]
            [missile-command.browser.global-scores :as global-client]
            [missile-command.browser.persist :as persist]
            [missile-command.browser.render :as render]
            [missile-command.browser.audio :as audio]
            [missile-command.world :as world]))

(def default-width 800)
(def default-height 600)
;; Cap canvas size so full-screen retina doesn't thrash the sim/draw loop.
(def max-canvas-edge 1280)

(defonce initials-draft (atom ""))
(defonce sfx-cursor (atom 0))
(defonce global-scores (atom global/empty-state))
(defonce high-scores-opened-ms (atom 0))
(defonce local-player-code (atom nil))
(defonce name-prompt-open? (atom false))

(def local-player-code-key "missile-command-local-player-code")

(defn- new-player-code
  []
  (-> (str (random-uuid))
      (str/replace #"-" "")
      (subs 0 6)
      str/upper-case))

(defn- read-local-player-code
  []
  (try
    (when (exists? js/localStorage)
      (.getItem js/localStorage local-player-code-key))
    (catch :default _ nil)))

(defn- save-local-player-code!
  [code]
  (try
    (when (exists? js/localStorage)
      (.setItem js/localStorage local-player-code-key code))
    (catch :default _ nil))
  code)

(defn- ensure-local-player-code!
  []
  (or @local-player-code
      (let [code (or (read-local-player-code) (new-player-code))]
        (reset! local-player-code code)
        (save-local-player-code! code))))

(defn- canvas-size
  []
  (let [w (or (.-innerWidth js/window) default-width)
        h (or (.-innerHeight js/window) default-height)
        scale (min 1.0
                   (/ (double max-canvas-edge) (double (max w 1)))
                   (/ (double max-canvas-edge) (double (max h 1))))]
    (let [raw-width (max 320 (long (* w scale)))
          raw-height (max 240 (long (* h scale)))
          fitted (world/fit-playfield-size raw-width raw-height)]
      [(:width fitted) (:height fitted)])))

(defn- phone-browser?
  []
  (let [w (or (.-innerWidth js/window) default-width)
        h (or (.-innerHeight js/window) default-height)
        ua (str/lower-case (or (.-userAgent (.-navigator js/window)) ""))
        narrow? (<= (min w h) 520)
        mobile-ua? (boolean (re-find #"iphone|ipod|android.+mobile|mobile" ua))]
    (or narrow? mobile-ua?)))

(defn- with-browser-traits
  [state]
  (assoc state :phone? (phone-browser?)))

(defn- maybe-resize
  "Resize playfield only when the canvas size actually changed."
  [state]
  (let [w (q/width)
        h (q/height)]
    (with-browser-traits
      (if (or (not= w (core/playfield-width state))
              (not= h (core/playfield-height state)))
        (core/resize state w h)
        state))))

(defn- play-new-sfx!
  "Play SFX appended since the host cursor."
  [prev-state state]
  (when (and (core/title? prev-state) (core/playing? state))
    (audio/stop-title!))
  (let [[fresh next-cursor] (core/sfx-take-new-with-cursor state @sfx-cursor)]
    (audio/play-events! fresh (core/mute? state))
    (reset! sfx-cursor next-cursor))
  ;; After unlock, retry title music if core already emitted :sfx/warning.
  (when (and (core/title? state) @audio/unlocked?)
    (audio/ensure-title! (core/mute? state))))

(defn- apply-handle
  "Apply a core command; persist settings when options/scores change."
  [state command]
  (let [command (cond-> command
                  (= :submit-high-score (:type command))
                  (assoc :public-code (ensure-local-player-code!)
                         :display-name (or (:display-name command)
                                           (:initials command))
                         :created-at (.toISOString (js/Date.))))
        result (core/handle state command)
        state' (:state result)]
    (play-new-sfx! state state')
    (when (and (= :submit-high-score (:type command))
               (core/high-score-entry? state)
               (not (core/high-score-entry? state')))
      (global-client/submit-score! global-scores
                                   state
                                   (or (core/submitted-high-score-initials state')
                                       (:initials command))
                                   (:display-name command))
      (reset! initials-draft "")
      (persist/save-settings! state'))
    (when (and (= :open-high-scores (:type command))
               (core/high-scores-view? state'))
      (reset! high-scores-opened-ms (.now js/Date))
      (global-client/fetch-leaderboard! global-scores))
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

(defn- usable-player-name
  [raw]
  (let [name (str/trim (str (or raw "")))]
    (if (seq name) name "PLAYER")))

(defn- prompt-player-name!
  [state]
  (let [score (or (core/pending-high-score state) (core/final-score state) 0)
        default-name (usable-player-name
                      (or (:player-name @global-scores)
                          @initials-draft))
        response (js/prompt (str "High score: " score "\nPlayer name:") default-name)]
    (usable-player-name response)))

(defn- resolve-high-score-entry
  [state]
  (if (and (core/high-score-entry? state)
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

(defn- game-crosshair-screen?
  [state]
  (core/playing? state))

(defn- set-canvas-cursor!
  [visible?]
  (try
    (when-let [c (.querySelector js/document "canvas")]
      (.style.setProperty (.-style c) "cursor" (if visible? "default" "none")))
    (catch :default _ nil)))

(defn setup
  []
  (q/frame-rate 30)
  (set-canvas-cursor! true)
  ;; Avoid 2× retina pixel density cost in p5/Quil.
  (try (q/pixel-density 1) (catch :default _))
  (audio/warm!)
  (reset! initials-draft "")
  (reset! sfx-cursor 0)
  (reset! global-scores (global-client/initial-state))
  (reset! high-scores-opened-ms 0)
  (reset! local-player-code nil)
  (reset! name-prompt-open? false)
  (global-client/fetch-leaderboard! global-scores)
  (js/setTimeout focus-canvas! 0)
  (let [[w h] (canvas-size)]
    (q/resize-sketch w h)
    (-> (core/new-game {:width w :height h})
        persist/load-into
        with-browser-traits)))

(defn update-state
  [state]
  (let [state (maybe-resize state)
        ;; Only aim while playing; shell screens skip aim work.
        state (if (game-crosshair-screen? state)
                (:state (core/handle state {:type :aim
                                            :x (q/mouse-x)
                                            :y (q/mouse-y)}))
                state)
        ;; Fixed step keeps sim stable in the browser; wall-clock lag only drops FPS.
        ticked (:state (core/tick state (/ 1.0 30.0)))]
    (play-new-sfx! state ticked)
    (resolve-high-score-entry ticked)))

(defn draw
  [state]
  (set-canvas-cursor! (not (game-crosshair-screen? state)))
  (render/draw-world!
   (global/attach state @global-scores @high-scores-opened-ms (.now js/Date))
   @initials-draft)
  (when (game-crosshair-screen? state)
    (render/crosshair-at! (q/mouse-x) (q/mouse-y))))

(defn mouse-pressed
  [state _]
  (focus-canvas!)
  (let [first-unlock? (not @audio/unlocked?)]
    (audio/unlock!)
    (when (and (core/title? state) first-unlock?)
      (audio/ensure-title! (core/mute? state)))
    (apply-handle state {:type :click :x (q/mouse-x) :y (q/mouse-y)})))

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
;; {:version 1, :tested-at "2026-07-27T13:33:54.408847-05:00", :module-hash "1265354794", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 12, :hash "-750581083"} {:id "def/default-width", :kind "def", :line 14, :end-line 14, :hash "1515114879"} {:id "def/default-height", :kind "def", :line 15, :end-line 15, :hash "1673066894"} {:id "def/max-canvas-edge", :kind "def", :line 17, :end-line 17, :hash "980421588"} {:id "form/4/defonce", :kind "defonce", :line 19, :end-line 19, :hash "490469823"} {:id "form/5/defonce", :kind "defonce", :line 20, :end-line 20, :hash "-56437432"} {:id "form/6/defonce", :kind "defonce", :line 21, :end-line 21, :hash "1631541882"} {:id "form/7/defonce", :kind "defonce", :line 22, :end-line 22, :hash "961660822"} {:id "form/8/defonce", :kind "defonce", :line 23, :end-line 23, :hash "-358982386"} {:id "def/local-player-code-key", :kind "def", :line 25, :end-line 25, :hash "936531632"} {:id "defn-/new-player-code", :kind "defn-", :line 27, :end-line 32, :hash "822970960"} {:id "defn-/read-local-player-code", :kind "defn-", :line 34, :end-line 39, :hash "-1030274412"} {:id "defn-/save-local-player-code!", :kind "defn-", :line 41, :end-line 47, :hash "-1173779028"} {:id "defn-/ensure-local-player-code!", :kind "defn-", :line 49, :end-line 54, :hash "1592719788"} {:id "defn-/canvas-size", :kind "defn-", :line 56, :end-line 64, :hash "2041325195"} {:id "defn-/maybe-resize", :kind "defn-", :line 66, :end-line 74, :hash "138955454"} {:id "defn-/play-new-sfx!", :kind "defn-", :line 76, :end-line 86, :hash "361546056"} {:id "defn-/apply-handle", :kind "defn-", :line 88, :end-line 115, :hash "-1264052956"} {:id "defn-/apply-input-intent", :kind "defn-", :line 117, :end-line 129, :hash "-1199831732"} {:id "defn-/key-name", :kind "defn-", :line 131, :end-line 133, :hash "1959061477"} {:id "defn-/backspace-key?", :kind "defn-", :line 135, :end-line 141, :hash "1076867633"} {:id "defn-/enter-key?", :kind "defn-", :line 143, :end-line 147, :hash "1257294141"} {:id "defn-/focus-canvas!", :kind "defn-", :line 149, :end-line 157, :hash "1762094652"} {:id "defn/setup", :kind "defn", :line 159, :end-line 175, :hash "-1275287975"} {:id "defn/update-state", :kind "defn", :line 177, :end-line 189, :hash "-1909946871"} {:id "defn/draw", :kind "defn", :line 191, :end-line 197, :hash "-682619820"} {:id "defn/mouse-pressed", :kind "defn", :line 199, :end-line 209, :hash "2096012436"} {:id "defn-/escape-key?", :kind "defn-", :line 211, :end-line 215, :hash "-1047378806"} {:id "defn/key-pressed", :kind "defn", :line 217, :end-line 230, :hash "914901487"} {:id "defn/run", :kind "defn", :line 232, :end-line 243, :hash "-146256283"}]}
;; clj-mutate-manifest-end

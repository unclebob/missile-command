(ns missile-command.browser.main
  "ClojureScript Quil browser host entrypoint."
  (:require [clojure.string :as str]
            [quil.core :as q :include-macros true]
            [quil.middleware :as m]
            [missile-command.core :as core]
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

(defn- ensure-wave-enemies
  "Spawn the full wave schedule when playing and sky is empty.
  Mirrors JVM host continuous-play spawn (not done inside core/tick)."
  [state]
  (if (or (not (core/playing? state))
          (seq (core/enemy-missiles state))
          (seq (core/flyers state)))
    state
    (core/activate-wave-schedule state)))

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
  [prev-state state]
  (let [prev (count (or (:sfx-events prev-state) []))
        all (or (:sfx-events state) [])
        fresh (drop prev all)]
    (audio/play-events! fresh (core/mute? state))))

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

(defn- toggle-pause
  [state]
  (cond
    (core/playing? state) (apply-handle state {:type :pause})
    (core/paused? state) (apply-handle state {:type :resume})
    :else state))

(defn- initials-char?
  [ch]
  (and ch (re-matches #"[A-Za-z0-9]" (str ch))))

(defn- append-initials-draft!
  [ch]
  (let [c (str/upper-case (str ch))
        cur @initials-draft]
    (when (< (count cur) 3)
      (reset! initials-draft (str cur c)))))

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
    (let [state (-> (core/new-game {:width w :height h})
                    persist/load-into)]
      (if (core/playing? state)
        (ensure-wave-enemies state)
        state))))

(defn update-state
  [state]
  (let [state (maybe-resize state)
        ;; Only aim while playing/paused; shell screens skip aim work.
        state (if (or (core/playing? state) (core/paused? state))
                (:state (core/handle state {:type :aim
                                            :x (q/mouse-x)
                                            :y (q/mouse-y)}))
                state)
        was-banner? (core/wave-banner? state)
        ;; Fixed step keeps sim stable in the browser; wall-clock lag only drops FPS.
        ticked (:state (core/tick state (/ 1.0 60.0)))
        ticked (if (and was-banner? (core/playing? ticked))
                 (ensure-wave-enemies ticked)
                 ticked)]
    (play-new-sfx! state ticked)
    ticked))

(defn draw
  [state]
  (render/draw-world! state @initials-draft)
  (when (or (core/playing? state) (core/paused? state))
    (render/crosshair-at! (q/mouse-x) (q/mouse-y))))

(defn mouse-pressed
  [state _]
  (audio/warm!)
  (focus-canvas!)
  (let [was-title? (core/title? state)
        next (apply-handle state {:type :click :x (q/mouse-x) :y (q/mouse-y)})]
    (if (and was-title? (core/playing? next))
      (ensure-wave-enemies next)
      next)))

(defn- escape-key?
  [ch]
  (or (try (= 27 (q/key-code)) (catch :default _ false))
      (= "Escape" (str ch))
      (= 27 (when (number? ch) ch))))

(defn- handle-escape
  [state]
  (cond
    (or (core/playing? state) (core/paused? state)) (toggle-pause state)
    (core/high-scores-view? state) (apply-handle state {:type :close-high-scores})
    (core/options? state) (apply-handle state {:type :leave-options})
    :else state))

(defn- handle-options-keys
  "Mute (M) and difficulty 1/2/3 while on options; nil if not handled."
  [state ch]
  (when (core/options? state)
    (cond
      (or (= \m ch) (= \M ch))
      (apply-handle state {:type :set-mute :mute (not (core/mute? state))})
      (= \1 ch) (apply-handle state {:type :set-difficulty :difficulty "easy"})
      (= \2 ch) (apply-handle state {:type :set-difficulty :difficulty "normal"})
      (= \3 ch) (apply-handle state {:type :set-difficulty :difficulty "arcade"})
      :else nil)))

(defn- handle-toggle-shell
  [state open-type close-type open? close?]
  (cond
    (open? state) (apply-handle state {:type open-type})
    (close? state) (apply-handle state {:type close-type})
    :else state))

(defn- handle-enter
  [state]
  (cond
    (core/title? state)
    (ensure-wave-enemies (apply-handle state {:type :start}))

    (core/the-end? state)
    (do (reset! initials-draft "")
        (apply-handle state {:type :confirm}))

    (core/high-score-entry? state)
    (let [draft @initials-draft]
      (if (seq draft)
        (apply-handle state {:type :submit-high-score :initials draft})
        state))

    :else state))

(defn- handle-initials-edit
  [state ch]
  (cond
    (initials-char? ch)
    (do (append-initials-draft! ch) state)

    (backspace-key? ch)
    (do (reset! initials-draft
                (let [d @initials-draft]
                  (if (seq d) (subs d 0 (dec (count d))) d)))
        state)

    :else nil))

(defn key-pressed
  [state _]
  (audio/warm!)
  (let [ch (q/raw-key)
        kn (key-name ch)]
    (or (when (escape-key? ch) (handle-escape state))
        (handle-options-keys state ch)
        (when (or (= \o ch) (= \O ch))
          (handle-toggle-shell state :open-options :leave-options
                               core/title? core/options?))
        (when (or (= \h ch) (= \H ch))
          (handle-toggle-shell state :open-high-scores :close-high-scores
                               core/title? core/high-scores-view?))
        (when (or (= \p ch) (= \P ch)
                  (and kn (core/pause-key-includes? state kn)))
          (toggle-pause state))
        (when (enter-key? ch) (handle-enter state))
        (when (core/high-score-entry? state)
          (handle-initials-edit state ch))
        (when (and kn (core/playing? state))
          (apply-handle state {:type :key :key kn}))
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

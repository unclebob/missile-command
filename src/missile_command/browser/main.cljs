(ns missile-command.browser.main
  "ClojureScript Quil browser host entrypoint."
  (:require [quil.core :as q :include-macros true]
            [quil.middleware :as m]
            [missile-command.core :as core]
            [missile-command.browser.persist :as persist]
            [missile-command.browser.render :as render]
            [missile-command.browser.audio :as audio]))

(def default-width 800)
(def default-height 600)
;; Cap canvas size so full-screen retina doesn't thrash the sim/draw loop.
(def max-canvas-edge 1280)

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

(defn setup
  []
  (q/frame-rate 60)
  (q/no-cursor)
  ;; Avoid 2× retina pixel density cost in p5/Quil.
  (try (q/pixel-density 1) (catch :default _))
  (audio/warm!)
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
  (render/draw-world! state)
  (when (or (core/playing? state) (core/paused? state))
    (render/crosshair-at! (q/mouse-x) (q/mouse-y))))

(defn mouse-pressed
  [state _]
  (audio/warm!)
  (let [was-title? (core/title? state)
        next (:state (core/handle state {:type :click :x (q/mouse-x) :y (q/mouse-y)}))]
    (play-new-sfx! state next)
    (if (and was-title? (core/playing? next))
      (ensure-wave-enemies next)
      next)))

(defn key-pressed
  [state _]
  (audio/warm!)
  (let [ch (q/raw-key)
        result (core/press-key state (str ch))]
    (cond
      (or (= \p ch) (= \P ch))
      (let [next (:state (core/handle state (if (core/playing? state)
                                             {:type :pause}
                                             {:type :resume})))]
        (play-new-sfx! state next)
        next)

      (or (= \newline ch) (= \return ch))
      (cond
        (core/title? state)
        (let [next (ensure-wave-enemies (:state (core/handle state {:type :start})))]
          (play-new-sfx! state next)
          next)

        (core/the-end? state)
        (let [next (:state (core/handle state {:type :confirm}))]
          (play-new-sfx! state next)
          next)

        :else state)

      :else
      (let [next (:state result)]
        (play-new-sfx! state next)
        next))))
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

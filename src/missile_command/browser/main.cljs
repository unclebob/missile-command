(ns missile-command.browser.main
  "ClojureScript Quil browser host entrypoint."
  (:require [quil.core :as q :include-macros true]
            [quil.middleware :as m]
            [missile-command.core :as core]
            [missile-command.browser.persist :as persist]
            [missile-command.browser.render :as render]))

(def default-width 800)
(def default-height 600)

(defn- canvas-size
  []
  (let [w (or (.-innerWidth js/window) default-width)
        h (or (.-innerHeight js/window) default-height)]
    [(max 320 (long w)) (max 240 (long h))]))

(defn setup
  []
  (q/frame-rate 60)
  (q/no-cursor)
  (let [[w h] (canvas-size)]
    (q/resize-sketch w h)
    (-> (core/new-game {:width w :height h})
        persist/load-into)))

(defn update-state
  [state]
  (let [w (q/width)
        h (q/height)
        state (if (or (not= w (core/playfield-width state))
                      (not= h (core/playfield-height state)))
                (core/resize state w h)
                state)
        aimed (:state (core/handle state {:type :aim
                                          :x (q/mouse-x)
                                          :y (q/mouse-y)}))
        ticked (:state (core/tick aimed (/ 1.0 60.0)))]
    ticked))

(defn draw
  [state]
  (render/draw-world! state)
  (render/crosshair-at! (q/mouse-x) (q/mouse-y)))

(defn mouse-pressed
  [state _]
  (:state (core/handle state {:type :click :x (q/mouse-x) :y (q/mouse-y)})))

(defn key-pressed
  [state _]
  (let [ch (q/raw-key)
        result (core/press-key state (str ch))]
    (cond
      (or (= \p ch) (= \P ch))
      (:state (core/handle state (if (core/playing? state)
                                   {:type :pause}
                                   {:type :resume})))

      (or (= \newline ch) (= \return ch))
      (cond
        (core/title? state) (:state (core/handle state {:type :start}))
        (core/the-end? state) (:state (core/handle state {:type :confirm}))
        :else state)

      :else (:state result))))

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
    :middleware [m/fun-mode]
    :features [:keep-on-top]))

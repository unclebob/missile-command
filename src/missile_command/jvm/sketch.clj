(ns missile-command.jvm.sketch
  "Quil sketch: route mouse/keyboard UI events into pure core commands."
  (:require [quil.core :as q]
            [quil.middleware :as m]
            [missile-command.core :as core]
            [missile-command.jvm.input :as input]
            [missile-command.jvm.render :as render]))

(def default-width 800)
(def default-height 600)

(defn- apply-command
  [state command]
  (:state (core/handle state command)))

(defn setup
  []
  (q/frame-rate 60)
  (core/new-game {:width (q/width) :height (q/height)}))

(defn update-state
  [state]
  ;; Keep core aim synced every frame; visual reticle uses live mouse in draw.
  (-> state
      (input/resize-if-needed (q/width) (q/height)
                              core/resize core/playfield-width core/playfield-height)
      (as-> s (apply-command s (input/aim-command (q/mouse-x) (q/mouse-y))))))

(defn draw
  [state]
  (render/draw-state! state (q/mouse-x) (q/mouse-y)))

(defn mouse-moved
  [state _event]
  ;; Still route aim on move events for responsiveness between update ticks.
  (apply-command state (input/aim-command (q/mouse-x) (q/mouse-y))))

(defn mouse-dragged
  [state event]
  (mouse-moved state event))

(defn key-pressed
  [state _event]
  (let [ch (q/raw-key)]
    (cond
      (input/escape-key? ch)
      (do (q/exit) state)

      (input/key-char->command ch)
      (apply-command state (input/key-char->command ch))

      :else state)))

(defn run-sketch!
  "Open the playfield window and process UI events until quit."
  ([]
   (run-sketch! default-width default-height))
  ([width height]
   (q/sketch
    :title "Missile Command"
    :size [width height]
    :setup setup
    :update update-state
    :draw draw
    :mouse-moved mouse-moved
    :mouse-dragged mouse-dragged
    :key-pressed key-pressed
    :middleware [m/fun-mode]
    :features [:resizable]
    :settings #(q/smooth 2))))

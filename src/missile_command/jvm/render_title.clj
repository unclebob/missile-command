(ns missile-command.jvm.render-title
  "Host drawing for the title screen."
  (:require [quil.core :as q]
            [missile-command.core :as core]))

(defn- button!
  [{:keys [x y w h label]}]
  (q/fill 20 30 45 220)
  (q/stroke 255 220 80)
  (q/stroke-weight 2)
  (q/rect x y w h 6)
  (q/no-stroke)
  (q/fill 245)
  (q/text-size 18)
  (q/text-align :center :center)
  (q/text label (+ x (/ w 2.0)) (+ y (/ h 2.0))))

(defn overlay!
  [state]
  (let [w (core/playfield-width state)
        h (core/playfield-height state)
        name (core/title-game-name-of state)
        prompt (or (:title-start-affordance state) "Press Enter or click to start")]
    (q/fill 0 0 0 160)
    (q/no-stroke)
    (q/rect 0 0 w h)
    (q/fill 255 220 80)
    (q/text-align :center :center)
    (q/text-size 48)
    (q/text name (/ w 2.0) (/ h 2.0))
    (q/fill 220)
    (q/text-size 18)
    (q/text prompt (/ w 2.0) (+ (/ h 2.0) 60))
    (doseq [button (core/title-buttons state)]
      (button! button))
    (q/text-align :left :baseline)))

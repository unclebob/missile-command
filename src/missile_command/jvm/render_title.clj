(ns missile-command.jvm.render-title
  "Host drawing for the title screen."
  (:require [quil.core :as q]
            [missile-command.core :as core]))

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
    (q/fill 180)
    (q/text-size 16)
    (q/text "H for high scores" (/ w 2.0) (+ (/ h 2.0) 95))
    (q/text-align :left :baseline)))

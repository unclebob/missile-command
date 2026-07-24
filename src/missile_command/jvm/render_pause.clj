(ns missile-command.jvm.render-pause
  "Host drawing for the paused overlay."
  (:require [quil.core :as q]
            [missile-command.core :as core]))

(defn overlay!
  [state]
  (let [w (core/playfield-width state)
        h (core/playfield-height state)]
    (q/fill 0 0 0 120)
    (q/no-stroke)
    (q/rect 0 0 w h)
    (q/fill 255 255 100)
    (q/text-align :center :center)
    (q/text-size 36)
    (q/text "PAUSED" (/ w 2.0) (/ h 2.0))
    (q/text-align :left :baseline)))

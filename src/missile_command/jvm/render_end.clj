(ns missile-command.jvm.render-end
  "Host drawing for THE END overlay."
  (:require [quil.core :as q]
            [missile-command.core :as core]))

(defn overlay!
  "Draw the centered end fireball and clip THE END letters to its disk."
  [state]
  (when-let [fb (core/end-fireball state)]
    (let [r (double (:radius fb 0.0))
          cx (double (:x fb))
          cy (double (:y fb))
          msg (or (core/end-message state) "THE END")
          max-r (double (:max-radius fb))
          text-h (max 12.0 (* 0.28 (* 2.0 max-r)))]
      (when (pos? r)
        (q/no-stroke)
        (q/fill 255 120 30 160)
        (q/ellipse cx cy (* 2 r) (* 2 r))
        (q/fill 255 200 80 200)
        (q/ellipse cx cy r r)
        (when (pos? (core/end-message-reveal state))
          (q/fill 20 0 0)
          (q/text-align :center :center)
          (q/text-size text-h)
          (q/text msg cx cy)
          (q/text-align :left :baseline))))))

(ns missile-command.jvm.render-options
  "Host drawing for the options screen."
  (:require [clojure.string :as str]
            [quil.core :as q]
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

(defn- checkbox!
  [{:keys [x y w h label checked?]}]
  (q/fill 10 15 24 230)
  (q/stroke 255 220 80)
  (q/stroke-weight 2)
  (q/rect x y w h 3)
  (when checked?
    (q/stroke 180 255 160)
    (q/stroke-weight 3)
    (q/line (+ x 5) (+ y (/ h 2.0)) (+ x 10) (+ y h -6))
    (q/line (+ x 10) (+ y h -6) (+ x w -4) (+ y 5)))
  (q/no-stroke)
  (q/fill 230)
  (q/text-size 18)
  (q/text-align :left :center)
  (q/text label (+ x w 12) (+ y (/ h 2.0))))

(defn overlay!
  [state]
  (let [w (core/playfield-width state)
        h (core/playfield-height state)
        opts (core/game-options state)
        diff (name (or (:difficulty opts) :arcade))
        left (str/join "," (sort (map str (get-in opts [:keys :fire :left] #{}))))
        center (str/join "," (sort (map str (get-in opts [:keys :fire :center] #{}))))
        right (str/join "," (sort (map str (get-in opts [:keys :fire :right] #{}))))
        pause (str/join "," (sort (map str (get-in opts [:keys :pause] #{}))))]
    (q/fill 0 0 0 175)
    (q/no-stroke)
    (q/rect 0 0 w h)
    (q/text-align :center :center)
    (q/fill 255 220 80)
    (q/text-size 36)
    (q/text "OPTIONS" (/ w 2.0) (- (/ h 2.0) 110))
    (q/fill 230)
    (q/text-size 18)
    (checkbox! (core/mute-checkbox state))
    (q/text (str "Difficulty: " diff "  (1 easy  2 normal  3 arcade)")
            (/ w 2.0) (- (/ h 2.0) 15))
    (q/text (str "Fire L/C/R: " left " / " center " / " right)
            (/ w 2.0) (+ (/ h 2.0) 25))
    (q/text (str "Pause: " pause)
            (/ w 2.0) (+ (/ h 2.0) 55))
    (doseq [button (core/options-buttons state)]
      (button! button))
    (q/text-align :left :baseline)))

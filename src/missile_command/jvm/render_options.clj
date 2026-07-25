(ns missile-command.jvm.render-options
  "Host drawing for the options screen."
  (:require [clojure.string :as str]
            [quil.core :as q]
            [missile-command.core :as core]))

(defn overlay!
  [state]
  (let [w (core/playfield-width state)
        h (core/playfield-height state)
        opts (core/game-options state)
        mute? (boolean (:mute opts))
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
    (q/text (str "Mute: " (if mute? "ON" "OFF") "  (M)")
            (/ w 2.0) (- (/ h 2.0) 50))
    (q/text (str "Difficulty: " diff "  (1 easy  2 normal  3 arcade)")
            (/ w 2.0) (- (/ h 2.0) 15))
    (q/text (str "Fire L/C/R: " left " / " center " / " right)
            (/ w 2.0) (+ (/ h 2.0) 25))
    (q/text (str "Pause: " pause)
            (/ w 2.0) (+ (/ h 2.0) 55))
    (q/fill 180)
    (q/text-size 16)
    (q/text "Esc or O to return to title"
            (/ w 2.0) (+ (/ h 2.0) 100))
    (q/text-align :left :baseline)))

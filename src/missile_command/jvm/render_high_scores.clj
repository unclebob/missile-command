(ns missile-command.jvm.render-high-scores
  "Host drawing for high-score entry and table view."
  (:require [quil.core :as q]
            [missile-command.core :as core]))

(defn- dim!
  [w h]
  (q/fill 0 0 0 170)
  (q/no-stroke)
  (q/rect 0 0 w h))

(defn entry-overlay!
  "Initials entry after a qualifying THE END score."
  [state draft]
  (let [w (core/playfield-width state)
        h (core/playfield-height state)
        pending (or (core/pending-high-score state) (core/final-score state) 0)
        shown (let [d (str (or draft ""))]
                (if (seq d) d "___"))
        pad (apply str (repeat (max 0 (- 3 (count shown))) "_"))
        display (str shown pad)]
    (dim! w h)
    (q/text-align :center :center)
    (q/fill 255 220 80)
    (q/text-size 36)
    (q/text "HIGH SCORE" (/ w 2.0) (- (/ h 2.0) 70))
    (q/fill 220)
    (q/text-size 20)
    (q/text (str "Score: " pending) (/ w 2.0) (- (/ h 2.0) 20))
    (q/fill 255 255 120)
    (q/text-size 42)
    (q/text display (/ w 2.0) (+ (/ h 2.0) 30))
    (q/fill 200)
    (q/text-size 16)
    (q/text "Type 3 initials, Enter to save" (/ w 2.0) (+ (/ h 2.0) 80))
    (q/text-align :left :baseline)))

(defn table-overlay!
  "Ranked high-score table from title."
  [state]
  (let [w (core/playfield-width state)
        h (core/playfield-height state)
        table (core/high-score-table state)
        start-y (- (/ h 2.0) 120)]
    (dim! w h)
    (q/text-align :center :center)
    (q/fill 255 220 80)
    (q/text-size 36)
    (q/text "HIGH SCORES" (/ w 2.0) start-y)
    (q/text-size 18)
    (if (seq table)
      (doseq [[i e] (map-indexed vector table)]
        (let [y (+ start-y 50 (* i 28))
              rank (inc i)
              line (str rank ".  " (:initials e) "   " (:score e))]
          (q/fill 230)
          (q/text line (/ w 2.0) y)))
      (do (q/fill 200)
          (q/text "No scores yet" (/ w 2.0) (+ start-y 60))))
    (q/fill 180)
    (q/text-size 16)
    (q/text "Esc or H to return" (/ w 2.0) (+ start-y 50 (* 11 28)))
    (q/text-align :left :baseline)))

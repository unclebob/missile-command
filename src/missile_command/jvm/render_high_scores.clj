(ns missile-command.jvm.render-high-scores
  "Host drawing for high-score entry and table view."
  (:require [quil.core :as q]
            [missile-command.core :as core]
            [missile-command.global-scores :as global]))

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
        display (let [d (str (or draft ""))]
                  (if (seq d) d "Your Name"))]
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
    (q/text "Type your name, Enter to save" (/ w 2.0) (+ (/ h 2.0) 80))
    (q/text-align :left :baseline)))

(defn- short-row
  [rank name score]
  (str rank ".  " name "   " score))

(defn- local-table
  [state]
  (map-indexed
   (fn [i e]
     {:rank (inc i)
      :name (global/entry-label e)
      :score (:score e)})
   (core/high-score-table state)))

(defn- global-table
  [state]
  (map
   (fn [e]
     {:rank (:rank e)
      :name (global/entry-label e)
      :score (:score e)})
   (get-in state [:global-high-scores :scores])))

(defn- draw-rows!
  [rows start-y empty-text]
  (q/text-size 18)
  (if (seq rows)
    (doseq [e rows]
      (let [i (dec (long (:rank e)))
            y (+ start-y 50 (* i 28))
            line (short-row (:rank e) (:name e) (:score e))]
        (q/fill 230)
        (q/text line (/ (q/width) 2.0) y)))
    (do (q/fill 200)
        (q/text empty-text (/ (q/width) 2.0) (+ start-y 60)))))

(defn- draw-global!
  [state start-y]
  (let [global (:global-high-scores state)
        status (:status global)]
    (q/fill 180)
    (q/text-size 15)
    (q/text (global/display-name global) (/ (q/width) 2.0) (+ start-y 28))
    (case status
      :ready (draw-rows! (global-table state) (+ start-y 22) "No global scores yet")
      :loading (do (q/fill 200)
                   (q/text-size 18)
                   (q/text "Loading..." (/ (q/width) 2.0) (+ start-y 82)))
      (do (q/fill 200)
          (q/text-size 18)
          (q/text "Unavailable" (/ (q/width) 2.0) (+ start-y 82))))))

(defn table-overlay!
  "Ranked high-score table from title."
  [state]
  (let [w (core/playfield-width state)
        h (core/playfield-height state)
        page (or (:high-score-page state) :local)
        start-y (- (/ h 2.0) 120)
        title (case page
                :global "HIGH SCORES - GLOBAL"
                "HIGH SCORES - LOCAL")]
    (dim! w h)
    (q/text-align :center :center)
    (q/fill 255 220 80)
    (q/text-size 36)
    (q/text title (/ w 2.0) start-y)
    (if (= :global page)
      (draw-global! state start-y)
      (draw-rows! (local-table state) start-y "No local scores yet"))
    (q/fill 180)
    (q/text-size 16)
    (q/text "H to return, Esc to quit" (/ w 2.0) (+ start-y 50 (* 11 28)))
    (q/text-align :left :baseline)))

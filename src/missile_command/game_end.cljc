(ns missile-command.game-end
  "Pure THE END geometry and presentation helpers."
  (:require [missile-command.missiles :as missiles]))

(def message-text "THE END")
(def wrong-message-text "Game Over")

(defn fill-radius
  "Radius from center that covers the playfield corners."
  [width height]
  (let [w (double width)
        h (double height)]
    (Math/ceil (Math/sqrt (+ (* (/ w 2.0) (/ w 2.0))
                             (* (/ h 2.0) (/ h 2.0)))))))

(defn make-fireball
  [fireball-id width height expand-seconds contract-seconds]
  (let [cx (quot width 2)
        cy (quot height 2)]
    {:id fireball-id
     :x cx
     :y cy
     :age 0.0
     :radius 0.0
     :max-radius (fill-radius width height)
     :expand-seconds expand-seconds
     :contract-seconds contract-seconds
     :end-fireball? true}))

(defn reveal-fraction
  [fireball]
  (let [max-r (double (or (:max-radius fireball) 1.0))
        r (double (or (:radius fireball) 0.0))]
    (min 1.0 (/ r max-r))))

(defn fireball-centered?
  [fireball width height]
  (and fireball
       (= (long (:x fireball)) (quot width 2))
       (= (long (:y fireball)) (quot height 2))))

(defn fireball-fills-playfield?
  [fireball]
  (and fireball
       (>= (double (:radius fireball))
           (* 0.99 (double (:max-radius fireball))))))

(defn message-layout
  "Glyph bounds: square matching the max end-fireball diameter."
  [fireball]
  (let [r (double (or (:max-radius fireball) 0.0))
        d (* 2.0 r)]
    {:center-x (double (or (:x fireball) 0.0))
     :center-y (double (or (:y fireball) 0.0))
     :width d
     :height d
     :radius r}))

(defn message-fills-max-expanse?
  [fireball]
  (let [layout (message-layout fireball)
        d (* 2.0 (double (:max-radius fireball 0.0)))]
    (and fireball
         (= (:width layout) d)
         (= (:height layout) d))))

(defn message-centered?
  [layout width height]
  (and (= (long (:center-x layout)) (quot width 2))
       (= (long (:center-y layout)) (quot height 2))))

(defn point-visible?
  [fireball x y]
  (boolean
   (when fireball
     (missiles/point-in-fireball? fireball x y))))

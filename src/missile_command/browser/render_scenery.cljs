(ns missile-command.browser.render-scenery
  "Browser ground scenery: sky, ground band, cities, batteries."
  (:require [quil.core :as q :include-macros true]
            [missile-command.core :as core]
            [missile-command.world :as world]))

(defn sky!
  [w h]
  (q/no-stroke)
  (q/fill 18 24 56)
  (q/rect 0 0 w h))

(defn ground!
  [state]
  (let [h (core/playfield-height state)
        band (world/ground-band h)
        top (:top band)
        w (core/playfield-width state)]
    (q/no-stroke)
    (q/fill 34 78 42)
    (q/rect 0 top w (- h top))
    (q/fill 48 100 55)
    (q/rect 0 top w 3)))

(defn- city-building!
  [cx base-y bw bh]
  (q/fill 160 175 195)
  (q/rect (- cx (quot bw 2)) (- base-y bh) bw bh)
  (q/fill 220 230 140)
  (let [left (+ (- cx (quot bw 2)) 2)
        top (+ (- base-y bh) 4)
        right (- (+ cx (quot bw 2)) 2)]
    (when (> (- right left) 2)
      (q/rect left top (- right left) 2)
      (when (> bh 14)
        (q/rect left (+ top 8) (- right left) 2)))))

(defn- city-rubble!
  "Destroyed city: plaza + broken building stubs (rubble)."
  [x y]
  (q/no-stroke)
  (q/fill 55 45 40)
  (q/rect (- x 18) (- y 4) 36 6)
  (q/fill 90 80 70)
  (q/triangle (- x 14) y (- x 8) (- y 10) (- x 2) y)
  (q/triangle (- x 4) y (+ x 2) (- y 14) (+ x 8) y)
  (q/triangle (+ x 4) y (+ x 12) (- y 8) (+ x 16) y)
  (q/fill 70 60 55)
  (q/rect (- x 12) (- y 8) 5 6)
  (q/rect (+ x 4) (- y 6) 4 4)
  (q/fill 40 30 28)
  (q/ellipse x (- y 2) 10 4))

(defn cities!
  [state]
  (doseq [city (core/cities state)]
    (let [x (:x city)
          y (:y city)]
      (if (:alive? city)
        (do
          (city-building! (- x 10) y 8 22)
          (city-building! x y 10 30)
          (city-building! (+ x 11) y 7 18)
          (q/fill 90 100 110)
          (q/rect (- x 16) (- y 3) 32 4))
        (city-rubble! x y)))))

(defn- ammo-triangle-positions
  "Classic 10-missile triangle under a battery: rows 4-3-2-1 bottom→top."
  [cx base-y]
  (let [row-counts [4 3 2 1]
        spacing 5
        row-h 5
        y0 (+ base-y 8)]
    (vec
     (mapcat (fn [row n]
               (let [y (+ y0 (* row row-h))
                     width (* (dec n) spacing)
                     x0 (- cx (quot width 2))]
                 (map (fn [i] [(+ x0 (* i spacing)) y]) (range n))))
             (range)
             row-counts))))

(defn- ammo-dots!
  [cx base-y ammo]
  (let [n (max 0 (min 10 (long ammo)))
        dots (take n (ammo-triangle-positions cx base-y))]
    (q/no-stroke)
    (q/fill 240 230 120)
    (doseq [[dx dy] dots]
      (q/ellipse dx dy 3 3))))

(defn- launcher!
  [x y destroyed? ammo]
  (q/no-stroke)
  (if destroyed?
    (do
      (q/fill 70 35 35)
      (q/rect (- x 12) (- y 6) 24 6)
      (q/fill 55 30 30)
      (q/triangle x (- y 22) (- x 6) (- y 6) (+ x 6) (- y 6)))
    (do
      (q/fill 90 90 95)
      (q/rect (- x 14) (- y 5) 28 6)
      (q/fill 210 205 190)
      (q/rect (- x 5) (- y 28) 10 24)
      (q/fill 200 80 70)
      (q/triangle x (- y 40) (- x 5) (- y 28) (+ x 5) (- y 28))
      (q/fill 170 165 150)
      (q/triangle (- x 5) (- y 10) (- x 12) (- y 4) (- x 5) (- y 4))
      (q/triangle (+ x 5) (- y 10) (+ x 12) (- y 4) (+ x 5) (- y 4))
      (q/fill 60 90 160)
      (q/rect (- x 5) (- y 18) 10 3)
      (ammo-dots! x y (or ammo 0)))))

(defn batteries!
  [state]
  (doseq [bat (core/batteries state)]
    (launcher! (:x bat) (:y bat) (:destroyed? bat) (:missiles bat))))

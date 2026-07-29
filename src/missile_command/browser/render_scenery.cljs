(ns missile-command.browser.render-scenery
  "Browser ground scenery: sky, ground band, cities, batteries."
  (:require [quil.core :as q :include-macros true]
            [missile-command.core :as core]
            [missile-command.world :as world]))

(def baseline-width 800.0)
(def baseline-height 600.0)

(defn- icon-scale
  [state]
  (min (/ (double (core/playfield-width state)) baseline-width)
       (/ (double (core/playfield-height state)) baseline-height)))

(defn- s
  [scale n]
  (* scale n))

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
  [scale cx base-y bw bh]
  (q/fill 160 175 195)
  (q/rect (- cx (/ bw 2.0)) (- base-y bh) bw bh)
  (q/fill 220 230 140)
  (let [window-margin (s scale 2)
        window-top (s scale 4)
        window-height (max 1.0 (s scale 2))
        window-row-gap (s scale 8)
        left (+ (- cx (/ bw 2.0)) window-margin)
        top (+ (- base-y bh) window-top)
        right (- (+ cx (/ bw 2.0)) window-margin)]
    (when (> (- right left) (s scale 2))
      (q/rect left top (- right left) window-height)
      (when (> bh (s scale 14))
        (q/rect left (+ top window-row-gap) (- right left) window-height)))))

(defn- city-rubble!
  "Destroyed city: plaza + broken building stubs (rubble)."
  [scale x y]
  (q/no-stroke)
  (q/fill 55 45 40)
  (q/rect (- x (s scale 18)) (- y (s scale 4)) (s scale 36) (s scale 6))
  (q/fill 90 80 70)
  (q/triangle (- x (s scale 14)) y (- x (s scale 8)) (- y (s scale 10)) (- x (s scale 2)) y)
  (q/triangle (- x (s scale 4)) y (+ x (s scale 2)) (- y (s scale 14)) (+ x (s scale 8)) y)
  (q/triangle (+ x (s scale 4)) y (+ x (s scale 12)) (- y (s scale 8)) (+ x (s scale 16)) y)
  (q/fill 70 60 55)
  (q/rect (- x (s scale 12)) (- y (s scale 8)) (s scale 5) (s scale 6))
  (q/rect (+ x (s scale 4)) (- y (s scale 6)) (s scale 4) (s scale 4))
  (q/fill 40 30 28)
  (q/ellipse x (- y (s scale 2)) (s scale 10) (s scale 4)))

(defn cities!
  [state]
  (let [scale (icon-scale state)]
    (doseq [city (core/cities state)]
      (let [x (:x city)
          y (:y city)]
        (if (:alive? city)
          (do
            (city-building! scale (- x (s scale 10)) y (s scale 8) (s scale 22))
            (city-building! scale x y (s scale 10) (s scale 30))
            (city-building! scale (+ x (s scale 11)) y (s scale 7) (s scale 18))
            (q/fill 90 100 110)
            (q/rect (- x (s scale 16)) (- y (s scale 3)) (s scale 32) (s scale 4)))
          (city-rubble! scale x y))))))

(defn- ammo-triangle-positions
  "Classic 10-missile triangle under a battery: rows 4-3-2-1 bottom→top."
  [scale cx base-y]
  (let [row-counts [4 3 2 1]
        spacing (s scale 5)
        row-h (s scale 5)
        y0 (+ base-y (s scale 8))]
    (vec
     (mapcat (fn [row n]
               (let [y (+ y0 (* row row-h))
                     width (* (dec n) spacing)
                     x0 (- cx (/ width 2.0))]
                 (map (fn [i] [(+ x0 (* i spacing)) y]) (range n))))
             (range)
             row-counts))))

(defn- ammo-dots!
  [scale cx base-y ammo]
  (let [n (max 0 (min 10 (long ammo)))
        dots (take n (ammo-triangle-positions scale cx base-y))
        dot-size (max 1.0 (s scale 3))]
    (q/no-stroke)
    (q/fill 240 230 120)
    (doseq [[dx dy] dots]
      (q/ellipse dx dy dot-size dot-size))))

(defn- launcher!
  [scale x y destroyed? ammo]
  (q/no-stroke)
  (if destroyed?
    (do
      (q/fill 70 35 35)
      (q/rect (- x (s scale 12)) (- y (s scale 6)) (s scale 24) (s scale 6))
      (q/fill 55 30 30)
      (q/triangle x (- y (s scale 22)) (- x (s scale 6)) (- y (s scale 6)) (+ x (s scale 6)) (- y (s scale 6))))
    (do
      (q/fill 90 90 95)
      (q/rect (- x (s scale 14)) (- y (s scale 5)) (s scale 28) (s scale 6))
      (q/fill 210 205 190)
      (q/rect (- x (s scale 5)) (- y (s scale 28)) (s scale 10) (s scale 24))
      (q/fill 200 80 70)
      (q/triangle x (- y (s scale 40)) (- x (s scale 5)) (- y (s scale 28)) (+ x (s scale 5)) (- y (s scale 28)))
      (q/fill 170 165 150)
      (q/triangle (- x (s scale 5)) (- y (s scale 10)) (- x (s scale 12)) (- y (s scale 4)) (- x (s scale 5)) (- y (s scale 4)))
      (q/triangle (+ x (s scale 5)) (- y (s scale 10)) (+ x (s scale 12)) (- y (s scale 4)) (+ x (s scale 5)) (- y (s scale 4)))
      (q/fill 60 90 160)
      (q/rect (- x (s scale 5)) (- y (s scale 18)) (s scale 10) (s scale 3))
      (ammo-dots! scale x y (or ammo 0)))))

(defn batteries!
  [state]
  (let [scale (icon-scale state)]
    (doseq [bat (core/batteries state)]
      (launcher! scale (:x bat) (:y bat) (:destroyed? bat) (:missiles bat)))))

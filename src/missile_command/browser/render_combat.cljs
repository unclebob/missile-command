(ns missile-command.browser.render-combat
  "Browser combat sprites: missiles, enemies, flyers, fireballs."
  (:require [quil.core :as q :include-macros true]
            [missile-command.core :as core]))

(defn missiles!
  "Trail from launch to current progress tip."
  [state]
  (doseq [m (core/defensive-missiles state)]
    (let [x (double (or (:x m) (:x0 m)))
          y (double (or (:y m) (:y0 m)))]
      (q/stroke 255 220 120)
      (q/stroke-weight 2)
      (q/line (:x0 m) (:y0 m) x y)
      (q/fill 255 240 160)
      (q/no-stroke)
      (q/ellipse x y 7 7)))
  (q/no-stroke))

(defn- draw-enemy-line-tip!
  [e x y stroke-rgb fill-rgb weight diameter]
  (let [[sr sg sb] stroke-rgb
        [fr fg fb] fill-rgb]
    (q/stroke sr sg sb)
    (q/stroke-weight weight)
    (q/line (:x0 e) (:y0 e) x y)
    (q/fill fr fg fb)
    (q/no-stroke)
    (q/ellipse x y diameter diameter)))

(defn- draw-smart-enemy!
  [e x y smart-evaded?]
  (if smart-evaded?
    (do (q/stroke 255 255 255)
        (q/stroke-weight 3)
        (q/line (:x0 e) (:y0 e) x y)
        (q/fill 180 255 255)
        (q/no-stroke)
        (q/quad x (- y 9) (+ x 9) y x (+ y 9) (- x 9) y))
    (do (q/stroke 80 255 220)
        (q/stroke-weight 2)
        (q/line (:x0 e) (:y0 e) x y)
        (q/fill 40 255 200)
        (q/no-stroke)
        (q/quad x (- y 7) (+ x 7) y x (+ y 7) (- x 7) y))))

(defn enemies!
  [state]
  (doseq [e (core/enemy-missiles state)]
    (let [x (double (or (:x e) (:x0 e)))
          y (double (or (:y e) (:y0 e)))
          kind (:enemy-kind e)
          dropped? (:dropped-from-flyer? e)
          smart-evaded? (and (= kind core/enemy-kind-smart) (:smart-evaded? e))]
      (cond
        (= kind core/enemy-kind-mirv)
        (draw-enemy-line-tip! e x y [255 200 40] [255 220 60] 3 12)
        (= kind core/enemy-kind-mirv-child)
        (draw-enemy-line-tip! e x y [255 140 40] [255 120 40] 2 7)
        (= kind core/enemy-kind-smart)
        (draw-smart-enemy! e x y smart-evaded?)
        dropped?
        (draw-enemy-line-tip! e x y [255 80 220] [255 60 200] 2 9)
        :else
        (draw-enemy-line-tip! e x y [255 80 80] [255 60 60] 2 8))))
  (q/no-stroke))

(defn- bomber!
  [x y]
  (q/fill 200 200 220)
  (q/no-stroke)
  (q/ellipse x y 18 8)
  (q/fill 160 160 180)
  (q/triangle (- x 14) y (- x 4) (- y 6) (- x 4) (+ y 6))
  (q/triangle (+ x 14) y (+ x 4) (- y 6) (+ x 4) (+ y 6))
  (q/fill 255 80 80)
  (q/ellipse (+ x 6) y 3 3))

(defn- satellite!
  [x y]
  (q/fill 180 255 120)
  (q/no-stroke)
  (q/ellipse x y 10 10)
  (q/fill 120 200 80)
  (q/quad x (- y 10) (+ x 8) y x (+ y 10) (- x 8) y)
  (q/stroke 180 255 120)
  (q/stroke-weight 1)
  (q/line (- x 12) y (+ x 12) y)
  (q/no-stroke))

(defn flyers!
  [state]
  (doseq [f (core/flyers state)]
    (let [x (double (or (:x f) (:x0 f)))
          y (double (or (:y f) (:y0 f)))]
      (if (= :bomber (:kind f))
        (bomber! x y)
        (satellite! x y))))
  (q/no-stroke))

(defn fireballs!
  [state]
  (doseq [fb (core/fireballs state)]
    (let [r (double (:radius fb 0.0))]
      (when (pos? r)
        (q/no-stroke)
        (q/fill 255 160 40 140)
        (q/ellipse (:x fb) (:y fb) (* 2 r) (* 2 r))
        (q/fill 255 220 120 180)
        (q/ellipse (:x fb) (:y fb) r r)))))

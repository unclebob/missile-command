(ns missile-command.jvm.render
  "Host-only drawing for the JVM Quil sketch. Reads pure core state; no rules."
  (:require [quil.core :as q]
            [missile-command.core :as core]
            [missile-command.world :as world]))

(defn- sky!
  "Solid night sky (full frame clear — prevents crosshair ghosts)."
  [w h]
  (q/no-stroke)
  (q/fill 18 24 56)
  (q/rect 0 0 w h))

(defn- ground!
  [state]
  (let [h (core/playfield-height state)
        band (world/ground-band h)
        top (:top band)
        w (core/playfield-width state)]
    (q/no-stroke)
    (q/fill 34 78 42)
    (q/rect 0 top w (- h top))
    ;; subtle ground lip
    (q/fill 48 100 55)
    (q/rect 0 top w 3)))

(defn- city-building!
  [cx base-y bw bh]
  (q/fill 160 175 195)
  (q/rect (- cx (quot bw 2)) (- base-y bh) bw bh)
  (q/fill 220 230 140)
  (doseq [wy (range (+ (- base-y bh) 4) (- base-y 4) 6)
          wx (range (+ (- cx (quot bw 2)) 3) (+ cx (quot bw 2) -2) 5)]
    (q/rect wx wy 2 3)))

(defn- cities!
  [state]
  (doseq [city (core/cities state)]
    (when (:alive? city)
      (let [x (:x city)
            y (:y city)]
        ;; cluster of buildings of mixed heights
        (city-building! (- x 10) y 8 22)
        (city-building! x y 10 30)
        (city-building! (+ x 11) y 7 18)
        ;; base plaza
        (q/fill 90 100 110)
        (q/rect (- x 16) (- y 3) 32 4)))))

(defn- launcher!
  [x y destroyed?]
  (if destroyed?
    (do
      (q/fill 70 35 35)
      (q/rect (- x 16) (- y 8) 32 8)
      (q/fill 50 25 25)
      (q/rect (- x 4) (- y 18) 8 12))
    (do
      ;; concrete pad
      (q/fill 95 95 100)
      (q/rect (- x 18) (- y 6) 36 8)
      ;; bunker body
      (q/fill 150 145 130)
      (q/rect (- x 14) (- y 16) 28 12)
      ;; launch tube / silo
      (q/fill 70 75 85)
      (q/rect (- x 4) (- y 34) 8 20)
      ;; tube rim
      (q/fill 200 190 80)
      (q/ellipse x (- y 34) 12 6)
      ;; small radar dish
      (q/stroke 180 180 190)
      (q/stroke-weight 1)
      (q/no-fill)
      (q/arc (+ x 10) (- y 20) 10 10 q/PI q/TWO-PI)
      (q/no-stroke))))

(defn- batteries!
  [state]
  (doseq [bat (core/batteries state)]
    (launcher! (:x bat) (:y bat) (:destroyed? bat))))

(defn- missiles!
  [state]
  (q/stroke 255 220 120)
  (q/stroke-weight 2)
  (doseq [m (core/defensive-missiles state)]
    (q/line (:x0 m) (:y0 m) (:x1 m) (:y1 m))
    (q/fill 255 240 160)
    (q/no-stroke)
    (q/ellipse (:x1 m) (:y1 m) 6 6)
    (q/stroke 255 220 120)
    (q/stroke-weight 2))
  (q/no-stroke))

(defn- crosshair!
  [state]
  (let [{:keys [x y]} (core/crosshair state)]
    (q/stroke 255 70 70)
    (q/stroke-weight 2)
    (q/no-fill)
    (q/ellipse x y 20 20)
    (q/line (- x 14) y (+ x 14) y)
    (q/line x (- y 14) x (+ y 14))
    (q/no-stroke)))

(defn- hud!
  [state]
  (let [line (str "Ammo L:" (:missiles (core/battery state :left))
                  " C:" (:missiles (core/battery state :center))
                  " R:" (:missiles (core/battery state :right))
                  "  Score:" (core/score state)
                  "  | Z/1 X/2 C/3 fire  Esc quit")]
    (q/fill 0 0 0 140)
    (q/no-stroke)
    (q/rect 0 0 (core/playfield-width state) 32)
    (q/fill 240)
    (q/text-size 14)
    (q/text line 12 22)))

(defn draw-state!
  "Draw one frame from core game state."
  [state]
  (let [w (core/playfield-width state)
        h (core/playfield-height state)]
    (sky! w h)
    (missiles! state)
    (ground! state)
    (cities! state)
    (batteries! state)
    (crosshair! state)
    (hud! state)))

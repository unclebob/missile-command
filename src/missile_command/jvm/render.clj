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
  ;; simple window bands (cheap; keeps frame rate high for aim tracking)
  (q/fill 220 230 140)
  (let [left (+ (- cx (quot bw 2)) 2)
        top (+ (- base-y bh) 4)
        right (- (+ cx (quot bw 2)) 2)]
    (when (> (- right left) 2)
      (q/rect left top (- right left) 2)
      (when (> bh 14)
        (q/rect left (+ top 8) (- right left) 2)))))

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
  "Draw a battery as a standing defensive missile on a small pad."
  [x y destroyed?]
  (q/no-stroke)
  (if destroyed?
    (do
      (q/fill 70 35 35)
      (q/rect (- x 12) (- y 6) 24 6)
      (q/fill 55 30 30)
      (q/triangle x (- y 22) (- x 6) (- y 6) (+ x 6) (- y 6)))
    (do
      ;; pad
      (q/fill 90 90 95)
      (q/rect (- x 14) (- y 5) 28 6)
      ;; body
      (q/fill 210 205 190)
      (q/rect (- x 5) (- y 28) 10 24)
      ;; nose cone
      (q/fill 200 80 70)
      (q/triangle x (- y 40) (- x 5) (- y 28) (+ x 5) (- y 28))
      ;; fins
      (q/fill 170 165 150)
      (q/triangle (- x 5) (- y 10) (- x 12) (- y 4) (- x 5) (- y 4))
      (q/triangle (+ x 5) (- y 10) (+ x 12) (- y 4) (+ x 5) (- y 4))
      ;; stripe
      (q/fill 60 90 160)
      (q/rect (- x 5) (- y 18) 10 3))))

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

(defn crosshair-at!
  "Draw crosshair at live screen coordinates (visual aim; avoids input lag)."
  [x y]
  (q/stroke 255 70 70)
  (q/stroke-weight 2)
  (q/no-fill)
  (q/ellipse x y 20 20)
  (q/line (- x 14) y (+ x 14) y)
  (q/line x (- y 14) x (+ y 14))
  (q/no-stroke))

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

(defn draw-world!
  "Draw playfield contents (everything except the live crosshair)."
  [state]
  (let [w (core/playfield-width state)
        h (core/playfield-height state)]
    (sky! w h)
    (missiles! state)
    (ground! state)
    (cities! state)
    (batteries! state)
    (hud! state)))

(defn draw-state!
  "Draw one frame including a crosshair at the given pointer position."
  ([state]
   (let [ch (core/crosshair state)]
     (draw-state! state (:x ch) (:y ch))))
  ([state crosshair-x crosshair-y]
   (draw-world! state)
   (crosshair-at! crosshair-x crosshair-y)))

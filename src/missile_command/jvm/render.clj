(ns missile-command.jvm.render
  "Host-only drawing for the JVM Quil sketch. Reads pure core state; no rules."
  (:require [quil.core :as q]
            [missile-command.core :as core]
            [missile-command.world :as world]))

(defn- sky!
  [w h]
  (doseq [y (range 0 h 4)]
    (let [t (/ (double y) (double (max 1 h)))]
      (q/stroke (q/lerp-color (q/color 12 18 48) (q/color 70 40 90) t))
      (q/line 0 y w y)))
  (q/no-stroke))

(defn- ground!
  [state]
  (let [h (core/playfield-height state)
        band (world/ground-band h)
        top (:top band)]
    (q/fill 40 90 50)
    (q/rect 0 top (core/playfield-width state) (- h top))))

(defn- cities!
  [state]
  (doseq [city (core/cities state)]
    (when (:alive? city)
      (q/fill 180 200 220)
      (q/rect (- (:x city) 10) (- (:y city) 18) 20 18)
      (q/fill 120 140 160)
      (q/rect (- (:x city) 4) (- (:y city) 28) 8 10))))

(defn- batteries!
  [state]
  (doseq [bat (core/batteries state)]
    (if (:destroyed? bat)
      (q/fill 80 40 40)
      (q/fill 220 180 60))
    (q/triangle (:x bat) (- (:y bat) 22)
                (- (:x bat) 14) (:y bat)
                (+ (:x bat) 14) (:y bat))))

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
    (q/stroke 255 80 80)
    (q/stroke-weight 2)
    (q/no-fill)
    (q/ellipse x y 18 18)
    (q/line (- x 12) y (+ x 12) y)
    (q/line x (- y 12) x (+ y 12))
    (q/no-stroke)))

(defn- hud!
  [state]
  (let [bats (core/batteries state)
        line (str "Ammo L:" (:missiles (core/battery state :left))
                  " C:" (:missiles (core/battery state :center))
                  " R:" (:missiles (core/battery state :right))
                  "  Score:" (core/score state)
                  "  | Z/1 X/2 C/3 fire  Esc quit")]
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

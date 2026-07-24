(ns missile-command.jvm.render
  "Host-only drawing for the JVM Quil sketch. Reads pure core state; no rules."
  (:require [quil.core :as q]
            [missile-command.core :as core]
            [missile-command.jvm.render-end :as render-end]
            [missile-command.jvm.render-scenery :as scenery]
            [missile-command.world :as world]))

(defn- missiles!
  "Trail from launch to current progress tip (not full instantaneous line)."
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

(defn- fireballs!
  [state]
  (doseq [fb (core/fireballs state)]
    (let [r (double (:radius fb 0.0))]
      (when (pos? r)
        (q/no-stroke)
        (q/fill 255 160 40 140)
        (q/ellipse (:x fb) (:y fb) (* 2 r) (* 2 r))
        (q/fill 255 220 120 180)
        (q/ellipse (:x fb) (:y fb) r r)))))

(defn- targets!
  [state]
  (doseq [t (core/destroyable-targets state)]
    (if (:destroyed? t)
      (q/fill 80 80 80)
      (q/fill 220 60 220))
    (q/no-stroke)
    (q/ellipse (:x t) (:y t) 12 12)))

(defn- enemies!
  [state]
  (doseq [e (core/enemy-missiles state)]
    (let [x (double (or (:x e) (:x0 e)))
          y (double (or (:y e) (:y0 e)))]
      (q/stroke 255 80 80)
      (q/stroke-weight 2)
      (q/line (:x0 e) (:y0 e) x y)
      (q/fill 255 60 60)
      (q/no-stroke)
      (q/ellipse x y 8 8)))
  (q/no-stroke))

(defn crosshair-at!
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
  (let [hud (core/hud state)
        line (str "Wave:" (:wave hud)
                  "  Mult:" (:multiplier hud) "x"
                  "  Ammo L:" (:missiles (core/battery state :left))
                  " C:" (:missiles (core/battery state :center))
                  " R:" (:missiles (core/battery state :right))
                  "  Score:" (:score hud)
                  "  FB:" (count (core/fireballs state))
                  "  | Z/1 X/2 C/3 fire  Esc quit")]
    (q/fill 0 0 0 140)
    (q/no-stroke)
    (q/rect 0 0 (core/playfield-width state) 32)
    (q/fill 240)
    (q/text-size 14)
    (q/text line 12 22)))

(defn draw-world!
  [state]
  (let [w (core/playfield-width state)
        h (core/playfield-height state)]
    (scenery/sky! w h)
    (when-not (core/the-end? state)
      (enemies! state)
      (missiles! state)
      (targets! state)
      (scenery/ground! state)
      (scenery/cities! state)
      (scenery/batteries! state)
      ;; Fireballs last among world so city/battery impacts draw on top of scenery.
      (fireballs! state)
      (hud! state))
    (when (core/the-end? state)
      (scenery/ground! state)
      (render-end/overlay! state)
      (hud! state))))

(defn draw-state!
  ([state]
   (let [ch (core/crosshair state)]
     (draw-state! state (:x ch) (:y ch))))
  ([state crosshair-x crosshair-y]
   (draw-world! state)
   (crosshair-at! crosshair-x crosshair-y)))

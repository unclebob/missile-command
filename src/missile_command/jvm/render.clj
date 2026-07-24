(ns missile-command.jvm.render
  "Host-only drawing for the JVM Quil sketch. Reads pure core state; no rules."
  (:require [quil.core :as q]
            [missile-command.core :as core]
            [missile-command.jvm.render-end :as render-end]
            [missile-command.jvm.render-high-scores :as render-hs]
            [missile-command.jvm.render-pause :as render-pause]
            [missile-command.jvm.render-title :as render-title]
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
  (when (:full-playing-hud? (core/hud state))
    (let [hud (core/hud state)
          line (str "Score:" (:score hud)
                    "  Wave:" (:wave hud)
                    "  Mult:" (:multiplier hud) "x"
                    "  Ammo L:" (:left-ammo hud)
                    " C:" (:center-ammo hud)
                    " R:" (:right-ammo hud)
                    "  Cities:" (:living-cities hud)
                    "  Bonus:" (:bonus-cities hud)
                    "  | P/Esc pause  Z/X/C fire")]
      (q/fill 0 0 0 160)
      (q/no-stroke)
      (q/rect 0 0 (core/playfield-width state) 36)
      (q/fill 240)
      (q/text-size 14)
      (q/text line 12 24))))


(defn draw-world!
  ([state]
   (draw-world! state ""))
  ([state initials-draft]
   (let [w (core/playfield-width state)
         h (core/playfield-height state)]
     (scenery/sky! w h)
     (cond
       (core/title? state)
       (do (scenery/ground! state)
           (render-title/overlay! state))

       (core/high-score-entry? state)
       (do (scenery/ground! state)
           (render-hs/entry-overlay! state initials-draft))

       (core/high-scores-view? state)
       (do (scenery/ground! state)
           (render-hs/table-overlay! state))

       (core/the-end? state)
       (do (scenery/ground! state)
           (render-end/overlay! state)
           (hud! state))

       :else
       (do (enemies! state)
           (missiles! state)
           (targets! state)
           (scenery/ground! state)
           (scenery/cities! state)
           (scenery/batteries! state)
           (fireballs! state)
           (hud! state)
           (when (core/paused? state)
             (render-pause/overlay! state)))))))

(defn draw-state!
  ([state]
   (let [ch (core/crosshair state)]
     (draw-state! state (:x ch) (:y ch) "")))
  ([state crosshair-x crosshair-y]
   (draw-state! state crosshair-x crosshair-y ""))
  ([state crosshair-x crosshair-y initials-draft]
   (draw-world! state initials-draft)
   (crosshair-at! crosshair-x crosshair-y)))

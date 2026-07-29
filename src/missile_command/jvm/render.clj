(ns missile-command.jvm.render
  "Host-only drawing for the JVM Quil sketch. Reads pure core state; no rules."
  (:require [quil.core :as q]
            [missile-command.core :as core]
            [missile-command.jvm.render-end :as render-end]
            [missile-command.jvm.render-high-scores :as render-hs]
            [missile-command.jvm.render-options :as render-options]
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
          y (double (or (:y e) (:y0 e)))
          kind (:enemy-kind e)
          dropped? (:dropped-from-flyer? e)
          smart-evaded? (and (= kind core/enemy-kind-smart) (:smart-evaded? e))]
      (cond
        (= kind core/enemy-kind-mirv)
        (do (q/stroke 255 200 40)
            (q/stroke-weight 3)
            (q/line (:x0 e) (:y0 e) x y)
            (q/fill 255 220 60)
            (q/no-stroke)
            (q/ellipse x y 12 12))
        (= kind core/enemy-kind-mirv-child)
        (do (q/stroke 255 140 40)
            (q/stroke-weight 2)
            (q/line (:x0 e) (:y0 e) x y)
            (q/fill 255 120 40)
            (q/no-stroke)
            (q/ellipse x y 7 7))
        (= kind core/enemy-kind-smart)
        (do (if smart-evaded?
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
        dropped?
        ;; Magenta — bombs released mid-flight from bombers/satellites.
        (do (q/stroke 255 80 220)
            (q/stroke-weight 2)
            (q/line (:x0 e) (:y0 e) x y)
            (q/fill 255 60 200)
            (q/no-stroke)
            (q/ellipse x y 9 9))
        :else
        (do (q/stroke 255 80 80)
            (q/stroke-weight 2)
            (q/line (:x0 e) (:y0 e) x y)
            (q/fill 255 60 60)
            (q/no-stroke)
            (q/ellipse x y 8 8)))))
  (q/no-stroke))

(defn- flyers!
  "Draw bombers (wide wings) and satellites (diamond + body) crossing the sky."
  [state]
  (doseq [f (core/flyers state)]
    (let [x (double (or (:x f) (:x0 f)))
          y (double (or (:y f) (:y0 f)))
          bomber? (= :bomber (:kind f))]
      (if bomber?
        (do (q/fill 200 200 220)
            (q/no-stroke)
            (q/ellipse x y 18 8)
            (q/fill 160 160 180)
            (q/triangle (- x 14) y (- x 4) (- y 6) (- x 4) (+ y 6))
            (q/triangle (+ x 14) y (+ x 4) (- y 6) (+ x 4) (+ y 6))
            (q/fill 255 80 80)
            (q/ellipse (+ x 6) y 3 3))
        (do (q/fill 180 255 120)
            (q/no-stroke)
            (q/ellipse x y 10 10)
            (q/fill 120 200 80)
            (q/quad x (- y 10) (+ x 8) y x (+ y 10) (- x 8) y)
            (q/stroke 180 255 120)
            (q/stroke-weight 1)
            (q/line (- x 12) y (+ x 12) y)
            (q/no-stroke)))))
  (q/no-stroke))

(defn- combat-background!
  "Combat elements that belong behind the ground line and launch sites."
  [state]
  (flyers! state)
  (targets! state))

(defn- combat-foreground!
  "Bomb trails, missile traces, and explosions draw over the ground."
  [state]
  (enemies! state)
  (missiles! state)
  (fireballs! state))

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
     (q/background 0)
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

       (core/options? state)
       (do (scenery/ground! state)
           (render-options/overlay! state))

       (core/the-end? state)
       (do (scenery/ground! state)
           (render-end/overlay! state)
           (hud! state))

       (core/wave-banner? state)
       (do (combat-background! state)
           (scenery/ground! state)
           (scenery/cities! state)
           (scenery/batteries! state)
           (let [pos (core/wave-banner-text-position state)
                 txt (core/wave-banner-text state)
                 sub (core/wave-banner-subtitle state)
                 w (core/playfield-width state)
                 h (core/playfield-height state)]
             (q/fill 0 0 0 100)
             (q/no-stroke)
             (q/rect 0 0 w h)
             (q/fill 255 220 80)
             (q/text-align :center :center)
             (q/text-size 48)
             (q/text txt (:x pos) (:y pos))
             (when (seq sub)
               (q/text-size 28)
               (q/fill 180 255 160)
               (q/text sub (:x pos) (+ (:y pos) 42.0)))
             (q/text-align :left :baseline))
           (combat-foreground! state)
           (hud! state))

       :else
       (do (combat-background! state)
           (scenery/ground! state)
           (scenery/cities! state)
           (scenery/batteries! state)
           (combat-foreground! state)
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

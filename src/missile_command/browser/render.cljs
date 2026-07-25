(ns missile-command.browser.render
  "Thin browser draw helpers (vector style, full canvas resolution)."
  (:require [quil.core :as q :include-macros true]
            [missile-command.core :as core]))

(defn- sky!
  [w h]
  (q/background 10 10 40)
  (q/fill 10 10 40)
  (q/rect 0 0 w h))

(defn- ground!
  [state]
  (let [w (core/playfield-width state)
        h (core/playfield-height state)
        gy (* h 0.88)]
    (q/fill 40 30 20)
    (q/no-stroke)
    (q/rect 0 gy w (- h gy))))

(defn- cities!
  [state]
  (doseq [c (core/cities state)]
    (if (:alive? c)
      (q/fill 80 180 255)
      (q/fill 60 60 60))
    (q/no-stroke)
    (q/rect (- (:x c) 10) (- (:y c) 12) 20 12)))

(defn- batteries!
  [state]
  (doseq [b (core/batteries state)]
    (if (:destroyed? b)
      (q/fill 80 40 40)
      (q/fill 200 200 80))
    (q/no-stroke)
    (q/triangle (:x b) (:y b)
                (- (:x b) 12) (+ (:y b) 16)
                (+ (:x b) 12) (+ (:y b) 16))))

(defn- missiles!
  [state]
  (doseq [m (core/defensive-missiles state)]
    (q/stroke 255 220 120)
    (q/stroke-weight 2)
    (q/line (:x0 m) (:y0 m) (:x m) (:y m)))
  (q/no-stroke))

(defn- enemies!
  [state]
  (doseq [e (core/enemy-missiles state)]
    (q/stroke 255 80 80)
    (q/stroke-weight 2)
    (q/line (:x0 e) (:y0 e) (:x e) (:y e)))
  (q/no-stroke))

(defn- fireballs!
  [state]
  (doseq [fb (core/fireballs state)]
    (let [r (double (:radius fb 0.0))]
      (when (pos? r)
        (q/no-stroke)
        (q/fill 255 160 40 140)
        (q/ellipse (:x fb) (:y fb) (* 2 r) (* 2 r))))))

(defn- hud!
  [state]
  (when (:full-playing-hud? (core/hud state))
    (let [hud (core/hud state)
          line (str "Score:" (:score hud)
                    " Wave:" (:wave hud)
                    " Mult:" (:multiplier hud) "x")]
      (q/fill 0 0 0 160)
      (q/no-stroke)
      (q/rect 0 0 (core/playfield-width state) 28)
      (q/fill 240)
      (q/text-size 14)
      (q/text line 10 20))))

(defn- title-overlay!
  [state]
  (let [w (core/playfield-width state)
        h (core/playfield-height state)]
    (q/fill 0 0 0 160)
    (q/rect 0 0 w h)
    (q/fill 255 220 80)
    (q/text-align :center :center)
    (q/text-size 42)
    (q/text (core/title-game-name-of state) (/ w 2.0) (/ h 2.0))
    (q/text-align :left :baseline)))

(defn draw-world!
  [state]
  (let [w (core/playfield-width state)
        h (core/playfield-height state)]
    (sky! w h)
    (cond
      (core/title? state)
      (do (ground! state) (title-overlay! state))

      :else
      (do (enemies! state)
          (missiles! state)
          (ground! state)
          (cities! state)
          (batteries! state)
          (fireballs! state)
          (hud! state)
          (when (core/paused? state)
            (q/fill 0 0 0 120)
            (q/rect 0 0 w h)
            (q/fill 255 255 100)
            (q/text-align :center :center)
            (q/text-size 32)
            (q/text "PAUSED" (/ w 2.0) (/ h 2.0))
            (q/text-align :left :baseline))))))

(defn crosshair-at!
  [x y]
  (q/stroke 255 70 70)
  (q/stroke-weight 2)
  (q/no-fill)
  (q/ellipse x y 20 20)
  (q/line (- x 14) y (+ x 14) y)
  (q/line x (- y 14) x (+ y 14))
  (q/no-stroke))

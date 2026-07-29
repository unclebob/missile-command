(ns missile-command.browser.render
  "Browser draw entry — composes scenery, combat, and shell overlays."
  (:require [quil.core :as q :include-macros true]
            [missile-command.core :as core]
            [missile-command.browser.render-combat :as combat]
            [missile-command.browser.render-scenery :as scenery]
            [missile-command.browser.render-shells :as shells]))

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
                    "  Bonus:" (:bonus-cities hud))]
      (q/fill 0 0 0 160)
      (q/no-stroke)
      (q/rect 0 0 (core/playfield-width state) 36)
      (q/fill 240)
      (q/text-size 14)
      (q/text-align :left :baseline)
      (q/text line 12 24))))

(defn- combat-background!
  "Combat elements that belong behind the ground line and launch sites."
  [state]
  (combat/flyers! state))

(defn- combat-foreground!
  "Bomb trails, missile traces, and explosions draw over the ground."
  [state]
  (combat/enemies! state)
  (combat/missiles! state)
  (combat/fireballs! state))

(defn- combat-field!
  "Flyers, ground, cities, batteries, bomb trails, missiles, fireballs (no HUD)."
  [state]
  (combat-background! state)
  (scenery/ground! state)
  (scenery/cities! state)
  (scenery/batteries! state)
  (combat-foreground! state))

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
       (do (scenery/ground! state) (shells/title-overlay! state))

       (core/the-end? state)
       (do (scenery/ground! state)
           (scenery/cities! state)
           (scenery/batteries! state)
           (shells/the-end-overlay! state)
           (hud! state))

       (core/wave-banner? state)
       (do (combat-field! state)
           (shells/wave-banner-overlay! state)
           (hud! state))

       (core/high-score-entry? state)
       (do (scenery/ground! state)
           (shells/high-score-entry-overlay! state initials-draft))

       (core/high-scores-view? state)
       (do (scenery/ground! state)
           (shells/high-scores-table-overlay! state))

       (core/options? state)
       (do (scenery/ground! state)
           (shells/options-overlay! state))

       :else
       (do (combat-field! state)
           (hud! state)
           (when (core/paused? state)
             (shells/pause-overlay! state)))))))

(defn crosshair-at!
  [x y]
  (q/stroke 255 70 70)
  (q/stroke-weight 2)
  (q/no-fill)
  (q/ellipse x y 20 20)
  (q/line (- x 14) y (+ x 14) y)
  (q/line x (- y 14) x (+ y 14))
  (q/no-stroke))

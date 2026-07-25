(ns missile-command.browser.render
  "Browser draw helpers — icons match the JVM desktop host scenery."
  (:require [clojure.string :as str]
            [quil.core :as q :include-macros true]
            [missile-command.core :as core]
            [missile-command.world :as world]))

(defn- sky!
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
    (q/fill 48 100 55)
    (q/rect 0 top w 3)))

(defn- city-building!
  [cx base-y bw bh]
  (q/fill 160 175 195)
  (q/rect (- cx (quot bw 2)) (- base-y bh) bw bh)
  (q/fill 220 230 140)
  (let [left (+ (- cx (quot bw 2)) 2)
        top (+ (- base-y bh) 4)
        right (- (+ cx (quot bw 2)) 2)]
    (when (> (- right left) 2)
      (q/rect left top (- right left) 2)
      (when (> bh 14)
        (q/rect left (+ top 8) (- right left) 2)))))

(defn- city-rubble!
  "Destroyed city: plaza + broken building stubs (rubble)."
  [x y]
  (q/no-stroke)
  (q/fill 55 45 40)
  (q/rect (- x 18) (- y 4) 36 6)
  (q/fill 90 80 70)
  (q/triangle (- x 14) y (- x 8) (- y 10) (- x 2) y)
  (q/triangle (- x 4) y (+ x 2) (- y 14) (+ x 8) y)
  (q/triangle (+ x 4) y (+ x 12) (- y 8) (+ x 16) y)
  (q/fill 70 60 55)
  (q/rect (- x 12) (- y 8) 5 6)
  (q/rect (+ x 4) (- y 6) 4 4)
  (q/fill 40 30 28)
  (q/ellipse x (- y 2) 10 4))

(defn- cities!
  [state]
  (doseq [city (core/cities state)]
    (let [x (:x city)
          y (:y city)]
      (if (:alive? city)
        (do
          (city-building! (- x 10) y 8 22)
          (city-building! x y 10 30)
          (city-building! (+ x 11) y 7 18)
          (q/fill 90 100 110)
          (q/rect (- x 16) (- y 3) 32 4))
        (city-rubble! x y)))))

(defn- ammo-triangle-positions
  "Classic 10-missile triangle under a battery: rows 4-3-2-1 bottom→top."
  [cx base-y]
  (let [row-counts [4 3 2 1]
        spacing 5
        row-h 5
        y0 (+ base-y 8)]
    (vec
     (mapcat (fn [row n]
               (let [y (+ y0 (* row row-h))
                     width (* (dec n) spacing)
                     x0 (- cx (quot width 2))]
                 (map (fn [i] [(+ x0 (* i spacing)) y]) (range n))))
             (range)
             row-counts))))

(defn- ammo-dots!
  [cx base-y ammo]
  (let [n (max 0 (min 10 (long ammo)))
        dots (take n (ammo-triangle-positions cx base-y))]
    (q/no-stroke)
    (q/fill 240 230 120)
    (doseq [[dx dy] dots]
      (q/ellipse dx dy 3 3))))

(defn- launcher!
  [x y destroyed? ammo]
  (q/no-stroke)
  (if destroyed?
    (do
      (q/fill 70 35 35)
      (q/rect (- x 12) (- y 6) 24 6)
      (q/fill 55 30 30)
      (q/triangle x (- y 22) (- x 6) (- y 6) (+ x 6) (- y 6)))
    (do
      (q/fill 90 90 95)
      (q/rect (- x 14) (- y 5) 28 6)
      (q/fill 210 205 190)
      (q/rect (- x 5) (- y 28) 10 24)
      (q/fill 200 80 70)
      (q/triangle x (- y 40) (- x 5) (- y 28) (+ x 5) (- y 28))
      (q/fill 170 165 150)
      (q/triangle (- x 5) (- y 10) (- x 12) (- y 4) (- x 5) (- y 4))
      (q/triangle (+ x 5) (- y 10) (+ x 12) (- y 4) (+ x 5) (- y 4))
      (q/fill 60 90 160)
      (q/rect (- x 5) (- y 18) 10 3)
      (ammo-dots! x y (or ammo 0)))))

(defn- batteries!
  [state]
  (doseq [bat (core/batteries state)]
    (launcher! (:x bat) (:y bat) (:destroyed? bat) (:missiles bat))))

(defn- missiles!
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
    (q/text-size 18)
    (q/fill 220)
    (q/text "Click for sound, then click or Enter to start" (/ w 2.0) (+ (/ h 2.0) 48))
    (q/text-size 15)
    (q/fill 180)
    (q/text "O options   H high scores" (/ w 2.0) (+ (/ h 2.0) 80))
    (q/text-align :left :baseline)))

(defn- dim!
  [w h]
  (q/fill 0 0 0 170)
  (q/no-stroke)
  (q/rect 0 0 w h))

(defn- high-score-entry-overlay!
  [state draft]
  (let [w (core/playfield-width state)
        h (core/playfield-height state)
        pending (or (core/pending-high-score state) (core/final-score state) 0)
        shown (let [d (str (or draft ""))]
                (if (seq d) d "___"))
        pad (apply str (repeat (max 0 (- 3 (count shown))) "_"))
        display (str shown pad)]
    (dim! w h)
    (q/text-align :center :center)
    (q/fill 255 220 80)
    (q/text-size 36)
    (q/text "HIGH SCORE" (/ w 2.0) (- (/ h 2.0) 70))
    (q/fill 220)
    (q/text-size 20)
    (q/text (str "Score: " pending) (/ w 2.0) (- (/ h 2.0) 20))
    (q/fill 255 255 120)
    (q/text-size 42)
    (q/text display (/ w 2.0) (+ (/ h 2.0) 30))
    (q/fill 200)
    (q/text-size 16)
    (q/text "Type 3 initials, Enter to save" (/ w 2.0) (+ (/ h 2.0) 80))
    (q/text-align :left :baseline)))

(defn- high-scores-table-overlay!
  [state]
  (let [w (core/playfield-width state)
        h (core/playfield-height state)
        table (core/high-score-table state)
        start-y (- (/ h 2.0) 120)]
    (dim! w h)
    (q/text-align :center :center)
    (q/fill 255 220 80)
    (q/text-size 36)
    (q/text "HIGH SCORES" (/ w 2.0) start-y)
    (q/text-size 18)
    (if (seq table)
      (doseq [[i e] (map-indexed vector table)]
        (let [y (+ start-y 50 (* i 28))
              rank (inc i)
              line (str rank ".  " (:initials e) "   " (:score e))]
          (q/fill 230)
          (q/text line (/ w 2.0) y)))
      (do (q/fill 200)
          (q/text "No scores yet" (/ w 2.0) (+ start-y 60))))
    (q/fill 180)
    (q/text-size 16)
    (q/text "Esc or H to return" (/ w 2.0) (+ start-y 50 (* 11 28)))
    (q/text-align :left :baseline)))

(defn- options-overlay!
  [state]
  (let [w (core/playfield-width state)
        h (core/playfield-height state)
        opts (core/game-options state)
        mute? (boolean (:mute opts))
        diff (name (or (:difficulty opts) :arcade))
        left (str/join "," (sort (map str (get-in opts [:keys :fire :left] #{}))))
        center (str/join "," (sort (map str (get-in opts [:keys :fire :center] #{}))))
        right (str/join "," (sort (map str (get-in opts [:keys :fire :right] #{}))))
        pause (str/join "," (sort (map str (get-in opts [:keys :pause] #{}))))]
    (dim! w h)
    (q/text-align :center :center)
    (q/fill 255 220 80)
    (q/text-size 36)
    (q/text "OPTIONS" (/ w 2.0) (- (/ h 2.0) 110))
    (q/fill 230)
    (q/text-size 18)
    (q/text (str "Mute: " (if mute? "ON" "OFF") "  (M)")
            (/ w 2.0) (- (/ h 2.0) 50))
    (q/text (str "Difficulty: " diff "  (1 easy  2 normal  3 arcade)")
            (/ w 2.0) (- (/ h 2.0) 15))
    (q/text (str "Fire L/C/R: " left " / " center " / " right)
            (/ w 2.0) (+ (/ h 2.0) 25))
    (q/text (str "Pause: " pause)
            (/ w 2.0) (+ (/ h 2.0) 55))
    (q/fill 180)
    (q/text-size 16)
    (q/text "Esc or O to return to title"
            (/ w 2.0) (+ (/ h 2.0) 100))
    (q/text-align :left :baseline)))

(defn- wave-banner-overlay!
  [state]
  (let [w (core/playfield-width state)
        h (core/playfield-height state)
        pos (core/wave-banner-text-position state)
        txt (or (core/wave-banner-text state) "")
        sub (or (core/wave-banner-subtitle state) "")]
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
    (q/text-align :left :baseline)))

(defn- the-end-overlay!
  "Centered expanding fireball with THE END text (simplified for p5)."
  [state]
  (when-let [fb (core/end-fireball state)]
    (let [r (double (:radius fb 0.0))
          cx (double (:x fb))
          cy (double (:y fb))
          msg (or (core/end-message state) "THE END")
          max-r (double (or (:max-radius fb) r))
          reveal (double (or (core/end-message-reveal state) 0.0))]
      (when (pos? r)
        (q/no-stroke)
        (q/fill 255 120 30 160)
        (q/ellipse cx cy (* 2 r) (* 2 r))
        (q/fill 255 200 80 200)
        (q/ellipse cx cy r r)
        (when (pos? reveal)
          (let [text-h (max 12.0 (* 0.55 r (/ 48.0 (max max-r 1.0))))]
            (q/text-align :center :center)
            (q/text-size text-h)
            (q/fill 255 240 200)
            (doseq [[dx dy] [[-2 0] [2 0] [0 -2] [0 2]]]
              (q/text msg (+ cx dx) (+ cy dy)))
            (q/fill 30 10 0)
            (q/text msg cx cy)
            (q/text-align :left :baseline)))))))

(defn- pause-overlay!
  [state]
  (let [w (core/playfield-width state)
        h (core/playfield-height state)]
    (q/fill 0 0 0 120)
    (q/rect 0 0 w h)
    (q/fill 255 255 100)
    (q/text-align :center :center)
    (q/text-size 32)
    (q/text "PAUSED" (/ w 2.0) (/ h 2.0))
    (q/text-align :left :baseline)))

(defn draw-world!
  ([state]
   (draw-world! state ""))
  ([state initials-draft]
   (let [w (core/playfield-width state)
         h (core/playfield-height state)]
     (sky! w h)
     (cond
       (core/title? state)
       (do (ground! state) (title-overlay! state))

       (core/the-end? state)
       (do (ground! state)
           (cities! state)
           (batteries! state)
           (the-end-overlay! state)
           (hud! state))

       (core/wave-banner? state)
       (do (enemies! state)
           (flyers! state)
           (missiles! state)
           (ground! state)
           (cities! state)
           (batteries! state)
           (fireballs! state)
           (wave-banner-overlay! state)
           (hud! state))

       (core/high-score-entry? state)
       (do (ground! state)
           (high-score-entry-overlay! state initials-draft))

       (core/high-scores-view? state)
       (do (ground! state)
           (high-scores-table-overlay! state))

       (core/options? state)
       (do (ground! state)
           (options-overlay! state))

       :else
       (do (enemies! state)
           (flyers! state)
           (missiles! state)
           (ground! state)
           (cities! state)
           (batteries! state)
           (fireballs! state)
           (hud! state)
           (when (core/paused? state)
             (pause-overlay! state)))))))

(defn crosshair-at!
  [x y]
  (q/stroke 255 70 70)
  (q/stroke-weight 2)
  (q/no-fill)
  (q/ellipse x y 20 20)
  (q/line (- x 14) y (+ x 14) y)
  (q/line x (- y 14) x (+ y 14))
  (q/no-stroke))

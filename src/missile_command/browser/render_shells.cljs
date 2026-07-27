(ns missile-command.browser.render-shells
  "Browser shell overlays: title, scores, options, end, pause, wave-banner."
  (:require [clojure.string :as str]
            [quil.core :as q :include-macros true]
            [missile-command.core :as core]
            [missile-command.global-scores :as global]))

(defn- dim!
  [w h]
  (q/fill 0 0 0 170)
  (q/no-stroke)
  (q/rect 0 0 w h))

(defn title-overlay!
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

(defn high-score-entry-overlay!
  [state draft]
  (let [w (core/playfield-width state)
        h (core/playfield-height state)
        pending (or (core/pending-high-score state) (core/final-score state) 0)
        display (let [d (str (or draft ""))]
                  (if (seq d) d "Your Name"))]
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
    (q/text "Type your name, Enter to save" (/ w 2.0) (+ (/ h 2.0) 80))
    (q/text-align :left :baseline)))

(defn- local-table
  [state]
  (map-indexed
   (fn [i e]
     {:rank (inc i)
      :name (global/entry-label e)
      :score (:score e)})
   (core/high-score-table state)))

(defn- global-table
  [state]
  (map
   (fn [e]
     {:rank (:rank e)
      :name (global/entry-label e)
      :score (:score e)})
   (get-in state [:global-high-scores :scores])))

(defn- draw-rows!
  [rows start-y empty-text]
  (q/text-size 18)
  (if (seq rows)
    (doseq [e rows]
      (let [i (dec (long (:rank e)))
            y (+ start-y 50 (* i 28))
            line (str (:rank e) ".  " (:name e) "   " (:score e))]
        (q/fill 230)
        (q/text line (/ (q/width) 2.0) y)))
    (do (q/fill 200)
        (q/text empty-text (/ (q/width) 2.0) (+ start-y 60)))))

(defn- draw-global!
  [state start-y]
  (let [g (:global-high-scores state)]
    (q/fill 180)
    (q/text-size 15)
    (q/text (global/display-name g) (/ (q/width) 2.0) (+ start-y 28))
    (case (:status g)
      :ready (draw-rows! (global-table state) (+ start-y 22) "No global scores yet")
      :loading (do (q/fill 200)
                   (q/text-size 18)
                   (q/text "Loading..." (/ (q/width) 2.0) (+ start-y 82)))
      (do (q/fill 200)
          (q/text-size 18)
          (q/text "Unavailable" (/ (q/width) 2.0) (+ start-y 82))))))

(defn high-scores-table-overlay!
  [state]
  (let [w (core/playfield-width state)
        h (core/playfield-height state)
        page (or (:high-score-page state) :local)
        start-y (- (/ h 2.0) 120)
        title (case page
                :global "HIGH SCORES - GLOBAL"
                "HIGH SCORES - LOCAL")]
    (dim! w h)
    (q/text-align :center :center)
    (q/fill 255 220 80)
    (q/text-size 36)
    (q/text title (/ w 2.0) start-y)
    (if (= :global page)
      (draw-global! state start-y)
      (draw-rows! (local-table state) start-y "No local scores yet"))
    (q/fill 180)
    (q/text-size 16)
    (q/text "H to return" (/ w 2.0) (+ start-y 50 (* 11 28)))
    (q/text-align :left :baseline)))

(defn options-overlay!
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

(defn wave-banner-overlay!
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

(defn the-end-overlay!
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

(defn pause-overlay!
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

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
      :score (:score e)
      :created-at (:created-at e)})
   (core/high-score-table state)))

(defn- global-table
  [state]
  (map
   (fn [e]
     {:rank (:rank e)
      :name (global/entry-label e)
      :score (:score e)
      :created-at (:created-at e)})
   (get-in state [:global-high-scores :scores])))

(def table-width 570)
(def rank-column-width 42)
(def name-column-width 220)
(def score-column-width 90)
(def column-gap 18)

(defn- column-x
  []
  (let [left (- (/ (q/width) 2.0) (/ table-width 2.0))
        rank-x (+ left rank-column-width)
        name-x (+ rank-x column-gap)
        score-x (+ name-x name-column-width column-gap)
        date-x (+ score-x score-column-width column-gap)]
    {:rank rank-x
     :name name-x
     :score score-x
     :date date-x}))

(defn- dotted-name
  [name]
  (let [name-text (str name)
        dot-width (max 1.0 (q/text-width "."))
        gap-width (q/text-width " ")
        available (- name-column-width (q/text-width name-text) gap-width)
        dot-count (max 0 (long (js/Math.floor (/ available dot-width))))
        dotted (str name-text " " (apply str (repeat dot-count ".")))]
    (if (pos? dot-count)
      dotted
      name-text)))

(defn- draw-rows!
  [rows start-y empty-text]
  (q/text-size 18)
  (if (seq rows)
    (let [{rank-x :rank name-x :name score-x :score date-x :date} (column-x)]
      (doseq [e rows]
        (let [i (dec (long (:rank e)))
              y (+ start-y 50 (* i 28))]
          (q/fill 230)
          (q/text-align :right :center)
          (q/text (str (:rank e) ".") rank-x y)
          (q/text-align :left :center)
          (q/text (dotted-name (:name e)) name-x y)
          (q/text (str (:score e)) score-x y)
          (q/text (global/date-time-label (:created-at e)) date-x y))))
    (do (q/fill 200)
        (q/text-align :center :center)
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

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-07-27T13:33:57.953845-05:00", :module-hash "-70103497", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 6, :hash "996861459"} {:id "defn-/dim!", :kind "defn-", :line 8, :end-line 12, :hash "2128645096"} {:id "defn/title-overlay!", :kind "defn", :line 14, :end-line 30, :hash "-1868220003"} {:id "defn/high-score-entry-overlay!", :kind "defn", :line 32, :end-line 53, :hash "2113507380"} {:id "defn-/local-table", :kind "defn-", :line 55, :end-line 62, :hash "-893323580"} {:id "defn-/global-table", :kind "defn-", :line 64, :end-line 71, :hash "-757410515"} {:id "defn-/draw-rows!", :kind "defn-", :line 73, :end-line 84, :hash "-978260945"} {:id "defn-/draw-global!", :kind "defn-", :line 86, :end-line 99, :hash "1771366859"} {:id "defn/high-scores-table-overlay!", :kind "defn", :line 101, :end-line 121, :hash "851832052"} {:id "defn/options-overlay!", :kind "defn", :line 123, :end-line 153, :hash "38343320"} {:id "defn/wave-banner-overlay!", :kind "defn", :line 155, :end-line 173, :hash "287092233"} {:id "defn/the-end-overlay!", :kind "defn", :line 175, :end-line 200, :hash "-224692311"} {:id "defn/pause-overlay!", :kind "defn", :line 202, :end-line 212, :hash "976185252"}]}
;; clj-mutate-manifest-end

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

(defn- button!
  [{:keys [x y w h label]}]
  (q/fill 20 30 45 220)
  (q/stroke 255 220 80)
  (q/stroke-weight 2)
  (q/rect x y w h 6)
  (q/no-stroke)
  (q/fill 245)
  (q/text-size 18)
  (q/text-align :center :center)
  (q/text label (+ x (/ w 2.0)) (+ y (/ h 2.0))))

(defn- checkbox!
  [{:keys [x y w h label checked?]}]
  (q/fill 10 15 24 230)
  (q/stroke 255 220 80)
  (q/stroke-weight 2)
  (q/rect x y w h 3)
  (when checked?
    (q/stroke 180 255 160)
    (q/stroke-weight 3)
    (q/line (+ x 5) (+ y (/ h 2.0)) (+ x 10) (+ y h -6))
    (q/line (+ x 10) (+ y h -6) (+ x w -4) (+ y 5)))
  (q/no-stroke)
  (q/fill 230)
  (q/text-size 18)
  (q/text-align :left :center)
  (q/text label (+ x w 12) (+ y (/ h 2.0))))

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
    (doseq [button (core/title-buttons state)]
      (button! button))
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

(defn- compact-score-layout?
  [w h]
  (or (< w 560) (< h 420)))

(defn- score-layout
  [w h global?]
  (let [compact? (compact-score-layout? w h)
        margin (if compact? 12 0)
        table-width (if compact? (- w (* 2 margin)) 570)
        gap (if compact? 8 18)
        rank-width (if compact? 30 42)
        score-width (if compact? 64 90)
        date-width (if compact? 112 180)
        name-width (max 80 (- table-width rank-width score-width date-width (* 3 gap)))
        left (- (/ w 2.0) (/ table-width 2.0))
        rank-x (+ left rank-width)
        name-x (+ rank-x gap)
        score-x (+ name-x name-width gap)
        date-x (+ score-x score-width gap)]
    {:compact? compact?
     :title-size (if compact? 24 36)
     :subtitle-size (if compact? 12 15)
     :row-size (if compact? 12 18)
     :row-height (if compact? 15 28)
     :title-y (if compact? 26 (- (/ h 2.0) 120))
     :subtitle-y (when global? (if compact? 44 (+ (- (/ h 2.0) 120) 28)))
     :rows-y (if compact?
               (if global? 61 48)
               (+ (- (/ h 2.0) 120) (if global? 72 50)))
     :empty-y (if compact? 88 (+ (- (/ h 2.0) 120) 60))
     :name-width name-width
     :rank-x rank-x
     :name-x name-x
     :score-x score-x
     :date-x date-x}))

(defn- fit-text
  [text max-width]
  (let [text (str text)
        full-length (count text)]
    (loop [n full-length]
      (let [candidate (if (= n full-length)
                        text
                        (str (subs text 0 n) "..."))]
        (cond
          (<= (q/text-width candidate) max-width) candidate
          (zero? n) ""
          :else (recur (dec n)))))))

(defn- dotted-name
  [name name-column-width]
  (let [name-text (fit-text name name-column-width)
        dot-width (max 1.0 (q/text-width "."))
        gap-width (q/text-width " ")
        available (- name-column-width (q/text-width name-text) gap-width)
        dot-count (max 0 (long (js/Math.floor (/ available dot-width))))
        dotted (str name-text " " (apply str (repeat dot-count ".")))]
    (if (pos? dot-count)
      dotted
      name-text)))

(defn- date-label
  [created-at compact?]
  (let [label (global/date-time-label created-at)]
    (if (and compact? (>= (count label) 16))
      (subs label 5 16)
      label)))

(defn- draw-rows!
  [rows layout empty-text]
  (q/text-size (:row-size layout))
  (if (seq rows)
    (let [{:keys [rank-x name-x score-x date-x row-height rows-y
                  name-width compact?]} layout]
      (doseq [e rows]
        (let [i (dec (long (:rank e)))
              y (+ rows-y (* i row-height))]
          (q/fill 230)
          (q/text-align :right :center)
          (q/text (str (:rank e) ".") rank-x y)
          (q/text-align :left :center)
          (q/text (dotted-name (:name e) name-width) name-x y)
          (q/text (str (:score e)) score-x y)
          (q/text (date-label (:created-at e) compact?) date-x y))))
    (do (q/fill 200)
        (q/text-align :center :center)
        (q/text empty-text (/ (q/width) 2.0) (:empty-y layout)))))

(defn- draw-global!
  [state layout]
  (let [g (:global-high-scores state)]
    (q/fill 180)
    (q/text-size (:subtitle-size layout))
    (q/text (global/display-name g) (/ (q/width) 2.0) (:subtitle-y layout))
    (case (:status g)
      :ready (draw-rows! (global-table state) layout "No global scores yet")
      :loading (do (q/fill 200)
                   (q/text-size 18)
                   (q/text "Loading..." (/ (q/width) 2.0) (:empty-y layout)))
      (do (q/fill 200)
          (q/text-size 18)
          (q/text "Unavailable" (/ (q/width) 2.0) (:empty-y layout))))))

(defn high-scores-table-overlay!
  [state]
  (let [w (core/playfield-width state)
        h (core/playfield-height state)
        page (or (:high-score-page state) :local)
        global? (= :global page)
        layout (score-layout w h global?)
        title (case page
                :global "HIGH SCORES - GLOBAL"
                "HIGH SCORES - LOCAL")]
    (dim! w h)
    (q/text-align :center :center)
    (q/fill 255 220 80)
    (q/text-size (:title-size layout))
    (q/text title (/ w 2.0) (:title-y layout))
    (if global?
      (draw-global! state layout)
      (draw-rows! (local-table state) layout "No local scores yet"))
    (doseq [button (core/high-scores-buttons state)]
      (button! button))
    (q/text-align :left :baseline)))

(defn options-overlay!
  [state]
  (let [w (core/playfield-width state)
        h (core/playfield-height state)
        opts (core/game-options state)
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
    (checkbox! (core/mute-checkbox state))
    (q/text (str "Difficulty: " diff "  (1 easy  2 normal  3 arcade)")
            (/ w 2.0) (- (/ h 2.0) 15))
    (q/text (str "Fire L/C/R: " left " / " center " / " right)
            (/ w 2.0) (+ (/ h 2.0) 25))
    (q/text (str "Pause: " pause)
            (/ w 2.0) (+ (/ h 2.0) 55))
    (doseq [button (core/options-buttons state)]
      (button! button))
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

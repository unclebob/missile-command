(ns missile-command.jvm.render-high-scores
  "Host drawing for high-score entry and table view."
  (:require [quil.core :as q]
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

(defn entry-overlay!
  "Initials entry after a qualifying THE END score."
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
        dot-count (max 0 (long (Math/floor (/ available dot-width))))
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
  (let [global (:global-high-scores state)
        status (:status global)]
    (q/fill 180)
    (q/text-size (:subtitle-size layout))
    (q/text (global/display-name global) (/ (q/width) 2.0) (:subtitle-y layout))
    (case status
      :ready (draw-rows! (global-table state) layout "No global scores yet")
      :loading (do (q/fill 200)
                   (q/text-size 18)
                   (q/text "Loading..." (/ (q/width) 2.0) (:empty-y layout)))
      (do (q/fill 200)
          (q/text-size 18)
          (q/text "Unavailable" (/ (q/width) 2.0) (:empty-y layout))))))

(defn table-overlay!
  "Ranked high-score table from title."
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

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-07-27T13:34:26.187161-05:00", :module-hash "555350046", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 5, :hash "1057077092"} {:id "defn-/dim!", :kind "defn-", :line 7, :end-line 11, :hash "2128645096"} {:id "defn/entry-overlay!", :kind "defn", :line 13, :end-line 35, :hash "865490904"} {:id "defn-/short-row", :kind "defn-", :line 37, :end-line 39, :hash "-853525910"} {:id "defn-/local-table", :kind "defn-", :line 41, :end-line 48, :hash "-893323580"} {:id "defn-/global-table", :kind "defn-", :line 50, :end-line 57, :hash "-757410515"} {:id "defn-/draw-rows!", :kind "defn-", :line 59, :end-line 70, :hash "1397065254"} {:id "defn-/draw-global!", :kind "defn-", :line 72, :end-line 86, :hash "2113148132"} {:id "defn/table-overlay!", :kind "defn", :line 88, :end-line 109, :hash "1071362492"}]}
;; clj-mutate-manifest-end

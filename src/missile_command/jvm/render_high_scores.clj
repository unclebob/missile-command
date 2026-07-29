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
        dot-count (max 0 (long (Math/floor (/ available dot-width))))
        dotted (str name-text " " (apply str (repeat dot-count ".")))]
    (if (pos? dot-count)
      dotted
      name-text)))

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
  (let [global (:global-high-scores state)
        status (:status global)]
    (q/fill 180)
    (q/text-size 15)
    (q/text (global/display-name global) (/ (q/width) 2.0) (+ start-y 28))
    (case status
      :ready (draw-rows! (global-table state) (+ start-y 22) "No global scores yet")
      :loading (do (q/fill 200)
                   (q/text-size 18)
                   (q/text "Loading..." (/ (q/width) 2.0) (+ start-y 82)))
      (do (q/fill 200)
          (q/text-size 18)
          (q/text "Unavailable" (/ (q/width) 2.0) (+ start-y 82))))))

(defn table-overlay!
  "Ranked high-score table from title."
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
    (doseq [button (core/high-scores-buttons state)]
      (button! button))
    (q/text-align :left :baseline)))

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-07-27T13:34:26.187161-05:00", :module-hash "555350046", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 5, :hash "1057077092"} {:id "defn-/dim!", :kind "defn-", :line 7, :end-line 11, :hash "2128645096"} {:id "defn/entry-overlay!", :kind "defn", :line 13, :end-line 35, :hash "865490904"} {:id "defn-/short-row", :kind "defn-", :line 37, :end-line 39, :hash "-853525910"} {:id "defn-/local-table", :kind "defn-", :line 41, :end-line 48, :hash "-893323580"} {:id "defn-/global-table", :kind "defn-", :line 50, :end-line 57, :hash "-757410515"} {:id "defn-/draw-rows!", :kind "defn-", :line 59, :end-line 70, :hash "1397065254"} {:id "defn-/draw-global!", :kind "defn-", :line 72, :end-line 86, :hash "2113148132"} {:id "defn/table-overlay!", :kind "defn", :line 88, :end-line 109, :hash "1071362492"}]}
;; clj-mutate-manifest-end

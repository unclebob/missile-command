(ns missile-command.screens)

(def title :title)
(def playing :playing)
(def paused :paused)
(def the-end :the-end)
(def high-score-entry :high-score-entry)
(def high-scores :high-scores)

(def title-game-name "Missile Command")
(def title-start-affordance "Press Enter or click to start")

(defn of
  [state]
  (or (:screen state) title))

(defn title?
  [state]
  (= title (of state)))

(defn playing?
  [state]
  (= playing (of state)))

(defn paused?
  [state]
  (= paused (of state)))

(defn the-end?
  [state]
  (= the-end (of state)))

(defn high-score-entry?
  [state]
  (= high-score-entry (of state)))

(defn high-scores-view?
  [state]
  (= high-scores (of state)))

(defn title-game-name-of
  [state]
  (or (:title-game-name state) title-game-name))

(defn title-shows-start-affordance?
  [state]
  (boolean (and (title? state) (:title-start-affordance state))))

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-07-24T15:57:20.690614-05:00", :module-hash "1708057488", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 1, :hash "-1059338734"} {:id "def/title", :kind "def", :line 3, :end-line 3, :hash "-1279836980"} {:id "def/playing", :kind "def", :line 4, :end-line 4, :hash "71391785"} {:id "def/paused", :kind "def", :line 5, :end-line 5, :hash "2043477345"} {:id "def/the-end", :kind "def", :line 6, :end-line 6, :hash "1706443994"} {:id "def/title-game-name", :kind "def", :line 8, :end-line 8, :hash "1933655789"} {:id "def/title-start-affordance", :kind "def", :line 9, :end-line 9, :hash "1825274283"} {:id "defn/of", :kind "defn", :line 11, :end-line 13, :hash "-387496363"} {:id "defn/title?", :kind "defn", :line 15, :end-line 17, :hash "-588353328"} {:id "defn/playing?", :kind "defn", :line 19, :end-line 21, :hash "-2135924141"} {:id "defn/paused?", :kind "defn", :line 23, :end-line 25, :hash "-1060730997"} {:id "defn/the-end?", :kind "defn", :line 27, :end-line 29, :hash "1142563796"} {:id "defn/title-game-name-of", :kind "defn", :line 31, :end-line 33, :hash "1728600183"} {:id "defn/title-shows-start-affordance?", :kind "defn", :line 35, :end-line 37, :hash "1225607674"}]}
;; clj-mutate-manifest-end

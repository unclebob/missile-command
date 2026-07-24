(ns missile-command.screens)

(def title :title)
(def playing :playing)
(def the-end :the-end)

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

(defn the-end?
  [state]
  (= the-end (of state)))

(defn title-game-name-of
  [state]
  (or (:title-game-name state) title-game-name))

(defn title-shows-start-affordance?
  [state]
  (boolean (and (title? state) (:title-start-affordance state))))

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-07-24T15:48:41.661003-05:00", :module-hash "-595806467", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 1, :hash "-1059338734"} {:id "def/title", :kind "def", :line 3, :end-line 3, :hash "-1279836980"} {:id "def/playing", :kind "def", :line 4, :end-line 4, :hash "71391785"} {:id "def/the-end", :kind "def", :line 5, :end-line 5, :hash "1706443994"} {:id "def/title-game-name", :kind "def", :line 7, :end-line 7, :hash "1933655789"} {:id "def/title-start-affordance", :kind "def", :line 8, :end-line 8, :hash "1825274283"} {:id "defn/of", :kind "defn", :line 10, :end-line 12, :hash "-387496363"} {:id "defn/title?", :kind "defn", :line 14, :end-line 16, :hash "-588353328"} {:id "defn/playing?", :kind "defn", :line 18, :end-line 20, :hash "-2135924141"} {:id "defn/the-end?", :kind "defn", :line 22, :end-line 24, :hash "1142563796"} {:id "defn/title-game-name-of", :kind "defn", :line 26, :end-line 28, :hash "1728600183"} {:id "defn/title-shows-start-affordance?", :kind "defn", :line 30, :end-line 32, :hash "1225607674"}]}
;; clj-mutate-manifest-end

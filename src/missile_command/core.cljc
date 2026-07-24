(ns missile-command.core)

(defn new-game
  "Create a new game state for the given playfield size."
  [{:keys [width height]}]
  {:width width
   :height height})

(defn playfield-width
  [state]
  (:width state))

(defn playfield-height
  [state]
  (:height state))

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-07-24T09:44:26.374268-05:00", :module-hash "-176712708", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 1, :hash "853033420"} {:id "defn/new-game", :kind "defn", :line 3, :end-line 7, :hash "2059373029"} {:id "defn/playfield-width", :kind "defn", :line 9, :end-line 11, :hash "-1043537513"} {:id "defn/playfield-height", :kind "defn", :line 13, :end-line 15, :hash "344252362"}]}
;; clj-mutate-manifest-end

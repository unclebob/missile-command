(ns missile-command.core
  (:require [missile-command.world :as world]))

(defn new-game
  "Create a new game state for the given playfield size."
  [{:keys [width height]}]
  (merge {:width width
          :height height}
         (world/apply-layout width height)))

(defn resize
  "Reflow layout for a new playfield size, preserving entity progress fields."
  [state width height]
  (merge state
         {:width width
          :height height}
         (world/apply-layout width height state)))

(defn playfield-width
  [state]
  (:width state))

(defn playfield-height
  [state]
  (:height state))

(defn cities
  [state]
  (:cities state))

(defn living-cities
  [state]
  (filterv :alive? (cities state)))

(defn batteries
  [state]
  (:batteries state))

(defn battery
  [state id]
  (first (filter #(= id (:id %)) (batteries state))))

(defn on-ground?
  "True when the entity's y sits in the ground band for this playfield."
  [state entity]
  (world/in-ground-band? (:y entity) (playfield-height state)))

(defn city-on-ground?
  [state city]
  (on-ground? state city))

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-07-24T11:39:00.229927-05:00", :module-hash "228081829", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 2, :hash "59968614"} {:id "defn/new-game", :kind "defn", :line 4, :end-line 9, :hash "392600071"} {:id "defn/resize", :kind "defn", :line 11, :end-line 17, :hash "1370634908"} {:id "defn/playfield-width", :kind "defn", :line 19, :end-line 21, :hash "-1043537513"} {:id "defn/playfield-height", :kind "defn", :line 23, :end-line 25, :hash "344252362"} {:id "defn/cities", :kind "defn", :line 27, :end-line 29, :hash "1240083502"} {:id "defn/living-cities", :kind "defn", :line 31, :end-line 33, :hash "-1556555524"} {:id "defn/batteries", :kind "defn", :line 35, :end-line 37, :hash "206298614"} {:id "defn/battery", :kind "defn", :line 39, :end-line 41, :hash "1338932847"} {:id "defn/on-ground?", :kind "defn", :line 43, :end-line 46, :hash "1609612027"} {:id "defn/city-on-ground?", :kind "defn", :line 48, :end-line 50, :hash "-1878088970"}]}
;; clj-mutate-manifest-end

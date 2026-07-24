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

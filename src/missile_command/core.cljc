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

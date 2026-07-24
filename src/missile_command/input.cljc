(ns missile-command.input
  "Pure input policy: playfield intent mapping (click zones, battery preference).")

(def click-fallback-orders
  "Battery preference order for each click zone, preferred first."
  {:left [:left :center :right]
   :center [:center :left :right]
   :right [:right :center :left]})

(defn click-zone
  "Horizontal third for a playfield x coordinate: :left, :center, or :right."
  [width x]
  (let [third (/ (double width) 3.0)]
    (cond
      (< x third) :left
      (< x (* 2.0 third)) :center
      :else :right)))

(defn click-fallback-order
  "Battery preference order for a click zone, preferred first."
  [zone]
  (get click-fallback-orders zone))

(defn first-preferred
  "Return the first battery id in the zone order that satisfies pred."
  [zone pred]
  (first (filter pred (click-fallback-order zone))))

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

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-07-24T11:56:35.52006-05:00", :module-hash "-814092771", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 2, :hash "-587976223"} {:id "def/click-fallback-orders", :kind "def", :line 4, :end-line 8, :hash "2106770874"} {:id "defn/click-zone", :kind "defn", :line 10, :end-line 17, :hash "1708187496"} {:id "defn/click-fallback-order", :kind "defn", :line 19, :end-line 22, :hash "-869328706"} {:id "defn/first-preferred", :kind "defn", :line 24, :end-line 27, :hash "-18512716"}]}
;; clj-mutate-manifest-end

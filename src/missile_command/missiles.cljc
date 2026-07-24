(ns missile-command.missiles)

(defn make-defensive
  "Create a defensive missile from a battery toward an aim point."
  [missile-id battery-id bat aim]
  {:id missile-id
   :battery battery-id
   :x0 (:x bat)
   :y0 (:y bat)
   :x1 (:x aim)
   :y1 (:y aim)
   :speed (:missile-speed bat)})

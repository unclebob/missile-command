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

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-07-24T11:56:50.002509-05:00", :module-hash "1119273116", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 1, :hash "-1619932998"} {:id "defn/make-defensive", :kind "defn", :line 3, :end-line 12, :hash "1115947862"}]}
;; clj-mutate-manifest-end

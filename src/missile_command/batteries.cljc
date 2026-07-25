(ns missile-command.batteries
  (:require [missile-command.entities :as entities]))

(def update-battery
  "Apply f to the battery with the given id; leave others unchanged."
  entities/update-by-id)

(defn can-fire?
  "True when the battery exists, is not destroyed, and has remaining ammo."
  [battery]
  (and battery
       (not (:destroyed? battery))
       (pos? (:missiles battery))))

(defn spend-ammo
  [battery]
  (update battery :missiles dec))

(defn set-ammo
  [battery ammo]
  (assoc battery :missiles ammo))

(defn destroy
  "Mark destroyed and empty ammo so the base cannot fire until restored."
  [battery]
  (assoc battery :destroyed? true :missiles 0))

(defn restore
  "Clear destroyed flag and set ammo (wave rearm)."
  [battery ammo]
  (assoc battery :destroyed? false :missiles (long ammo)))

(defn living
  "Non-destroyed batteries only."
  [batteries]
  (filterv (complement :destroyed?) (or batteries [])))

(defn map-living
  "Apply f to non-destroyed batteries; leave destroyed batteries unchanged."
  [batteries f]
  (mapv (fn [b] (if (:destroyed? b) b (f b))) (or batteries [])))

(defn set-living-ammo
  "Set ammo on every non-destroyed battery."
  [batteries ammo]
  (map-living batteries #(set-ammo % ammo)))

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-07-24T14:35:25.991954-05:00", :module-hash "814722448", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 2, :hash "1828854097"} {:id "defn/update-battery", :kind "defn", :line 4, :end-line 7, :hash "-773024030"} {:id "defn/can-fire?", :kind "defn", :line 9, :end-line 14, :hash "-1043056803"} {:id "defn/spend-ammo", :kind "defn", :line 16, :end-line 18, :hash "2076101157"} {:id "defn/set-ammo", :kind "defn", :line 20, :end-line 22, :hash "1974119726"} {:id "defn/destroy", :kind "defn", :line 24, :end-line 26, :hash "-1826858571"}]}
;; clj-mutate-manifest-end

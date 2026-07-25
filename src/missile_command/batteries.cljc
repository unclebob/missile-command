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

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-07-24T21:13:20.578863-05:00", :module-hash "1077486974", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 2, :hash "1828854097"} {:id "def/update-battery", :kind "def", :line 4, :end-line 6, :hash "1522337776"} {:id "defn/can-fire?", :kind "defn", :line 8, :end-line 13, :hash "-1043056803"} {:id "defn/spend-ammo", :kind "defn", :line 15, :end-line 17, :hash "2076101157"} {:id "defn/set-ammo", :kind "defn", :line 19, :end-line 21, :hash "1974119726"} {:id "defn/destroy", :kind "defn", :line 23, :end-line 26, :hash "-13006368"} {:id "defn/restore", :kind "defn", :line 28, :end-line 31, :hash "-1487944834"}]}
;; clj-mutate-manifest-end

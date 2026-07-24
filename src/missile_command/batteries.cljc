(ns missile-command.batteries)

(defn update-battery
  "Apply f to the battery with the given id; leave others unchanged."
  [batteries battery-id f]
  (mapv (fn [battery]
          (if (= battery-id (:id battery))
            (f battery)
            battery))
        batteries))

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
  [battery]
  (assoc battery :destroyed? true))

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-07-24T11:56:47.898337-05:00", :module-hash "573554695", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 1, :hash "1935816410"} {:id "defn/update-battery", :kind "defn", :line 3, :end-line 10, :hash "1980959089"} {:id "defn/can-fire?", :kind "defn", :line 12, :end-line 17, :hash "-1043056803"} {:id "defn/spend-ammo", :kind "defn", :line 19, :end-line 21, :hash "2076101157"} {:id "defn/set-ammo", :kind "defn", :line 23, :end-line 25, :hash "1974119726"} {:id "defn/destroy", :kind "defn", :line 27, :end-line 29, :hash "-1826858571"}]}
;; clj-mutate-manifest-end

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

(ns missile-command.missiles)

(def max-dt 0.05)
(def fireball-expand-seconds 0.4)
(def fireball-contract-seconds 0.4)
(def fireball-max-radius 40.0)

(defn- hypot
  [dx dy]
  (Math/sqrt (+ (* dx dx) (* dy dy))))

(defn path-length
  [missile]
  (hypot (- (:x1 missile) (:x0 missile))
         (- (:y1 missile) (:y0 missile))))

(defn position-at-progress
  [missile progress]
  (let [p (max 0.0 (min 1.0 progress))]
    {:x (+ (:x0 missile) (* p (- (:x1 missile) (:x0 missile))))
     :y (+ (:y0 missile) (* p (- (:y1 missile) (:y0 missile))))}))

(defn make-defensive
  "Create a defensive missile from a battery toward an aim point."
  [missile-id battery-id bat aim]
  (let [missile {:id missile-id
                 :battery battery-id
                 :x0 (:x bat)
                 :y0 (:y bat)
                 :x1 (:x aim)
                 :y1 (:y aim)
                 :speed (:missile-speed bat)
                 :progress 0.0}]
    (merge missile (position-at-progress missile 0.0))))

(defn clamp-dt
  [dt]
  (min (double dt) max-dt))

(def arrived
  "Sentinel returned by advance-defensive when the missile reaches its aim."
  :arrived)

(defn advance-defensive
  "Advance a defensive missile by dt seconds. Returns updated missile or
  `arrived` when it reaches the aim point."
  [missile dt]
  (let [length (path-length missile)
        speed (double (:speed missile))
        delta (if (zero? length)
                1.0
                (/ (* speed dt) length))
        progress (+ (double (:progress missile 0.0)) delta)]
    (if (>= progress 1.0)
      arrived
      (merge missile
             {:progress progress}
             (position-at-progress missile progress)))))

(defn make-fireball
  [fireball-id x y]
  {:id fireball-id
   :x x
   :y y
   :age 0.0
   :radius 0.0
   :max-radius fireball-max-radius
   :expand-seconds fireball-expand-seconds
   :contract-seconds fireball-contract-seconds})

(defn fireball-lifetime
  [fireball]
  (+ (:expand-seconds fireball) (:contract-seconds fireball)))

(defn fireball-radius-at
  [fireball age]
  (let [expand (:expand-seconds fireball)
        contract (:contract-seconds fireball)
        max-r (:max-radius fireball)]
    (cond
      (<= age 0.0) 0.0
      (< age expand) (* max-r (/ age expand))
      (< age (+ expand contract))
      (let [t (/ (- age expand) contract)]
        (* max-r (- 1.0 t)))
      :else 0.0)))

(def expired
  "Sentinel returned by advance-fireball when the fireball is done."
  :expired)

(defn advance-fireball
  "Advance a fireball by dt. Returns updated fireball or `expired`."
  [fireball dt]
  (let [age (+ (double (:age fireball 0.0)) (double dt))]
    (if (>= age (fireball-lifetime fireball))
      expired
      (assoc fireball
             :age age
             :radius (fireball-radius-at fireball age)))))

(defn point-in-fireball?
  [fireball x y]
  (let [dx (- x (:x fireball))
        dy (- y (:y fireball))
        r (double (:radius fireball 0.0))]
    (<= (hypot dx dy) r)))

(ns missile-command.missiles)

(def max-dt 0.05)
(def fireball-expand-seconds 0.4)
(def fireball-contract-seconds 0.4)
(def fireball-max-radius 40.0)
(def static-fireball-flag true)
(def default-enemy-speed 120.0)

(def arrived
  "Sentinel returned when a ballistic missile reaches its aim."
  :arrived)

(defn arrived?
  [result]
  (identical? arrived result))

(def expired
  "Sentinel returned by advance-fireball when the fireball is done."
  :expired)

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

(defn make-enemy
  "Create an enemy ballistic missile from origin toward a target point."
  [missile-id origin target speed target-kind target-id]
  (let [missile {:id missile-id
                 :x0 (:x origin)
                 :y0 (:y origin)
                 :x1 (:x target)
                 :y1 (:y target)
                 :speed speed
                 :progress 0.0
                 :target-kind target-kind
                 :target-id target-id}]
    (merge missile (position-at-progress missile 0.0))))

(defn advance-enemy
  "Advance an enemy missile by dt. Returns updated missile or `arrived`."
  [missile dt]
  (advance-defensive missile dt))

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

(defn make-static-fireball
  "Fireball fixture with fixed radius that does not age."
  [fireball-id x y radius]
  {:id fireball-id
   :x x
   :y y
   :age 0.0
   :radius (double radius)
   :max-radius (double radius)
   :expand-seconds 0.0
   :contract-seconds 0.0
   :static? static-fireball-flag})

(defn fireball-lifetime
  [fireball]
  (+ (:expand-seconds fireball) (:contract-seconds fireball)))

(defn fireball-phase
  "Lifecycle phase for a fireball age: :pre, :expand, :contract, or :post."
  [fireball age]
  (let [expand (double (:expand-seconds fireball))
        contract (double (:contract-seconds fireball))
        a (double age)]
    (cond
      (<= a 0.0) :pre
      (< a expand) :expand
      (< a (+ expand contract)) :contract
      :else :post)))

(defn fireball-radius-at
  [fireball age]
  (let [expand (double (:expand-seconds fireball))
        contract (double (:contract-seconds fireball))
        max-r (double (:max-radius fireball))
        a (double age)]
    (case (fireball-phase fireball a)
      :pre 0.0
      :expand (* max-r (/ a expand))
      :contract (let [t (/ (- a expand) contract)]
                  (* max-r (- 1.0 t)))
      :post 0.0)))

(defn advance-fireball
  "Advance a fireball by dt. Returns updated fireball or `expired`.
   Static fireballs keep a fixed radius forever."
  [fireball dt]
  (if (:static? fireball)
    fireball
    (let [age (+ (double (:age fireball 0.0)) (double dt))]
      (if (>= age (fireball-lifetime fireball))
        expired
        (assoc fireball
               :age age
               :radius (fireball-radius-at fireball age))))))

(defn point-in-fireball?
  [fireball x y]
  (let [dx (- x (:x fireball))
        dy (- y (:y fireball))
        r (double (:radius fireball 0.0))]
    (<= (hypot dx dy) r)))

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-07-24T12:47:15.855093-05:00", :module-hash "839235433", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 1, :hash "-1619932998"} {:id "def/max-dt", :kind "def", :line 3, :end-line 3, :hash "-989171296"} {:id "def/fireball-expand-seconds", :kind "def", :line 4, :end-line 4, :hash "404326517"} {:id "def/fireball-contract-seconds", :kind "def", :line 5, :end-line 5, :hash "-1491641581"} {:id "def/fireball-max-radius", :kind "def", :line 6, :end-line 6, :hash "-1156590171"} {:id "def/static-fireball-flag", :kind "def", :line 7, :end-line 7, :hash "421322458"} {:id "def/default-enemy-speed", :kind "def", :line 8, :end-line 8, :hash "982539494"} {:id "def/arrived", :kind "def", :line 10, :end-line 12, :hash "1968139005"} {:id "defn/arrived?", :kind "defn", :line 14, :end-line 16, :hash "522520574"} {:id "def/expired", :kind "def", :line 18, :end-line 20, :hash "1501164590"} {:id "defn-/hypot", :kind "defn-", :line 22, :end-line 24, :hash "80937335"} {:id "defn/path-length", :kind "defn", :line 26, :end-line 29, :hash "1657109437"} {:id "defn/position-at-progress", :kind "defn", :line 31, :end-line 35, :hash "-1851331410"} {:id "defn/make-defensive", :kind "defn", :line 37, :end-line 48, :hash "-1218311748"} {:id "defn/clamp-dt", :kind "defn", :line 50, :end-line 52, :hash "1581799727"} {:id "defn/advance-defensive", :kind "defn", :line 54, :end-line 68, :hash "924066886"} {:id "defn/make-enemy", :kind "defn", :line 70, :end-line 82, :hash "1173309442"} {:id "defn/advance-enemy", :kind "defn", :line 84, :end-line 87, :hash "-1663219647"} {:id "defn/make-fireball", :kind "defn", :line 89, :end-line 98, :hash "-585279014"} {:id "defn/make-static-fireball", :kind "defn", :line 100, :end-line 111, :hash "-520659645"} {:id "defn/fireball-lifetime", :kind "defn", :line 113, :end-line 115, :hash "-21731022"} {:id "defn/fireball-phase", :kind "defn", :line 117, :end-line 127, :hash "-1988616685"} {:id "defn/fireball-radius-at", :kind "defn", :line 129, :end-line 140, :hash "511592659"} {:id "defn/advance-fireball", :kind "defn", :line 142, :end-line 153, :hash "554281146"} {:id "defn/point-in-fireball?", :kind "defn", :line 155, :end-line 160, :hash "1166764552"}]}
;; clj-mutate-manifest-end

(ns missile-command.world)

(def city-count 6)
(def default-ammo 10)
(def ground-band-fraction 0.10)
(def city-margin-fraction 0.12)
(def left-battery-x-fraction 0.08)
(def center-battery-x-fraction 0.50)
(def right-battery-x-fraction 0.92)
(def wing-missile-speed 200.0)
(def center-missile-speed 300.0)
(def city-starts-alive? true)
(def battery-starts-destroyed? false)

(defn ground-band
  "Ground band is the bottom fraction of the playfield height."
  [height]
  (let [top (long (Math/floor (* height (- 1.0 ground-band-fraction))))]
    {:top top
     :bottom height}))

(defn in-ground-band?
  [y height]
  (let [{:keys [top bottom]} (ground-band height)]
    (and (>= y top) (<= y bottom))))

(defn- ground-y
  [height]
  (let [{:keys [top bottom]} (ground-band height)]
    (long (+ top (quot (- bottom top) 2)))))

(defn- city-x
  [width index]
  (let [margin (long (* width city-margin-fraction))
        span (- width (* 2 margin))
        step (/ (double span) (dec city-count))]
    (+ margin (long (* index step)))))

(defn layout-cities
  [width height]
  (let [y (ground-y height)]
    (mapv (fn [i]
            {:id i
             :x (city-x width i)
             :y y
             :alive? city-starts-alive?})
          (range city-count))))

(defn- make-battery
  [id x y missile-speed]
  {:id id
   :x x
   :y y
   :missiles default-ammo
   :destroyed? battery-starts-destroyed?
   :missile-speed missile-speed})

(defn layout-batteries
  [width height]
  (let [y (ground-y height)]
    [(make-battery :left (long (* width left-battery-x-fraction)) y wing-missile-speed)
     (make-battery :center (long (* width center-battery-x-fraction)) y center-missile-speed)
     (make-battery :right (long (* width right-battery-x-fraction)) y wing-missile-speed)]))

(defn- reflow-entities
  "Keep prior entity fields, overlaying only the given layout keys from fresh entities."
  [fresh prior layout-keys]
  (let [prior-by-id (into {} (map (juxt :id identity) prior))]
    (mapv (fn [entity]
            (if-let [old (get prior-by-id (:id entity))]
              (merge old (select-keys entity layout-keys))
              entity))
          fresh)))

(defn apply-layout
  "Reflow fixed scenery from playfield size. Preserves city/battery identity
  fields other than position when present on prior entities."
  ([width height]
   {:cities (layout-cities width height)
    :batteries (layout-batteries width height)})
  ([width height prior]
   (let [fresh (apply-layout width height)]
     {:cities (reflow-entities (:cities fresh) (:cities prior) [:x :y])
      :batteries (reflow-entities (:batteries fresh) (:batteries prior)
                                  [:x :y :missile-speed])})))

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-07-24T11:38:33.695924-05:00", :module-hash "-1346382326", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 1, :hash "1550218494"} {:id "def/city-count", :kind "def", :line 3, :end-line 3, :hash "-359528501"} {:id "def/default-ammo", :kind "def", :line 4, :end-line 4, :hash "-432402765"} {:id "def/ground-band-fraction", :kind "def", :line 5, :end-line 5, :hash "-293125078"} {:id "def/city-margin-fraction", :kind "def", :line 6, :end-line 6, :hash "1463537848"} {:id "def/left-battery-x-fraction", :kind "def", :line 7, :end-line 7, :hash "1147538318"} {:id "def/center-battery-x-fraction", :kind "def", :line 8, :end-line 8, :hash "500262858"} {:id "def/right-battery-x-fraction", :kind "def", :line 9, :end-line 9, :hash "-1535432486"} {:id "def/wing-missile-speed", :kind "def", :line 10, :end-line 10, :hash "2070370270"} {:id "def/center-missile-speed", :kind "def", :line 11, :end-line 11, :hash "-490138680"} {:id "def/city-starts-alive?", :kind "def", :line 12, :end-line 12, :hash "-1584995795"} {:id "def/battery-starts-destroyed?", :kind "def", :line 13, :end-line 13, :hash "-1080992022"} {:id "defn/ground-band", :kind "defn", :line 15, :end-line 20, :hash "-496363753"} {:id "defn/in-ground-band?", :kind "defn", :line 22, :end-line 25, :hash "-776455302"} {:id "defn-/ground-y", :kind "defn-", :line 27, :end-line 30, :hash "-713919959"} {:id "defn-/city-x", :kind "defn-", :line 32, :end-line 37, :hash "564985785"} {:id "defn/layout-cities", :kind "defn", :line 39, :end-line 47, :hash "-1608789729"} {:id "defn-/make-battery", :kind "defn-", :line 49, :end-line 56, :hash "-474396181"} {:id "defn/layout-batteries", :kind "defn", :line 58, :end-line 63, :hash "-809107404"} {:id "defn-/reflow-entities", :kind "defn-", :line 65, :end-line 73, :hash "958506677"} {:id "defn/apply-layout", :kind "defn", :line 75, :end-line 85, :hash "-844652355"}]}
;; clj-mutate-manifest-end

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
             :alive? true})
          (range city-count))))

(defn- make-battery
  [id x y missile-speed]
  {:id id
   :x x
   :y y
   :missiles default-ammo
   :destroyed? false
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

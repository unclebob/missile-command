(ns missile-command.core-property-test
  (:require [clojure.test :refer [deftest is]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :refer [for-all]]
            [missile-command.core :as core]
            [missile-command.input :as input]
            [missile-command.missiles :as missiles]
            [missile-command.scoring :as scoring]
            [missile-command.waves :as waves]))

;; Layout math truncates to longs; keep sizes large enough for distinct city xs.
(def playfield-size-gen
  (gen/large-integer* {:min 200 :max 4000}))

(def coordinate-gen
  (gen/large-integer* {:min -5000 :max 10000}))

(defn- in-playfield?
  [width height {:keys [x y]}]
  (and (integer? x) (integer? y)
       (<= 0 x) (< x width)
       (<= 0 y) (< y height)))

(def battery-id-gen
  (gen/elements [:left :center :right]))

(defn- aim
  [state x y]
  (:state (core/handle state {:type :aim :x x :y y})))

(defn- fire
  [state battery-id]
  (core/handle state {:type :fire :battery battery-id}))

(defn- click
  [state x y]
  (core/handle state {:type :click :x x :y y}))

(defn- total-ammo
  [state]
  (reduce + (map :missiles (core/batteries state))))

(defn- missile-from
  [state battery-id]
  (first (filter #(= battery-id (:battery %))
                 (core/defensive-missiles state))))

(defspec new-game-preserves-playfield-dimensions
  100
  (for-all [width playfield-size-gen
            height playfield-size-gen]
    (let [state (assoc (core/new-game {:width width :height height}) :screen :playing)]
      (and (= width (core/playfield-width state))
           (= height (core/playfield-height state))))))

(defspec playfield-accessors-agree-with-map-keys
  50
  (for-all [width playfield-size-gen
            height playfield-size-gen]
    (let [state (assoc (core/new-game {:width width :height height}) :screen :playing)]
      (and (= (:width state) (core/playfield-width state))
           (= (:height state) (core/playfield-height state))))))

(defspec new-game-layout-invariants
  80
  (for-all [width playfield-size-gen
            height playfield-size-gen]
    (let [state (assoc (core/new-game {:width width :height height}) :screen :playing)
          cities (core/cities state)
          living (core/living-cities state)
          bats (core/batteries state)
          left (core/battery state :left)
          center (core/battery state :center)
          right (core/battery state :right)
          city-xs (mapv :x (sort-by :id cities))
          crosshair (core/crosshair state)]
      (and (= 6 (count living))
           (= 6 (count cities))
           (= 3 (count bats))
           (= #{:left :center :right} (set (map :id bats)))
           (every? :alive? cities)
           (every? (complement :destroyed?) bats)
           (every? #(= 10 (:missiles %)) bats)
           (apply < city-xs)
           (every? #(and (<= 0 %) (< % width)) city-xs)
           (every? #(core/on-ground? state %) cities)
           (every? #(core/on-ground? state %) bats)
           (< (:x left) (:x center) (:x right))
           (< (:x left) (/ width 3.0))
           (< (/ width 3.0) (:x center) (* width (/ 2.0 3)))
           (> (:x right) (* width (/ 2.0 3)))
           (> (:missile-speed center) (:missile-speed left))
           (> (:missile-speed center) (:missile-speed right))
           (zero? (core/score state))
           (in-playfield? width height crosshair)))))

(defspec resize-reflows-and-preserves-progress
  60
  (for-all [w0 playfield-size-gen
            h0 playfield-size-gen
            w1 playfield-size-gen
            h1 playfield-size-gen]
    (let [before (-> (assoc (core/new-game {:width w0 :height h0}) :screen :playing)
                     (update :cities (fn [cs]
                                       (mapv #(if (zero? (:id %))
                                                (assoc % :alive? false)
                                                %)
                                             cs)))
                     (update :batteries (fn [bs]
                                          (mapv #(if (= :left (:id %))
                                                   (assoc % :missiles 3 :destroyed? true)
                                                   %)
                                                bs))))
          after (core/resize before w1 h1)
          left (core/battery after :left)]
      (and (= w1 (core/playfield-width after))
           (= h1 (core/playfield-height after))
           (= 5 (count (core/living-cities after)))
           (:destroyed? left)
           (= 3 (:missiles left))
           (every? #(core/on-ground? after %) (core/cities after))
           (every? #(core/on-ground? after %) (core/batteries after))
           (every? #(and (<= 0 (:x %)) (< (:x %) w1)) (core/cities after))
           (in-playfield? w1 h1 (core/crosshair after))))))

(defspec aim-keeps-crosshair-in-playfield
  100
  (for-all [width playfield-size-gen
            height playfield-size-gen
            x coordinate-gen
            y coordinate-gen]
    (let [state (aim (assoc (core/new-game {:width width :height height}) :screen :playing) x y)
          crosshair (core/crosshair state)]
      (in-playfield? width height crosshair))))

(defspec aim-is-idempotent-for-the-same-point
  50
  (for-all [width playfield-size-gen
            height playfield-size-gen
            x coordinate-gen
            y coordinate-gen]
    (let [once (aim (assoc (core/new-game {:width width :height height}) :screen :playing) x y)
          twice (aim once x y)]
      (= (core/crosshair once) (core/crosshair twice)))))

(defspec aim-preserves-forces-and-score
  80
  (for-all [width playfield-size-gen
            height playfield-size-gen
            x coordinate-gen
            y coordinate-gen]
    (let [before (assoc (core/new-game {:width width :height height}) :screen :playing)
          after (aim before x y)]
      (and (= (core/cities before) (core/cities after))
           (= (core/batteries before) (core/batteries after))
           (= (core/score before) (core/score after))))))

(defspec aim-leaves-in-bounds-points-unchanged
  80
  (for-all [width playfield-size-gen
            height playfield-size-gen
            x-offset (gen/large-integer* {:min 0 :max 100000})
            y-offset (gen/large-integer* {:min 0 :max 100000})]
    (let [x (mod x-offset width)
          y (mod y-offset height)
          state (aim (assoc (core/new-game {:width width :height height}) :screen :playing) x y)]
      (= {:x x :y y} (core/crosshair state)))))

(defspec resize-reclamps-crosshair
  60
  (for-all [w0 playfield-size-gen
            h0 playfield-size-gen
            w1 playfield-size-gen
            h1 playfield-size-gen
            x coordinate-gen
            y coordinate-gen]
    (let [aimed (aim (assoc (core/new-game {:width w0 :height h0}) :screen :playing) x y)
          resized (core/resize aimed w1 h1)]
      (in-playfield? w1 h1 (core/crosshair resized)))))

(defspec fire-stocked-battery-launches-toward-crosshair
  80
  (for-all [width playfield-size-gen
            height playfield-size-gen
            battery-id battery-id-gen
            x coordinate-gen
            y coordinate-gen]
    (let [before (aim (assoc (core/new-game {:width width :height height}) :screen :playing) x y)
          aim-point (core/crosshair before)
          bat (core/battery before battery-id)
          result (fire before battery-id)
          after (:state result)
          missile (missile-from after battery-id)]
      (and (= 9 (:missiles (core/battery after battery-id)))
           (= 1 (count (core/defensive-missiles after)))
           (= (dec (total-ammo before)) (total-ammo after))
           (= battery-id (:battery missile))
           (= (:x bat) (:x0 missile))
           (= (:y bat) (:y0 missile))
           (= (:x aim-point) (:x1 missile))
           (= (:y aim-point) (:y1 missile))
           (= (:missile-speed bat) (:speed missile))
           (= [{:type :sfx/launch :battery battery-id}] (:events result))))))

(defspec fire-does-not-spend-other-batteries
  60
  (for-all [width playfield-size-gen
            height playfield-size-gen
            battery-id battery-id-gen]
    (let [before (assoc (core/new-game {:width width :height height}) :screen :playing)
          after (:state (fire before battery-id))
          others (remove #(= battery-id (:id %)) (core/batteries after))]
      (every? #(= 10 (:missiles %)) others))))

(defspec fire-empty-or-destroyed-is-a-no-op
  60
  (for-all [width playfield-size-gen
            height playfield-size-gen
            battery-id battery-id-gen
            mode (gen/elements [:empty :destroyed])]
    (let [before (case mode
                   :empty (core/set-battery-ammo
                           (assoc (core/new-game {:width width :height height}) :screen :playing)
                           battery-id 0)
                   :destroyed (core/destroy-battery
                               (assoc (core/new-game {:width width :height height}) :screen :playing)
                               battery-id))
          result (fire before battery-id)
          after (:state result)]
      (and (empty? (core/defensive-missiles after))
           (empty? (:events result))
           (= (:missiles (core/battery before battery-id))
              (:missiles (core/battery after battery-id)))
           (= (:destroyed? (core/battery before battery-id))
              (:destroyed? (core/battery after battery-id)))))))

(defspec center-missile-is-faster-than-side-missiles
  40
  (for-all [width playfield-size-gen
            height playfield-size-gen]
    (let [state (reduce (fn [s id] (:state (fire s id)))
                        (assoc (core/new-game {:width width :height height}) :screen :playing)
                        [:left :center :right])
          by-battery (into {} (map (juxt :battery identity)
                                   (core/defensive-missiles state)))]
      (and (= 3 (count by-battery))
           (> (:speed (by-battery :center)) (:speed (by-battery :left)))
           (> (:speed (by-battery :center)) (:speed (by-battery :right)))))))

(defspec click-zone-partitions-width
  100
  (for-all [width playfield-size-gen
            x-offset (gen/large-integer* {:min 0 :max 100000})]
    (let [x (mod x-offset width)
          zone (input/click-zone width x)
          third (/ (double width) 3.0)]
      (case zone
        :left (< x third)
        :center (and (<= third x) (< x (* 2.0 third)))
        :right (>= x (* 2.0 third))))))

(defspec click-prefers-zone-battery-when-stocked
  80
  (for-all [width playfield-size-gen
            height playfield-size-gen
            x-offset (gen/large-integer* {:min 0 :max 100000})
            y coordinate-gen]
    (let [x (mod x-offset width)
          before (assoc (core/new-game {:width width :height height}) :screen :playing)
          expected-zone (input/click-zone width x)
          result (click before x y)
          after (:state result)
          missile (first (core/defensive-missiles after))
          aim-point (core/crosshair after)]
      (and (= expected-zone (:battery missile))
           (in-playfield? width height aim-point)
           (= 9 (:missiles (core/battery after expected-zone)))
           (= (:x aim-point) (:x1 missile))
           (= (:y aim-point) (:y1 missile))))))

(defspec click-falls-back-along-zone-order
  60
  (for-all [width playfield-size-gen
            height playfield-size-gen
            zone (gen/elements [:left :center :right])
            skip-count (gen/elements [1 2])]
    (let [order (input/click-fallback-order zone)
          emptied (take skip-count order)
          expected (nth order skip-count)
          ;; pick an x firmly inside the zone
          third (/ (double width) 3.0)
          x (long (case zone
                    :left (/ third 2.0)
                    :center (+ third (/ third 2.0))
                    :right (+ (* 2.0 third) (/ third 2.0))))
          before (reduce (fn [s id] (core/set-battery-ammo s id 0))
                         (assoc (core/new-game {:width width :height height}) :screen :playing)
                         emptied)
          after (:state (click before x 10))
          missile (first (core/defensive-missiles after))]
      (and (= expected (:battery missile))
           (= 9 (:missiles (core/battery after expected)))
           (every? #(= 0 (:missiles (core/battery after %))) emptied)))))

(defspec click-with-no-fireable-battery-only-aims
  40
  (for-all [width playfield-size-gen
            height playfield-size-gen
            x-offset (gen/large-integer* {:min 0 :max 100000})
            y coordinate-gen]
    (let [x (mod x-offset width)
          before (reduce (fn [s id] (core/set-battery-ammo s id 0))
                         (assoc (core/new-game {:width width :height height}) :screen :playing)
                         [:left :center :right])
          result (click before x y)
          after (:state result)]
      (and (empty? (core/defensive-missiles after))
           (empty? (:events result))
           (in-playfield? width height (core/crosshair after))
           (= 0 (total-ammo after))))))

(defn- fire-and-aim
  [width height battery-id aim-x aim-y]
  (-> (assoc (core/new-game {:width width :height height}) :screen :playing)
      (aim aim-x aim-y)
      (fire battery-id)
      :state))

(defn- tick-n
  [state dt n]
  (reduce (fn [s _] (:state (core/tick s dt))) state (range n)))

(defspec tick-clamps-large-dt
  50
  (for-all [dt (gen/double* {:min 0.06 :max 10.0 :NaN? false :infinite? false})]
    (let [state (fire-and-aim 800 600 :center 400 100)
          after (:state (core/tick state dt))]
      (<= (core/last-applied-dt after) 0.05))))

(defspec tick-advances-missile-progress
  40
  (for-all [battery-id battery-id-gen]
    (let [before (fire-and-aim 800 600 battery-id 400 100)
          p0 (:progress (first (core/defensive-missiles before)))
          after (:state (core/tick before 0.05))
          missiles (core/defensive-missiles after)]
      (or (and (empty? missiles) (= 1 (count (core/fireballs after))))
          (and (= 1 (count missiles))
               (> (:progress (first missiles)) p0))))))

(defspec missile-arrival-spawns-fireball-at-aim
  30
  (for-all [battery-id battery-id-gen
            aim-x (gen/large-integer* {:min 50 :max 750})
            aim-y (gen/large-integer* {:min 50 :max 550})]
    (let [state (fire-and-aim 800 600 battery-id aim-x aim-y)
          after (loop [s state n 0]
                  (cond
                    (> n 5000) s
                    (seq (core/fireballs s)) s
                    :else (recur (:state (core/tick s 0.05)) (inc n))))
          fb (first (core/fireballs after))]
      (and (empty? (core/defensive-missiles after))
           (some? fb)
           (= (double aim-x) (double (:x fb)))
           (= (double aim-y) (double (:y fb)))))))

(defspec fireball-expands-then-contracts-then-expires
  20
  (for-all []
    (let [state (fire-and-aim 800 600 :center 400 200)
          arrived (loop [s state n 0]
                    (if (or (seq (core/fireballs s)) (> n 5000))
                      s
                      (recur (:state (core/tick s 0.05)) (inc n))))
          expand missiles/fireball-expand-seconds
          contract missiles/fireball-contract-seconds
          mid-expand (:state (core/tick arrived (/ expand 2.0)))
          peak (tick-n arrived 0.05 (int (Math/ceil (/ expand 0.05))))
          mid-contract (tick-n peak 0.05 (max 1 (int (Math/ceil (/ contract 4.0)))))
          gone (tick-n arrived 0.05 (int (Math/ceil (/ (+ expand contract 0.1) 0.05))))]
      (and (pos? (:radius (first (core/fireballs mid-expand))))
           (>= (:radius (first (core/fireballs peak)))
               (:radius (first (core/fireballs mid-expand))))
           (or (empty? (core/fireballs mid-contract))
               (<= (:radius (first (core/fireballs mid-contract)))
                   (:radius (first (core/fireballs peak)))))
           (empty? (core/fireballs gone))))))

(defspec destroyable-target-hit-depends-on-distance
  30
  (for-all [inside? gen/boolean]
    (let [aim-x 400 aim-y 200
          target-x (if inside? 400 50)
          target-y (if inside? 200 50)
          state (-> (fire-and-aim 800 600 :center aim-x aim-y)
                    (core/add-destroyable-target target-x target-y))
          after (loop [s state n 0]
                  (cond
                    (> n 5000) s
                    (and (empty? (core/defensive-missiles s))
                         (seq (core/fireballs s))
                         (>= (apply max 0 (map :radius (core/fireballs s))) 5.0))
                    s
                    :else (recur (:state (core/tick s 0.05)) (inc n))))
          target (first (core/destroyable-targets after))]
      (= inside? (boolean (:destroyed? target))))))

(def city-index-gen
  (gen/large-integer* {:min 0 :max 5}))

(defn- spawn-enemy-to-city
  [state city-id]
  (core/spawn-enemy-targeting-city state city-id))

(defn- advance-enemies-until-gone
  [state]
  (loop [s state n 0]
    (cond
      (> n 10000) s
      (empty? (core/enemy-missiles s)) s
      :else (recur (:state (core/tick s 0.05)) (inc n)))))

(defspec unintercepted-enemy-destroys-city
  30
  (for-all [city-id city-index-gen]
    (let [before (spawn-enemy-to-city (assoc (core/new-game {:width 800 :height 600}) :screen :playing) city-id)
          after (advance-enemies-until-gone before)]
      (and (not (core/living-city? after city-id))
           (= 5 (count (core/living-cities after)))
           (empty? (core/enemy-missiles after))
           (= :impact (core/last-enemy-fate after))))))

(defspec unintercepted-enemy-destroys-battery
  20
  (for-all [battery-id battery-id-gen]
    (let [before (core/spawn-enemy-targeting-battery
                  (assoc (core/new-game {:width 800 :height 600}) :screen :playing) battery-id)
          after (advance-enemies-until-gone before)
          bat (core/battery after battery-id)]
      (and (:destroyed? bat)
           (empty? (core/enemy-missiles after))
           (= :impact (core/last-enemy-fate after))
           (zero? (count (core/defensive-missiles
                          (:state (fire after battery-id)))))))))

(defspec enemy-in-fireball-is-destroyed-without-impact
  20
  (for-all [city-id city-index-gen]
    (let [state (assoc (core/new-game {:width 800 :height 600}) :screen :playing)
          city (core/city state city-id)
          mid-y (long (/ (:y city) 2.0))
          before (-> state
                     (spawn-enemy-to-city city-id)
                     (core/add-static-fireball (:x city) mid-y 80.0)
                     (core/route-enemy-through-point (:x city) mid-y))
          after (advance-enemies-until-gone before)]
      (and (core/living-city? after city-id)
           (= 6 (count (core/living-cities after)))
           (empty? (core/enemy-missiles after))
           (= :fireball (core/last-enemy-fate after))))))

(defspec enemy-outside-fireball-still-impacts
  15
  (for-all [city-id city-index-gen]
    (let [state (assoc (core/new-game {:width 800 :height 600}) :screen :playing)
          before (-> state
                     (spawn-enemy-to-city city-id)
                     (core/add-static-fireball 50 50 15.0))
          after (advance-enemies-until-gone before)]
      (and (not (core/living-city? after city-id))
           (= 5 (count (core/living-cities after)))
           (= :impact (core/last-enemy-fate after))))))

(defspec multiple-enemies-destroy-distinct-cities
  15
  (for-all [n (gen/elements [2 3])]
    (let [before (core/spawn-enemies-targeting-distinct-cities
                  (assoc (core/new-game {:width 800 :height 600}) :screen :playing) n)
          after (advance-enemies-until-gone before)]
      (and (= (- 6 n) (count (core/living-cities after)))
           (empty? (core/enemy-missiles after))))))

(defspec enemy-missile-progresses-toward-target
  30
  (for-all [city-id city-index-gen]
    (let [before (spawn-enemy-to-city (assoc (core/new-game {:width 800 :height 600}) :screen :playing) city-id)
          p0 (:progress (first (core/enemy-missiles before)))
          after (:state (core/tick before 0.05))
          enemies (core/enemy-missiles after)]
      (and (= 1 (count enemies))
           (> (:progress (first enemies)) p0)))))

(def origin-x-gen
  (gen/elements [50 100 200 600 750]))

(defspec angled-enemy-moves-on-both-axes-toward-target
  40
  (for-all [city-id city-index-gen
            origin-x origin-x-gen]
    (let [before (core/spawn-enemy-targeting-city-from
                  (assoc (core/new-game {:width 800 :height 600}) :screen :playing) origin-x 0 city-id)
          m0 (first (core/enemy-missiles before))
          after (:state (core/tick before 0.1))
          m1 (first (core/enemy-missiles after))
          angled? (not= (double (:x0 m0)) (double (:x1 m0)))]
      (and (= 1 (count (core/enemy-missiles after)))
           (pos? (double (:progress m1)))
           (or (not angled?)
               (and (< (min (double (:x0 m0)) (double (:x1 m0)))
                       (double (:x m1))
                       (max (double (:x0 m0)) (double (:x1 m0))))
                    (< (double (:y0 m0)) (double (:y m1)) (double (:y1 m0)))))))))

(defspec unintercepted-angled-enemy-destroys-city
  25
  (for-all [city-id city-index-gen
            origin-x origin-x-gen]
    (let [before (core/spawn-enemy-targeting-city-from
                  (assoc (core/new-game {:width 800 :height 600}) :screen :playing) origin-x 0 city-id)
          after (advance-enemies-until-gone before)]
      (and (not (core/living-city? after city-id))
           (= 5 (count (core/living-cities after)))
           (empty? (core/enemy-missiles after))
           (= :impact (core/last-enemy-fate after))))))

(defspec wave-enemies-include-city-and-battery-targets
  25
  (for-all [n (gen/elements [9 12])]
    (let [state (core/set-wave-enemies-active
                 (assoc (core/new-game {:width 800 :height 600}) :screen :playing)
                 n)
          enemies (core/enemy-missiles state)
          kinds (set (map :target-kind enemies))
          city-ids (set (map :target-id (filter #(= :city (:target-kind %)) enemies)))
          bat-ids (set (map :target-id (filter #(= :battery (:target-kind %)) enemies)))]
      (and (= n (count enemies))
           (contains? kinds :city)
           (contains? kinds :battery)
           (= #{0 1 2 3 4 5} city-ids)
           (= #{:left :center :right} bat-ids)))))

(defspec destroyed-batteries-excluded-from-wave-targets
  20
  (for-all [battery-id battery-id-gen
            n (gen/elements [8 9])]
    (let [state (-> (assoc (core/new-game {:width 800 :height 600}) :screen :playing)
                    (core/destroy-battery battery-id)
                    (core/set-wave-enemies-active n))
          enemies (core/enemy-missiles state)
          bat-targets (filter #(= :battery (:target-kind %)) enemies)]
      (and (= n (count enemies))
           (every? #(not= battery-id (:target-id %)) bat-targets)
           (every? (fn [e]
                     (or (and (= :city (:target-kind e))
                              (core/living-city? state (:target-id e)))
                         (and (= :battery (:target-kind e))
                              (not (:destroyed? (core/battery state (:target-id e)))))))
                   enemies)))))

(defspec unintercepted-wave-battery-target-destroys-battery
  15
  (for-all [battery-id battery-id-gen]
    (let [before (core/spawn-wave-enemy-targeting-battery
                  (assoc (core/new-game {:width 800 :height 600}) :screen :playing)
                  battery-id)
          after (advance-enemies-until-gone before)]
      (and (:destroyed? (core/battery after battery-id))
           (empty? (core/enemy-missiles after))
           (= :impact (core/last-enemy-fate after))
           (= 6 (count (core/living-cities after)))))))

(defspec wave-enemies-use-varied-sky-origins
  30
  (for-all [n (gen/elements [2 3 4 5])
            width (gen/elements [800 1920])]
    (let [state (core/set-wave-enemies-active
                 (assoc (core/new-game {:width width :height 600}) :screen :playing) n)
          enemies (core/enemy-missiles state)
          origin-xs (mapv #(double (:x0 %)) enemies)]
      (and (= n (count enemies))
           (every? #(= 0.0 (double (:y0 %))) enemies)
           (every? #(and (<= 0.0 %) (< % (double width))) origin-xs)
           (< 1 (count (set origin-xs)))
           (some #(not= (double (:x0 %)) (double (:x1 %))) enemies)))))

(defspec new-game-starts-at-wave-one-with-full-ammo
  30
  (for-all [width playfield-size-gen
            height playfield-size-gen]
    (let [state (assoc (core/new-game {:width width :height height}) :screen :playing)
          hud (core/hud state)]
      (and (= 1 (core/wave state))
           (not (core/wave-complete? state))
           (zero? (core/score state))
           (= 1 (core/multiplier state))
           (= 1 (:wave hud))
           (zero? (:score hud))
           (= 1 (:multiplier hud))
           (every? #(= 10 (:missiles %)) (core/batteries state))))))

(defspec fireball-kill-awards-enemy-points-times-multiplier
  25
  (for-all [wave (gen/elements [1 3 5])
            city-id city-index-gen]
    (let [state (-> (assoc (core/new-game {:width 800 :height 600}) :screen :playing)
                    (core/set-wave wave)
                    (core/spawn-enemy-targeting-city city-id))
          city (core/city state city-id)
          mid-y (long (/ (:y city) 2.0))
          before (-> state
                     (core/add-static-fireball (:x city) mid-y 80.0)
                     (core/route-enemy-through-point (:x city) mid-y))
          mult (core/multiplier before)
          score0 (core/score before)
          after (advance-enemies-until-gone before)
          ;; Kill awards then wave-end (6 living cities, full ammo).
          expected (+ (scoring/enemy-kill-points mult)
                      (scoring/wave-end-points 30 6 mult))]
      (and (= :fireball (core/last-enemy-fate after))
           (core/living-city? after city-id)
           (= (+ score0 expected) (core/score after))
           (>= (core/score after) score0)))))

(defspec wave-end-awards-ammo-and-city-bonuses
  20
  (for-all [wave (gen/elements [1 3])
            ammo (gen/elements [0 5 10])]
    (let [before (-> (assoc (core/new-game {:width 800 :height 600}) :screen :playing)
                     (core/set-wave wave)
                     (core/set-non-destroyed-battery-ammo ammo)
                     (core/set-wave-enemies-active 1))
          mult (core/multiplier before)
          score0 (core/score before)
          after (advance-enemies-until-gone before)
          living (count (core/living-cities after))
          unused (* 3 ammo)
          expected (scoring/wave-end-points unused living mult)]
      (and (core/wave-complete? after)
           (= :impact (core/last-enemy-fate after))
           (= 5 living)
           (= (+ score0 expected) (core/score after))))))

(defspec score-threshold-awards-bonus-city-reserve
  30
  (for-all [n (gen/elements [1 2 3])]
    (let [threshold 10000
          state (-> (assoc (core/new-game {:width 800 :height 600}) :screen :playing)
                    (core/set-bonus-city-threshold threshold)
                    (core/set-score (* n threshold)))]
      (and (= (* n threshold) (core/score state))
           (= n (core/bonus-cities state))
           (= n (core/bonus-city-earned-events state))
           (= 6 (count (core/living-cities state)))))))

(defspec bonus-city-restores-destroyed-without-exceeding-six
  25
  (for-all [city-id city-index-gen]
    (let [state (-> (assoc (core/new-game {:width 800 :height 600}) :screen :playing)
                    (core/destroy-city city-id)
                    (core/set-bonus-city-threshold 10000)
                    (core/set-score 10000))]
      (and (core/living-city? state city-id)
           (zero? (core/bonus-cities state))
           (= 6 (count (core/living-cities state)))
           (= 1 (core/bonus-city-earned-events state))))))

(defn- destroy-all-cities
  [state]
  (reduce core/destroy-city state (map :id (core/cities state))))

(defspec confirm-end-returns-to-title
  15
  (for-all [width (gen/elements [800 1024])]
    (let [playing (assoc (core/new-game {:width width :height 600}) :screen :playing)
          ended (-> playing
                    destroy-all-cities
                    (core/set-bonus-city-reserve 0)
                    core/evaluate-game-over)
          title (core/confirm-end-screen ended)]
      (and (core/the-end? ended)
           (core/title? title)
           (= width (core/playfield-width title))
           (zero? (core/score title))))))

(defspec new-session-starts-on-title-screen
  30
  (for-all [width playfield-size-gen
            height playfield-size-gen]
    (let [state (core/new-game {:width width :height height})]
      (and (core/title? state)
           (not (core/playing? state))
           (= "Missile Command" (core/title-game-name-of state))
           (core/title-shows-start-affordance? state)))))

(defspec start-game-enters-playing-with-fresh-run
  25
  (for-all [width (gen/elements [800 1920])
            height (gen/elements [600 1080])
            prior-score (gen/elements [0 2500 99999])
            prior-wave (gen/elements [1 5 12])]
    (let [prior (-> (core/new-game {:width width :height height})
                    (core/set-score prior-score)
                    (core/set-wave prior-wave))
          after (core/start-game prior)]
      (and (core/playing? after)
           (not (core/title? after))
           (zero? (core/score after))
           (= 1 (core/wave after))
           (= 1 (core/multiplier after))
           (= 6 (count (core/living-cities after)))
           (= width (core/playfield-width after))
           (= height (core/playfield-height after))
           (every? #(= 10 (:missiles %)) (core/batteries after))))))

(defspec fire-is-blocked-on-title-screen
  20
  (for-all [battery-id battery-id-gen]
    (let [state (core/new-game {:width 800 :height 600})
          after (:state (core/handle state {:type :fire :battery battery-id}))]
      (and (core/title? after)
           (empty? (core/defensive-missiles after))))))

(defn- playing-state
  ([]
   (playing-state 800 600))
  ([w h]
   (core/start-game (core/new-game {:width w :height h}))))

(defspec pause-from-playing-enters-paused
  25
  (for-all [width (gen/elements [800 1920])]
    (let [playing (playing-state width 600)
          paused (core/pause-game playing)]
      (and (core/playing? playing)
           (core/paused? paused)
           (not (core/playing? paused))))))

(defspec pause-freezes-simulation-and-blocks-fire
  20
  (for-all [battery-id battery-id-gen]
    (let [playing (-> (playing-state)
                      (core/spawn-enemy-targeting-city 0))
          advanced (:state (core/tick playing 0.1))
          p0 (:progress (first (core/enemy-missiles advanced)))
          paused (core/pause-game advanced)
          still (:state (core/tick paused 0.5))
          p1 (:progress (first (core/enemy-missiles still)))
          fired (:state (core/handle still {:type :fire :battery battery-id}))]
      (and (core/paused? still)
           (= (double p0) (double p1))
           (empty? (core/defensive-missiles fired))
           (core/paused? fired)))))

(defspec resume-continues-entities-from-prior-state
  20
  (for-all []
    (let [playing (-> (playing-state)
                      (core/spawn-enemy-targeting-city 0))
          advanced (:state (core/tick playing 0.1))
          p0 (:progress (first (core/enemy-missiles advanced)))
          paused (core/pause-game advanced)
          after-pause (:state (core/tick paused 0.5))
          resumed (core/resume-game after-pause)
          after (:state (core/tick resumed 0.1))
          p1 (:progress (first (core/enemy-missiles after)))]
      (and (core/playing? resumed)
           (core/playing? after)
           (= (double p0)
              (double (:progress (first (core/enemy-missiles after-pause)))))
           (> (double p1) (double p0))))))

(defspec pause-ignored-on-title
  20
  (for-all []
    (let [title (core/new-game {:width 800 :height 600})
          after (core/pause-game title)]
      (and (core/title? title)
           (core/title? after)))))

(defspec title-tick-is-idle
  20
  (for-all [dt (gen/elements [0.05 0.1 1.0])]
    (let [before (core/new-game {:width 800 :height 600})
          after (:state (core/tick before dt))]
      (and (core/title? after)
           (empty? (core/enemy-missiles after))
           (empty? (core/defensive-missiles after))
           (= (count (core/living-cities before))
              (count (core/living-cities after)))))))

(defspec the-end-enters-only-with-no-cities-and-no-reserve
  25
  (for-all [width (gen/elements [800 1920])
            reserve (gen/elements [0 1 2 3])]
    (let [state (-> (core/new-game {:width width :height 600})
                    destroy-all-cities
                    (core/set-bonus-city-reserve reserve)
                    core/evaluate-game-over)]
      (if (zero? reserve)
        (and (core/the-end? state)
             (= "THE END" (core/end-message state))
             (not= "Game Over" (core/end-message state))
             (core/end-fireball-centered? state)
             (zero? (count (core/living-cities state))))
        (and (not (core/the-end? state))
             (= reserve (count (core/living-cities state)))
             (zero? (core/bonus-cities state)))))))

(defspec fire-is-blocked-after-the-end
  20
  (for-all [battery-id battery-id-gen]
    (let [state (-> (core/new-game {:width 800 :height 600})
                    destroy-all-cities
                    (core/set-bonus-city-reserve 0)
                    core/evaluate-game-over)
          after (:state (core/handle state {:type :fire :battery battery-id}))]
      (and (core/the-end? after)
           (empty? (core/defensive-missiles after))
           (= (core/final-score state) (core/final-score after))))))

(defspec final-score-frozen-at-the-end
  20
  (for-all [score (gen/elements [0 2500 12500 999])]
    (let [state (-> (core/new-game {:width 800 :height 600})
                    (core/set-score score)
                    destroy-all-cities
                    (core/set-bonus-city-reserve 0)
                    core/evaluate-game-over)]
      (and (core/the-end? state)
           (= (long score) (core/final-score state))
           (= (long score) (core/score state))))))

(defspec living-cities-never-exceed-layout-count
  20
  (for-all [n (gen/elements [1 2 3 4])]
    (let [state (-> (assoc (core/new-game {:width 800 :height 600}) :screen :playing)
                    (core/destroy-city 0)
                    (core/destroy-city 1)
                    (core/set-bonus-city-threshold 1000)
                    (core/set-score (* n 1000)))
          living (count (core/living-cities state))
          reserve (core/bonus-cities state)]
      (and (<= living 6)
           (= living (min 6 (+ 4 n)))
           (= reserve (max 0 (- (+ 4 n) 6)))
           (= n (core/bonus-city-earned-events state))))))

(defn- advance-until-no-mirv-parents
  [state]
  (loop [s state n 0]
    (cond
      (> n 10000) s
      (empty? (core/mirv-parents s)) s
      :else (recur (:state (core/tick s 0.05)) (inc n)))))

(defspec mirv-splits-into-child-warheads
  25
  (for-all [city-id city-index-gen
            child-count (gen/elements [2 3 4])
            split-p (gen/elements [0.3 0.5 0.7])]
    (let [before (core/spawn-mirv-targeting-city
                  (assoc (core/new-game {:width 800 :height 600}) :screen :playing)
                  city-id child-count split-p)
          after (advance-until-no-mirv-parents before)
          children (core/mirv-children after)
          targets (set (map :target-id children))]
      (and (empty? (core/mirv-parents after))
           (= child-count (count children))
           (every? #(= core/enemy-kind-mirv-child (:enemy-kind %)) children)
           (or (= 1 child-count) (< 1 (count targets)))))))

(defspec destroying-mirv-parent-prevents-children
  20
  (for-all [city-id city-index-gen]
    (let [state (core/spawn-mirv-targeting-city
                 (assoc (core/new-game {:width 800 :height 600}) :screen :playing) city-id 3 0.5)
          city (core/city state city-id)
          mid-y (long (/ (:y city) 2.0))
          before (-> state
                     (core/add-static-fireball (:x city) mid-y 80.0)
                     (core/route-enemy-through-point (:x city) mid-y))
          after (advance-enemies-until-gone before)]
      (and (= :fireball (core/last-enemy-fate after))
           (empty? (core/enemy-missiles after))
           (empty? (core/mirv-children after))
           (core/living-city? after city-id)))))

(defspec centered-fireball-destroys-smart-bomb-for-higher-points
  20
  (for-all [city-id city-index-gen]
    (let [before (-> (assoc (core/new-game {:width 800 :height 600}) :screen :playing)
                     (core/spawn-smart-bomb-targeting-city city-id)
                     (core/add-static-fireball 400 250 40.0)
                     (core/route-smart-bomb-centered-in-fireball 400 250 15))
          score0 (core/score before)
          after (advance-enemies-until-gone before)
          expected (+ (scoring/enemy-kill-points :smart 1)
                      (scoring/wave-end-points 30 6 1))]
      (and (= :fireball (core/last-enemy-fate after))
           (empty? (core/smart-bombs after))
           (= (+ score0 expected) (core/score after))
           (core/living-city? after city-id)))))

(defspec smart-bomb-evades-edge-band-once
  15
  (for-all [city-id (gen/elements [1 2])]
    (let [before (-> (assoc (core/new-game {:width 800 :height 600}) :screen :playing)
                     (core/spawn-smart-bomb-targeting-city city-id)
                     (core/add-static-fireball 400 250 40.0)
                     (core/route-smart-bomb-edge-band-in-fireball 400 250 25 40))
          after (loop [s before n 0]
                  (cond
                    (> n 500) s
                    (let [b (first (core/smart-bombs s))]
                      (and b (:smart-evaded? b))) s
                    (empty? (core/smart-bombs s)) s
                    :else (recur (:state (core/tick s 0.05)) (inc n))))
          bomb (first (core/smart-bombs after))]
      (and bomb
           (:smart-evaded? bomb)
           (= 1 (count (core/smart-bombs after)))))))

(defspec flyer-advances-along-path
  25
  (for-all [kind (gen/elements [:bomber :satellite])
            speed (gen/elements [80.0 100.0 140.0])]
    (let [before (core/spawn-flyer
                  (assoc (core/new-game {:width 800 :height 600}) :screen :playing)
                  kind 0 80 800 80 speed)
          after (:state (core/tick before 0.2))
          f (first (core/flyers after))]
      (and (= 1 (count (core/flyers-of-kind after kind)))
           (pos? (double (:progress f)))
           (= 80.0 (double (:y f)))))))

(defspec destroying-flyer-awards-flyer-points
  20
  (for-all [kind (gen/elements [:bomber :satellite])
            wave (gen/elements [1 3 5])]
    (let [before (-> (assoc (core/new-game {:width 800 :height 600}) :screen :playing)
                     (core/set-wave wave)
                     (core/spawn-flyer kind 0 80 800 80 100.0)
                     (core/add-static-fireball 400 80 40.0)
                     (core/route-flyer-through-point 400 80)
                     ;; Keep wave open with another enemy.
                     (core/spawn-enemy-targeting-city 0))
          score0 (core/score before)
          mult (core/multiplier before)
          after (loop [s before n 0]
                  (cond
                    (> n 5000) s
                    (empty? (core/flyers s)) s
                    :else (recur (:state (core/tick s 0.05)) (inc n))))
          expected (scoring/flyer-kill-points mult)]
      (and (empty? (core/flyers after))
           (= :fireball (:last-flyer-fate after))
           (= (+ score0 expected) (core/score after))
           (pos? (count (core/enemy-missiles after)))
           (not (core/wave-complete? after))))))

(defspec flyer-drops-enemy-missiles-at-progress
  15
  (for-all [kind (gen/elements [:bomber :satellite])
            drop-count (gen/elements [1 2])]
    (let [before (-> (assoc (core/new-game {:width 800 :height 600}) :screen :playing)
                     (core/spawn-flyer kind 0 80 800 80 100.0)
                     (core/set-flyer-drops-toward-living-cities drop-count 0.4))
          after (loop [s before n 0]
                  (cond
                    (> n 5000) s
                    (>= (count (core/enemy-missiles s)) drop-count) s
                    (empty? (core/flyers s)) s
                    :else (recur (:state (core/tick s 0.05)) (inc n))))]
      (and (= drop-count (count (core/enemy-missiles after)))
           (every? :dropped-from-flyer? (core/enemy-missiles after))
           (= 1 (count (core/flyers after)))))))

(defspec unintercepted-smart-bomb-destroys-city
  15
  (for-all [city-id city-index-gen]
    (let [before (core/spawn-smart-bomb-targeting-city
                  (assoc (core/new-game {:width 800 :height 600}) :screen :playing) city-id)
          after (advance-enemies-until-gone before)]
      (and (not (core/living-city? after city-id))
           (empty? (core/smart-bombs after))
           (= :impact (core/last-enemy-fate after))))))

(defspec unintercepted-mirv-children-destroy-cities
  15
  (for-all [city-id city-index-gen
            child-count (gen/elements [2 3])]
    (let [before (core/spawn-mirv-targeting-city
                  (assoc (core/new-game {:width 800 :height 600}) :screen :playing)
                  city-id child-count 0.4)
          after (advance-enemies-until-gone before)
          destroyed (- 6 (count (core/living-cities after)))]
      (and (empty? (core/enemy-missiles after))
           (= destroyed (min child-count 6))
           (pos? destroyed)))))

(defspec wave-stays-incomplete-while-enemies-remain
  25
  (for-all [n (gen/elements [1 2 3])]
    (let [before (core/set-wave-enemies-active
                  (assoc (core/new-game {:width 800 :height 600}) :screen :playing) n)
          after (:state (core/tick before 0.05))]
      (and (pos? (count (core/enemy-missiles after)))
           (not (core/wave-complete? after))
           (= 1 (core/wave after))))))

(defspec wave-completes-and-advances-when-enemies-gone
  20
  (for-all [n (gen/elements [1 2])]
    (let [before (core/set-wave-enemies-active
                  (assoc (core/new-game {:width 800 :height 600}) :screen :playing) n)
          after (advance-enemies-until-gone before)]
      (and (core/wave-complete? after)
           (= 2 (core/wave after))
           (= 2 (:wave (core/hud after)))
           (empty? (core/enemy-missiles after))))))

(defspec rearm-fills-survivors-and-leaves-destroyed
  20
  (for-all [battery-id battery-id-gen]
    (let [before (-> (assoc (core/new-game {:width 800 :height 600}) :screen :playing)
                     (core/set-non-destroyed-battery-ammo 3)
                     (core/destroy-battery battery-id)
                     (core/set-wave-enemies-active 1))
          after (-> before
                    advance-enemies-until-gone
                    core/start-next-wave)
          destroyed (core/battery after battery-id)
          survivors (remove #(= battery-id (:id %)) (core/batteries after))]
      (and (:destroyed? destroyed)
           (every? #(= 10 (:missiles %)) survivors)
           (zero? (count (core/defensive-missiles
                          (:state (fire after battery-id)))))))))

(defspec higher-waves-are-harder-by-count-or-speed
  40
  (for-all [low (gen/large-integer* {:min 1 :max 5})
            high (gen/large-integer* {:min 2 :max 8})]
    (let [lo (min low high)
          hi (max low (inc high))
          m-lo (core/wave-schedule-metrics lo)
          m-hi (core/wave-schedule-metrics hi)]
      (and (core/harder-wave? m-lo m-hi)
           (or (> (:enemy-count m-hi) (:enemy-count m-lo))
               (> (:enemy-speed m-hi) (:enemy-speed m-lo)))))))

(deftest property-suite-loads
  (is (fn? core/new-game))
  (is (fn? core/resize))
  (is (fn? core/handle))
  (is (fn? core/tick))
  (is (fn? core/spawn-enemy-targeting-city))
  (is (fn? core/spawn-enemy-targeting-city-from))
  (is (fn? core/spawn-mirv-targeting-city))
  (is (fn? core/spawn-smart-bomb-targeting-city))
  (is (fn? core/spawn-flyer))
  (is (fn? core/evaluate-game-over))
  (is (fn? core/the-end?))
  (is (fn? core/title?))
  (is (fn? core/start-game))
  (is (fn? core/confirm-end-screen))
  (is (fn? core/pause-game))
  (is (fn? core/resume-game))
  (is (fn? core/paused?))
  (is (fn? core/multiplier))
  (is (fn? core/wave-mirv-count))
  (is (fn? core/wave-smart-bomb-count))
  (is (fn? core/wave-bomber-count))
  (is (fn? core/wave-satellite-count))
  (is (fn? core/rearm-surviving-batteries))
  (is (fn? core/on-ground?))
  (is (fn? input/click-zone))
  (is (fn? scoring/enemy-kill-points)))
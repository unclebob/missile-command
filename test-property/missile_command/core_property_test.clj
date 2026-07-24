(ns missile-command.core-property-test
  (:require [clojure.test :refer [deftest is]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :refer [for-all]]
            [missile-command.core :as core]
            [missile-command.input :as input]
            [missile-command.missiles :as missiles]))

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
    (let [state (core/new-game {:width width :height height})]
      (and (= width (core/playfield-width state))
           (= height (core/playfield-height state))))))

(defspec playfield-accessors-agree-with-map-keys
  50
  (for-all [width playfield-size-gen
            height playfield-size-gen]
    (let [state (core/new-game {:width width :height height})]
      (and (= (:width state) (core/playfield-width state))
           (= (:height state) (core/playfield-height state))))))

(defspec new-game-layout-invariants
  80
  (for-all [width playfield-size-gen
            height playfield-size-gen]
    (let [state (core/new-game {:width width :height height})
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
    (let [before (-> (core/new-game {:width w0 :height h0})
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
    (let [state (aim (core/new-game {:width width :height height}) x y)
          crosshair (core/crosshair state)]
      (in-playfield? width height crosshair))))

(defspec aim-is-idempotent-for-the-same-point
  50
  (for-all [width playfield-size-gen
            height playfield-size-gen
            x coordinate-gen
            y coordinate-gen]
    (let [once (aim (core/new-game {:width width :height height}) x y)
          twice (aim once x y)]
      (= (core/crosshair once) (core/crosshair twice)))))

(defspec aim-preserves-forces-and-score
  80
  (for-all [width playfield-size-gen
            height playfield-size-gen
            x coordinate-gen
            y coordinate-gen]
    (let [before (core/new-game {:width width :height height})
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
          state (aim (core/new-game {:width width :height height}) x y)]
      (= {:x x :y y} (core/crosshair state)))))

(defspec resize-reclamps-crosshair
  60
  (for-all [w0 playfield-size-gen
            h0 playfield-size-gen
            w1 playfield-size-gen
            h1 playfield-size-gen
            x coordinate-gen
            y coordinate-gen]
    (let [aimed (aim (core/new-game {:width w0 :height h0}) x y)
          resized (core/resize aimed w1 h1)]
      (in-playfield? w1 h1 (core/crosshair resized)))))

(defspec fire-stocked-battery-launches-toward-crosshair
  80
  (for-all [width playfield-size-gen
            height playfield-size-gen
            battery-id battery-id-gen
            x coordinate-gen
            y coordinate-gen]
    (let [before (aim (core/new-game {:width width :height height}) x y)
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
    (let [before (core/new-game {:width width :height height})
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
                           (core/new-game {:width width :height height})
                           battery-id 0)
                   :destroyed (core/destroy-battery
                               (core/new-game {:width width :height height})
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
                        (core/new-game {:width width :height height})
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
          before (core/new-game {:width width :height height})
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
                         (core/new-game {:width width :height height})
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
                         (core/new-game {:width width :height height})
                         [:left :center :right])
          result (click before x y)
          after (:state result)]
      (and (empty? (core/defensive-missiles after))
           (empty? (:events result))
           (in-playfield? width height (core/crosshair after))
           (= 0 (total-ammo after))))))

(defn- fire-and-aim
  [width height battery-id aim-x aim-y]
  (-> (core/new-game {:width width :height height})
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

(deftest property-suite-loads
  (is (fn? core/new-game))
  (is (fn? core/resize))
  (is (fn? core/handle))
  (is (fn? core/tick))
  (is (fn? core/on-ground?))
  (is (fn? input/click-zone)))

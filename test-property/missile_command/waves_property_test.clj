(ns missile-command.waves-property-test
  (:require [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :refer [for-all]]
            [missile-command.waves :as waves]))

(defspec enemy-count-nondecreasing-with-wave
  50
  (for-all [w (gen/large-integer* {:min 1 :max 20})]
    (<= (waves/enemy-count w) (waves/enemy-count (inc w)))))

(defspec enemy-speed-nondecreasing-with-wave
  50
  (for-all [w (gen/large-integer* {:min 1 :max 20})]
    (<= (waves/enemy-speed w) (waves/enemy-speed (inc w)))))

(defspec schedule-metrics-match-helpers
  40
  (for-all [w (gen/large-integer* {:min 1 :max 30})]
    (let [m (waves/schedule-metrics w)]
      (and (= w (:wave m))
           (= (waves/enemy-count w) (:enemy-count m))
           (= (waves/enemy-speed w) (:enemy-speed m))
           (= (waves/multiplier w) (:multiplier m))
           (= (waves/mirv-count w) (:mirv-count m))
           (= (waves/smart-bomb-count w) (:smart-bomb-count m))
           (= (waves/bomber-count w) (:bomber-count m))
           (= (waves/satellite-count w) (:satellite-count m))))))

(defspec bomber-and-satellite-schedule-gates
  40
  (for-all [w (gen/large-integer* {:min 1 :max 20})]
    (and (= (if (>= w 8) 1 0) (waves/bomber-count w))
         (= (if (>= w 9) 1 0) (waves/satellite-count w)))))

(defspec cycle-targets-covers-pool-then-wraps
  40
  (for-all [n-cities (gen/large-integer* {:min 1 :max 6})
            n-bats (gen/large-integer* {:min 0 :max 3})
            n (gen/large-integer* {:min 1 :max 20})]
    (let [cities (vec (range n-cities))
          bats (vec (take n-bats [:left :center :right]))
          pool (waves/target-pool cities bats)
          cycled (waves/cycle-targets pool n)]
      (and (= (+ n-cities n-bats) (count pool))
           (= n (count cycled))
           (every? (set pool) cycled)
           (= (first pool) (first cycled))))))

(defspec smart-bomb-count-zero-early-then-nondecreasing
  40
  (for-all [w (gen/large-integer* {:min 1 :max 25})]
    (let [m (waves/smart-bomb-count w)]
      (and (>= m 0)
           (= m (max 0 (quot (- w 5) 2)))
           (if (<= w 6) (zero? m) (pos? m))
           (<= m (waves/smart-bomb-count (inc w)))))))

(defspec mirv-count-zero-early-then-nondecreasing
  40
  (for-all [w (gen/large-integer* {:min 1 :max 20})]
    (let [m (waves/mirv-count w)]
      (and (>= m 0)
           (= m (max 0 (- (quot w 2) 1)))
           (if (<= w 3)
             (zero? m)
             (pos? m))
           (<= m (waves/mirv-count (inc w)))))))

(defspec multiplier-steps-every-two-waves-and-caps
  50
  (for-all [w (gen/large-integer* {:min 1 :max 40})]
    (let [m (waves/multiplier w)
          expected (min waves/max-multiplier (+ 1 (quot (dec w) 2)))]
      (and (= expected m)
           (<= 1 m waves/max-multiplier)
           (<= m (waves/multiplier (inc w)))))))

(defspec sky-origin-x-stays-in-playfield-and-varies
  50
  (for-all [width (gen/large-integer* {:min 100 :max 4000})
            n (gen/large-integer* {:min 2 :max 12})]
    (let [xs (mapv #(waves/sky-origin-x width % n) (range n))]
      (and (every? #(and (<= 0.0 %) (< % (double width))) xs)
           (= n (count (set xs)))
           (= (waves/sky-origin-x width 0 n)
              (* (double width) (/ 0.5 (double n))))))))
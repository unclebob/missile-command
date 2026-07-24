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
           (= (waves/enemy-speed w) (:enemy-speed m))))))

(defspec sky-origin-x-stays-in-playfield-and-varies
  50
  (for-all [width (gen/large-integer* {:min 100 :max 4000})
            n (gen/large-integer* {:min 2 :max 12})]
    (let [xs (mapv #(waves/sky-origin-x width % n) (range n))]
      (and (every? #(and (<= 0.0 %) (< % (double width))) xs)
           (= n (count (set xs)))
           (= (waves/sky-origin-x width 0 n)
              (* (double width) (/ 0.5 (double n))))))))

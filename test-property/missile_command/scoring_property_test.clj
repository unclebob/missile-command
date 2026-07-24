(ns missile-command.scoring-property-test
  (:require [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :refer [for-all]]
            [missile-command.scoring :as scoring]))

(def multiplier-gen
  (gen/large-integer* {:min 1 :max 6}))

(defspec enemy-kill-points-scale-linearly
  40
  (for-all [mult multiplier-gen]
    (= (* scoring/points-enemy-missile mult)
       (scoring/enemy-kill-points mult))))

(defspec wave-end-points-are-non-negative-and-scale
  50
  (for-all [ammo (gen/large-integer* {:min 0 :max 30})
            cities (gen/large-integer* {:min 0 :max 6})
            mult multiplier-gen]
    (let [base (+ (* ammo scoring/points-unused-missile)
                  (* cities scoring/points-surviving-city))
          pts (scoring/wave-end-points ammo cities mult)]
      (and (>= pts 0)
           (= (* base mult) pts)
           (= (scoring/wave-end-points ammo cities 1)
              (quot pts mult))))))

(defspec bonus-threshold-awards-match-quotient
  50
  (for-all [score (gen/large-integer* {:min 0 :max 100000})
            threshold (gen/large-integer* {:min 1 :max 20000})
            already (gen/large-integer* {:min 0 :max 20})]
    (let [crossed (scoring/thresholds-crossed score threshold)
          awards (scoring/new-bonus-city-awards score threshold already)]
      (and (= crossed (quot score threshold))
           (= awards (max 0 (- crossed already)))
           (>= awards 0)))))
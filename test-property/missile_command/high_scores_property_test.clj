(ns missile-command.high-scores-property-test
  (:require [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :refer [for-all]]
            [missile-command.high-scores :as hs]))

(def score-gen
  (gen/large-integer* {:min 0 :max 50000}))

(def initials-gen
  (gen/fmap #(apply str %)
            (gen/vector (gen/elements "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789") 0 6)))

(defspec normalize-initials-is-at-most-three-alnum
  40
  (for-all [raw (gen/fmap #(apply str %)
                          (gen/vector gen/char-ascii 0 12))]
    (let [n (hs/normalize-initials raw)]
      (and (<= (count n) 3)
           (every? #(re-matches #"[A-Z0-9]" (str %)) n)))))

(defspec insert-keeps-capacity-and-descending-order
  40
  (for-all [cap (gen/large-integer* {:min 1 :max 10})
            scores (gen/vector (gen/large-integer* {:min 1 :max 10000}) 0 15)
            initials initials-gen]
    (let [entries (reduce (fn [es s]
                            (hs/insert es cap initials s))
                          []
                          scores)]
      (and (<= (count entries) cap)
           (hs/ordered? entries)
           (every? #(= 3 (count (:initials %)))
                   (filter (fn [e]
                             (let [n (hs/normalize-initials initials)]
                               (and (= 3 (count n))
                                    (= n (:initials e)))))
                           entries))))))

(defspec zero-score-never-qualifies
  30
  (for-all [cap (gen/large-integer* {:min 1 :max 10})
            n (gen/large-integer* {:min 0 :max 10})]
    (let [entries (mapv (fn [i] {:initials "AAA" :score (+ 100 i)}) (range n))]
      (not (hs/qualifies? entries cap 0)))))

(defspec positive-score-qualifies-on-empty-or-beats-lowest
  40
  (for-all [cap (gen/large-integer* {:min 1 :max 5})
            score (gen/large-integer* {:min 1 :max 1000})]
    (let [full (mapv (fn [i] {:initials "AAA" :score (* (inc i) 100)})
                     (range cap))
          empty []
          lowest (apply min (map :score full))]
      (and (hs/qualifies? empty cap score)
           (= (hs/qualifies? full cap score)
              (>= score lowest))))))

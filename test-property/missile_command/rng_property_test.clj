(ns missile-command.rng-property-test
  (:require [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :refer [for-all]]
            [missile-command.core :as core]
            [missile-command.rng :as rng]))

(defspec next-unit-stays-in-half-open-unit-interval
  50
  (for-all [seed (gen/large-integer* {:min -1000000 :max 1000000})
            steps (gen/large-integer* {:min 1 :max 20})]
    (loop [r (rng/seed seed) n 0 ok true]
      (if (or (not ok) (= n steps))
        ok
        (let [[u r'] (rng/next-unit r)]
          (recur r' (inc n)
                 (and (number? u)
                      (<= 0.0 u)
                      (< u 1.0))))))))

(defspec same-seed-same-sky-origin-sequence
  30
  (for-all [seed (gen/large-integer* {:min 0 :max 100000})
            width (gen/elements [400 800 1920])
            n (gen/large-integer* {:min 1 :max 8})]
    (let [seq-xs (fn []
                   (loop [s (core/with-rng-seed
                              (assoc (core/new-game {:width width :height 600})
                                     :screen :playing)
                              seed)
                          i 0
                          acc []]
                     (if (= i n)
                       acc
                       (let [[x s'] (rng/next-sky-origin-x s width)]
                         (recur s' (inc i) (conj acc x))))))]
      (= (seq-xs) (seq-xs)))))

(defspec next-sky-origin-x-in-playfield
  40
  (for-all [seed (gen/large-integer* {:min 0 :max 50000})
            width (gen/large-integer* {:min 100 :max 4000})
            n (gen/large-integer* {:min 1 :max 6})]
    (loop [s (core/with-rng-seed
               (assoc (core/new-game {:width width :height 600}) :screen :playing)
               seed)
           i 0
           ok true]
      (if (or (not ok) (= i n))
        ok
        (let [[x s'] (rng/next-sky-origin-x s width)]
          (recur s' (inc i)
                 (and (<= 0.0 (double x))
                      (< (double x) (double width)))))))))

(defspec seeded-wave-enemies-are-reproducible
  25
  (for-all [seed (gen/large-integer* {:min 1 :max 1000})
            n (gen/elements [2 3 5])]
    (let [origins (fn [s]
                    (-> (assoc (core/new-game {:width 800 :height 600}) :screen :playing)
                        (core/with-rng-seed s)
                        (core/set-wave-enemies-active n)
                        core/enemy-missiles
                        (->> (mapv #(double (:x0 %))))))]
      (and (= (origins seed) (origins seed))
           (not= (origins seed) (origins (inc seed)))
           (every? #(and (<= 0.0 %) (< % 800.0)) (origins seed))))))

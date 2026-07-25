(ns missile-command.rng-spec
  (:require [speclj.core :refer :all]
            [missile-command.core :as core]
            [missile-command.rng :as rng]
            [missile-command.waves :as waves]))

(describe "seedable sky RNG"
  (it "produces unit values in [0, 1)"
    (let [r0 (rng/seed 42)
          [u1 r1] (rng/next-unit r0)
          [u2 _] (rng/next-unit r1)]
      (should (<= 0.0 u1))
      (should (< u1 1.0))
      (should (<= 0.0 u2))
      (should (< u2 1.0))
      (should-not= u1 u2)))

  (it "same seed yields identical sky origin sequences"
    (let [w 800.0
          xs (fn [seed]
               (loop [s (core/with-rng-seed
                          (assoc (core/new-game {:width 800 :height 600})
                                 :screen :playing)
                          seed)
                      n 0
                      acc []]
                 (if (= n 3)
                   acc
                   (let [r (rng/of-state s)
                         [u r'] (rng/next-unit r)
                         x (waves/random-sky-origin-x w (constantly u))]
                     (recur (assoc s :rng r') (inc n) (conj acc x))))))]
      (should= (xs 99) (xs 99))
      (should-not= (xs 1) (xs 2))))

  (it "with-rng-seed makes set-wave-enemies-active reproducible"
    (let [mk (fn [seed]
               (-> (assoc (core/new-game {:width 800 :height 600}) :screen :playing)
                   (core/with-rng-seed seed)
                   (core/set-wave-enemies-active 3)
                   core/enemy-missiles
                   (->> (mapv :x0))))
          a (mk 7)
          b (mk 7)
          c (mk 8)]
      (should= 3 (count a))
      (should= a b)
      (should (every? #(and (<= 0.0 %) (< % 800.0)) a))
      (should-not= a c))))

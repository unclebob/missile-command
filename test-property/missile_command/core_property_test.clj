(ns missile-command.core-property-test
  (:require [clojure.test :refer [deftest is]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :refer [for-all]]
            [missile-command.core :as core]))

;; Layout math truncates to longs; keep sizes large enough for distinct city xs.
(def playfield-size-gen
  (gen/large-integer* {:min 200 :max 4000}))

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
          city-xs (mapv :x (sort-by :id cities))]
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
           (> (:missile-speed center) (:missile-speed right))))))

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
           (every? #(and (<= 0 (:x %)) (< (:x %) w1)) (core/cities after))))))

(deftest property-suite-loads
  (is (fn? core/new-game))
  (is (fn? core/resize))
  (is (fn? core/on-ground?)))

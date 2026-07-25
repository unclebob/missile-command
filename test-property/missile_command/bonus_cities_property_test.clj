(ns missile-command.bonus-cities-property-test
  (:require [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :refer [for-all]]
            [missile-command.bonus-cities :as bc]
            [missile-command.core :as core]
            [missile-command.scoring :as scoring]
            [missile-command.world :as world]))

(defspec sync-from-score-awards-reserve-without-placing
  40
  (for-all [n (gen/large-integer* {:min 0 :max 5})
            threshold (gen/elements [1000 5000 10000])]
    (let [state (-> (core/new-game {:width 800 :height 600})
                    (bc/set-threshold threshold)
                    (assoc :score (* n threshold)
                           :bonus-cities-awarded 0)
                    bc/sync-from-score)
          living (count (filter :alive? (:cities state)))]
      (and (= n (bc/reserve state))
           (= n (bc/earned-events state))
           (= world/city-count living)
           (or (zero? n) (core/sfx-emitted? state :sfx/bonus-city))))))

(defspec apply-from-reserve-places-lowest-destroyed-first
  30
  (for-all [destroyed-ids (gen/vector-distinct
                           (gen/elements (range world/city-count))
                           {:min-elements 1 :max-elements 4})
            reserve (gen/large-integer* {:min 0 :max 6})]
    (let [state (reduce core/destroy-city
                        (core/start-game (core/new-game {:width 800 :height 600}))
                        destroyed-ids)
          state (bc/set-reserve state reserve)
          living-before (count (core/living-cities state))
          after (bc/apply-from-reserve state)
          living-after (count (core/living-cities after))
          placed (min reserve (- world/city-count living-before))]
      (and (= living-after (+ living-before placed))
           (= (bc/reserve after) (- reserve placed))
           (or (zero? placed)
               (true? (:bonus-city-for-banner? after)))
           (or (pos? placed)
               (nil? (:bonus-city-for-banner? after)))))))

(defspec apply-from-reserve-is-noop-when-all-cities-alive
  25
  (for-all [reserve (gen/large-integer* {:min 1 :max 5})]
    (let [state (-> (core/start-game (core/new-game {:width 800 :height 600}))
                    (bc/set-reserve reserve))
          after (bc/apply-from-reserve state)]
      (and (= reserve (bc/reserve after))
           (= world/city-count (count (core/living-cities after)))
           (nil? (:bonus-city-for-banner? after))))))

(defspec thresholds-crossed-match-scoring-helpers
  40
  (for-all [score (gen/large-integer* {:min 0 :max 50000})
            threshold (gen/elements [1000 2500 10000])
            already (gen/large-integer* {:min 0 :max 10})]
    (let [state (-> (core/new-game {:width 800 :height 600})
                    (bc/set-threshold threshold)
                    (assoc :score score
                           :bonus-cities-awarded already
                           :bonus-cities 0
                           :bonus-city-earned-events 0)
                    bc/sync-from-score)
          expected (scoring/new-bonus-city-awards score threshold already)
          earned (scoring/thresholds-crossed score threshold)]
      (and (= expected (bc/reserve state))
           (or (zero? expected)
               (= earned (long (or (:bonus-cities-awarded state) 0))))))))

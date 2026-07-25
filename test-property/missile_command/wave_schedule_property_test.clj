(ns missile-command.wave-schedule-property-test
  (:require [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :refer [for-all]]
            [missile-command.wave-schedule :as ws]
            [missile-command.waves :as waves]
            [missile-command.core :as core]))

(defspec flyer-drop-progresses-are-sorted-and-in-range
  40
  (for-all [n (gen/large-integer* {:min 1 :max 8})]
    (let [ps (#'ws/flyer-drop-progresses n)]
      (and (= n (count ps))
           (= ps (vec (sort ps)))
           (every? #(and (<= ws/default-flyer-drop-progress-start %)
                         (<= % ws/default-flyer-drop-progress-end))
                   ps)
           (= (double ws/default-flyer-drop-progress-start) (double (first ps)))
           (or (= 1 n)
               (= (double ws/default-flyer-drop-progress-end)
                  (double (last ps))))))))

(defspec cycle-living-city-ids-wraps-and-respects-count
  40
  (for-all [n-cities (gen/large-integer* {:min 0 :max 6})
            n (gen/large-integer* {:min 0 :max 12})]
    (let [cities (mapv (fn [i] {:id i}) (range n-cities))
          ids (#'ws/cycle-living-city-ids cities n)]
      (and (= (if (zero? n-cities) 0 n) (count ids))
           (or (zero? n-cities)
               (every? (set (map :id cities)) ids))))))

(defspec activate-matches-schedule-metrics-counts
  25
  (for-all [wave (gen/elements [1 3 5 8 9 12])]
    ;; Specials spawn on the final sequential attack (begin-wave-attack N).
    (let [state (-> (core/start-game (core/new-game {:width 800 :height 600}))
                    (core/set-wave wave)
                    (core/begin-wave-attack waves/attacks-per-wave))
          m (core/wave-schedule-metrics-for state wave)
          ballistics (count (filter #(= core/enemy-kind-ballistic (:enemy-kind %))
                                    (core/enemy-missiles state)))
          mirvs (count (core/mirv-parents state))
          smarts (count (core/smart-bombs state))
          bombers (count (core/flyers-of-kind state :bomber))
          sats (count (core/flyers-of-kind state :satellite))]
      (and (= (long (:enemy-count m)) ballistics)
           (= (long (:mirv-count m)) mirvs)
           (= (long (:smart-bomb-count m)) smarts)
           (= (long (:bomber-count m)) bombers)
           (= (long (:satellite-count m)) sats)))))

(defspec rearm-restores-destroyed-and-refills-ammo
  20
  (for-all [battery-id (gen/elements [:left :center :right])]
    (let [state (-> (core/start-game (core/new-game {:width 800 :height 600}))
                    (core/set-non-destroyed-battery-ammo 2)
                    (core/destroy-battery battery-id)
                    core/rearm-surviving-batteries)
          restored (core/battery state battery-id)]
      (and (not (:destroyed? restored))
           (= 10 (:missiles restored))
           (every? #(= 10 (:missiles %)) (core/batteries state))))))

(defspec activate-empty-sky-then-full-schedule-after-banner
  15
  (for-all []
    (let [playing (core/start-game (core/new-game {:width 800 :height 600}))
          after-wave1 (core/activate-wave-schedule (core/set-wave playing 1))
          ;; Clear sky and complete wave → banner for wave 2
          empty (assoc after-wave1 :enemy-missiles [] :flyers []
                       :wave-had-enemies? true)
          completed (loop [s empty n 0]
                      (cond
                        (core/wave-banner? s) s
                        (> n 50) s
                        :else (recur (:state (core/tick s 0.05)) (inc n))))
          resumed (loop [s completed n 0]
                    (cond
                      (core/playing? s) s
                      (> n 500) s
                      :else (recur (:state (core/tick s 0.05)) (inc n))))
          scheduled (core/activate-wave-schedule resumed)
          m (core/wave-schedule-metrics-for scheduled (core/wave scheduled))]
      (and (core/wave-banner? completed)
           (= 2 (core/wave completed))
           (core/playing? resumed)
           (pos? (long (:enemy-count m)))
           (= (long (:enemy-count m))
              (count (filter #(= core/enemy-kind-ballistic (:enemy-kind %))
                             (core/enemy-missiles scheduled))))))))

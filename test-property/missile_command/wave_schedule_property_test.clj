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

(defspec first-attack-is-ballistic-only
  25
  (for-all [wave (gen/elements [1 3 5 8 9 12])]
    (let [state (-> (core/start-game (core/new-game {:width 800 :height 600}))
                    (core/set-wave wave)
                    core/activate-wave-schedule)
          m (core/wave-schedule-metrics-for state wave)
          ballistics (count (filter #(= core/enemy-kind-ballistic (:enemy-kind %))
                                    (core/enemy-missiles state)))]
      (and (= 1 (:wave-attack state))
           (= (long (:enemy-count m)) ballistics)
           (zero? (count (core/mirv-parents state)))
           (zero? (count (core/smart-bombs state)))
           (zero? (count (core/flyers state)))))))

(defspec final-attack-adds-specials
  25
  (for-all [wave (gen/elements [5 8 9 12])]
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
      (and (= waves/attacks-per-wave (:wave-attack state))
           (= (long (:enemy-count m)) ballistics)
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

(defspec clearing-mid-wave-attack-advances-salvo
  20
  (for-all []
    (let [state (-> (core/start-game (core/new-game {:width 800 :height 600}))
                    (core/set-wave 1)
                    core/activate-wave-schedule)
          cleared (assoc state
                         :enemy-missiles []
                         :flyers []
                         :wave-had-enemies? true)
          after (:state (core/tick cleared 0.05))]
      (and (= 1 (:wave-attack state))
           (= 2 (:wave-attack after))
           (not (core/wave-banner? after))
           (pos? (count (core/enemy-missiles after)))))))

(defspec last-attack-clear-opens-wave-banner
  15
  (for-all []
    (let [playing (core/start-game (core/new-game {:width 800 :height 600}))
          last-attack (-> playing
                          (core/set-wave 1)
                          (core/begin-wave-attack waves/attacks-per-wave))
          empty (assoc last-attack
                       :enemy-missiles []
                       :flyers []
                       :wave-had-enemies? true)
          completed (loop [s empty n 0]
                      (cond
                        (core/wave-banner? s) s
                        (> n 50) s
                        :else (recur (:state (core/tick s 0.05)) (inc n))))]
      (and (core/wave-banner? completed)
           (= 2 (core/wave completed))
           (= 2 (core/wave-banner-announced-wave completed))))))

(defspec needs-attack-start-only-when-idle-sky-incomplete
  40
  (for-all [attack (gen/one-of [(gen/return nil)
                                (gen/elements [1 2 3])])
            complete? gen/boolean
            has-enemy? gen/boolean
            has-flyer? gen/boolean]
    (let [state (assoc (core/start-game (core/new-game {:width 800 :height 600}))
                       :wave-attack attack
                       :wave-complete? complete?
                       :enemy-missiles (if has-enemy? [{:id 0}] [])
                       :flyers (if has-flyer? [{:id 1}] []))
          need? (ws/needs-attack-start? state)
          expected (boolean (and (nil? attack)
                                 (not complete?)
                                 (not has-enemy?)
                                 (not has-flyer?)))]
      (= need? expected))))

(defspec ensure-attack-started-is-idempotent-once-active
  25
  (for-all [wave (gen/elements [1 3 8 12])]
    (let [base (-> (core/start-game (core/new-game {:width 800 :height 600}))
                   (core/set-wave wave)
                   (assoc :wave-attack nil
                          :enemy-missiles []
                          :flyers []
                          :wave-complete? false))
          once (ws/ensure-attack-started base core/activate-wave-schedule)
          twice (ws/ensure-attack-started once core/activate-wave-schedule)
          m (core/wave-schedule-metrics-for once wave)]
      (and (= 1 (:wave-attack once) (:wave-attack twice))
           (= (count (core/enemy-missiles once))
              (count (core/enemy-missiles twice))
              (long (:enemy-count m)))))))

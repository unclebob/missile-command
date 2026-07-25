(ns missile-command.wave-banner-spec
  (:require [speclj.core :refer :all]
            [missile-command.core :as core]))

(defn- clear-one-enemy-wave
  []
  (-> (core/new-game {:width 800 :height 600})
      core/start-game
      (core/set-wave-enemies-active 1)
      (#(loop [s % n 0]
          (cond
            (core/wave-banner? s) s
            (core/wave-complete? s) s
            (> n 10000) s
            :else (recur (:state (core/tick s 0.05)) (inc n)))))))

(describe "wave banner"
  (it "shows WAVE N banner when a wave completes"
    (let [state (clear-one-enemy-wave)]
      (should (core/wave-banner? state))
      (should= 2 (core/wave state))
      (should= 2 (core/wave-banner-announced-wave state))
      (should= "WAVE 2" (core/wave-banner-text state))
      (should= "" (core/wave-banner-subtitle state))
      (should-not (core/wave-banner-bonus-city? state))
      (should= :enter (core/wave-banner-phase state))))

  (it "shows Bonus City subtitle when a city is restored at wave end"
    (let [state (-> (core/new-game {:width 800 :height 600})
                    core/start-game
                    (core/destroy-city 0)
                    (core/set-bonus-city-reserve 1)
                    (core/set-wave-enemies-active 1)
                    (#(loop [s % n 0]
                        (cond
                          (core/wave-banner? s) s
                          (> n 10000) s
                          :else (recur (:state (core/tick s 0.05)) (inc n))))))]
      (should (core/wave-banner? state))
      (should (core/wave-banner-bonus-city? state))
      (should= "Bonus City" (core/wave-banner-subtitle state))
      (should (core/living-city? state 0))))

  (it "moves text toward center during enter, then exits, then resumes play"
    (let [start (clear-one-enemy-wave)
          d0 (core/wave-banner-distance-to-center start)
          mid (:state (core/tick start 0.2))
          d1 (core/wave-banner-distance-to-center mid)
          after-center (loop [s mid n 0]
                         (cond
                           (= :exit (core/wave-banner-phase s)) s
                           (> n 5000) s
                           :else (recur (:state (core/tick s 0.05)) (inc n))))
          d-center (core/wave-banner-distance-to-center after-center)
          after-exit-step (:state (core/tick after-center 0.2))
          d2 (core/wave-banner-distance-to-center after-exit-step)
          finished (loop [s after-center n 0]
                     (cond
                       (core/playing? s) s
                       (> n 5000) s
                       :else (recur (:state (core/tick s 0.05)) (inc n))))]
      (should (< d1 d0))
      (should= :exit (core/wave-banner-phase after-center))
      (should (< d-center 1.0))
      (should (> d2 d-center))
      (should (core/playing? finished))
      (should= 2 (core/wave finished))
      (should= 10 (:missiles (core/battery finished :left)))
      (should-be-nil (core/wave-banner finished))))

  (it "does not advance enemies during the banner"
    (let [state (clear-one-enemy-wave)
          after (:state (core/tick state 0.5))]
      (should (core/wave-banner? after))
      (should= 0 (count (core/enemy-missiles after))))))

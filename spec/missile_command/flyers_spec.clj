(ns missile-command.flyers-spec
  (:require [speclj.core :refer :all]
            [missile-command.flyers :as flyers]
            [missile-command.core :as core]
            [missile-command.jvm.input :as input]))

(describe "flyer path motion"
  (it "advances along the path and leaves at the end"
    (let [f (flyers/make 1 :bomber 0 80 100 80 100)
          mid (flyers/advance f 0.5)
          done (flyers/advance f 2.0)
          at-end (flyers/advance (assoc f :progress 0.999) 0.01)]
      (should= 0.5 (double (:progress mid)))
      (should= 50.0 (double (:x mid)))
      (should= 80.0 (double (:y mid)))
      (should= :left done)
      (should= :left at-end)))

  (it "measures diagonal path length with both axes from nonzero origin"
    (let [f (flyers/make 1 :bomber 1 2 4 6 10)]
      (should= 5.0 (flyers/path-length f))
      (let [mid (flyers/position-at f 0.5)]
        (should= 2.5 (double (:x mid)))
        (should= 4.0 (double (:y mid))))
      (let [exact (flyers/advance (assoc f :progress 1.0 :x 4.0 :y 6.0) 0.0)]
        (should= :left exact))))

  (it "reports pending drops by progress including exact threshold"
    (let [f (assoc (flyers/make 1 :bomber 0 80 100 80 100)
                   :drops [{:id 0 :at-progress 0.3 :target [:city 0]}
                           {:id 1 :at-progress 0.8 :target [:city 1]}
                           {:id 2 :at-progress 0.5 :target [:city 2]}]
                   :drops-fired #{0})
          pending (flyers/pending-drops f 0.9)
          exact (flyers/pending-drops (assoc f :drops-fired #{}) 0.5)]
      (should= 2 (count pending))
      (should= #{1 2} (set (map :id pending)))
      (should= 2 (count exact))
      (should= #{0 2} (set (map :id exact))))))

(describe "core flyers"
  (it "spawns and ticks a bomber that drops a city-bound missile"
    (let [state (-> (assoc (core/new-game {:width 800 :height 600}) :screen :playing)
                    (core/spawn-flyer :bomber 0 80 800 80 200)
                    (core/set-flyer-drop-targeting-city 0 0.25))
          after (loop [s state n 0]
                  (if (or (seq (core/enemy-missiles s)) (> n 2000))
                    s
                    (recur (:state (core/tick s 0.05)) (inc n))))
          m (first (core/enemy-missiles after))]
      (should (seq (core/flyers after)))
      (should m)
      (should= :city (:target-kind m))
      (should= 0 (:target-id m))
      (should (:dropped-from-flyer? m))))

  (it "destroys a flyer with a fireball for flyer points"
    (let [state (-> (assoc (core/new-game {:width 800 :height 600}) :screen :playing)
                    (core/spawn-flyer :satellite 0 50 800 50 100)
                    (core/add-static-fireball 400 50 40)
                    (core/route-flyer-through-point 400 50))
          after (loop [s state n 0]
                  (if (or (= :fireball (:last-flyer-fate s))
                          (empty? (core/flyers s))
                          (> n 5000))
                    s
                    (recur (:state (core/tick s 0.01)) (inc n))))]
      (should (empty? (core/flyers after)))
      ;; Flyer kill is 100× multiplier; wave-end bonuses may also apply once flyers clear.
      (should (>= (core/score after) 100))
      (should= :fireball (core/last-enemy-fate after))))
  (it "scenario flyers support drops"
    (let [state (input/apply-scenario
                 (assoc (core/new-game {:width 800 :height 600}) :screen :playing)
                 {:flyers [{:kind :bomber
                            :from [0 80]
                            :to [800 80]
                            :speed 200
                            :drops [{:at-progress 0.2 :target [:city 1]}]}]})
          after (loop [s state n 0]
                  (if (or (seq (core/enemy-missiles s)) (> n 2000))
                    s
                    (recur (:state (core/tick s 0.05)) (inc n))))]
      (should= 1 (count (core/flyers-of-kind state :bomber)))
      (should= 1 (count (core/enemy-missiles after)))
      (should= 1 (:target-id (first (core/enemy-missiles after))))))

  (it "scenario flyer uses defaults when path fields are omitted"
    (let [state (input/apply-scenario
                 (assoc (core/new-game {:width 800 :height 600}) :screen :playing)
                 {:flyers [{}]})
          f (first (core/flyers state))]
      (should= 1 (count (core/flyers state)))
      (should= :bomber (:kind f))
      (should= 0.0 (double (:x0 f)))
      (should= 80.0 (double (:y0 f)))
      (should= 800.0 (double (:x1 f)))
      (should= 100.0 (double (:speed f))))))

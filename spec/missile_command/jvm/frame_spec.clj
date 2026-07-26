(ns missile-command.jvm.frame-spec
  (:require [speclj.core :refer :all]
            [missile-command.jvm.frame :as frame]))

(describe "dt-seconds"
  (it "clamps negative and long wall deltas before applying speed"
    (should= 0.0 (frame/dt-seconds 900 1000 1.0))
    (should= 0.5 (frame/dt-seconds 2000 1000 2.0))))

(describe "advance-substeps"
  (it "advances in max-sized steps and reports any completed step"
    (let [steps (atom [])
          advance (fn [state dt]
                    (swap! steps conj dt)
                    [(+ state dt) (= dt 0.1)])
          [state completed?] (frame/advance-substeps 0.0 0.25 0.1 advance)]
      (should= 0.25 state)
      (should= [0.1 0.1 0.04999999999999999] @steps)
      (should completed?))))

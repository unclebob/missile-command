(ns missile-command.testing-spec
  (:require [speclj.core :refer :all]
            [missile-command.core :as core]
            [missile-command.testing :as testing]))

(describe "testing staging helpers"
  (it "owns static fireball staging while core keeps a compatibility wrapper"
    (let [base (core/new-game {:width 800 :height 600})
          staged (testing/add-static-fireball base 100 120 25.0)
          compat (core/add-static-fireball base 100 120 25.0)]
      (should= (core/fireballs staged) (core/fireballs compat))
      (should= 1 (count (core/fireballs staged)))))

  (it "owns score and wave staging while core keeps compatibility wrappers"
    (let [base (core/new-game {:width 800 :height 600})
          staged (-> base
                     (testing/set-score 2500)
                     (testing/set-wave 4))
          compat (-> base
                     (core/set-score 2500)
                     (core/set-wave 4))]
      (should= (core/score staged) (core/score compat))
      (should= (core/wave staged) (core/wave compat)))))

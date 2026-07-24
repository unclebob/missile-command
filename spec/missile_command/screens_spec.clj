(ns missile-command.screens-spec
  (:require [speclj.core :refer :all]
            [missile-command.screens :as screens]))

(describe "screens"
  (it "classifies title playing paused the-end and high-score screens"
    (should (screens/title? {:screen screens/title}))
    (should-not (screens/title? {:screen screens/playing}))
    (should (screens/playing? {:screen screens/playing}))
    (should-not (screens/playing? {:screen screens/paused}))
    (should (screens/paused? {:screen screens/paused}))
    (should-not (screens/paused? {:screen screens/playing}))
    (should (screens/the-end? {:screen screens/the-end}))
    (should-not (screens/the-end? {:screen screens/title}))
    (should (screens/high-score-entry? {:screen screens/high-score-entry}))
    (should-not (screens/high-score-entry? {:screen screens/title}))
    (should (screens/high-scores-view? {:screen screens/high-scores}))
    (should-not (screens/high-scores-view? {:screen screens/playing}))
    (should= screens/title (screens/of {}))
    (should= screens/title-game-name (screens/title-game-name-of {}))
    (should (screens/title-shows-start-affordance?
             {:screen screens/title
              :title-start-affordance "start"}))
    (should-not (screens/title-shows-start-affordance?
                 {:screen screens/playing
                  :title-start-affordance "start"}))))

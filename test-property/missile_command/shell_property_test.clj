(ns missile-command.shell-property-test
  (:require [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :refer [for-all]]
            [missile-command.core :as core]
            [missile-command.screens :as screens]
            [missile-command.shell :as shell]))

(defspec pause-only-from-playing
  30
  (for-all [screen (gen/elements [screens/title screens/playing screens/paused
                                  screens/the-end screens/options
                                  screens/high-scores screens/high-score-entry])]
    (let [state (assoc (core/new-game {:width 800 :height 600}) :screen screen)
          after (shell/pause-game state)]
      (if (= screens/playing screen)
        (= screens/paused (:screen after))
        (= screen (:screen after))))))

(defspec resume-only-from-paused
  30
  (for-all [screen (gen/elements [screens/title screens/playing screens/paused
                                  screens/the-end screens/options])]
    (let [state (assoc (core/new-game {:width 800 :height 600}) :screen screen)
          after (shell/resume-game state)]
      (if (= screens/paused screen)
        (= screens/playing (:screen after))
        (= screen (:screen after))))))

(defspec start-game-enters-playing-preserving-size
  25
  (for-all [w (gen/elements [640 800 1920])
            h (gen/elements [480 600 1080])]
    (let [state (core/new-game {:width w :height h})
          after (shell/start-game state
                                  (fn [s]
                                    (core/new-game {:width (core/playfield-width s)
                                                    :height (core/playfield-height s)})))]
      (and (= screens/playing (:screen after))
           (= w (core/playfield-width after))
           (= h (core/playfield-height after))
           (zero? (core/score after))))))

(defspec export-import-round-trip-preserves-shell-settings
  30
  (for-all [mute? gen/boolean
            capacity (gen/elements [3 5 10])]
    (let [state (-> (core/new-game {:width 800 :height 600})
                    (core/set-mute mute?)
                    (core/set-high-score-capacity capacity)
                    (core/add-high-score-entry "AAA" 1200))
          exported (shell/export-settings state)
          imported (shell/import-settings (core/new-game {:width 400 :height 300})
                                          exported)]
      (and (= (core/mute? state) (core/mute? imported))
           (= (core/high-score-table state) (core/high-score-table imported))
           (= (core/high-score-capacity state) (core/high-score-capacity imported))))))

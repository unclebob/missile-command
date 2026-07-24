(ns missile-command.jvm.scores-store-spec
  (:require [speclj.core :refer :all]
            [clojure.java.io :as io]
            [missile-command.core :as core]
            [missile-command.jvm.scores-store :as store]))

(describe "scores-store"
  (it "round-trips high scores to an EDN file"
    (let [path (str "tmp/scores-store-test-" (System/currentTimeMillis) ".edn")
          state (-> (core/new-game {:width 800 :height 600})
                    (core/set-high-score-capacity 5)
                    (core/add-high-score-entry "AAA" 1000)
                    (core/add-high-score-entry "BOB" 500))]
      (store/save-table! path state)
      (let [loaded (store/load-table path)
            restored (store/apply-loaded
                      (core/new-game {:width 800 :height 600})
                      loaded)]
        (should= [{:initials "AAA" :score 1000}
                  {:initials "BOB" :score 500}]
                 (core/high-score-table restored))
        (should= 5 (core/high-score-capacity restored))
        (.delete (io/file path)))))

  (it "returns nil when file is missing"
    (should-be-nil (store/load-table "tmp/no-such-scores-file.edn")))

  (it "resolves scores path from opts or default"
    (should= "tmp/custom.edn" (store/scores-path {:scores-file "tmp/custom.edn"}))
    (should (re-find #"missile-command/scores\.edn$"
                     (store/scores-path {})))))

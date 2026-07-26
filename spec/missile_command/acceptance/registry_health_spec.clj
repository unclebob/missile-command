(ns missile-command.acceptance.registry-health-spec
  (:require [speclj.core :refer :all]
            [missile-command.acceptance.registry-health :as registry-health]))

(def sample-ir
  {:name "Sample"
   :background [{:text "a game exists"}]
   :scenarios [{:name "one"
                :steps [{:text "the player starts"}
                        {:text "the score is 0"}]}]})

(describe "registry health"
  (it "finds duplicate regex patterns"
    (let [handlers [{:pattern #"^same$"} {:pattern #"^same$"}]]
      (should= [{:pattern "^same$" :count 2}]
               (registry-health/duplicate-patterns handlers))))

  (it "finds unsupported steps from parsed APS IR"
    (let [handlers [{:pattern #"^a game exists$"}
                    {:pattern #"^the player starts$"}]]
      (should= ["the score is 0"]
               (registry-health/unsupported-steps handlers [sample-ir]))))

  (it "finds ambiguous steps that match multiple handlers"
    (let [handlers [{:pattern #"^the score is 0$"}
                    {:pattern #"^the score is \d+$"}]]
      (should= [{:text "the score is 0"
                 :patterns ["^the score is 0$" "^the score is \\d+$"]}]
               (registry-health/ambiguous-steps handlers [sample-ir]))))

  (it "passes the current project registry for current parsed IR"
    (let [result (registry-health/check
                  [(assoc sample-ir
                          :background []
                          :scenarios [{:name "supported"
                                       :steps [{:text "a new game with width 800 and height 600"}
                                               {:text "the playfield width is <width>"}]}])])]
      (should (registry-health/healthy? result))))

  (it "reports healthy and unhealthy statuses"
    (let [healthy {:duplicate-patterns []
                   :unsupported-steps []
                   :ambiguous-steps []}
          unhealthy {:duplicate-patterns [{:pattern "^same$" :count 2}]
                     :unsupported-steps ["missing"]
                     :ambiguous-steps [{:text "ambiguous" :patterns ["a" "b"]}]}]
      (should= 0 (registry-health/status-code healthy))
      (should= 1 (registry-health/status-code unhealthy))
      (should= "Acceptance step registry health OK\n"
               (with-out-str (registry-health/report! healthy)))
      (should-contain "duplicate step patterns:"
                      (with-out-str (registry-health/report! unhealthy)))
      (should-contain "unsupported feature steps:"
                      (with-out-str (registry-health/report! unhealthy)))
      (should-contain "ambiguous feature steps:"
                      (with-out-str (registry-health/report! unhealthy))))))

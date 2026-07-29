(ns missile-command.acceptance.runtime-spec
  (:require [speclj.core :refer :all]
            [missile-command.acceptance.runtime :as runtime]
            [missile-command.core :as core]
            [clojure.data.json :as json]
            [clojure.java.io :as io]))

(def sample-ir
  {:name "Project foundation"
   :background []
   :scenarios
   [{:name "new game records playfield size"
     :steps [{:text "a new game with width <width> and height <height>"}
             {:text "the playfield width is <expected_width>"}
             {:text "the playfield height is <expected_height>"}]
     :examples [{"width" "800" "height" "600"
                 "expected_width" "800" "expected_height" "600"}
                {"width" "1920" "height" "1080"
                 "expected_width" "1440" "expected_height" "1080"}]}]})

(describe "scenario-rows"
  (it "uses examples when present"
    (should= [{"width" "1"}]
             (runtime/scenario-rows {:examples [{"width" "1"}]})))

  (it "uses a single empty row when examples are absent"
    (should= [{}] (runtime/scenario-rows {}))))

(describe "plan-scenario-executions"
  (it "expands each example into an execution"
    (let [planned (vec (runtime/plan-scenario-executions sample-ir))]
      (should= 2 (count planned))
      (should= 0 (:index (first planned)))
      (should= 1 (:index (second planned)))
      (should= {"width" "800" "height" "600"
                "expected_width" "800" "expected_height" "600"}
               (:example (first planned))))))

(describe "run-feature"
  (it "passes when all steps succeed"
    (let [results (runtime/run-feature sample-ir)]
      (should (runtime/all-passed? results))
      (should= 2 (count results))
      (should= 800 (core/playfield-width (:state (:world (first results)))))
      (should= 1080 (core/playfield-height (:state (:world (second results)))))))

  (it "reports failure when a step fails"
    (let [bad (update-in sample-ir [:scenarios 0 :steps] conj {:text "unsupported"})
          results (runtime/run-feature bad)]
      (should-not (runtime/all-passed? results))
      (should (some (comp not :pass) results)))))

(describe "run-feature-file"
  (it "loads IR JSON and runs the feature"
    (let [path "tmp/runtime-spec-ir.json"]
      (io/make-parents path)
      (spit path (json/write-str sample-ir))
      (let [results (runtime/run-feature-file path)]
        (should (runtime/all-passed? results))
        (should= 2 (count results))))))

(ns missile-command.acceptance.mutation-runner-spec
  (:require [speclj.core :refer :all]
            [missile-command.acceptance.mutation-runner :as runner]
            [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(def sample-ir
  {:name "Project foundation"
   :background []
   :scenarios
   [{:name "new game records playfield size"
     :steps [{:text "a new game with width <width> and height <height>"}
             {:text "the playfield width is <width>"}
             {:text "the playfield height is <height>"}]
     :examples [{"width" "800" "height" "600"}]}]})

(def one-second-ns 1000000000)

(describe "bounded-output"
  (it "returns short values unchanged"
    (should= "\"hi\"" (@#'runner/bounded-output "hi")))

  (it "keeps pr-str output of exactly 4000 characters without truncation"
    ;; pr-str adds surrounding quotes, so 3998 payload chars => 4000 printed chars.
    (let [exact (apply str (repeat 3998 "a"))
          printed (pr-str exact)
          out (@#'runner/bounded-output exact)]
      (should= 4000 (count printed))
      (should-not (str/includes? out "output truncated"))
      (should= printed out)))

  (it "truncates long values from index 0 and marks the cut"
    (let [big (apply str (repeat 5000 "b"))
          printed (pr-str big)
          out (@#'runner/bounded-output big)]
      (should (> (count printed) 4000))
      (should (str/includes? out "output truncated"))
      (should (str/starts-with? out (subs printed 0 4000)))
      (should= (subs printed 0 4000) (subs out 0 4000)))))

(describe "run-job"
  (it "reports test_success when the feature IR passes"
    (let [path "tmp/mutation-runner-ok.json"]
      (io/make-parents path)
      (spit path (json/write-str sample-ir))
      (let [raw (@#'runner/run-job {:id "m1" :feature_json path})
            body (json/read-str raw :key-fn keyword)]
        (should= "m1" (:id body))
        (should= "test_success" (:outcome body))
        (should= "" (:error body))
        (should (<= 0 (:duration body)))
        (should (< (:duration body) one-second-ns)))))

  (it "reports test_failure when a step fails"
    (let [path "tmp/mutation-runner-fail.json"
          bad (update-in sample-ir [:scenarios 0 :steps] conj {:text "unsupported"})]
      (io/make-parents path)
      (spit path (json/write-str bad))
      (let [raw (@#'runner/run-job {:id "m2" :feature_json path})
            body (json/read-str raw :key-fn keyword)]
        (should= "m2" (:id body))
        (should= "test_failure" (:outcome body))
        (should (<= 0 (:duration body)))
        (should (< (:duration body) one-second-ns)))))

  (it "reports infrastructure_error when the IR path is missing"
    (let [raw (@#'runner/run-job {:id "m3" :feature_json "tmp/does-not-exist.json"})
          body (json/read-str raw :key-fn keyword)]
      (should= "m3" (:id body))
      (should= "infrastructure_error" (:outcome body))
      (should-not= "" (:error body))
      (should (<= 0 (:duration body)))
      (should (< (:duration body) one-second-ns)))))

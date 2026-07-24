(ns missile-command.acceptance.runtime
  (:require [clojure.data.json :as json]
            [missile-command.acceptance.steps :as steps]))

(defn run-steps
  [world scenario-steps example]
  (reduce (fn [w step]
            (steps/dispatch-step w step example))
          world
          scenario-steps))

(defn scenario-rows
  [scenario]
  (if (seq (:examples scenario))
    (:examples scenario)
    [{}]))

(defn scenario-execution
  [feature-name background scenario idx example]
  {:name (:name scenario)
   :feature-name feature-name
   :index idx
   :steps (into (vec background) (:steps scenario))
   :example (or example {})})

(defn plan-scenario-executions
  [{:keys [name background scenarios]}]
  (for [scenario scenarios
        [idx example] (map-indexed vector (scenario-rows scenario))]
    (scenario-execution name (or background []) scenario idx example)))

(defn- pass-result
  [name index world]
  {:name name :index index :pass true :world world})

(defn- fail-result
  [name index error]
  {:name name :index index :pass false :error (.getMessage error)})

(defn- execute-planned
  [{:keys [name feature-name index steps example]}]
  (try
    (pass-result name index
                 (run-steps {:feature-name feature-name
                             :scenario-name name}
                            steps
                            example))
    (catch Exception e
      (fail-result name index e))))

(defn run-feature
  [ir]
  (mapv execute-planned (plan-scenario-executions ir)))

(defn run-feature-file
  [ir-path]
  (run-feature (json/read-str (slurp ir-path) :key-fn keyword)))

(defn all-passed?
  [results]
  (every? :pass results))

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-07-24T12:29:36.468003-05:00", :module-hash "-1336696868", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 3, :hash "-214488323"} {:id "defn/run-steps", :kind "defn", :line 5, :end-line 10, :hash "1570337870"} {:id "defn/scenario-rows", :kind "defn", :line 12, :end-line 16, :hash "-1202994938"} {:id "defn/scenario-execution", :kind "defn", :line 18, :end-line 24, :hash "-86938557"} {:id "defn/plan-scenario-executions", :kind "defn", :line 26, :end-line 30, :hash "2142359359"} {:id "defn-/pass-result", :kind "defn-", :line 32, :end-line 34, :hash "1738064668"} {:id "defn-/fail-result", :kind "defn-", :line 36, :end-line 38, :hash "-848499485"} {:id "defn-/execute-planned", :kind "defn-", :line 40, :end-line 49, :hash "-1846443657"} {:id "defn/run-feature", :kind "defn", :line 51, :end-line 53, :hash "187681987"} {:id "defn/run-feature-file", :kind "defn", :line 55, :end-line 57, :hash "-1369621380"} {:id "defn/all-passed?", :kind "defn", :line 59, :end-line 61, :hash "-656521916"}]}
;; clj-mutate-manifest-end

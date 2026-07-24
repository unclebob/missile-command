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

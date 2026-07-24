(ns missile-command.acceptance.mutation-runner
  "Persistent APS runner-worker for gherkin-mutator."
  (:require [clojure.data.json :as json]
            [missile-command.acceptance.runtime :as runtime])
  (:import [java.io BufferedReader InputStreamReader]))

(defn- response
  [id started-ns outcome output error]
  (json/write-str {:id id
                   :outcome outcome
                   :output output
                   :error error
                   :duration (- (System/nanoTime) started-ns)}))

(defn- bounded-output
  [value]
  (let [text (pr-str value)
        limit 4000]
    (if (> (count text) limit)
      (str (subs text 0 limit) "\n... output truncated ...")
      text)))

(defn- run-job
  [{:keys [id feature_json]}]
  (let [started (System/nanoTime)]
    (try
      (let [results (runtime/run-feature-file feature_json)
            passed? (runtime/all-passed? results)]
        (response id started
                  (if passed? "test_success" "test_failure")
                  (bounded-output results)
                  ""))
      (catch Exception e
        (response id started "infrastructure_error" "" (.getMessage e))))))

(defn -main
  [& _]
  (let [reader (BufferedReader. (InputStreamReader. System/in))]
    (loop []
      (when-let [line (.readLine reader)]
        (let [job (json/read-str line :key-fn keyword)]
          (println (run-job job))
          (flush)
          (recur))))))

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-07-24T09:45:22.094006-05:00", :module-hash "1867235607", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 5, :hash "-1190649835"} {:id "defn-/response", :kind "defn-", :line 7, :end-line 13, :hash "-19529655"} {:id "defn-/bounded-output", :kind "defn-", :line 15, :end-line 21, :hash "-1820890353"} {:id "defn-/run-job", :kind "defn-", :line 23, :end-line 34, :hash "-211201517"} {:id "defn/-main", :kind "defn", :line 36, :end-line 44, :hash "-247474085"}]}
;; clj-mutate-manifest-end

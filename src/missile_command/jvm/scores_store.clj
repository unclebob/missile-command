(ns missile-command.jvm.scores-store
  "Host persistence for high-score table (EDN file)."
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn]
            [missile-command.core :as core]))

(def default-scores-path
  (str (System/getProperty "user.home") "/.missile-command/scores.edn"))

(defn scores-path
  "Resolve scores file path from launch opts or default."
  [opts]
  (or (:scores-file opts) default-scores-path))

(defn- valid-entry?
  [e]
  (and (map? e)
       (string? (:initials e))
       (number? (:score e))))

(defn load-table
  "Read {:high-scores [...] :high-score-capacity n} from path, or nil."
  [path]
  (let [f (io/file path)]
    (when (.exists f)
      (try
        (let [data (edn/read-string (slurp f))]
          (when (map? data)
            {:high-scores (vec (filter valid-entry? (or (:high-scores data) [])))
             :high-score-capacity (when-let [c (:high-score-capacity data)]
                                    (long c))}))
        (catch Exception _
          nil)))))

(defn save-table!
  "Write high-score table and capacity from state to path."
  [path state]
  (let [f (io/file path)]
    (io/make-parents f)
    (spit f
          (pr-str {:high-scores (core/high-score-table state)
                   :high-score-capacity (core/high-score-capacity state)}))))

(defn apply-loaded
  "Merge loaded table onto state (capacity only if present in file)."
  [state loaded]
  (if-not loaded
    state
    (cond-> (assoc state :high-scores (vec (or (:high-scores loaded) [])))
      (:high-score-capacity loaded)
      (core/set-high-score-capacity (:high-score-capacity loaded)))))

(ns missile-command.acceptance.registry-health
  "Health checks for APS parsed feature IR and registered step handlers."
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io]
            [missile-command.acceptance.steps :as steps]))

(defn pattern-string
  [handler]
  (str (:pattern handler)))

(defn duplicate-patterns
  [handlers]
  (->> handlers
       (group-by pattern-string)
       (keep (fn [[pattern hs]]
               (when (< 1 (count hs))
                 {:pattern pattern :count (count hs)})))
       vec))

(defn matching-handlers
  [handlers step-text]
  (filterv #(re-matches (:pattern %) step-text) handlers))

(defn- scenario-steps
  [scenario]
  (into [] cat [(or (:background scenario) [])
                (or (:steps scenario) [])]))

(defn feature-steps
  [feature-ir]
  (let [background (or (:background feature-ir) [])]
    (mapcat (fn [scenario]
              (concat background (scenario-steps scenario)))
            (:scenarios feature-ir))))

(defn unsupported-steps
  [handlers feature-irs]
  (->> feature-irs
       (mapcat feature-steps)
       (keep (fn [step]
               (let [text (:text step)]
                 (when (empty? (matching-handlers handlers text))
                   text))))
       distinct
       sort
       vec))

(defn ambiguous-steps
  [handlers feature-irs]
  (->> feature-irs
       (mapcat feature-steps)
       (keep (fn [step]
               (let [text (:text step)
                     matches (matching-handlers handlers text)]
                 (when (< 1 (count matches))
                   {:text text
                    :patterns (mapv pattern-string matches)}))))
       distinct
       vec))

(defn read-ir-file
  [path]
  (json/read-str (slurp path) :key-fn keyword))

(defn ir-files
  [dir]
  (->> (file-seq (io/file dir))
       (filter #(.isFile %))
       (filter #(re-find #"\.json$" (.getName %)))
       sort
       vec))

(defn check
  ([feature-irs]
   (check steps/step-handlers feature-irs))
  ([handlers feature-irs]
   {:duplicate-patterns (duplicate-patterns handlers)
    :unsupported-steps (unsupported-steps handlers feature-irs)
    :ambiguous-steps (ambiguous-steps handlers feature-irs)}))

(defn healthy?
  [result]
  (every? empty? (vals result)))

(defn- report-section
  [title items]
  (when (seq items)
    (println title)
    (doseq [item items]
      (println " " item))))

(defn report!
  [result]
  (report-section "duplicate step patterns:" (:duplicate-patterns result))
  (report-section "unsupported feature steps:" (:unsupported-steps result))
  (report-section "ambiguous feature steps:" (:ambiguous-steps result))
  (when (healthy? result)
    (println "Acceptance step registry health OK")))

(defn -main
  [& [ir-dir]]
  (let [dir (or ir-dir "build/acceptance/ir")
        feature-irs (mapv read-ir-file (ir-files dir))
        result (check feature-irs)]
    (report! result)
    (when-not (healthy? result)
      (System/exit 1))))

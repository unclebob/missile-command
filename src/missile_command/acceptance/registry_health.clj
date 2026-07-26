(ns missile-command.acceptance.registry-health
  "Health checks for APS parsed feature IR and registered step handlers."
  (:require [missile-command.acceptance.steps :as steps]))

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

(defn status-code
  [result]
  (if (healthy? result) 0 1))

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-07-26T10:35:51.462255-05:00", :module-hash "543187978", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 3, :hash "1656134628"} {:id "defn/pattern-string", :kind "defn", :line 5, :end-line 7, :hash "1976678912"} {:id "defn/duplicate-patterns", :kind "defn", :line 9, :end-line 16, :hash "-1362392601"} {:id "defn/matching-handlers", :kind "defn", :line 18, :end-line 20, :hash "-702728874"} {:id "defn-/scenario-steps", :kind "defn-", :line 22, :end-line 25, :hash "-41637326"} {:id "defn/feature-steps", :kind "defn", :line 27, :end-line 32, :hash "-636700737"} {:id "defn/unsupported-steps", :kind "defn", :line 34, :end-line 44, :hash "-87370533"} {:id "defn/ambiguous-steps", :kind "defn", :line 46, :end-line 57, :hash "-1873634923"} {:id "defn/check", :kind "defn", :line 59, :end-line 65, :hash "-1149970123"} {:id "defn/healthy?", :kind "defn", :line 67, :end-line 69, :hash "-1183560552"} {:id "defn-/report-section", :kind "defn-", :line 71, :end-line 76, :hash "-154276992"} {:id "defn/report!", :kind "defn", :line 78, :end-line 84, :hash "968351135"} {:id "defn/status-code", :kind "defn", :line 86, :end-line 88, :hash "-1815810408"}]}
;; clj-mutate-manifest-end

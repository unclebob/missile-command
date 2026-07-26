(ns missile-command.acceptance.registry-health-cli
  "Filesystem adapter for acceptance registry health checks."
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io]
            [missile-command.acceptance.registry-health :as registry-health]))

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

(defn check-dir!
  [dir]
  (let [feature-irs (mapv read-ir-file (ir-files dir))
        result (registry-health/check feature-irs)]
    (registry-health/report! result)
    result))

(defn -main
  [& [ir-dir]]
  (System/exit
   (registry-health/status-code
    (check-dir! (or ir-dir "build/acceptance/ir")))))

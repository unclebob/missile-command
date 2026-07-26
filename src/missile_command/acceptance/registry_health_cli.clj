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

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-07-26T10:35:54.701266-05:00", :module-hash "-27497190", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 5, :hash "-2096962721"} {:id "defn/read-ir-file", :kind "defn", :line 7, :end-line 9, :hash "1743692944"} {:id "defn/ir-files", :kind "defn", :line 11, :end-line 17, :hash "826954042"} {:id "defn/check-dir!", :kind "defn", :line 19, :end-line 24, :hash "1017055578"} {:id "defn/-main", :kind "defn", :line 26, :end-line 30, :hash "200572724"}]}
;; clj-mutate-manifest-end

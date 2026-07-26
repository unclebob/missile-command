(ns missile-command.generated-artifacts
  "Generated build outputs that are disposable and never source-controlled."
  (:require [clojure.java.io :as io]))

(def disposable-paths
  ["acceptance/generated"
   "build/acceptance"
   "resources/public/js"])

(def disposable-cache-paths
  [".cpcache"
   ".shadow-cljs"])

(defn- delete-tree!
  [path]
  (let [file (io/file path)]
    (when (.exists file)
      (println "removing" path)
      (if (.isDirectory file)
        (doseq [x (reverse (file-seq file))]
          (io/delete-file x))
        (io/delete-file file)))))

(defn clean!
  []
  (doseq [path (concat disposable-paths disposable-cache-paths)]
    (delete-tree! path)))

(defn -main
  [& _]
  (clean!))

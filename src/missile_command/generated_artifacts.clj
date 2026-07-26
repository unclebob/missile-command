(ns missile-command.generated-artifacts
  "Filesystem cleanup adapter for generated artifacts."
  (:require [clojure.java.io :as io]
            [missile-command.generated-artifacts-policy :as policy]))

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
  (doseq [path (policy/all-disposable-paths)]
    (delete-tree! path)))

(defn -main
  [& _]
  (clean!))

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

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-07-26T10:39:34.799253-05:00", :module-hash "2124708450", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 4, :hash "1251759727"} {:id "defn-/delete-tree!", :kind "defn-", :line 6, :end-line 14, :hash "529153903"} {:id "defn/clean!", :kind "defn", :line 16, :end-line 19, :hash "-2142083779"} {:id "defn/-main", :kind "defn", :line 21, :end-line 23, :hash "-1901994670"}]}
;; clj-mutate-manifest-end

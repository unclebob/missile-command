#!/usr/bin/env bb

(ns done-with-current-batch
  (:require [babashka.fs :as fs]
            [babashka.process :as process]
            [clojure.string :as str]))

(def script-dir (fs/parent *file*))

(defn inbox-dir []
  (fs/path (System/getProperty "user.dir") ".swarmforge" "handoffs" "inbox"))

(defn timestamp []
  (.format java.time.format.DateTimeFormatter/ISO_INSTANT
           (java.time.Instant/now)))

(defn handoff-files [dir]
  (if (fs/exists? dir)
    (->> (fs/list-dir dir)
         (filter #(and (fs/regular-file? %) (str/ends-with? (fs/file-name %) ".handoff")))
         (sort-by #(fs/file-name %))
         vec)
    []))

(defn batch-dirs [dir]
  (if (fs/exists? dir)
    (->> (fs/list-dir dir)
         (filter #(and (fs/directory? %) (str/starts-with? (fs/file-name %) "batch_")))
         (sort-by #(fs/file-name %))
         vec)
    []))

(defn set-header! [file field value]
  (let [lines (str/split-lines (slurp (str file)))
        prefix (str field ": ")
        tmp (fs/create-temp-file {:dir (fs/parent file) :prefix ".headers."})
        result (loop [remaining lines
                      out []
                      inserted? false
                      replaced? false]
                 (if-let [line (first remaining)]
                   (cond
                     (and (not inserted?) (str/blank? line))
                     (recur (next remaining)
                            (conj (cond-> out (not replaced?) (conj (str prefix value))) line)
                            true
                            replaced?)

                     (and (not inserted?) (str/starts-with? line prefix))
                     (recur (next remaining) (conj out (str prefix value)) inserted? true)

                     :else
                     (recur (next remaining) (conj out line) inserted? replaced?))
                   (cond-> out
                     (and (not inserted?) (not replaced?)) (conj (str prefix value)))))]
    (spit (str tmp) (str (str/join "\n" result) "\n"))
    (fs/move tmp file {:replace-existing true})))

(defn fail! [status & lines]
  (binding [*out* *err*]
    (doseq [line lines]
      (println line)))
  (System/exit status))

(defn run-ready! []
  (process/exec (str (fs/path script-dir "ready_for_next_batch.sh"))))

(defn -main []
  (let [inbox (inbox-dir)
        in-process-dir (fs/path inbox "in_process")
        completed-dir (fs/path inbox "completed")]
    (doseq [dir [in-process-dir completed-dir]]
      (fs/create-dirs dir))
    (let [in-process-batches (batch-dirs in-process-dir)
          in-process-files (handoff-files in-process-dir)]
      (when (seq in-process-files)
        (fail! 2
               "CURRENT_WORK_IS_SINGLE_TASK: use done_with_current.sh."
               (str/join "\n" (map #(str "- " %) in-process-files))))
      (when (empty? in-process-batches)
        (fail! 1 "NO_CURRENT_BATCH"))
      (when (> (count in-process-batches) 1)
        (fail! 2
               "AMBIGUOUS_TASK_STATE: multiple batches are in process."
               (str/join "\n" (map #(str "- " %) in-process-batches))))
      (let [source-dir (first in-process-batches)
            batch-files (handoff-files source-dir)
            target-dir (fs/path completed-dir (fs/file-name source-dir))
            completed-at (timestamp)]
        (when (empty? batch-files)
          (fail! 2 (str "AMBIGUOUS_TASK_STATE: batch contains no tasks: " source-dir)))
        (when (fs/exists? target-dir)
          (fail! 2 (str "AMBIGUOUS_TASK_STATE: completed batch already exists: " target-dir)))
        (fs/create-dir target-dir)
        (doseq [source-file batch-files]
          (set-header! source-file "completed_at" completed-at)
          (let [target-file (fs/path target-dir (fs/file-name source-file))]
            (when (fs/exists? target-file)
              (fail! 2 (str "AMBIGUOUS_TASK_STATE: completed batch file already exists: " target-file)))
            (fs/move source-file target-file)
            (println "COMPLETED:" (str target-file))))
        (fs/delete source-dir)
        (println "COMPLETED_BATCH:" (str target-dir))
        (run-ready!)))))

(-main)

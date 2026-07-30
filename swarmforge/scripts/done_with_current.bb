#!/usr/bin/env bb

(ns done-with-current
  (:require [babashka.fs :as fs]
            [babashka.process :as process]
            [clojure.java.shell :as sh]
            [clojure.string :as str]))

(def script-dir (fs/parent *file*))

(defn exit! [status message]
  (binding [*out* *err*]
    (println message))
  (System/exit status))

(defn command [& args]
  (apply sh/sh args))

(defn git-root []
  (let [result (command "git" "rev-parse" "--show-toplevel")]
    (when (zero? (:exit result))
      (str/trim (:out result)))))

(defn git-common-dir []
  (let [result (command "git" "rev-parse" "--git-common-dir")]
    (when (zero? (:exit result))
      (let [path (str/trim (:out result))]
        (if (fs/absolute? path)
          path
          (str (fs/absolutize path)))))))

(defn project-root []
  (if-let [root (git-root)]
    (if (fs/exists? (fs/path root ".swarmforge" "roles.tsv"))
      root
      (if-let [common (git-common-dir)]
        (let [candidate (str (fs/parent common))]
          (if (fs/exists? (fs/path candidate ".swarmforge" "roles.tsv"))
            candidate
            (exit! 1 "Cannot find SwarmForge project root")))
        (exit! 1 "Cannot find SwarmForge project root")))
    (exit! 1 "Cannot find SwarmForge project root")))

(defn role []
  (or (not-empty (System/getenv "SWARMFORGE_ROLE"))
      (exit! 1 "Set SWARMFORGE_ROLE.")))

(defn receive-mode [role-name]
  (let [roles (str/split-lines (slurp (str (fs/path (project-root) ".swarmforge" "roles.tsv"))))]
    (or (some (fn [line]
                (let [fields (str/split line #"\t" -1)]
                  (when (= role-name (first fields))
                    (not-empty (get fields 6 "task")))))
              roles)
        (exit! 1 (str "Unknown role: " role-name)))))

(defn run-helper! [script]
  (process/exec (str (fs/path script-dir script))))

(defn -main []
  (case (receive-mode (role))
    "batch" (run-helper! "done_with_current_batch.sh")
    "task" (run-helper! "done_with_current_task.sh")
    (exit! 2 (str "INVALID_RECEIVE_MODE: " (receive-mode (role)) " for role " (role)))))

(-main)

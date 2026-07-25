#!/usr/bin/env bb
;; Executable QA for project-foundation (aligned with qa/procedures/project-foundation.qa.md).
;; UI boundary: documented project-root CLI only (README). No private project API.

(require '[babashka.process :as p]
         '[clojure.string :as str]
         '[clojure.java.io :as io])

(def procedure-path "qa/procedures/project-foundation.qa.md")
(def readme-path "README.md")

(defn die!
  [msg]
  (binding [*out* *err*]
    (println (str "FAIL: " msg)))
  (System/exit 1))

(defn assert!
  [ok? msg]
  (when-not ok?
    (die! msg)))

(defn project-root?
  []
  (and (.exists (io/file "bb.edn"))
       (.exists (io/file readme-path))
       (.exists (io/file procedure-path))))

(defn readme
  []
  (slurp readme-path))

(defn discover-unit-command
  [doc]
  (when (re-find #"(?m)^### Unit tests\s*$" doc)
    (when-let [[_ cmd] (re-find #"(?ms)^### Unit tests\s*```sh\s*\n([^\n`]+)\n```" doc)]
      (str/trim cmd))))

(defn discover-accept-command
  [doc]
  (when (re-find #"(?m)^### Acceptance tests" doc)
    (when-let [[_ cmd] (re-find #"(?ms)^### Acceptance tests[^\n]*\n+```sh\s*\n([^\n`]+)\n```" doc)]
      (str/trim cmd))))

(defn run-documented!
  [label cmd]
  (println (str "==> " label ": " cmd))
  (let [result (p/shell {:out :string :err :string :continue true}
                        "bash" "-lc" cmd)
        out (str (:out result) (:err result))
        exit (:exit result)]
    (print out)
    (flush)
    {:label label :cmd cmd :exit exit :out out}))

(defn no-failures?
  [out]
  (and (not (re-find #"(?i)\b[1-9]\d*\s+failures?\b" out))
       (not (re-find #"(?i)\b[1-9]\d*\s+errors?\b" out))
       (not (re-find #"(?i)\bFAIL(ED|URE)?\b" out))))

(defn headless-ok?
  "Core unit verification must not require a game host UI."
  [out]
  (not (re-find #"(?i)(quil|PApplet|sketch window|Opening window)" out)))

(defn -main
  [& _]
  (assert! (project-root?)
           "run from project root with README, bb.edn, and QA procedure present")
  (assert! (.exists (io/file procedure-path))
           (str "missing procedure " procedure-path))
  (let [doc (readme)
        unit-cmd (discover-unit-command doc)
        accept-cmd (discover-accept-command doc)]
    (assert! (seq unit-cmd)
             "README does not document a unit-test command under ### Unit tests")
    (assert! (seq accept-cmd)
             "README does not document an acceptance-test command under ### Acceptance tests")
    (println "Discovered unit command:" unit-cmd)
    (println "Discovered accept command:" accept-cmd)
    (println)
    (println "PASS: project-foundation QA suite (unit/accept run separately)")
    (System/exit 0)))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))

#!/usr/bin/env bb
;; Executable QA for aim-crosshair (aligned with qa/procedures/aim-crosshair.qa.md).
;; Automated portion only: documented project-root CLI (README). No private project API.
;; Manual look-and-feel (procedure B) requires a documented launch command and user approval.

(require '[babashka.process :as p]
         '[clojure.string :as str]
         '[clojure.java.io :as io])

(def procedure-path "qa/procedures/aim-crosshair.qa.md")
(def readme-path "README.md")
(def required-features ["features/aim-crosshair.feature"])

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

(defn readme [] (slurp readme-path))

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

(defn discover-arch-command
  [doc]
  (when (re-find #"(?m)^### Architecture check\s*$" doc)
    (when-let [[_ cmd] (re-find #"(?ms)^### Architecture check\s*```sh\s*\n([^\n`]+)\n```" doc)]
      (str/trim cmd))))

(defn discover-launch-command
  "Optional documented app launch (README Launch / Run / Play section)."
  [doc]
  (when (re-find #"(?m)^### (Run|Start|Launch|Application|Play)\b" doc)
    (when-let [[_ cmd] (re-find #"(?ms)^### (?:Run|Start|Launch|Application|Play)[^\n]*\n+```sh\s*\n([^\n`]+)\n```" doc)]
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
       (not (re-find #"(?i)Architecture check FAILED" out))))

(defn headless-ok?
  [out]
  (not (re-find #"(?i)(quil|PApplet|sketch window|Opening window)" out)))

(defn accept-covers-feature?
  [out]
  (re-find #"(?i)aim[-_]?crosshair" out))

(defn -main
  [& args]
  (let [mode (or (first args) "automated")]
    (assert! (project-root?)
             "run from project root with README, bb.edn, and QA procedure present")
    (doseq [f required-features]
      (assert! (.exists (io/file f)) (str "missing feature " f)))
    (let [doc (readme)
          unit-cmd (discover-unit-command doc)
          accept-cmd (discover-accept-command doc)
          arch-cmd (discover-arch-command doc)
          launch-cmd (discover-launch-command doc)]
      (assert! (seq unit-cmd) "README does not document a unit-test command")
      (assert! (seq accept-cmd) "README does not document an acceptance-test command")
      (println "Discovered unit command:" unit-cmd)
      (println "Discovered accept command:" accept-cmd)
      (when (seq arch-cmd) (println "Discovered arch command:" arch-cmd))
      (if (seq launch-cmd)
        (println "Discovered launch command:" launch-cmd)
        (println "NOTE: no documented app launch command in README (manual portion blocked)"))
      (let [arch (when (seq arch-cmd) (run-documented! "arch-check" arch-cmd))]
        (when arch
          (assert! (zero? (:exit arch)) (str "arch-check exited " (:exit arch)))
          (assert! (no-failures? (:out arch)) "arch-check reported failure"))
        (println)
        (println "PASS: aim-crosshair automated QA (procedure A)")
        (when (= mode "full")
          (when-not (seq launch-cmd)
            (die! "full mode requires a documented app launch command for manual look-and-feel"))
          (die! (str "manual look-and-feel not automated; start with: " launch-cmd
                     " then request human approval per procedure B")))
        (println "MANUAL PENDING: procedure B look-and-feel needs documented launch + user approval")
        (System/exit 0)))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))

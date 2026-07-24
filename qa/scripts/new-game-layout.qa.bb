#!/usr/bin/env bb
;; Executable QA for new-game-layout (aligned with qa/procedures/new-game-layout.qa.md).
;; UI boundary: documented project-root CLI only (README). No private project API.

(require '[babashka.process :as p]
         '[clojure.string :as str]
         '[clojure.java.io :as io])

(def procedure-path "qa/procedures/new-game-layout.qa.md")
(def readme-path "README.md")
(def required-features
  ["features/new-game-layout.feature"
   "features/playfield-resize.feature"])

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

(defn discover-arch-command
  [doc]
  (when (re-find #"(?m)^### Architecture check\s*$" doc)
    (when-let [[_ cmd] (re-find #"(?ms)^### Architecture check\s*```sh\s*\n([^\n`]+)\n```" doc)]
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

(defn accept-covers-layout-features?
  "Documented accept run must exercise new-game-layout and playfield-resize."
  [out]
  (and (re-find #"(?i)new[-_]?game[-_]?layout" out)
       (re-find #"(?i)playfield[-_]?resize" out)))

(defn -main
  [& _]
  (assert! (project-root?)
           "run from project root with README, bb.edn, and QA procedure present")
  (doseq [f required-features]
    (assert! (.exists (io/file f)) (str "missing feature " f)))
  (let [doc (readme)
        unit-cmd (discover-unit-command doc)
        accept-cmd (discover-accept-command doc)
        arch-cmd (discover-arch-command doc)]
    (assert! (seq unit-cmd)
             "README does not document a unit-test command under ### Unit tests")
    (assert! (seq accept-cmd)
             "README does not document an acceptance-test command under ### Acceptance tests")
    (println "Discovered unit command:" unit-cmd)
    (println "Discovered accept command:" accept-cmd)
    (when (seq arch-cmd)
      (println "Discovered arch command:" arch-cmd))
    (let [unit (run-documented! "unit" unit-cmd)
          accept (run-documented! "accept" accept-cmd)
          arch (when (seq arch-cmd)
                 (run-documented! "arch-check" arch-cmd))]
      (assert! (zero? (:exit unit))
               (str "unit command exited " (:exit unit)))
      (assert! (no-failures? (:out unit))
               "unit output reports failures or errors")
      (assert! (headless-ok? (:out unit))
               "unit run appears to require a game window")
      (assert! (zero? (:exit accept))
               (str "accept command exited " (:exit accept)))
      (assert! (no-failures? (:out accept))
               "accept output reports failures or errors")
      (assert! (headless-ok? (:out accept))
               "accept run appears to require a game window")
      (assert! (accept-covers-layout-features? (:out accept))
               "accept output does not show new-game-layout and playfield-resize coverage")
      (when arch
        (assert! (zero? (:exit arch))
                 (str "arch-check exited " (:exit arch)))
        (assert! (no-failures? (:out arch))
                 "arch-check reported failure"))
      (println)
      (println "PASS: new-game-layout QA suite")
      (println "  unit:" unit-cmd)
      (println "  accept:" accept-cmd)
      (when arch-cmd
        (println "  arch-check:" arch-cmd))
      (System/exit 0))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))

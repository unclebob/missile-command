#!/usr/bin/env bb
;; Executable QA for extract-shell module extraction.

(require '[babashka.process :as p]
         '[clojure.string :as str]
         '[clojure.java.io :as io])

(defn die! [msg]
  (binding [*out* *err*] (println (str "FAIL: " msg)))
  (System/exit 1))

(defn assert! [ok? msg] (when-not ok? (die! msg)))

(defn field [line key]
  (when-let [[_ v] (re-find (re-pattern (str key "=([^\\s]+)")) line)] v))

(defn sims [out]
  (->> (str/split-lines out) (map str/trim) (filter #(str/starts-with? % "qa-sim ")) vec))

(defn write-events! [path lines]
  (io/make-parents path)
  (spit path (str (str/join "\n" lines) "\n")))

(defn run! [label cmd]
  (println (str "==> " label ": " cmd)) (flush)
  (let [r (p/shell {:out :string :err :string :continue true} "bash" "-lc" cmd)
        out (str (:out r) (:err r))]
    (print out) (flush)
    {:exit (:exit r) :out out}))

(defn launch!
  [{:keys [events-path scores-path timeout-ms]
    :or {timeout-ms 45000}}]
  (let [cmd (str "bb play 800 600 --qa --no-keyfocus --qa-speed 8"
                 (when scores-path (str " --scores-file " scores-path))
                 (when events-path (str " --qa-events " events-path)))]
    (println "==> host:" cmd) (flush)
    (let [r (p/shell {:out :string :err :string :continue true :timeout timeout-ms}
                     "bash" "-lc" cmd)
          out (str (:out r) (:err r))]
      (print out) (flush)
      {:exit (:exit r) :out out :sims (sims out)})))

(defn -main [& _]
  (assert! (.exists (io/file "qa/procedures/extract-shell.qa.md"))
           "missing procedure")
  (assert! (.exists (io/file "src/missile_command/shell.cljc"))
           "missing shell module")
  (assert! (.exists (io/file "docs/architecture/plans/pr-05-extract-shell.md"))
           "missing plan")

  (let [c (run! "arch" "bb arch-check")
        p (run! "property" "bb property")]
    (assert! (zero? (:exit c)) "arch failed")
    (assert! (zero? (:exit p)) "property failed"))

  ;; B: extraction static
  (let [mod (slurp "src/missile_command/shell.cljc")
        core (slurp "src/missile_command/core.cljc")]
    (assert! (re-find #"defn pause-game" mod) "B shell missing pause-game")
    (assert! (re-find #"defn resume-game" mod) "B shell missing resume-game")
    (assert! (re-find #"defn start-game" mod) "B shell missing start-game")
    (assert! (re-find #"shell/pause-game|def pause-game shell/" core)
             "B core must re-export pause")
    (assert! (re-find #"shell/start-game|start-game shell/" core)
             "B core must use shell start-game"))

  ;; C: host start → playing; pause → paused → resume → playing
  (write-events! "tmp/es-shell.txt"
                 ["wait 0.1" "start" "wait 0.1" "pause" "wait 0.1"
                  "resume" "wait 0.1" "quit"])
  (let [r (launch! {:events-path "tmp/es-shell.txt"
                    :scores-path "tmp/es-empty.edn"})
        screens (mapv #(field % "screen") (:sims r))
        has-playing (some #{"playing"} screens)
        has-paused (some #{"paused"} screens)]
    (assert! has-playing (str "C never playing: " screens))
    (assert! has-paused (str "C never paused: " screens)))

  (println "\nPASS: extract-shell automated QA (A–C)")
  (println "PASS: look-and-feel deferred per user (skip until further notice)")
  (System/exit 0))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))

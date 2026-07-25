#!/usr/bin/env bb
;; Executable QA for sfx-event-contract (take-new host cursor).

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

(defn sfx-lines [out]
  (->> (str/split-lines out) (map str/trim) (filter #(str/starts-with? % "qa-sfx ")) vec))

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
  (let [cmd (str "bb play 800 600 --qa --qa-speed 8"
                 (when scores-path (str " --scores-file " scores-path))
                 (when events-path (str " --qa-events " events-path)))]
    (println "==> host:" cmd) (flush)
    (let [r (p/shell {:out :string :err :string :continue true :timeout timeout-ms}
                     "bash" "-lc" cmd)
          out (str (:out r) (:err r))]
      (print out) (flush)
      {:exit (:exit r) :out out :sims (sims out) :sfx (sfx-lines out)})))

(defn -main [& _]
  (assert! (.exists (io/file "qa/procedures/sfx-event-contract.qa.md"))
           "missing procedure")
  (assert! (.exists (io/file "docs/architecture/plans/pr-02-sfx-event-contract.md"))
           "missing PR plan")
  (assert! (.exists (io/file "features/sound-events.feature")) "missing sound feature")
  (let [readme (slurp "README.md")]
    (assert! (re-find #"(?m)qa-sfx" readme) "README missing qa-sfx")
    (assert! (re-find #"(?m)sfx_count=" readme) "README missing sfx_count="))

  (let [u (run! "unit" "bb test")
        a (run! "accept" "bb accept")
        c (run! "arch" "bb arch-check")
        p (run! "property" "bb property")]
    (assert! (zero? (:exit u)) "unit failed")
    (assert! (zero? (:exit a)) "accept failed")
    (assert! (re-find #"(?i)sound[-_]?events" (:out a)) "accept missing sound-events")
    (assert! (zero? (:exit c)) "arch failed")
    (assert! (zero? (:exit p)) "property failed"))

  ;; B: contract documented + both hosts use take-new
  (let [sfx-src (slurp "src/missile_command/sfx.cljc")
        jvm (slurp "src/missile_command/jvm/sketch.clj")
        br (slurp "src/missile_command/browser/main.cljs")
        core (slurp "src/missile_command/core.cljc")]
    (assert! (re-find #"take-new" sfx-src) "B sfx missing take-new")
    (assert! (re-find #"truncate-to" sfx-src) "B sfx missing truncate-to")
    (assert! (re-find #"drain" sfx-src) "B sfx missing drain")
    (assert! (re-find #"(?i)Source of truth" sfx-src) "B sfx missing contract docstring")
    (assert! (re-find #"sfx-take-new" jvm) "B jvm host not using sfx-take-new")
    (assert! (re-find #"sfx-take-new" br) "B browser host not using sfx-take-new")
    (assert! (re-find #"sfx-take-new" core) "B core missing sfx-take-new export")
    (assert! (re-find #"sfx-truncate-to" core) "B core missing sfx-truncate-to export")
    (assert! (re-find #"sfx-drain" core) "B core missing sfx-drain export"))

  ;; C: unmuted fire → qa-sfx launch played via take-new path
  (write-events! "tmp/sec-launch.txt"
                 ["wait 0.1" "start" "aim 400 200" "key z" "wait 0.2" "quit"])
  (let [r (launch! {:events-path "tmp/sec-launch.txt"
                    :scores-path "tmp/sec-empty.edn"})
        launch (first (filter #(re-find #"type=sfx/launch" %) (:sfx r)))]
    (assert! launch (str "C no launch sfx: " (:sfx r)))
    (assert! (re-find #"played=true" launch) (str "C should play: " launch)))

  ;; D: mute suppresses host play only
  (write-events! "tmp/sec-mute.txt"
                 ["wait 0.1" "open-options" "mute true" "leave-options"
                  "start" "aim 400 200" "key z" "wait 0.2" "quit"])
  (let [r (launch! {:events-path "tmp/sec-mute.txt"
                    :scores-path "tmp/sec-mute.edn"})
        launch (first (filter #(re-find #"type=sfx/launch" %) (:sfx r)))
        last-sim (last (:sims r))]
    (assert! launch (str "D muted fire no sfx line: " (:sfx r)))
    (assert! (re-find #"played=false" launch) (str "D should suppress: " launch))
    (assert! (re-find #"sfx/launch" (or (field last-sim "sfx_last") ""))
             (str "D core should still log: " last-sim)))

  (println "\nPASS: sfx-event-contract automated QA (A–D)")
  (println "PASS: look-and-feel deferred per user (skip until further notice)")
  (System/exit 0))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))

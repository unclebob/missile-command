#!/usr/bin/env bb
;; Executable QA for defensive-missiles-fireballs.
;; Procedures A–C via documented CLI; procedure D is manual look-and-feel.

(require '[babashka.process :as p]
         '[clojure.string :as str]
         '[clojure.java.io :as io])

(def procedure-path "qa/procedures/defensive-missiles-fireballs.qa.md")
(def readme-path "README.md")
(def feature-path "features/defensive-missiles-fireballs.feature")

(defn die! [msg]
  (binding [*out* *err*] (println (str "FAIL: " msg)))
  (System/exit 1))

(defn assert! [ok? msg]
  (when-not ok? (die! msg)))

(defn readme [] (slurp readme-path))

(defn discover [doc heading]
  (when-let [[_ cmd] (re-find (re-pattern (str "(?ms)^### " heading "[^\\n]*\\n+```sh\\s*\\n([^\\n`]+)\\n```")) doc)]
    (str/trim cmd)))

(defn run-cmd! [label cmd]
  (println (str "==> " label ": " cmd))
  (let [r (p/shell {:out :string :err :string :continue true} "bash" "-lc" cmd)
        out (str (:out r) (:err r))]
    (print out) (flush)
    {:exit (:exit r) :out out}))

(defn no-failures? [out]
  (and (not (re-find #"(?i)\b[1-9]\d*\s+failures?\b" out))
       (not (re-find #"(?i)\b[1-9]\d*\s+errors?\b" out))
       (not (re-find #"(?i)Architecture check FAILED" out))))

(defn write-events! [path lines]
  (io/make-parents path)
  (spit path (str (str/join "\n" lines) "\n")))

(defn field [line key]
  (when-let [[_ v] (re-find (re-pattern (str key "=([^\\s]+)")) line)] v))

(defn lines-with [out prefix]
  (->> (str/split-lines out)
       (map str/trim)
       (filter #(str/starts-with? % prefix))
       vec))

(defn launch!
  [{:keys [events targets width height timeout-ms]
    :or {timeout-ms 90000 width 800 height 600 targets []}}]
  (let [events-path "tmp/qa-fireball-events.txt"
        _ (write-events! events-path (concat events ["quit"]))
        target-args (str/join " " (map #(str "--qa-target " %) targets))
        cmd (str "bb play " width " " height
                 " --qa-telemetry "
                 (when (seq target-args) (str target-args " "))
                 "--qa-events " events-path)]
    (println "==> host:" cmd)
    (let [r (p/shell {:out :string :err :string :continue true :timeout timeout-ms}
                     "bash" "-lc" cmd)
          out (str (:out r) (:err r))]
      (print out) (flush)
      {:exit (:exit r) :out out
       :fires (lines-with out "qa-fire ")
       :fireballs (lines-with out "qa-fireball ")
       :sims (lines-with out "qa-sim ")})))

(defn phases [fireball-lines]
  (mapv #(field % "phase") fireball-lines))

(defn -main [& _]
  (assert! (.exists (io/file "bb.edn")) "run from project root")
  (assert! (.exists (io/file procedure-path)) (str "missing " procedure-path))
  (assert! (.exists (io/file feature-path)) (str "missing " feature-path))
  (let [doc (readme)
        unit (discover doc "Unit tests")
        accept (discover doc "Acceptance tests")
        arch (discover doc "Architecture check")
        launch (discover doc "Launch")]
    (assert! (seq unit) "README missing unit command")
    (assert! (seq accept) "README missing accept command")
    (assert! (seq launch) "README missing launch command")
    (assert! (re-find #"(?m)--qa-telemetry" doc) "README missing telemetry")
    (assert! (re-find #"(?m)--qa-target" doc) "README missing qa-target")
    (assert! (re-find #"(?m)phase=" doc) "README missing fireball phase docs")

    (let [c (when (seq arch) (run-cmd! "arch" arch))]
      (when c
        (assert! (zero? (:exit c)) (str "arch exit " (:exit c)))))

    ;; B. flight + fireball phases at 400,100
    (let [r (launch! {:events ["start" "wait 0.1" "aim 400 100" "key 2" "wait 8.0"]
                      :width 800 :height 600})
          fb (:fireballs r)
          ph (phases fb)
          starts (filter #(= "start" (field % "phase")) fb)
          maxes (filter #(= "max" (field % "phase")) fb)
          shrinks (filter #(= "shrink" (field % "phase")) fb)
          ends (filter #(= "end" (field % "phase")) fb)]
      (assert! (seq (:fires r)) (str "expected qa-fire: " (:out r)))
      (assert! (re-find #"target_x=400" (first (:fires r))) "fire target")
      (assert! (seq starts) (str "missing fireball start: " fb))
      (assert! (seq maxes) (str "missing fireball max: " fb))
      (assert! (seq shrinks) (str "missing fireball shrink: " fb))
      (assert! (seq ends) (str "missing fireball end: " fb))
      (let [s (first starts) m (first maxes)
            rm (Double/parseDouble (field m "radius"))
            ;; shrink phase line may be at the peak edge; use qa-sim samples after t_max
            tm (Double/parseDouble (field m "t"))
            sim-after-max (->> (:sims r)
                               (filter #(when-let [t (field % "t")]
                                          (>= (Double/parseDouble t) tm)))
                               (keep #(when-let [rad (field % "radius")]
                                        (Double/parseDouble rad))))
            smaller (some #(when (< % (- rm 0.5)) %) sim-after-max)]
        (assert! (= "400" (field s "center_x")) (str "start center " s))
        (assert! (= "100" (field s "center_y")) (str "start center y " s))
        (assert! (field s "radius") (str "start radius " s))
        (assert! (= "400" (field m "center_x")) (str "max center " m))
        (let [rs (Double/parseDouble (field s "radius"))
              ts (Double/parseDouble (field s "t"))]
          (assert! (>= tm ts) (str "t_max < t_start " tm " " ts))
          (assert! (> rm rs) (str "radius did not grow " rm " " rs))
          (assert! smaller
                   (str "no sim radius < max after t_max=" tm " max=" rm
                        " sims=" (take 5 sim-after-max))))))

    ;; C. destroyable hit / miss
    (let [r (launch! {:events ["aim 400 200" "key 2" "wait 3.5"]
                      :targets ["400,200"]
                      :width 800 :height 600})
          sims (:sims r)
          hit (some #(and (re-find #"destroyed=true" %)
                          (re-find #"target_x=400" %))
                    sims)]
      (assert! hit (str "expected destroyed target in sim telemetry: " sims)))

    (let [r (launch! {:events ["aim 400 200" "key 2" "wait 3.5"]
                      :targets ["50,50"]
                      :width 800 :height 600})
          sims (:sims r)
          far (filter #(re-find #"target_x=50" %) sims)]
      (assert! (seq far) (str "expected far target telemetry: " sims))
      (assert! (every? #(re-find #"destroyed=false" %) far)
               (str "far target should survive: " far)))

    (println)
    (println "PASS: defensive-missiles-fireballs automated QA (A–C)")
    (println "MANUAL PENDING: procedure D look-and-feel needs user approval")
    (System/exit 0)))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))

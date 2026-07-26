#!/usr/bin/env bb
;; Executable QA for enemy-battery-targets.

(require '[babashka.process :as p]
         '[clojure.string :as str]
         '[clojure.java.io :as io])

(defn die! [msg]
  (binding [*out* *err*] (println (str "FAIL: " msg)))
  (System/exit 1))

(defn assert! [ok? msg] (when-not ok? (die! msg)))

(defn field [line key]
  (when-let [[_ v] (re-find (re-pattern (str key "=([^\\s]+)")) line)] v))

(defn fields [line key]
  (->> (re-seq (re-pattern (str key "=([^\\s]+)")) line) (map second) vec))

(defn long-field [line key]
  (when-let [v (field line key)] (Long/parseLong v)))

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

(defn write-edn! [path data]
  (io/make-parents path)
  (spit path (pr-str data)))

(defn launch!
  [{:keys [events-path scenario-path extra timeout-ms]
    :or {timeout-ms 45000 extra ""}}]
  (let [cmd (str "bb play 800 600 --qa --no-keyfocus --qa-speed 10"
                 (when (seq extra) (str " " extra))
                 (when scenario-path (str " --qa-scenario " scenario-path))
                 (when events-path (str " --qa-events " events-path)))]
    (println "==> host:" cmd) (flush)
    (let [r (p/shell {:out :string :err :string :continue true :timeout timeout-ms}
                     "bash" "-lc" cmd)
          out (str (:out r) (:err r))]
      (print out) (flush)
      {:exit (:exit r) :out out :sims (sims out)})))

(defn -main [& _]
  (assert! (.exists (io/file "qa/procedures/enemy-battery-targets.qa.md"))
           "missing procedure")
  (assert! (.exists (io/file "features/enemy-battery-targets.feature"))
           "missing feature")
  (let [readme (slurp "README.md")]
    (assert! (re-find #"(?m)enemy_target=" readme) "README missing enemy_target"))

  (let [c (run! "arch" "bb arch-check")]
    (assert! (zero? (:exit c)) "arch failed"))

  ;; B: mixed city + battery targets after start (scripted enemies)
  (write-events! "tmp/bat-mix.txt"
                 ["wait 0.1" "start"
                  "enemy city:0"
                  "enemy city:1"
                  "enemy battery:left"
                  "enemy battery:center"
                  "wait 0.25" "quit"])
  (let [r (launch! {:events-path "tmp/bat-mix.txt"})
        multi (first (filter (fn [line]
                               (and (= "playing" (field line "screen"))
                                    (some #(str/starts-with? % "battery:")
                                          (fields line "enemy_target"))))
                             (:sims r)))
        targets (when multi (fields multi "enemy_target"))]
    (assert! multi (str "no battery-target playing sim: " (last (:sims r))))
    (assert! (some #(str/starts-with? % "city:") targets)
             (str "need city target: " targets))
    (assert! (some #(str/starts-with? % "battery:") targets)
             (str "need battery target: " targets)))

  ;; C: unintercepted battery impact (only battery-bound enemy; no concurrent wave)
  (write-edn! "tmp/bat-hit.edn"
              {:screen :playing
               :enemies [{:target [:battery :left]}]})
  (write-events! "tmp/bat-hit.txt" ["wait 2.0" "quit"])
  (let [r (launch! {:scenario-path "tmp/bat-hit.edn"
                    :events-path "tmp/bat-hit.txt"})
        hit (first (filter #(= "true" (field % "battery_left_destroyed"))
                           (:sims r)))]
    (assert! hit (str "left battery never destroyed: " (last (:sims r))))
    (assert! (= 6 (long-field hit "cities_alive"))
             (str "cities should survive battery hit: " hit)))

  ;; D: destroyed left excluded from wave schedule
  (write-events! "tmp/bat-excl.txt" ["wait 0.1" "start" "wait 0.2" "quit"])
  (let [r (launch! {:events-path "tmp/bat-excl.txt"
                    :extra "--destroy-batteries left"})
        wave (first (filter #(and (= "playing" (field % "screen"))
                                  (pos? (or (long-field % "enemy_missiles") 0)))
                            (:sims r)))
        targets (when wave (fields wave "enemy_target"))]
    (assert! wave (str "no wave enemies: " (first (:sims r))))
    (assert! (not-any? #{"battery:left"} targets)
             (str "destroyed left must not be targeted: " targets))
    (assert! (every? #(or (str/starts-with? % "city:")
                          (str/starts-with? % "battery:"))
                     targets)
             (str "unexpected targets: " targets)))

  (println "\nPASS: enemy-battery-targets automated QA (A–D)")
  (System/exit 0))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))

#!/usr/bin/env bb
;; Executable QA for wave-start-in-core (attack 1 starts in core tick).

(require '[babashka.process :as p]
         '[clojure.string :as str]
         '[clojure.java.io :as io])

(defn die! [msg]
  (binding [*out* *err*] (println (str "FAIL: " msg)))
  (System/exit 1))

(defn assert! [ok? msg] (when-not ok? (die! msg)))

(defn field [line key]
  (when-let [[_ v] (re-find (re-pattern (str key "=([^\\s]+)")) line)] v))

(defn long-field [line key]
  (when-let [v (field line key)]
    (try (Long/parseLong v) (catch Exception _ nil))))

(defn sims [out]
  (->> (str/split-lines out) (map str/trim) (filter #(str/starts-with? % "qa-sim ")) vec))

(defn write-edn! [path data]
  (io/make-parents path)
  (spit path (pr-str data)))

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
  [{:keys [scenario-path events-path scores-path timeout-ms]
    :or {timeout-ms 90000}}]
  (let [cmd (str "bb play 800 600 --qa --no-keyfocus --qa-speed 12"
                 (when scores-path (str " --scores-file " scores-path))
                 (when scenario-path (str " --qa-scenario " scenario-path))
                 (when events-path (str " --qa-events " events-path)))]
    (println "==> host:" cmd) (flush)
    (let [r (p/shell {:out :string :err :string :continue true :timeout timeout-ms}
                     "bash" "-lc" cmd)
          out (str (:out r) (:err r))]
      (print out) (flush)
      {:exit (:exit r) :out out :sims (sims out)})))

(defn first-attack [sim-lines n]
  (first (filter #(and (= "playing" (field % "screen"))
                       (= n (long-field % "wave_attack")))
                 sim-lines)))

(defn host-has-wave-start-policy?
  "True if a host source still owns ensure-wave-enemies / activate-wave-schedule."
  [path]
  (let [src (slurp path)]
    (or (re-find #"ensure-wave-enemies" src)
        (re-find #"activate-wave-schedule" src))))

(defn -main [& _]
  (assert! (.exists (io/file "qa/procedures/wave-start-in-core.qa.md"))
           "missing procedure")
  (assert! (.exists (io/file "docs/architecture/plans/pr-01-wave-start-in-core.md"))
           "missing PR plan")
  (assert! (.exists (io/file "src/missile_command/jvm/sketch.clj")) "missing jvm sketch")
  (assert! (.exists (io/file "src/missile_command/browser/main.cljs")) "missing browser main")
  (let [readme (slurp "README.md")]
    (assert! (re-find #"(?m)wave_attack=" readme) "README missing wave_attack="))

  (let [c (run! "arch" "bb arch-check")
        p (run! "property" "bb property")]
    (assert! (zero? (:exit c)) "arch failed")
    (assert! (zero? (:exit p)) "property failed"))

  ;; B: hosts must not own wave-start policy
  (doseq [host ["src/missile_command/jvm/sketch.clj"
                "src/missile_command/browser/main.cljs"]]
    (assert! (not (host-has-wave-start-policy? host))
             (str "B host still has wave-start policy: " host)))
  (let [core (slurp "src/missile_command/core.cljc")]
    (assert! (re-find #"ensure-wave-attack-started" core)
             "B core missing ensure-wave-attack-started")
    (assert! (re-find #"\(ensure-wave-attack-started\)" core)
             "B tick must call ensure-wave-attack-started"))

  ;; C: start → tick starts attack 1 (no host ensure)
  (write-events! "tmp/wsc-start.txt" ["wait 0.1" "start" "wait 0.25" "quit"])
  (let [r (launch! {:events-path "tmp/wsc-start.txt"
                    :scores-path "tmp/wsc-empty.edn"
                    :timeout-ms 45000})
        a1 (first-attack (:sims r) 1)]
    (assert! a1 (str "C never attack 1 after start: "
                     (mapv #(vector (field % "screen") (field % "wave_attack"))
                           (:sims r))))
    (assert! (= 1 (long-field a1 "wave")) (str "C wave: " a1))
    (assert! (pos? (long-field a1 "ballistic_missiles"))
             (str "C need ballistics from core attack start: " a1)))

  ;; D: after final attack clears → banner → wave 2 attack 1 via tick
  (write-edn! "tmp/wsc-banner.edn"
              {:wave 1
               :screen :playing
               :wave-attack 3
               :bonus-cities 6
               :batteries {:left {:ammo 0} :center {:ammo 0} :right {:ammo 0}}
               :enemies []})
  (write-events! "tmp/wsc-banner.txt" ["wait 25" "quit"])
  (let [r (launch! {:scenario-path "tmp/wsc-banner.edn"
                    :events-path "tmp/wsc-banner.txt"
                    :scores-path "tmp/wsc-empty.edn"
                    :timeout-ms 120000})
        all (:sims r)
        banner (first (filter #(= "wave-banner" (field % "screen")) all))
        w2 (first (filter #(and (= "playing" (field % "screen"))
                                (= 2 (long-field % "wave"))
                                (= 1 (long-field % "wave_attack")))
                          all))]
    (assert! banner (str "D never banner: " (mapv #(field % "screen") all)))
    (assert! w2 (str "D never wave 2 attack 1 after banner via tick: "
                     (mapv #(vector (field % "screen")
                                    (field % "wave")
                                    (field % "wave_attack"))
                           all)))
    (assert! (pos? (long-field w2 "ballistic_missiles"))
             (str "D wave 2 attack 1 needs ballistics: " w2)))

  (println "\nPASS: wave-start-in-core automated QA (A–D)")
  (println "PASS: look-and-feel deferred per user (skip until further notice)")
  (System/exit 0))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))

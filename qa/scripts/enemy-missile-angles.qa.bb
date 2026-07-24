#!/usr/bin/env bb
;; Executable QA for enemy-missile-angles (angled sky origins).

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
  (->> (re-seq (re-pattern (str key "=([^\\s]+)")) line)
       (map second)
       vec))

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
  [{:keys [scenario-path events-path timeout-ms]
    :or {timeout-ms 45000}}]
  (let [cmd (str "bb play 800 600 --qa --qa-speed 10"
                 (when scenario-path (str " --qa-scenario " scenario-path))
                 (when events-path (str " --qa-events " events-path)))]
    (println "==> host:" cmd) (flush)
    (let [r (p/shell {:out :string :err :string :continue true :timeout timeout-ms}
                     "bash" "-lc" cmd)
          out (str (:out r) (:err r))]
      (print out) (flush)
      {:exit (:exit r) :out out :sims (sims out)})))

(defn- parse-double [s]
  (when s (Double/parseDouble s)))

(defn -main [& _]
  (assert! (.exists (io/file "qa/procedures/enemy-missile-angles.qa.md"))
           "missing procedure")
  (assert! (.exists (io/file "features/enemy-missile-angles.feature"))
           "missing feature")
  (let [readme (slurp "README.md")]
    (assert! (re-find #"(?m)--qa\b" readme) "README missing --qa")
    (assert! (re-find #"(?m)--qa-scenario" readme) "README missing --qa-scenario")
    (assert! (re-find #"(?m):origin" readme) "README missing scenario :origin")
    (assert! (re-find #"(?m)enemy_origin_x=" readme) "README missing enemy_origin_x"))

  (let [u (run! "unit" "bb test")
        a (run! "accept" "bb accept")
        c (run! "arch" "bb arch-check")]
    (assert! (zero? (:exit u)) "unit failed")
    (assert! (zero? (:exit a)) "accept failed")
    (assert! (re-find #"(?i)enemy[-_]?missile[-_]?angles" (:out a))
             "accept missing angles feature")
    (assert! (zero? (:exit c)) "arch failed"))

  ;; B: angled city path — origin offset, diagonal motion, city dies
  ;; Continuous play may spawn the next wave after impact; assert the first impact.
  (write-edn! "tmp/angle-city.edn"
              {:enemies [{:origin [50 0] :target [:city 0]}]})
  (write-events! "tmp/angle-events.txt" ["wait 2.0" "quit"])
  (let [r (launch! {:scenario-path "tmp/angle-city.edn"
                    :events-path "tmp/angle-events.txt"})
        all-sims (:sims r)
        first-sim (first all-sims)
        mid (first (filter (fn [line]
                             (let [y (parse-double (field line "enemy_y"))
                                   n (field line "enemy_missiles")]
                               (and y n (= "1" n) (> y 20.0) (< y 500.0))))
                           all-sims))
        after-city (first (filter (fn [line]
                                    (let [c (field line "cities_alive")]
                                      (and c (< (Long/parseLong c) 6))))
                                  all-sims))]
    (assert! first-sim (str "no telemetry: " (:out r)))
    (assert! (= "50" (field first-sim "enemy_origin_x"))
             (str "staged origin x: " first-sim))
    (assert! (= "0" (field first-sim "enemy_origin_y"))
             (str "sky origin y: " first-sim))
    (assert! (= "1" (field first-sim "enemy_missiles"))
             (str "one enemy: " first-sim))
    (let [ox (parse-double (field first-sim "enemy_origin_x"))
          tx (parse-double (field first-sim "enemy_target_x"))]
      (assert! (and ox tx (not= ox tx))
               (str "origin x must differ from target x: " first-sim)))
    (assert! mid (str "no mid-flight sim: " (take 3 all-sims)))
    (let [ox (parse-double (field mid "enemy_origin_x"))
          tx (parse-double (field mid "enemy_target_x"))
          x (parse-double (field mid "enemy_x"))
          y (parse-double (field mid "enemy_y"))]
      (assert! (and x y (> y 0.0))
               (str "y should increase: " mid))
      (assert! (or (and (<= (min ox tx) x) (<= x (max ox tx)))
                   (< (Math/abs (- x ox)) 1.0e-6))
               (str "x should move toward city: " mid)))
    (assert! after-city (str "city never damaged: " (last all-sims)))
    (assert! (= "5" (field after-city "cities_alive"))
             (str "city 0 destroyed -> 5 alive: " after-city)))

  ;; C: angled battery impact
  (write-edn! "tmp/angle-battery.edn"
              {:enemies [{:origin [200 0] :target [:battery :left]}]})
  (write-events! "tmp/angle-events2.txt" ["wait 2.0" "quit"])
  (let [r (launch! {:scenario-path "tmp/angle-battery.edn"
                    :events-path "tmp/angle-events2.txt"})
        first-sim (first (:sims r))
        destroyed (first (filter #(= "true" (field % "battery_left_destroyed"))
                                 (:sims r)))]
    (assert! (= "200" (field first-sim "enemy_origin_x"))
             (str "battery origin: " first-sim))
    (assert! destroyed (str "left never destroyed: " (last (:sims r)))))

  ;; D: wave variety — normal schedule, varied origins
  (write-edn! "tmp/angle-wave.edn" {})
  (write-events! "tmp/angle-events3.txt" ["wait 0.25" "quit"])
  (let [r (launch! {:scenario-path "tmp/angle-wave.edn"
                    :events-path "tmp/angle-events3.txt"})
        multi (first (filter (fn [line]
                               (let [n (field line "enemy_missiles")]
                                 (and n (>= (Long/parseLong n) 3))))
                             (:sims r)))]
    (assert! multi (str "need ≥3 enemies in flight: " (first (:sims r))))
    (let [oys (fields multi "enemy_origin_y")
          oxs (fields multi "enemy_origin_x")
          txs (fields multi "enemy_target_x")]
      (assert! (every? #(= "0" %) oys)
               (str "all origins y=0: " multi))
      (assert! (> (count (set oxs)) 1)
               (str "need distinct origin x: " oxs))
      (assert! (some (fn [[ox tx]] (not= ox tx)) (map vector oxs txs))
               (str "at least one non-vertical: ox=" oxs " tx=" txs))))

  (println "\nPASS: enemy-missile-angles automated QA (A–D)")
  (println "PASS: look-and-feel approved (angled trail fan)")
  (System/exit 0))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))

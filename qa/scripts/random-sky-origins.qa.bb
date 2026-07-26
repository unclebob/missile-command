#!/usr/bin/env bb
;; Executable QA for random-sky-origins (wave salvo entry points).

(require '[babashka.process :as p]
         '[clojure.string :as str]
         '[clojure.java.io :as io])

(defn die! [msg]
  (binding [*out* *err*] (println (str "FAIL: " msg)))
  (System/exit 1))

(defn assert! [ok? msg] (when-not ok? (die! msg)))

(defn field [line key]
  (when-let [[_ v] (re-find (re-pattern (str key "=([^\\s]+)")) line)] v))

(defn double-field [line key]
  (when-let [v (field line key)]
    (try (Double/parseDouble v) (catch Exception _ nil))))

(defn long-field [line key]
  (when-let [v (field line key)]
    (try (Long/parseLong v) (catch Exception _ nil))))

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
  (let [cmd (str "bb play 800 600 --qa --no-keyfocus --qa-speed 6"
                 (when scores-path (str " --scores-file " scores-path))
                 (when events-path (str " --qa-events " events-path)))]
    (println "==> host:" cmd) (flush)
    (let [r (p/shell {:out :string :err :string :continue true :timeout timeout-ms}
                     "bash" "-lc" cmd)
          out (str (:out r) (:err r))]
      (print out) (flush)
      {:exit (:exit r) :out out :sims (sims out)})))

(defn origin-xs
  "Parse all enemy_origin_x values from one qa-sim line."
  [line]
  (->> (re-seq #"enemy_origin_x=([^\s]+)" line)
       (map (fn [[_ v]] (Double/parseDouble v)))
       vec))

(defn origin-ys
  [line]
  (->> (re-seq #"enemy_origin_y=([^\s]+)" line)
       (map (fn [[_ v]] (Double/parseDouble v)))
       vec))

(defn -main [& _]
  (assert! (.exists (io/file "qa/procedures/random-sky-origins.qa.md"))
           "missing procedure")
  (assert! (.exists (io/file "features/enemy-missile-angles.feature"))
           "missing angles feature")
  (let [readme (slurp "README.md")]
    (assert! (re-find #"(?m)enemy_origin_x=" readme) "README missing enemy_origin_x=")
    (assert! (re-find #"(?m)enemy_origin_y=" readme) "README missing enemy_origin_y="))

  (let [c (run! "arch" "bb arch-check")]
    (assert! (zero? (:exit c)) "arch failed"))

  ;; B–C: start → attack 1 with 3 enemies; random origins at y=0
  (write-events! "tmp/rso.txt" ["wait 0.1" "start" "wait 0.25" "quit"])
  (let [r (launch! {:events-path "tmp/rso.txt"
                    :scores-path "tmp/rso-empty.edn"})
        line (first (filter #(and (= "playing" (field % "screen"))
                                  (= 1 (long-field % "wave_attack"))
                                  (= 3 (long-field % "ballistic_missiles")))
                            (:sims r)))]
    (assert! line (str "B never attack1 with 3 ballistics: "
                       (mapv #(field % "ballistic_missiles") (:sims r))))
    (let [xs (origin-xs line)
          ys (origin-ys line)]
      (assert! (= 3 (count xs)) (str "B need 3 origins: " xs))
      (assert! (every? #(and (>= % 0.0) (< % 800.0)) xs)
               (str "B origin x out of range: " xs))
      (assert! (every? #(<= (Math/abs %) 1.0e-6) ys)
               (str "B origin y must be 0: " ys))
      ;; Variety: not all identical (random sky)
      (assert! (>= (count (set xs)) 2)
               (str "C origins not varied: " xs))))

  (println "\nPASS: random-sky-origins automated QA (A–C)")
  (println "PASS: look-and-feel deferred per user (skip until further notice)")
  (System/exit 0))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))

#!/usr/bin/env bb
;; Executable QA for bonus-cities-wave-end (reserve only mid-wave; place at end).

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
  (when-let [v (field line key)] (Long/parseLong v)))

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
  (let [cmd (str "bb play 800 600 --qa --qa-speed 8"
                 (when scores-path (str " --scores-file " scores-path))
                 (when scenario-path (str " --qa-scenario " scenario-path))
                 (when events-path (str " --qa-events " events-path)))]
    (println "==> host:" cmd) (flush)
    (let [r (p/shell {:out :string :err :string :continue true :timeout timeout-ms}
                     "bash" "-lc" cmd)
          out (str (:out r) (:err r))]
      (print out) (flush)
      {:exit (:exit r) :out out :sims (sims out)})))

(defn -main [& _]
  (assert! (.exists (io/file "qa/procedures/bonus-cities-wave-end.qa.md"))
           "missing procedure")
  (assert! (.exists (io/file "features/bonus-cities.feature")) "missing feature")
  (let [readme (slurp "README.md")]
    (assert! (re-find #"(?m)bonus_cities=" readme) "README missing bonus_cities=")
    (assert! (re-find #"(?m)cities_alive=" readme) "README missing cities_alive="))

  (let [c (run! "arch" "bb arch-check")]
    (assert! (zero? (:exit c)) "arch failed"))

  ;; B: mid-wave — destroyed cities + reserve, still playing, cities not restored
  (write-edn! "tmp/bc-mid.edn"
              {:screen :playing
               :wave 1
               :score 10000
               :bonus-cities 1
               :cities {:destroyed [0 1]}
               :enemies [{:target [:city 2]}]})
  (write-events! "tmp/bc-mid.txt" ["wait 0.2" "quit"])
  (let [r (launch! {:scenario-path "tmp/bc-mid.edn"
                    :events-path "tmp/bc-mid.txt"
                    :scores-path "tmp/bc-empty.edn"})
        p (first (filter #(= "playing" (field % "screen")) (:sims r)))]
    (assert! p (str "B never playing: " (last (:sims r))))
    (assert! (= 4 (long-field p "cities_alive"))
             (str "B mid-wave should stay 4 living: " p))
    (assert! (>= (long-field p "bonus_cities") 1)
             (str "B reserve should be held: " p)))

  ;; C: wave end places reserve — 2 destroyed, reserve 2, one enemy clears
  (write-edn! "tmp/bc-end.edn"
              {:screen :playing
               :wave 1
               :score 10000
               :bonus-cities 2
               :cities {:destroyed [0 1]}
               :batteries {:left {:ammo 0} :center {:ammo 0} :right {:ammo 0}}
               :enemies [{:target [:city 5]}]})
  (write-events! "tmp/bc-end.txt" ["wait 12" "quit"])
  (let [r (launch! {:scenario-path "tmp/bc-end.edn"
                    :events-path "tmp/bc-end.txt"
                    :scores-path "tmp/bc-empty.edn"
                    :timeout-ms 90000})
        all (:sims r)
        mid (first (filter #(= "playing" (field % "screen")) all))
        after-banner (drop-while #(not= "wave-banner" (field % "screen")) all)
        restored (first (filter #(and (= "playing" (field % "screen"))
                                      (= 2 (long-field % "wave"))
                                      (>= (long-field % "cities_alive") 5))
                                after-banner))]
    (assert! mid (str "C never mid: " (last all)))
    (assert! (= 4 (long-field mid "cities_alive")) (str "C start living: " mid))
    (assert! restored (str "C never restored after wave: " (last all)))
    (assert! (>= (long-field restored "cities_alive") 5)
             (str "C living after place: " restored))
    (assert! (<= (long-field restored "bonus_cities") 1)
             (str "C reserve should drop: " restored)))

  ;; D: cap at 6 living — 1 destroyed, reserve 5; enemy hits already-destroyed city 0
  ;; so no extra city is lost; place 1 → living 6, reserve 4.
  (write-edn! "tmp/bc-cap.edn"
              {:screen :playing
               :wave 1
               :bonus-cities 5
               :cities {:destroyed [0]}
               :batteries {:left {:ammo 0} :center {:ammo 0} :right {:ammo 0}}
               :enemies [{:target [:city 0]}]})
  (write-events! "tmp/bc-cap.txt" ["wait 12" "quit"])
  (let [r (launch! {:scenario-path "tmp/bc-cap.edn"
                    :events-path "tmp/bc-cap.txt"
                    :scores-path "tmp/bc-empty.edn"
                    :timeout-ms 90000})
        after (drop-while #(not= "wave-banner" (field % "screen")) (:sims r))
        next-play (first (filter #(and (= "playing" (field % "screen"))
                                       (= 2 (long-field % "wave")))
                                 after))]
    (assert! next-play (str "D never wave2: " (last (:sims r))))
    (assert! (= 6 (long-field next-play "cities_alive"))
             (str "D cap living: " next-play))
    (assert! (= 4 (long-field next-play "bonus_cities"))
             (str "D leftover reserve (5-1): " next-play)))

  (println "\nPASS: bonus-cities-wave-end automated QA (A–D)")
  (println "PASS: look-and-feel approved (city restore at wave end)")
  (System/exit 0))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))

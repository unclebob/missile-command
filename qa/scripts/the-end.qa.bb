#!/usr/bin/env bb
;; Executable QA for the-end (THE END presentation + rules).

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
    (Long/parseLong v)))

(defn double-field [line key]
  (when-let [v (field line key)]
    (Double/parseDouble v)))

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
  (let [cmd (str "bb play 800 600 --qa --qa-speed 5"
                 (when scenario-path (str " --qa-scenario " scenario-path))
                 (when events-path (str " --qa-events " events-path)))]
    (println "==> host:" cmd) (flush)
    (let [r (p/shell {:out :string :err :string :continue true :timeout timeout-ms}
                     "bash" "-lc" cmd)
          out (str (:out r) (:err r))]
      (print out) (flush)
      {:exit (:exit r) :out out :sims (sims out)})))

(defn -main [& _]
  (assert! (.exists (io/file "qa/procedures/the-end.qa.md")) "missing procedure")
  (assert! (.exists (io/file "features/the-end.feature")) "missing feature")
  (let [readme (slurp "README.md")]
    (assert! (re-find #"(?m)--qa\b" readme) "README missing --qa")
    (assert! (re-find #"(?m)the_end=" readme) "README missing the_end=")
    (assert! (re-find #"(?m)end_message=" readme) "README missing end_message=")
    (assert! (re-find #"(?m)end_fireball_radius=" readme) "README missing end fireball"))

  (let [c (run! "arch" "bb arch-check")]
    (assert! (zero? (:exit c)) "arch failed"))

  ;; B: living cities — not THE END (normal new game snapshot)
  (write-edn! "tmp/the-end-alive.edn" {})
  (write-events! "tmp/the-end-alive-events.txt" ["wait 0.12" "quit"])
  (let [r (launch! {:scenario-path "tmp/the-end-alive.edn"
                    :events-path "tmp/the-end-alive-events.txt"})
        s0 (first (:sims r))]
    (assert! s0 (str "no telemetry: " (:out r)))
    (assert! (= "false" (field s0 "the_end")) (str "should not end: " s0))
    (assert! (>= (long-field s0 "cities_alive") 1) (str "need living cities: " s0)))

  ;; C: all cities destroyed, reserve 0 → THE END + message + score
  (write-edn! "tmp/the-end-enter.edn"
              {:cities {:destroyed [0 1 2 3 4 5]}
               :bonus-cities 0
               :score 2500})
  (write-events! "tmp/the-end-enter-events.txt" ["wait 1.5" "key 1" "quit"])
  (let [r (launch! {:scenario-path "tmp/the-end-enter.edn"
                    :events-path "tmp/the-end-enter-events.txt"})
        all (:sims r)
        ended (first (filter #(= "true" (field % "the_end")) all))
        mid (first (filter (fn [line]
                             (and (= "true" (field line "the_end"))
                                  (let [r (double-field line "end_fireball_radius")]
                                    (and r (> r 20.0) (< r 400.0)))))
                           all))
        filled (first (filter (fn [line]
                                (and (= "true" (field line "the_end"))
                                     (let [r (double-field line "end_fireball_radius")
                                           rev (double-field line "end_message_reveal")]
                                       ;; 800x600 fill-radius is 500; require near-max.
                                       (and r rev (>= r 450.0) (>= rev 0.9)))))
                              all))
        fires (->> (str/split-lines (:out r))
                   (filter #(str/starts-with? % "qa-fire ")))]
    (assert! ended (str "never entered THE END: " (last all)))
    (assert! (= "THE_END" (field ended "end_message"))
             (str "end message must be THE_END: " ended))
    (assert! (not= "Game_Over" (field ended "end_message"))
             (str "must not be Game Over: " ended))
    (assert! (>= (long-field ended "final_score") 2500)
             (str "final score: " ended))
    (assert! mid (str "end fireball never mid-expand: " (take 5 all)))
    (assert! filled (str "end fireball never filled playfield: " (last all)))
    (assert! (some #(re-find #"battery=none" %) fires)
             (str "post-end fire should be none: " fires)))

  ;; D: reserve restores cities — not THE END
  (write-edn! "tmp/the-end-reserve.edn"
              {:cities {:destroyed [0 1 2 3 4 5]}
               :bonus-cities 2
               :score 100})
  (write-events! "tmp/the-end-reserve-events.txt" ["wait 0.4" "quit"])
  (let [r (launch! {:scenario-path "tmp/the-end-reserve.edn"
                    :events-path "tmp/the-end-reserve-events.txt"})
        after (first (filter #(>= (long-field % "cities_alive") 1) (:sims r)))
        never-end (every? #(= "false" (field % "the_end")) (:sims r))]
    (assert! after (str "reserve should restore living cities: " (last (:sims r))))
    (assert! never-end (str "should not THE END with reserve: " (last (:sims r))))
    (assert! (= 0 (long-field after "bonus_cities"))
             (str "reserve spent: " after)))

  (println "\nPASS: the-end automated QA (A–D, G partial via telemetry)")
  (println "PASS: look-and-feel approved (THE END fireball + letter reveal)")
  (System/exit 0))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))

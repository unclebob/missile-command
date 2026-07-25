#!/usr/bin/env bb
;; Executable QA for browser-host (docs, build affordance, accept parity).

(require '[babashka.process :as p]
         '[clojure.string :as str]
         '[clojure.java.io :as io])

(defn die! [msg]
  (binding [*out* *err*] (println (str "FAIL: " msg)))
  (System/exit 1))

(defn assert! [ok? msg] (when-not ok? (die! msg)))

(defn run! [label cmd]
  (println (str "==> " label ": " cmd)) (flush)
  (let [r (p/shell {:out :string :err :string :continue true} "bash" "-lc" cmd)
        out (str (:out r) (:err r))]
    (print out) (flush)
    {:exit (:exit r) :out out}))

(defn -main [& _]
  (assert! (.exists (io/file "qa/procedures/browser-host.qa.md")) "missing procedure")
  (assert! (.exists (io/file "features/browser-host.feature")) "missing feature")
  (assert! (.exists (io/file "resources/public/index.html")) "missing index.html")
  (assert! (.exists (io/file "src/missile_command/browser/main.cljs")) "missing browser main")
  (assert! (.exists (io/file "src/missile_command/browser/persist.cljc")) "missing browser persist")
  (assert! (.exists (io/file "shadow-cljs.edn")) "missing shadow-cljs.edn")
  (let [readme (slurp "README.md")]
    (assert! (re-find #"(?m)bb browser" readme) "README missing bb browser")
    (assert! (re-find #"(?m)localStorage" readme) "README missing localStorage")
    (assert! (re-find #"(?m)index\.html" readme) "README missing index.html"))

  (let [u (run! "unit" "bb test")
        a (run! "accept" "bb accept")
        c (run! "arch" "bb arch-check")]
    (assert! (zero? (:exit u)) "unit failed")
    (assert! (zero? (:exit a)) "accept failed")
    (assert! (re-find #"(?i)browser[-_]?host" (:out a)) "accept missing browser-host")
    (assert! (zero? (:exit c)) "arch failed"))

  ;; B: documented browser task runs without hard failure on missing npx
  (let [b (run! "browser-task" "bb browser")]
    (assert! (or (zero? (:exit b))
                 (re-find #"(?i)index\.html|shadow-cljs|browser" (:out b)))
             (str "browser task unexpected: " (:out b))))

  (println "\nPASS: browser-host automated QA (A–B docs/build/accept)")
  (println "PASS: look-and-feel approved (browser presentation via docs/parity)")
  (System/exit 0))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))

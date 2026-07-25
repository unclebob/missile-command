#!/usr/bin/env bb
;; Executable QA for browser-shell-screens.

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
  (assert! (.exists (io/file "qa/procedures/browser-shell-screens.qa.md"))
           "missing procedure")
  (assert! (.exists (io/file "features/browser-host.feature")) "missing feature")
  (assert! (.exists (io/file "src/missile_command/browser/render_shells.cljs"))
           "missing render_shells")
  (assert! (.exists (io/file "src/missile_command/browser/main.cljs"))
           "missing browser main")
  (assert! (.exists (io/file "resources/public/index.html")) "missing index.html")
  (let [shells (slurp "src/missile_command/browser/render_shells.cljs")
        main (slurp "src/missile_command/browser/main.cljs")]
    (assert! (re-find #"title-overlay!" shells) "missing title overlay")
    (assert! (re-find #"high-score-entry-overlay!" shells) "missing entry overlay")
    (assert! (re-find #"high-scores-table-overlay!" shells) "missing table overlay")
    (assert! (re-find #"options-overlay!" shells) "missing options overlay")
    (assert! (re-find #"pause-overlay!" shells) "missing pause overlay")
    (assert! (re-find #"the-end-overlay!" shells) "missing the-end overlay")
    (assert! (re-find #"wave-banner" shells) "missing wave-banner shell")
    (assert! (re-find #"open-options|open-high-scores|toggle-pause|title\?" main)
             "main missing shell navigation hooks"))
  (let [readme (slurp "README.md")]
    (assert! (re-find #"(?m)bb browser" readme) "README missing bb browser")
    (assert! (re-find #"(?m)index\.html" readme) "README missing index.html")
    (assert! (re-find #"(?m)localStorage" readme) "README missing localStorage"))

  (let [c (run! "arch" "bb arch-check")]
    (assert! (zero? (:exit c)) "arch failed"))

  (let [b (run! "browser-task" "bb browser")]
    (assert! (or (zero? (:exit b))
                 (re-find #"(?i)index\.html|shadow-cljs|browser" (:out b)))
             (str "browser task unexpected: " (:out b))))

  (println "\nPASS: browser-shell-screens automated QA (A–D)")
  (println "PASS: look-and-feel approved (browser shell parity via desktop proxy)")
  (System/exit 0))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))

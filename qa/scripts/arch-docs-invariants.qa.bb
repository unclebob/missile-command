#!/usr/bin/env bb
;; Executable QA for arch-docs-invariants.

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
  (assert! (.exists (io/file "qa/procedures/arch-docs-invariants.qa.md"))
           "missing procedure")
  (assert! (.exists (io/file "docs/architecture/plans/pr-09-arch-docs-invariants.md"))
           "missing plan")
  (assert! (.exists (io/file "docs/architecture/ADR-001-modular-core-and-extraction-plan.md"))
           "missing ADR")
  (assert! (.exists (io/file "docs/architecture/plans/README.md")) "missing plan index")

  (let [u (run! "unit" "bb test")
        a (run! "accept" "bb accept")
        c (run! "arch" "bb arch-check")
        p (run! "property" "bb property")]
    (assert! (zero? (:exit u)) "unit failed")
    (assert! (zero? (:exit a)) "accept failed")
    (assert! (zero? (:exit c)) "arch failed")
    (assert! (zero? (:exit p)) "property failed"))

  (let [idx (slurp "docs/architecture/plans/README.md")
        adr (slurp "docs/architecture/ADR-001-modular-core-and-extraction-plan.md")]
    (doseq [task ["wave-start-in-core" "sfx-event-contract" "extract-bonus-cities"
                  "extract-combat" "extract-shell" "core-testing-api"
                  "host-input-split" "seedable-sky-rng" "arch-docs-invariants"]]
      (assert! (re-find (re-pattern task) idx)
               (str "B plan index missing task " task)))
    (assert! (re-find #"ADR-001|modular core" adr) "B ADR content missing")
    (assert! (.exists (io/file "test-property/missile_command/rng_property_test.clj"))
             "B missing rng property tests")
    (assert! (.exists (io/file "test-property/missile_command/bonus_cities_property_test.clj"))
             "B missing bonus-cities property tests"))

  (println "\nPASS: arch-docs-invariants automated QA (A–B)")
  (System/exit 0))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))

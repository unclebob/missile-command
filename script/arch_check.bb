#!/usr/bin/env bb
;; Lightweight architecture boundary check for Missile Command.
;; Pure core (.cljc game rules) must not depend on hosts, acceptance, or IO libs.

(require '[babashka.fs :as fs]
         '[clojure.string :as str])

(def src-root "src")

(def forbidden-require-prefixes
  ["quil."
   "missile-command.acceptance."
   "missile-command.jvm."
   "missile-command.browser."
   "clojure.java.io"
   "clojure.data.json"
   "babashka."
   "java.io"
   "java.nio"])

;; Acceptance adapters must depend on the core public API, not layout internals.
(def acceptance-forbidden-require-prefixes
  ["missile-command.world"
   "quil."
   "missile-command.jvm."
   "missile-command.browser."])

;; The JVM input facade is testable host mapping. Keep it independent from
;; rendering, window placement, sound, and persistence adapters.
(def jvm-input-forbidden-require-prefixes
  ["quil."
   "missile-command.jvm.audio"
   "missile-command.jvm.persist"
   "missile-command.jvm.render"
   "missile-command.jvm.sketch"
   "missile-command.jvm.window"
   "java.awt"
   "javax.swing"])

(def jvm-production-host-forbidden-require-prefixes
  ["missile-command.testing"])

(defn- balanced-form
  [content start]
  (loop [i (inc start) depth 1]
    (if (>= i (count content))
      nil
      (let [c (nth content i)]
        (cond
          (= c \() (recur (inc i) (inc depth))
          (and (= c \)) (= depth 1)) (subs content start (inc i))
          (= c \)) (recur (inc i) (dec depth))
          :else (recur (inc i) depth))))))

(defn- require-block
  [content]
  (when-let [start (str/index-of content "(:require")]
    (balanced-form content start)))

(defn- read-ns-and-requires
  [path]
  (let [content (slurp path)
        ns-m (re-find #"\(ns\s+([A-Za-z][A-Za-z0-9_.-]*)" content)
        req-block (require-block content)]
    {:path path
     :ns (some-> ns-m second)
     :requires (when req-block
                 (->> (re-seq #"(?:\[|\s)([A-Za-z][A-Za-z0-9_.-]*(?:\.[A-Za-z0-9_.-]+)+)"
                              req-block)
                      (map second)
                      distinct
                      vec))}))

(defn- pure-core-files
  []
  (->> (fs/glob src-root "**/*.cljc")
       (map str)
       (filter #(str/includes? % "/missile_command/"))
       (remove #(or (str/includes? % "/acceptance/")
                    (str/includes? % "/jvm/")
                    (str/includes? % "/browser/")))
       sort))

(defn- acceptance-files
  []
  (->> (fs/glob src-root "**/*.clj")
       (map str)
       (filter #(str/includes? % "/missile_command/acceptance/"))
       sort))

(defn- jvm-input-files
  []
  (->> ["src/missile_command/jvm/input.clj"
        "src/missile_command/jvm/cli.clj"
        "src/missile_command/jvm/telemetry.clj"]
       (filter fs/exists?)
       sort))

(defn- jvm-production-host-files
  []
  (->> (fs/glob src-root "missile_command/jvm/*.clj")
       (map str)
       (remove #(or (str/ends-with? % "/scenario.clj")
                    (str/ends-with? % "/input.clj")
                    (str/ends-with? % "/cli.clj")
                    (str/ends-with? % "/telemetry.clj")))
       sort))

(defn- forbidden-require?
  [req prefixes]
  (some #(or (= req %) (str/starts-with? req (str % ".")) (str/starts-with? req %))
        prefixes))

(defn- violations-for
  [{:keys [path ns requires]} prefixes]
  (for [req (or requires [])
        :when (forbidden-require? req prefixes)]
    {:path path :ns ns :require req}))

(defn- report-section!
  [label violations]
  (when (seq violations)
    (println (str "Architecture check FAILED: " label))
    (doseq [{:keys [path ns require]} violations]
      (println (str "  " path " (" ns ") requires " require)))))

(let [core-files (pure-core-files)
      acceptance (acceptance-files)
      jvm-input (jvm-input-files)
      jvm-production-host (jvm-production-host-files)]
  (when (empty? core-files)
    (println "Architecture check FAILED: no pure core .cljc files under src/missile_command")
    (System/exit 1))
  (let [core-violations (->> core-files
                             (map read-ns-and-requires)
                             (mapcat #(violations-for % forbidden-require-prefixes)))
        acceptance-violations (->> acceptance
                                   (map read-ns-and-requires)
                                   (mapcat #(violations-for % acceptance-forbidden-require-prefixes)))
        jvm-input-violations (->> jvm-input
                                  (map read-ns-and-requires)
                                  (mapcat #(violations-for % jvm-input-forbidden-require-prefixes)))
        jvm-production-host-violations
        (->> jvm-production-host
             (map read-ns-and-requires)
             (mapcat #(violations-for % jvm-production-host-forbidden-require-prefixes)))]
    (if (or (seq core-violations)
            (seq acceptance-violations)
            (seq jvm-input-violations)
            (seq jvm-production-host-violations))
      (do
        (report-section! "pure core depends on forbidden namespaces" core-violations)
        (report-section! "acceptance depends on non-core internals/hosts"
                         acceptance-violations)
        (report-section! "JVM input facade depends on low-level host adapters"
                         jvm-input-violations)
        (report-section! "JVM production hosts depend on testing helpers"
                         jvm-production-host-violations)
        (System/exit 1))
      (do
        (println "Architecture check OK")
        (println (str "  pure core files: " (count core-files)))
        (println (str "  acceptance files: " (count acceptance)))
        (println (str "  JVM input facade files: " (count jvm-input)))
        (println (str "  JVM production host files: " (count jvm-production-host)))
        (System/exit 0)))))

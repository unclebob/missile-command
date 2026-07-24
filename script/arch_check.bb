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
       (remove #(str/includes? % "/acceptance/"))
       sort))

(defn- forbidden-require?
  [req]
  (some #(str/starts-with? req %) forbidden-require-prefixes))

(defn- violations-for
  [{:keys [path ns requires]}]
  (for [req (or requires [])
        :when (forbidden-require? req)]
    {:path path :ns ns :require req}))

(defn- report!
  [violations]
  (if (seq violations)
    (do
      (println "Architecture check FAILED: pure core depends on forbidden namespaces")
      (doseq [{:keys [path ns require]} violations]
        (println (str "  " path " (" ns ") requires " require)))
      (System/exit 1))
    (do
      (println "Architecture check OK")
      (println (str "  pure core files: " (count (pure-core-files))))
      (System/exit 0))))

(let [files (pure-core-files)]
  (when (empty? files)
    (println "Architecture check FAILED: no pure core .cljc files under src/missile_command")
    (System/exit 1))
  (->> files
       (map read-ns-and-requires)
       (mapcat violations-for)
       report!))

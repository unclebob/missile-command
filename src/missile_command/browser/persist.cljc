(ns missile-command.browser.persist
  "Browser localStorage settings: pure encode/decode + host load/save."
  (:require [missile-command.core :as core]
            #?(:cljs [cljs.reader :as reader])))

(def storage-key "missile-command-settings")

#?(:clj (defonce ^:private settings-atom (atom nil)))

(defn encode
  [settings]
  (pr-str settings))

(defn decode
  [raw]
  (when (and raw (pos? (count (str raw))))
    #?(:clj (read-string raw)
       :cljs (reader/read-string raw))))

(defn save-settings!
  "Persist exported settings. JVM tests use an in-memory atom; CLJS uses localStorage."
  [state]
  (let [blob (encode (core/export-settings state))]
    #?(:clj (do (reset! settings-atom blob) blob)
       :cljs (do
               (when (exists? js/localStorage)
                 (.setItem js/localStorage storage-key blob))
               blob))))

(defn load-settings
  []
  #?(:clj (decode @settings-atom)
     :cljs (when (exists? js/localStorage)
             (decode (.getItem js/localStorage storage-key)))))

(defn load-into
  [state]
  (if-let [settings (load-settings)]
    (core/import-settings state settings)
    state))

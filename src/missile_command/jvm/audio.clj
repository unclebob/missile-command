(ns missile-command.jvm.audio
  "Play short SFX clips for core :sfx/* events (javax.sound)."
  (:require [clojure.java.io :as io])
  (:import [javax.sound.sampled AudioSystem Clip DataLine$Info]
           [java.io ByteArrayInputStream ByteArrayOutputStream File]))

(def ^:private type->file
  {:sfx/launch "launch.wav"
   :sfx/explosion "explosion.wav"
   :sfx/city-destroyed "city-destroyed.wav"
   :sfx/battery-destroyed "battery-destroyed.wav"
   :sfx/low-ammo "low-ammo.wav"
   :sfx/wave-clear "wave-clear.wav"
   :sfx/bonus-city "bonus-city.wav"
   :sfx/the-end "the-end.wav"
   :sfx/ui "ui.wav"})

(defonce ^:private bytes-cache (atom {}))

(defn- load-bytes
  [filename]
  (or (get @bytes-cache filename)
      (let [from-cp (io/resource (str "sounds/" filename))
            from-fs (let [f (io/file "resources/sounds" filename)]
                      (when (.isFile f) f))
            src (or from-cp from-fs)]
        (when src
          (with-open [in (io/input-stream src)
                      out (ByteArrayOutputStream.)]
            (io/copy in out)
            (let [b (.toByteArray out)]
              (swap! bytes-cache assoc filename b)
              b))))))

(defn- normalize-type
  [type]
  (let [kw (if (keyword? type) type (keyword (str type)))]
    (if (namespace kw) kw (keyword "sfx" (name kw)))))

(defn- open-clip
  [filename]
  (when-let [bytes (load-bytes filename)]
    (with-open [ais (AudioSystem/getAudioInputStream
                     (ByteArrayInputStream. bytes))]
      (let [format (.getFormat ais)
            info (DataLine$Info. Clip format)
            ^Clip clip (AudioSystem/getLine info)]
        (with-open [ais2 (AudioSystem/getAudioInputStream
                          (ByteArrayInputStream. bytes))]
          (.open clip ais2)
          clip)))))

(defn play!
  "Play SFX for event type unless muted. Safe no-op if clip missing."
  [type muted?]
  (when-not muted?
    (try
      (let [kw (normalize-type type)
            filename (get type->file kw)]
        (when filename
          (when-let [^Clip clip (open-clip filename)]
            ;; Fresh clip per play so overlapping SFX work.
            (doto clip
              (.setFramePosition 0)
              (.start)))))
      (catch Exception _
        nil))))

(defn play-events!
  "Play each event in events (maps with :type)."
  [events muted?]
  (doseq [e events]
    (play! (:type e) muted?)))

(defn warm!
  "Preload WAV bytes for all known clips."
  []
  (doseq [f (vals type->file)]
    (try (load-bytes f) (catch Exception _ nil)))
  (count @bytes-cache))

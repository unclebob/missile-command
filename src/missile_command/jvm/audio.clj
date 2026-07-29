(ns missile-command.jvm.audio
  "Play short SFX clips for core :sfx/* events (javax.sound)."
  (:require [clojure.java.io :as io])
  (:import [javax.sound.sampled AudioSystem Clip DataLine$Info
            LineEvent LineEvent$Type LineListener]
           [java.io ByteArrayInputStream ByteArrayOutputStream File]))

(def ^:private type->file
  {:sfx/launch "launch.wav"
   :sfx/boom "boom.wav"
   :sfx/explosion "explosion.wav"
   :sfx/intercepted "intercepted.wav"
   :sfx/city-destroyed "city.wav"
   :sfx/battery-destroyed "city.wav"
   :sfx/low-ammo "low-ammo.wav"
   :sfx/wave "wave.wav"
   :sfx/wave-clear "wave-clear.wav"
   :sfx/bonus-city "bonus-city.wav"
   :sfx/the-end "the-end.wav"
   :sfx/warning "warning.wav"
   :sfx/ui "ui.wav"})

(defonce ^:private bytes-cache (atom {}))
(defonce ^:private active-clips (atom #{}))
(defonce ^:private active-title-clips (atom #{}))

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

(defn- close-clip!
  [^Clip clip]
  (when (contains? @active-clips clip)
    (swap! active-clips disj clip)
    (swap! active-title-clips disj clip)
    (try
      (when (.isRunning clip) (.stop clip))
      (.close clip)
      (catch Exception _ nil))))

(defn- track-clip!
  "Remember open clips so window close can release audio lines."
  [^Clip clip title?]
  (swap! active-clips conj clip)
  (when title?
    (swap! active-title-clips conj clip))
  (.addLineListener
   clip
   (reify LineListener
     (update [_ event]
       (when (or (= LineEvent$Type/STOP (.getType event))
                 (= LineEvent$Type/CLOSE (.getType event)))
         (close-clip! clip))))))

(defn stop-title!
  "Stop any in-flight title warning sound."
  []
  (doseq [^Clip c @active-title-clips]
    (close-clip! c))
  (reset! active-title-clips #{}))

(defn stop-all!
  "Stop and close every open audio clip."
  []
  (doseq [^Clip c @active-clips]
    (close-clip! c))
  (reset! active-clips #{})
  (reset! active-title-clips #{}))

(defn play!
  "Play SFX for event type unless muted. Safe no-op if clip missing."
  [type muted?]
  (when-not muted?
    (try
      (let [kw (normalize-type type)
            filename (get type->file kw)]
        (when filename
          (when-let [^Clip clip (open-clip filename)]
            (track-clip! clip (= kw :sfx/warning))
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

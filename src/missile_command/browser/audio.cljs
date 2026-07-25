(ns missile-command.browser.audio
  "Play short WAV SFX from /sounds for core :sfx/* events."
  (:require [clojure.string :as str]))

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

(defonce ^:private cache (atom {}))

(defn- normalize-type
  [type]
  (let [kw (if (keyword? type) type (keyword type))]
    (if (namespace kw) kw (keyword "sfx" (name kw)))))

(defn- url-for
  [filename]
  (str "sounds/" filename))

(defn- audio-el
  [filename]
  (or (get @cache filename)
      (let [a (js/Audio. (url-for filename))]
        (set! (.-preload a) "auto")
        (swap! cache assoc filename a)
        a)))

(defn play!
  "Play SFX unless muted. Uses a clone so overlapping plays work."
  [type muted?]
  (when-not muted?
    (try
      (let [kw (normalize-type type)
            filename (get type->file kw)]
        (when filename
          (let [base (audio-el filename)
                node (.cloneNode base)]
            (set! (.-volume node) 0.55)
            (.play node))))
      (catch :default _ nil))))

(defn play-events!
  [events muted?]
  (doseq [e events]
    (play! (:type e) muted?)))

(defn warm!
  "Preload clips (call after user gesture if browser blocks autoplay)."
  []
  (doseq [f (vals type->file)]
    (try
      (let [a (audio-el f)]
        (.load a))
      (catch :default _ nil)))
  (count @cache))

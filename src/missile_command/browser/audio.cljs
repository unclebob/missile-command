(ns missile-command.browser.audio
  "Play short WAV SFX from /sounds for core :sfx/* events."
  (:require [clojure.string :as str]))

;; Bump when WAV assets change so browsers skip stale caches.
(def ^:private asset-version "20260725c")

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

(defonce ^:private preload-cache (atom {}))
(defonce ^:private active-title-nodes (atom #{}))
;; Browsers block autoplay until a user gesture; host sets this after unlock.
(defonce unlocked? (atom false))

(defn- normalize-type
  [type]
  (let [kw (if (keyword? type) type (keyword type))]
    (if (namespace kw) kw (keyword "sfx" (name kw)))))

(defn- url-for
  [filename]
  (str "sounds/" filename "?v=" asset-version))

(defn- make-audio
  "Fresh Audio element pointed at a versioned asset URL."
  [filename]
  (let [a (js/Audio. (url-for filename))]
    (set! (.-preload a) "auto")
    (set! (.-volume a) 0.55)
    a))

(defn warm!
  "Preload all mapped clips (best after a user gesture)."
  []
  (doseq [f (vals type->file)]
    (try
      (when-not (get @preload-cache f)
        (let [a (make-audio f)]
          (.load a)
          (swap! preload-cache assoc f a)))
      (catch :default _ nil)))
  (count @preload-cache))

(defn title-playing?
  []
  (boolean (seq @active-title-nodes)))

(defn stop-title!
  "Stop any in-flight title warning sound."
  []
  (doseq [n @active-title-nodes]
    (try
      (set! (.-onended n) nil)
      (set! (.-currentTime n) 0)
      (.pause n)
      (catch :default _ nil)))
  (reset! active-title-nodes #{}))

(defn- play-node!
  "Start playback. For promise rejections, drop title tracking."
  [node title?]
  (try
    (let [p (.play node)]
      (when (and p (.-catch p))
        (.catch p (fn [_]
                    (when title?
                      (swap! active-title-nodes disj node)))))
      true)
    (catch :default _
      (when title?
        (swap! active-title-nodes disj node))
      false)))

(defn play!
  "Play SFX unless muted. Fresh Audio per play so overlaps work."
  [type muted?]
  (when-not muted?
    (try
      (let [kw (normalize-type type)
            filename (get type->file kw)]
        (when filename
          (let [node (make-audio filename)
                title? (= kw :sfx/warning)]
            (when title?
              (swap! active-title-nodes conj node)
              (set! (.-onended node)
                    (fn [_]
                      (swap! active-title-nodes disj node))))
            (play-node! node title?))))
      (catch :default _ nil))))

(defn ensure-title!
  "Start title warning if unlocked, unmuted, and not already playing.
  Call after a user gesture when autoplay may have failed earlier."
  [muted?]
  (when (and @unlocked? (not muted?) (not (title-playing?)))
    (play! :sfx/warning false)))

(defn unlock!
  "Mark audio as allowed after a user gesture (required by browsers)."
  []
  (reset! unlocked? true)
  (warm!)
  true)

(defn play-events!
  [events muted?]
  (doseq [e events]
    (let [t (normalize-type (:type e))]
      ;; Warning needs unlock; other SFX still try (may no-op without gesture).
      (when (or @unlocked? (not= t :sfx/warning))
        (play! t muted?)))))

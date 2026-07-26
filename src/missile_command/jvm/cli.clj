(ns missile-command.jvm.cli
  "Launch CLI parsing for the JVM host: size, QA switches, scores file. No Quil."
  (:require [clojure.string :as str]))

(defn parse-destroy-list
  "Parse comma-separated battery names into keyword ids."
  [s]
  (->> (str/split (str s) #",")
       (map str/trim)
       (remove str/blank?)
       (map keyword)
       vec))

(defn parse-xy-pair
  "Parse \"x,y\" into {:x :y}."
  [s]
  (let [[xs ys] (str/split (str s) #"," 2)]
    {:x (Integer/parseInt (str/trim xs))
     :y (Integer/parseInt (str/trim ys))}))

(defn parse-enemy-spec
  "Parse city:N or battery:left|center|right."
  [s]
  (let [[kind id] (str/split (str s) #":" 2)
        kind (str/trim kind)
        id (str/trim (str id))]
    (case kind
      "city" {:kind :city :id (Integer/parseInt id)}
      "battery" {:kind :battery :id (keyword id)}
      (throw (ex-info (str "unknown enemy spec: " s) {:spec s})))))

(defn parse-fireball-spec
  "Parse x,y,radius for a static QA fireball."
  [s]
  (let [[xs ys rs] (str/split (str s) #"," 3)]
    {:x (Integer/parseInt (str/trim xs))
     :y (Integer/parseInt (str/trim ys))
     :radius (Double/parseDouble (str/trim rs))}))

(defn int-token?
  [s]
  (boolean (re-matches #"-?\d+" (str s))))

(defn parse-int-token
  [s]
  (Integer/parseInt (str s)))

(defn parse-float-token
  [s]
  (Double/parseDouble (str s)))

(defn parse-qa-speed
  "Positive sim-time multiplier (default 1.0). Speeds wall-clock QA waits."
  [s]
  (let [n (parse-float-token s)]
    (when-not (and (double? n) (pos? n) (Double/isFinite n))
      (throw (ex-info (str "invalid --qa-speed (need positive number): " s)
                      {:arg s})))
    n))

(def switch-handlers
  {"--" (fn [opts xs] [opts (rest xs)])
   "--qa" (fn [opts xs] [(assoc opts :qa-telemetry? true) (rest xs)])
   "--qa-telemetry" (fn [opts xs] [(assoc opts :qa-telemetry? true) (rest xs)])
   "--no-keyfocus" (fn [opts xs] [(assoc opts :no-keyfocus? true) (rest xs)])
   "--destroy-batteries"
   (fn [opts xs]
     [(assoc opts :destroy-batteries (parse-destroy-list (second xs)))
      (drop 2 xs)])
   "--qa-events"
   (fn [opts xs]
     [(assoc opts :qa-events (second xs)) (drop 2 xs)])
   "--qa-scenario"
   (fn [opts xs]
     [(assoc opts :qa-scenario (second xs)) (drop 2 xs)])
   "--qa-speed"
   (fn [opts xs]
     [(assoc opts :qa-speed (parse-qa-speed (second xs))) (drop 2 xs)])
   "--qa-target"
   (fn [opts xs]
     [(update opts :qa-targets (fnil conj []) (parse-xy-pair (second xs)))
      (drop 2 xs)])
   "--qa-enemy"
   (fn [opts xs]
     [(update opts :qa-enemies (fnil conj []) (parse-enemy-spec (second xs)))
      (drop 2 xs)])
   "--qa-fireball"
   (fn [opts xs]
     [(update opts :qa-fireballs (fnil conj []) (parse-fireball-spec (second xs)))
      (drop 2 xs)])
   "--scores-file"
   (fn [opts xs]
     [(assoc opts :scores-file (second xs)) (drop 2 xs)])})

(defn apply-switch
  "Consume one CLI switch from xs. Returns [opts remaining-xs] or nil."
  [opts xs]
  (when-let [handler (get switch-handlers (first xs))]
    (handler opts xs)))

(defn apply-size-token
  "Consume a numeric width/height token. Returns [opts remaining-xs] or nil."
  [opts xs]
  (when (int-token? (first xs))
    (let [n (parse-int-token (first xs))]
      (case (:size-phase opts)
        :need-width [(assoc opts :width n :size-phase :need-height) (rest xs)]
        :need-height [(assoc opts :height n :size-phase :done) (rest xs)]
        nil))))

(defn parse-cli-args
  "Parse launch args: optional width height, then switches."
  ([args]
   (parse-cli-args args 800 600))
  ([args default-width default-height]
   (loop [xs (seq args)
          opts {:width default-width
                :height default-height
                :size-phase :need-width
                :qa-speed 1.0
                :destroy-batteries []
                :qa-events nil
                :qa-scenario nil
                :qa-targets []
                :qa-enemies []
                :qa-fireballs []
                :no-keyfocus? false
                :scores-file nil}]
     (if-not xs
       (-> opts
           (dissoc :size-phase)
           (update :qa-telemetry? boolean))
       (if-let [[opts' xs'] (or (apply-switch opts xs)
                                (apply-size-token opts xs))]
         (recur (seq xs') opts')
         (throw (ex-info (str "unknown launch argument: " (first xs))
                         {:arg (first xs)})))))))

(defn parse-window-size
  [args default-width default-height]
  (let [opts (parse-cli-args args default-width default-height)]
    [(:width opts) (:height opts)]))

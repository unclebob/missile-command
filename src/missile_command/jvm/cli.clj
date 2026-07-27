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
  "Positive sim-time multiplier (default 1.0). Does not change wall-clock QA waits."
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
     [(assoc opts :scores-file (second xs)) (drop 2 xs)])
   "--leaderboard-url"
   (fn [opts xs]
     [(assoc opts :leaderboard-url (second xs)) (drop 2 xs)])
   "--leaderboard-name"
   (fn [opts xs]
     [(assoc opts :leaderboard-name (second xs)) (drop 2 xs)])
   "--player-name"
   (fn [opts xs]
     [(assoc opts :player-name (second xs)) (drop 2 xs)])
   "--no-global-scores"
   (fn [opts xs]
     [(assoc opts :no-global-scores? true) (rest xs)])})

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
                :scores-file nil
                :leaderboard-url nil
                :leaderboard-name nil
                :player-name nil
                :no-global-scores? false}]
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

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-07-26T10:35:38.817452-05:00", :module-hash "1392262090", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 3, :hash "-2047725135"} {:id "defn/parse-destroy-list", :kind "defn", :line 5, :end-line 12, :hash "700474553"} {:id "defn/parse-xy-pair", :kind "defn", :line 14, :end-line 19, :hash "-1833854870"} {:id "defn/parse-enemy-spec", :kind "defn", :line 21, :end-line 30, :hash "1564602249"} {:id "defn/parse-fireball-spec", :kind "defn", :line 32, :end-line 38, :hash "188453021"} {:id "defn/int-token?", :kind "defn", :line 40, :end-line 42, :hash "1363479279"} {:id "defn/parse-int-token", :kind "defn", :line 44, :end-line 46, :hash "419440019"} {:id "defn/parse-float-token", :kind "defn", :line 48, :end-line 50, :hash "2108051018"} {:id "defn/parse-qa-speed", :kind "defn", :line 52, :end-line 59, :hash "-1072047162"} {:id "def/switch-handlers", :kind "def", :line 61, :end-line 93, :hash "-434742608"} {:id "defn/apply-switch", :kind "defn", :line 95, :end-line 99, :hash "502688327"} {:id "defn/apply-size-token", :kind "defn", :line 101, :end-line 109, :hash "-1318813968"} {:id "defn/parse-cli-args", :kind "defn", :line 111, :end-line 137, :hash "-862334904"} {:id "defn/parse-window-size", :kind "defn", :line 139, :end-line 142, :hash "1406683016"}]}
;; clj-mutate-manifest-end

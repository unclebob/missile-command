(ns missile-command.host-input
  "Pure host key policy shared by JVM and browser hosts.

  Hosts normalize platform events into `{:ch ... :key-name ... :escape? ...
  :enter? ... :backspace? ...}`. This namespace returns intent maps:
  `{:command {...}}`, `{:draft next-draft}`, or nil when ignored."
  (:require [clojure.string :as str]
            [missile-command.options :as options]
            [missile-command.screens :as screens]))

(def max-initials-length 3)

(defn- key-name
  [key-event]
  (when-let [k (:key-name key-event)]
    (options/normalize-key k)))

(defn- key-char
  [key-event]
  (:ch key-event))

(defn- char-matches?
  [key-event ch]
  (let [c (key-char key-event)]
    (or (= ch c)
        (= (str/lower-case (str ch))
           (key-name key-event)))))

(defn- command
  [cmd]
  {:command cmd})

(defn- draft
  [text]
  {:draft text})

(defn- toggle-pause-command
  [state]
  (cond
    (screens/playing? state) (command {:type :pause})
    (screens/paused? state) (command {:type :resume})
    :else nil))

(defn- escape-intent
  [state]
  (cond
    (or (screens/playing? state) (screens/paused? state))
    (toggle-pause-command state)
    (screens/high-scores-view? state)
    (command {:type :close-high-scores})
    (screens/options? state)
    (command {:type :leave-options})
    :else nil))

(defn- options-shortcut
  [state key-event]
  (when (screens/options? state)
    (cond
      (char-matches? key-event \m)
      (command {:type :set-mute
                :mute (not (options/mute-state? state))})
      (char-matches? key-event \1)
      (command {:type :set-difficulty :difficulty "easy"})
      (char-matches? key-event \2)
      (command {:type :set-difficulty :difficulty "normal"})
      (char-matches? key-event \3)
      (command {:type :set-difficulty :difficulty "arcade"})
      :else nil)))

(defn- options-toggle
  [state key-event]
  (when (char-matches? key-event \o)
    (cond
      (screens/title? state) (command {:type :open-options})
      (screens/options? state) (command {:type :leave-options})
      :else nil)))

(defn- high-scores-toggle
  [state key-event]
  (when (char-matches? key-event \h)
    (cond
      (screens/title? state) (command {:type :open-high-scores})
      (screens/high-scores-view? state) (command {:type :close-high-scores})
      :else nil)))

(defn- pause-toggle
  [state key-event]
  (let [k (key-name key-event)]
    (when (or (char-matches? key-event \p)
              (and k (options/pause-key-includes? (options/of state) k)))
      (toggle-pause-command state))))

(defn- enter-intent
  [state initials-draft]
  (cond
    (screens/title? state) (command {:type :start})
    (screens/the-end? state) {:command {:type :confirm} :draft ""}
    (and (screens/high-score-entry? state) (seq initials-draft))
    (command {:type :submit-high-score :initials initials-draft})
    :else nil))

(defn- initials-char?
  [ch]
  (and ch (re-matches #"[A-Za-z0-9]" (str ch))))

(defn- initials-edit
  [state initials-draft key-event]
  (when (screens/high-score-entry? state)
    (cond
      (:backspace? key-event)
      (draft (if (seq initials-draft)
               (subs initials-draft 0 (dec (count initials-draft)))
               initials-draft))

      (initials-char? (key-char key-event))
      (if (< (count initials-draft) max-initials-length)
        (draft (str initials-draft
                    (str/upper-case (str (key-char key-event)))))
        (draft initials-draft))

      :else nil)))

(defn- playing-key
  [state key-event]
  (when-let [k (key-name key-event)]
    (when (screens/playing? state)
      (command {:type :key :key k}))))

(defn key-intent
  "Return the host-independent intent for a normalized key event."
  [state initials-draft key-event]
  (or (when (:escape? key-event) (escape-intent state))
      (options-shortcut state key-event)
      (options-toggle state key-event)
      (high-scores-toggle state key-event)
      (pause-toggle state key-event)
      (when (:enter? key-event) (enter-intent state initials-draft))
      (initials-edit state initials-draft key-event)
      (playing-key state key-event)))

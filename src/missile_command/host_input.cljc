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

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-07-26T10:25:41.13582-05:00", :module-hash "-2123719317", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 9, :hash "-715656636"} {:id "def/max-initials-length", :kind "def", :line 11, :end-line 11, :hash "-1450593818"} {:id "defn-/key-name", :kind "defn-", :line 13, :end-line 16, :hash "-1450947047"} {:id "defn-/key-char", :kind "defn-", :line 18, :end-line 20, :hash "-2020994996"} {:id "defn-/char-matches?", :kind "defn-", :line 22, :end-line 27, :hash "-694554305"} {:id "defn-/command", :kind "defn-", :line 29, :end-line 31, :hash "1898966849"} {:id "defn-/draft", :kind "defn-", :line 33, :end-line 35, :hash "-2068803211"} {:id "defn-/toggle-pause-command", :kind "defn-", :line 37, :end-line 42, :hash "652462666"} {:id "defn-/escape-intent", :kind "defn-", :line 44, :end-line 53, :hash "1359367895"} {:id "defn-/options-shortcut", :kind "defn-", :line 55, :end-line 68, :hash "1583501978"} {:id "defn-/options-toggle", :kind "defn-", :line 70, :end-line 76, :hash "1766870964"} {:id "defn-/high-scores-toggle", :kind "defn-", :line 78, :end-line 84, :hash "258949817"} {:id "defn-/pause-toggle", :kind "defn-", :line 86, :end-line 91, :hash "-248355559"} {:id "defn-/enter-intent", :kind "defn-", :line 93, :end-line 100, :hash "-303469524"} {:id "defn-/initials-char?", :kind "defn-", :line 102, :end-line 104, :hash "-1078115175"} {:id "defn-/initials-edit", :kind "defn-", :line 106, :end-line 121, :hash "2107617498"} {:id "defn-/playing-key", :kind "defn-", :line 123, :end-line 127, :hash "-826448176"} {:id "defn/key-intent", :kind "defn", :line 129, :end-line 139, :hash "-826754104"}]}
;; clj-mutate-manifest-end

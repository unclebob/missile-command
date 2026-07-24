(ns missile-command.acceptance.step-support)

(defn fail!
  [message]
  (throw (ex-info message {})))

(defn assert-condition
  [condition message]
  (when-not condition
    (fail! message)))

(defn assert-count
  [actual expected label]
  (when-not (= expected actual)
    (fail! (str label " count " actual " expected " expected))))

(defn assert-gt
  [actual bound message]
  (assert-condition (> actual bound) message))

(defn assert-lt
  [actual bound message]
  (assert-condition (< actual bound) message))
(defn require-value
  [example param-name]
  (or (get example param-name)
      (get example (keyword param-name))
      (fail! (str "missing example value for " param-name))))

(defn parse-int
  [value label]
  (try
    (Integer/parseInt (str value))
    (catch NumberFormatException _
      (fail! (str "invalid integer for " label ": " value)))))

(defn example-int
  "Read and parse an integer example parameter."
  [example param-name label]
  (parse-int (require-value example param-name) label))

(defn parse-number
  [value label]
  (try
    (Double/parseDouble (str value))
    (catch NumberFormatException _
      (fail! (str "invalid number for " label ": " value)))))

(defn example-double
  "Read and parse a floating-point example parameter."
  [example param-name label]
  (parse-number (require-value example param-name) label))

(def battery-ids
  {"left" :left
   "center" :center
   "right" :right})

(defn parse-battery-id
  [value]
  (or (get battery-ids (str value))
      (fail! (str "unknown battery: " value))))

(defn example-battery
  "Read a battery id example parameter (:left, :center, or :right)."
  [example param-name]
  (parse-battery-id (require-value example param-name)))

(defn advance-until
  "Tick world state via tick-fn until pred is truthy, or fail after max-steps."
  [world pred tick-fn dt max-steps fail-message]
  (loop [s (:state world) steps-left max-steps]
    (cond
      (pred s) (assoc world :state s)
      (zero? steps-left) (fail! fail-message)
      :else (recur (:state (tick-fn s dt)) (dec steps-left)))))

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-07-24T14:22:36.292281-05:00", :module-hash "1019486428", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 1, :hash "-1891434015"} {:id "defn/fail!", :kind "defn", :line 3, :end-line 5, :hash "-526803145"} {:id "defn/assert-condition", :kind "defn", :line 7, :end-line 10, :hash "1030982162"} {:id "defn/assert-count", :kind "defn", :line 12, :end-line 15, :hash "-188395449"} {:id "defn/assert-gt", :kind "defn", :line 17, :end-line 19, :hash "-146492793"} {:id "defn/assert-lt", :kind "defn", :line 21, :end-line 23, :hash "493759074"} {:id "defn/require-value", :kind "defn", :line 24, :end-line 28, :hash "-545191389"} {:id "defn/parse-int", :kind "defn", :line 30, :end-line 35, :hash "-520638366"} {:id "defn/example-int", :kind "defn", :line 37, :end-line 40, :hash "-33918046"} {:id "defn/parse-number", :kind "defn", :line 42, :end-line 47, :hash "-1752119701"} {:id "defn/example-double", :kind "defn", :line 49, :end-line 52, :hash "414908757"} {:id "def/battery-ids", :kind "def", :line 54, :end-line 57, :hash "-773658772"} {:id "defn/parse-battery-id", :kind "defn", :line 59, :end-line 62, :hash "-1222227857"} {:id "defn/example-battery", :kind "defn", :line 64, :end-line 67, :hash "1294296887"} {:id "defn/advance-until", :kind "defn", :line 69, :end-line 76, :hash "-1157592488"}]}
;; clj-mutate-manifest-end

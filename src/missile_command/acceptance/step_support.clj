(ns missile-command.acceptance.step-support)

(defn fail!
  [message]
  (throw (ex-info message {})))

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

(defn parse-battery-id
  [value]
  (case (str value)
    "left" :left
    "center" :center
    "right" :right
    (fail! (str "unknown battery: " value))))

(defn example-battery
  "Read a battery id example parameter (:left, :center, or :right)."
  [example param-name]
  (parse-battery-id (require-value example param-name)))

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-07-24T11:39:02.028884-05:00", :module-hash "-1019504644", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 1, :hash "-1891434015"} {:id "defn/fail!", :kind "defn", :line 3, :end-line 5, :hash "-526803145"} {:id "defn/require-value", :kind "defn", :line 7, :end-line 11, :hash "-545191389"} {:id "defn/parse-int", :kind "defn", :line 13, :end-line 18, :hash "-520638366"} {:id "defn/example-int", :kind "defn", :line 20, :end-line 23, :hash "-33918046"}]}
;; clj-mutate-manifest-end

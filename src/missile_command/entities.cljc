(ns missile-command.entities)

(defn update-by-id
  "Apply f to the entity with the given id; leave others unchanged."
  [entities id f]
  (mapv (fn [entity]
          (if (= id (:id entity))
            (f entity)
            entity))
        entities))

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-07-24T14:35:21.016778-05:00", :module-hash "-730522267", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 1, :hash "-164478869"} {:id "defn/update-by-id", :kind "defn", :line 3, :end-line 10, :hash "1002351432"}]}
;; clj-mutate-manifest-end

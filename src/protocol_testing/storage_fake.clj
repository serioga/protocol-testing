(ns protocol-testing.storage-fake
  (:require
   [protocol-testing.storage :as storage]))

(defn- map-vals [f m]
  (reduce-kv
   (fn [acc k v] (assoc acc k (f v)))
   {} m))

(defn- reverse-merge [m1 m2]
  (merge m2 m1))

(deftype Transaction [data identity-map]
  storage/Transaction
  (-get-many [_ ids]
    (let [ids-for-select (remove #(contains? @identity-map %) ids)
          selected       (->> ids-for-select
                              (select-keys data)
                              (map-vals ref))]
      ;; Здесь принципиально использование reverse-merge,
      ;; т.к. другой поток может успеть извлечь данные из базы,
      ;; создать объект-идентичность, записать его в identity map
      ;; и сделать в нем изменения.
      ;; Если использовать merge, то этот поток затрет идентичность
      ;; другим объектом-идентичностью с начальным состоянием.
      ;; Фактически это нарушает саму идею identity-map -
      ;; сопоставление ссылки на объект с его идентификатором
      (-> identity-map
          (swap! reverse-merge selected)
          (select-keys ids))))

  (-create [_ state]
    (let [id     (:id state)
          istate (ref state)]
      (swap! identity-map (fn [map]
                            {:pre [(not (contains? map id))]}
                            (assoc map id istate)))
      istate)))

(deftype Storage [db]
  storage/Storage
  (-wrap-tx [_ body]
    (loop []
      (let [data         @db
            identity-map (atom {})
            t            (Transaction. data identity-map)
            res          (body t)
            changed      (map-vals deref @identity-map)
            new-data     (merge data changed)]
        (if (compare-and-set! db data new-data)
          res
          (recur))))))

(defn build-db []
  (atom {}))

(defn binding-map [db]
  {#'storage/*storage* (->Storage db)})

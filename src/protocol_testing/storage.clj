(ns protocol-testing.storage)

(defn- map-vals [f m]
  (reduce-kv
   (fn [acc k v] (assoc acc k (f v)))
   {} m))


(defprotocol Storage
  (-wrap-tx [this body]))

(defprotocol Transaction
  (-get-many [t ids])
  (-create [t state]))

(defn get-many [t ids] (-get-many t ids))
(defn create   [t state] (-create t state))

(declare ^:dynamic *storage*)

(defmacro with-tx
  "Note that body forms may be called multiple times,
   and thus should be free of side effects."
  [tx-name & body-forms-free-of-side-effects]
  `(-wrap-tx *storage*
            (fn [~tx-name]
              ~@body-forms-free-of-side-effects)))

(defn get-one [t id]
  (let [res (get-many t [id])]
    (get res id)))


(defn tx-get-one [id]
  (with-tx t
    (when-let [x (get-one t id)]
      @x)))

(defn tx-get-many [ids]
  (with-tx t
    (->> ids
         (get-many t)
         (map-vals deref))))

(defn tx-create [state]
  (with-tx t
    @(create t state)))

(defn tx-alter [state f & args]
  (with-tx t
    (when-let [x (get-one t (:id state))]
      (dosync
       (apply alter x f args)))))

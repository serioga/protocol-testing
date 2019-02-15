(ns protocol-testing.storage-test
  (:require
    [clojure.test :as t]
    [protocol-testing.storage :as storage]))

(defn- build-entity [counter]
  {:id 42
   :counter counter})

(t/deftest create
  (let [test (storage/tx-create (build-entity 0))
        id (:id test)]
    (t/is (some? test))
    (t/is (some? (storage/tx-get-one id)))))

(t/deftest change
  (let [test (storage/tx-create (build-entity 0))
        id   (:id test)
        _    (storage/tx-alter test update :counter inc)
        test (storage/tx-get-one id)]
    (t/is (= 1 (:counter test)))))

(t/deftest identity-map-persisted
  (let [test (storage/tx-create (build-entity 0))
        id   (:id test)]
    (storage/with-tx t
                     (let [x (storage/get-one t id)
                           y (storage/get-one t id)]
                       (t/is (identical? x y))))))

(t/deftest identity-map-in-memory
  (storage/with-tx t
                   (let [x (storage/create t (build-entity 0))
                         y (storage/get-one t (:id @x))]
                     (t/is (identical? x y)))))

(t/deftest identity-map-swap
  (storage/with-tx t
                   (let [x (storage/create t (build-entity 0))
                         y (storage/get-one t (:id @x))
                         _ (dosync (alter x update :counter inc))]
                     (t/is (= 1 (:counter @x) (:counter @y))))))

(t/deftest concurrency
  (let [test (storage/tx-create (build-entity 0))
        id   (:id test)
        n    10
        _    (->> (repeatedly #(future (storage/tx-alter test update :counter inc)))
                  (take n)
                  (doall)
                  (map deref)
                  (doall))
        test (storage/tx-get-one id)]
    (t/is (= n (:counter test)))))

(t/deftest inner-concurrency
  (let [test (storage/tx-create (build-entity 0))
        id   (:id test)
        n    10
        _    (storage/with-tx t
                              (->> (repeatedly #(future (as-> id <>
                                                                             (storage/get-one t <>)
                                                                             (dosync (alter <> update :counter inc)))))
                                   (take n)
                                   (doall)
                                   (map deref)
                                   (doall)))
        test (storage/tx-get-one id)]
    (t/is (= n (:counter test)))))

(defn test-suite
  [fixtures]
  (doseq [test [create
                change
                identity-map-persisted
                identity-map-in-memory
                identity-map-swap
                concurrency
                inner-concurrency]]
    (fixtures (fn [] (test)))))

(defn test-ns-hook [])

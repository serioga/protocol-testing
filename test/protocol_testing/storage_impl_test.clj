(ns protocol-testing.storage-impl-test
  (:require
    [protocol-testing.storage-impl :as sut]
    [protocol-testing.storage-test :as storage-test]))

(defn test-ns-hook []
  (storage-test/test-suite
    (fn [f]
      ;; эта часть для для настоящей реализации точно будет отличаться от таковой для storage-fake-test
      (println "fixtures: impl")
      (with-bindings (sut/binding-map (sut/build-db))
          (f)))))

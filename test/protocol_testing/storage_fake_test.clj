(ns protocol-testing.storage-fake-test
  (:require
   [protocol-testing.storage-fake :as sut]
   [protocol-testing.storage-test :as storage-test]))

(defn test-ns-hook []
  (storage-test/test-suite
    (fn [f]
      (println "fixtures: fake")
      (with-bindings (sut/binding-map (sut/build-db))
        (f)))))


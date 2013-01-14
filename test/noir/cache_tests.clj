(ns noir.cache-tests
  (:use clojure.test noir.util.cache))

(deftest cache!-test
  (clear!)
  (cache! :foo "bar")
  (is (= "bar" (cache! :foo "baz"))))

(deftest clear!-test
  (clear!)
  (cache! :foo "bar")
  (clear!)
  (is (= "baz" (cache! :foo "baz"))))

(deftest invalidate!-test
  (clear!)
  (cache! :foo "bar")
  (invalidate! :foo)
  (is (= "baz" (cache! :foo "baz"))))

(deftest set-size!-test
  (clear!)
  (set-size! 2)
  (cache! :foo "foo")
  (cache! :bar "bar")
  (cache! :baz "baz")
  (= [:bar :baz] (->> @cached :items (map keys))))

(deftest set-timeout!-test  
  (clear!)
  (set-timeout! 1)
  (cache! :foo "bar")
  (Thread/sleep 1100)
  (is (= "baz" (cache! :foo "baz"))))

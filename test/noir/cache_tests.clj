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
  ;; Several items are accessed 2 times during these tests.  They
  ;; should deterministically win over any items accessed only 1 time
  ;; when the size is exceeded.  If all are only accessed once, then
  ;; the items kept depend upon the order that Clojure's seq returns
  ;; from a map, which is unspecified, and can change across Clojure
  ;; versions.
  (clear!)
  (set-size! 2)
  (cache! :foo "foo")
  (cache! :bar "bar")
  (cache! :foo "foo")
  (cache! :baz "baz")
  (is (= #{:foo :baz} (->> @cached :items keys set)))

  (clear!)
  (set-size! 5)
  (cache! :foo "foo")
  (cache! :bar "bar")
  (cache! :baz "baz")
  (is (= #{:foo :bar :baz} (->> @cached :items keys set)))

  (clear!)
  (set-size! 5)
  (cache! :first "first")
  (cache! :first "first")
  (cache! :second "second")
  (cache! :second "second")
  (cache! :third "third")
  (cache! :third "third")
  (cache! :fourth "fourth")
  (cache! :fourth "fourth")
  (cache! :fifth "fifth")
  (cache! :sixth "sixth")
  (cache! :seventh "seventh")
  (is (= #{:seventh :first :third :second :fourth} (->> @cached :items keys set))))

(deftest set-timeout!-test
  (clear!)
  (set-timeout! 1)
  (cache! :foo "bar")
  (Thread/sleep 1100)
  (is (= "baz" (cache! :foo "baz")))

  (clear!)
  (set-size! 2)
  (cache! :foo "foo")
  (cache! :bar "bar")

  (Thread/sleep 1100)
  (cache! :bar "bar")

  (cache! :baz "baz")
  (is (= #{:bar :baz} (->> @cached :items keys set))))

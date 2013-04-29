(ns noir.validation-tests
  (:use clojure.test)
  (:require [noir.validation :as validation]))

(deftest test-nil
  (is (= true
         (validation/not-nil? true)))
  (is (= true
         (validation/not-nil? 23)))
  (is (= true
         (validation/not-nil? false)))
  (is (= false
         (validation/not-nil? nil))))

(deftest test-min-length
  (is (= true
         (validation/min-length? "abcde" 5)))
  (is (= false
         (validation/min-length? "abcde" 6))))

(deftest test-max-length
  (is (= true
         (validation/max-length? "abcde" 5)))
  (is (= false
         (validation/max-length? "abcde" 4))))

(deftest test-matches-regex
  (is (= true
         (validation/matches-regex? "abcde" #"^[a-zA-Z0-9_-]{3,20}")))
  (is (= true
         (validation/matches-regex? "abc_123-xyz" #"^[a-zA-Z0-9_-]{3,20}")))
  (is (= false
         (validation/matches-regex? "ab" #"^[a-zA-Z0-9_-]{3,20}")))
  (is (= false
         (validation/matches-regex? "abc_@#" #"^[a-zA-Z0-9_-]{3,20}")))
  (is (= false
         (validation/matches-regex? "abcdefghijklmnopqrstuvwxyz"
                                    #"^[a-zA-Z0-9_-]{3,20}"))))

(deftest test-is-email
  (is (= true
         (validation/is-email? "ab@cde.com")))
  (is (= true
         (validation/is-email? "TEST@NOIR.ORG")))
  (is (= false
         (validation/is-email? "@abc.com")))
  (is (= false
         (validation/is-email? "test@abc")))
  (is (= false
         (validation/is-email? "test")))
  (is (= false
         (validation/is-email? "test@.net"))))

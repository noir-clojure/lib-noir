(ns noir.validation-tests
  (:use clojure.test)
  (:require [noir.validation :as validation]))

(deftest test-nil
  (is (= true
         (validation/not-nil? true)))
  (is (= 23
         (validation/not-nil? 23)))
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
  (is (= "abcde"
         (validation/matches-regex? "abcde" #"^[a-zA-Z0-9_-]{3,20}")))
  (is (= "abc_123-xyz"
         (validation/matches-regex? "abc_123-xyz" #"^[a-zA-Z0-9_-]{3,20}")))
  (is (= nil
         (validation/matches-regex? "ab" #"^[a-zA-Z0-9_-]{3,20}")))
  (is (= nil
         (validation/matches-regex? "abc_@#" #"^[a-zA-Z0-9_-]{3,20}")))
  (is (= nil
         (validation/matches-regex? "abcdefghijklmnopqrstuvwxyz"
                                    #"^[a-zA-Z0-9_-]{3,20}"))))

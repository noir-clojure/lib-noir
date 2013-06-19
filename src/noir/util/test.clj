(ns noir.util.test
  "A set of utilities for testing a Noir project"
  (:require [clojure.test :refer :all]
            [clojure.pprint :refer [pprint]]            
            [noir.session :as session]
            [noir.validation :as vali]
            [noir.cookies :as cookies]))

(defmacro with-noir
  "Executes the body within the context of Noir's bindings"
  [& body]
  `(binding [vali/*errors* (atom {})
             session/*noir-session* (atom {})
             session/*noir-flash* (atom {})
             cookies/*new-cookies* (atom {})
             cookies/*cur-cookies* (atom {})]
     ~@body))

(defn has-content-type
  "Asserts that the response has the given content type"
  [resp ct]
  (is (= ct (get-in resp [:headers "Content-Type"])))
  resp)

(defn has-status
  "Asserts that the response has the given status"
  [resp stat]
  (is (= stat (:status resp)))
  resp)

(defn has-body
  "Asserts that the response has the given body"
  [resp cont]
  (is (= cont (:body resp)))
  resp)
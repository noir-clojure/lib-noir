(ns noir.route-tests
  (:use clojure.test noir.util.route))

(defn method [url params body] body)
(defn r1 [m url params] true)
(defn r2 [m url params] false)

(deftest test-redirect 
  (binding [noir.request/*request* {:access-rules [[{:redirect "/bar"} r1 r2]]}]
    (is (= {:status 302, :headers {"Location" "/bar"}, :body ""} 
           (restricted method "/restricted" nil "foo")))))

(deftest test-pass 
  (binding [noir.request/*request* {:access-rules [[{:redirect "/bar"} r1 r2]]}]
    (is (= "foo" (restricted method "/restricted" nil "foo")))))


(deftest test-multiple-rule-sets 
  (binding [noir.request/*request* {:access-rules [[{:redirect "/foo"} r1]
                                                   [{:redirect "/bar"} r2]
                                                   [{:redirect "/baz"} r1 r2]]}]
    (is (= {:status 302, :headers {"Location" "/bar"}, :body ""} 
           (restricted method "/restricted" nil "foo"))))
  
  (binding [noir.request/*request* {:access-rules [[{:redirect "/foo"} r1]
                                                   [{:redirect "/bar"} r1 r2]
                                                   [{:redirect "/baz"} r1 r2]]}]
    (is (= "foo" 
           (restricted method "/restricted" nil "foo")))))

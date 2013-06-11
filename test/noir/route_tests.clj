(ns noir.route-tests
  (:use clojure.test noir.util.route))

(defn r1 [req] true)
(defn r2 [req] false)

(deftest test-simple-redirect
  (is (= {:status 302, :headers {"Location" "/"}, :body ""}
         ((restricted "I shouldn't be here!")
           {:access-rules [{:rules [r2]}]}))))

(deftest test-redirect
  (is (= {:status 302, :headers {"Location" "/bar"}, :body ""}
         ((restricted "I shouldn't be here!")
           {:access-rules [{:redirect "/bar"
                            :rules [r2 r2]}]}))))

(deftest test-pass
  (is (= "I should be here!"
         ((restricted "I should be here!")
           {:access-rules [{:redirect "/bar" :rules [r1 r2]}]}))))

(deftest test-multiple-rule-sets
  (is (= {:status 302, :headers {"Location" "/bar"}, :body ""}
         ((restricted "I shouldn't be here!")
           {:access-rules [{:redirect "/foo" :rules [r1]}
                           {:redirect "/bar" :rules [r2]}
                           {:redirect "/baz" :rules [r1 r2]}]})))

  (is (= "I should be here!"
         ((restricted "I should be here!")
           {:access-rules [{:redirect "/foo" :rules [r1]}
                           {:redirect "/bar" :rules [r1 r2]}
                           {:redirect "/baz" :rules [r1 r2]}]}))))

(deftest access-rule-test
  (is (= "success"
         ((access-rule "/foo/*" req "success") {:uri "/foo/bar"}))))
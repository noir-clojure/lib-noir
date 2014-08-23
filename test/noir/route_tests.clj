(ns noir.route-tests
  (:use clojure.test noir.util.route))

(defn allow [req] true)
(defn deny [req] false)

(deftest test-simple-redirect
  (is (= {:status 302, :headers {"Location" "/"}, :body ""}
         ((restricted "I shouldn't be here!")
           {:access-rules [{:rule deny}]}))))

(deftest test-redirect
  (is (= {:status 302, :headers {"Location" "/bar"}, :body ""}
         ((restricted "I shouldn't be here!")
           {:access-rules [{:redirect "/bar" :rules [allow deny]}]}))))

(deftest test-pass
  (is (= "I should be here!"
         ((restricted "I should be here!")
           {:access-rules [{:redirect "/bar" :rules [allow]}]}))))

(deftest test-multiple-rule-sets
  (is (= {:status 302, :headers {"Location" "/"}, :body ""}
         ((restricted "I shouldn't be here!")
           {:uri "/bar"
            :access-rules [{:uri "/foo" :rules [allow]}
                           {:uri "/bar" :rules [deny]}
                           {:uri "/baz" :rules [allow deny]}]})))

  (is (= {:status 302, :headers {"Location" "/baz"}, :body ""}
         ((restricted "I shouldn't be here!")
           {:uri "/foo/x"
           :access-rules [{:uri "/foo*" :rules [allow]}
                          {:uri "/foo/x" :rules [allow] :redirect "/bar"}
                          {:uri "/foo/x" :rules [deny] :redirect "/baz"}
                          {:uri "/foo/y" :rules [deny deny] :redirect "/qux"}]})))

  (is (= {:status 302, :headers {"Location" "/baz"}, :body ""}
         ((restricted "I shouldn't be here!")
           {:uri "/bar"
            :access-rules [{:uris ["/foo" "/bar"] :rule deny :redirect "/baz"}
                           {:uri "/bar" :rule allow :redirect "/qux"}]})))

  (is (= {:status 302, :headers {"Location" "/bar"}, :body ""}
         ((restricted "I shouldn't be here!")
           {:access-rules [{:redirect "/bar" :rules {:every [allow deny]}}]})))
  (is (= "I should be here!"
         ((restricted "I should be here!")
           {:access-rules [{:redirect "/bar" :rules {:any [allow deny]}}]})))
  (is (= "I should be here!"
         ((restricted "I should be here!")
           {:access-rules [{:redirect "/bar" :rules {:every [allow allow]
                                                     :any [allow deny]}}]}))))



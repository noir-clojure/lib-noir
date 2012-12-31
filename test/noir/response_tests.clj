(ns noir.response-tests
  (:use clojure.test noir.request)
  (:require [noir.response :as response]))

(deftest test-set-headers
  (is (= {:headers {"x-csrf" "csrf"} :scheme :http}
        (response/set-headers {"x-csrf" "csrf"} {:scheme :http})))
  (is (= {:headers {"x-csrf" "csrf"}, :body "foo"} 
         (response/set-headers {"x-csrf" "csrf"} "foo"))))

(deftest test-content-type
  (is (= {:headers {"Content-Type" "text/html"}, :body "foo"}
         (response/content-type "text/html" "foo"))))

(deftest test-xml 
  (is (= {:headers {"Content-Type" "text/xml; charset=utf-8"}, :body "foo"}
         (response/xml "foo"))))

(deftest test-json 
  (is (= {:headers {"Content-Type" "application/json; charset=utf-8"}, :body "\"foo\""}
         (response/json "foo"))))

(deftest test-jsonp 
  (is (= {:headers {"Content-Type" "application/json; charset=utf-8"}, :body "foo(\"bar\");"}
         (response/jsonp "foo" "bar"))))

(deftest test-edn 
  (is (= {:headers {"Content-Type" "application/edn; charset=utf-8"}, :body "\"bar\""}
         (response/edn "bar"))))

(deftest test-status
  (is (= {:status :found, :body "foo"} (response/status :found "foo"))))

(deftest test-redirect
  (is (= {:status 302 :headers {"Location" "/bar"} :body ""}
         (response/redirect "/bar")))
  
  (is (= {:status 305, :headers {"Location" "/foo/bar"}, :body ""}
         (response/redirect "/bar" :proxy {:scheme :http :context "/foo"})))
  
  (binding [*request* {:scheme :http :context "/foo"}]
    (is (= {:status 302 :headers {"Location" "http://bar"} :body ""}
           (response/redirect "http://bar"))))
 
  (binding [*request* {:scheme :http :context "/foo"}]
    (is (= {:status 302 :headers {"Location" "/foo/bar"} :body ""}
           (response/redirect "/bar"))))
  
  (binding [*request* {:scheme :http :uri "/bar" :context "/foo"}]
    (is (= {:status 301 :headers {"Location" "/foo/bar"} :body ""}
           (response/redirect "/bar" :permanent)))))

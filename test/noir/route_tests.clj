(ns noir.route-tests
  (:use clojure.test noir.util.route))

(deftest test-redirect 
  (let [r1 (fn [m url params] false)
        r2 (fn [m url params] false)
        method (fn [url params body]
                 body)] 
    (binding [noir.request/*request* {:access-rules [{:redirect "/bar"} r1 r2]}]
      (is (= {:status 302, :headers {"Location" "/bar"}, :body ""} 
             (restricted method "/foo" nil "foo"))))))

(deftest test-pass 
  (let [r1 (fn [m url params] true)
        r2 (fn [m url params] false)
        method (fn [url params body]
                 body)] 
    (binding [noir.request/*request* {:access-rules [{:redirect "/bar"} r1 r2]}]
      (is (= "foo" (restricted method "/foo" nil "foo"))))))
(ns noir.middleware-tests
  (:use clojure.test noir.util.middleware))


(defn wrap [wrapper & params] 
  (apply (partial wrapper (fn [req] req)) params))

(deftest test-wrap-canonical-host
  (let [wrapped (wrap wrap-canonical-host "foo.net")
        can-req {:headers (fn [_] "foo.net")
                 :scheme :http
                 :uri "/bar"}
        noncan-req {:headers (fn [_] "bar.net")
                    :scheme :http
                    :uri "/baz"}]
    (let [{:keys [headers scheme uri]} (wrapped can-req)] 
      (is (and
            (= "foo.net" (headers "host"))
            (= :http scheme)
            (= "/bar" uri))))
    (is (= {:status 301
            :headers {"Location" "http://foo.net/baz"} 
            :body ""}
           (wrapped noncan-req)))))


(deftest test-wrap-force-ssl
  (let [wrapped (wrap wrap-force-ssl)
        req1 (wrapped {:headers (fn [_] "foo.net") :scheme :https})
        req2 (wrapped {:headers (fn [_] "https")})
        req3 (wrapped {:headers (fn [_] "foo.com") :uri "/bar"})
        {:keys [headers scheme]} req1]
    (is (= "foo.net" (headers "foo.net")))
    (is (= :https scheme))
    (is (= "https" ((:headers req2) "")))
    (is (= {:status 301, :headers {"Location" "https://foo.com/bar"}, :body ""} req3))))

(deftest test-wrap-strip-trailing-slash
  (is (= {:uri "/foo"} ((wrap wrap-strip-trailing-slash) {:uri "/foo/"}))))

(deftest test-wrap-rewrites
  (let [handler (fn [updated-req] updated-req)]    
    (is (= {:uri "/bar"} ((wrap-rewrites handler #"/foo" "/bar") {:uri "/foo"})))
    (is (= {:uri "/bar"} ((wrap-rewrites handler #"/foo" "/bar") {:uri "/foo"})))    
    (is (thrown? Exception ((wrap-rewrites handler #"/foo" "/bar" #"/baz") {:uri "/foo"})))
    (is (= {:uri "/baz"} ((wrap-rewrites handler) {:uri "/baz"})))))

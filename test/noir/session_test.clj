(ns noir.session-test
  (:use clojure.test
        [noir.session :only [wrap-noir-session*]]))

(deftest noir-session
  (let [base-map {:uri "/foo" :request-method :get }]
    (testing "Put session value in")
    (is (= "bar" (get-in ((wrap-noir-session*
                           #(assoc-in % [:session :foo] "bar"))
                          base-map)
                         [:session :foo])))
    (let [base-map (assoc base-map :session {:foo "bar"})]
      (testing "Pass session value through")
      (is (= "bar" (get-in ((wrap-noir-session* identity)
                            base-map)
                           [:session :foo])))
      (testing "Change session value")
      (is  (= "baz" (get-in ((wrap-noir-session*
                              #(assoc-in % [:session :foo] "baz"))
                             base-map)
                            [:session :foo])))
      (testing "Dissoc session value")
      (is  (not (contains? (:session
                            ((wrap-noir-session*
                              #(assoc % :session (dissoc (:session %) :foo)))
                             base-map))
                           :foo))))
    (testing "Dissocing one key doesn't affect any others")
    (let [base-map (assoc base-map :session {:foo "bar" :quuz "auugh"})
          part-dissoc (:session
                       ((wrap-noir-session*
                         #(assoc % :session (dissoc (:session %) :foo)))
                        base-map))]
      (is (not (contains? part-dissoc :foo)))
      (is (= "auugh" (:quuz  part-dissoc)))
      (testing "Changing one key doesn't affect any others")
      (let [part-change (:session
                         ((wrap-noir-session*
                           #(assoc-in % [:session :foo] "baz"))
                          base-map))]
        (is (= "baz" (:foo part-change)))
        (is (= "auugh" (:quuz  part-change)))))
    (testing "Delete whole session.
     Ring takes nil to mean delete session, so it must get passed through.")
    (is (nil?  (:session ((wrap-noir-session*
                           #(assoc % :session nil))
                          base-map))))
    (testing "Make sure the whole session goes away and stays away if deleted")
    (is (not (contains?  ((wrap-noir-session*
                           #(dissoc % :session))
                          base-map)
                         :session)))))

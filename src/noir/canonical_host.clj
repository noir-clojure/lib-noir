(ns noir.canonical-host
  (:use [noir.response :only [redirect]]))

(defn wrap-canonical-host [app canonical]
  (fn [req]
    (let [headers (:headers req)]
      (when canonical
        (if (= (headers "host") canonical)
          (app req)
          (redirect (str (:scheme req) "://" canonical (:uri req))
                    :permanent))))))
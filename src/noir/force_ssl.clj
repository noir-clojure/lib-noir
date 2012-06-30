(ns noir.force-ssl
  (:use [noir.response :only [redirect]]))

(defn wrap-force-ssl
  "If the request's scheme is not https, redirect with https.
   Also checks the X-Forwarded-Proto header."
  [app]
  (fn [req]
    (let [headers (:headers req)]
      (if (or (= :https (:scheme req))
              (= "https" (headers "x-forwarded-proto")))
        (app req)
        (redirect (str "https://" (headers "host") (:uri req)) :permanent)))))
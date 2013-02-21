(ns noir.util.route
  (:use [noir.request :only [*request*]]
        [noir.response :only [redirect]]))

(defmacro restricted
  "checks if any of the rules defined in wrap-access-rules match the method,
   if no rules match then the response is a redirect to \"/\""
  [method url params & body]
  `(~method ~url ~params
     (let [[x# & xs# :as items#] (:access-rules noir.request/*request*)
           redirect# (if (map? x#) (:redirect x#) "/")
           rules#    (if (map? x#) xs# items#)]       
       (if (or (nil? rules#)
               (some (fn [rule#] (rule# '~method ~url ~params)) rules#))
         (do ~@body)
         (noir.response/redirect redirect#)))))




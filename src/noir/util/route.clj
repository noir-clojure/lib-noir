(ns noir.util.route
  (:use [noir.request :only [*request*]]
        [noir.response :only [redirect]]))

(defmacro check-rules
  "checks if any of the rules defined in wrap-access-rules match the method,
   if no rules match then the response is a redirect to \"/\""
  [method url params rule-set]
  `(let [[x# & xs# :as items#] ~rule-set
         options?# (map? x#)
         redirect# (if options?# (:redirect x#) "/")
         rules#    (if options?# xs# items#)]
     (or 
       (or (empty? rules#) 
           (some (fn [rule#] (rule# '~method ~url ~params)) rules#))       
       (noir.response/redirect redirect#))))

(defmacro restricted
  "checks if any of the rules defined in wrap-access-rules match the method,
   if no rules match then the response is a redirect to \"/\""
  [method url params & body]
  `(~method ~url ~params
            (loop [rules# (:access-rules noir.request/*request*)]
              (let [result# (check-rules ~method ~url ~params (first rules#))]                
                (cond 
                  (map? result#) result#
                  (empty? rules#) (do ~@body)
                  :else (recur (rest rules#)))))))

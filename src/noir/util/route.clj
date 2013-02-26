(ns noir.util.route
  (:use [clout.core :only [route-matches]]  
        [noir.request :only [*request*]]
        [noir.response :only [redirect]]))

(defmacro ^{:skip-wiki true} check-rules  
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
  "Checks if any of the rules defined in wrap-access-rules match the method,
   if no rules match then the response is a redirect to the location specified
   by the noir.util.middleware/wrap-access-rules wrapper."
  [method url params & body]
  `(~method ~url ~params
            (loop [rules# (:access-rules noir.request/*request*)]
              (let [result# (check-rules ~method ~url ~params (first rules#))]                
                (cond
                  (= 302 (:status result#)) result#
                  (empty? rules#) (do ~@body)
                  :else (recur (rest rules#)))))))

(defmacro access-rule 
  "Creates an access rule for the given url pattern.
   The second argument must be a vector with three items
   that represent the arguments for the rule: [method url params].
   
   The condition must return a boolean value to indicate where 
   the rule passes, eg:
   
   (access-rule \"/users/:id\" [_ _ params] (= (first params) \"foo\"))
   
   The above rule will only be checked for urls matching \"/users/:id\"
   and succeed if :id is equal to \"foo\"
  "
  [target-url fn-params condition]
  `(fn ~fn-params
     (or (nil?  (route-matches ~target-url {:uri (second ~fn-params)})) 
         ~condition)))

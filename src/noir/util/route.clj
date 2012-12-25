(ns noir.util.route
  (:use [noir.response :only [redirect]]))

(defmacro restricted
  "checks if any of the rules defined in wrap-access-rules match the method,
   if no rules match then the response is a redirect to \"/\""
  [method url params body]
  `(~method ~url ~params
     (let [rules# (:access-rules noir.util.middleware/*request*)]       
       (if (or (nil? rules#) 
               (some (fn [~'rule] (~'rule '~method ~url ~params)) rules#)) 
         ~body 
         (noir.response/redirect "/")))))
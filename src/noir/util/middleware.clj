(ns noir.util.middleware
  (:use [noir.request :only [*request*]] 
        [noir.response :only [redirect]]        
        [compojure.core :only [routes]]
        [compojure.handler :only [api]]
        [hiccup.middleware :only [wrap-base-url]]
        [noir.validation :only [wrap-noir-validation]]
        [noir.cookies :only [wrap-noir-cookies]]
        [noir.session :only [mem wrap-noir-session wrap-noir-flash]]        
        [ring.middleware.multipart-params :only [wrap-multipart-params]]                
        [ring.middleware.session.memory :only [memory-store]]
        [ring.middleware.resource :only [wrap-resource]]
        [ring.middleware.file-info :only [wrap-file-info]])
  (:require [clojure.string :as s]))

(defn- with-opts [routes middleware opts]
  (if opts
    (middleware routes opts)
    (middleware routes)))

(defn wrap-request-map [handler]
  (fn [req]
    (binding [*request* req]
      (handler req))))

(defn wrap-canonical-host
  "If the request is not targeting host canonical, redirect the
   request to that host."
  [app canonical]
  (fn [req]
    (let [headers (:headers req)]
      (when canonical
        (if (= (headers "host") canonical)
          (app req)
          (redirect (str (name (:scheme req)) "://" canonical (:uri req))
                    :permanent))))))

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

(defn wrap-strip-trailing-slash
  "If the requested url has a trailing slash, remove it."
  [handler]
  (fn [request]
    (handler (update-in request [:uri] s/replace #"(?<=.)/$" ""))))

(defn wrap-access-rules
  "wraps the handler with the supplied access rules, each rule accepts
   method, url, and params and returns a boolean indicating whether it
   passed or not, eg:

   (defn private-pages [method url params]
    (and (some #{url} [\"/private-page1\" \"/private-page2\"]) 
         (session/get :user)))

   by default if none of the rules return true the client will be redirected
   to /, it's possible to pass a custom redirect target by providing a map 
   with a redirect key pointing to a URI string before specifying the rules:

   (wrap-access-rules handler {:redirect \"/unauthorized\"} rule1 rule2)

   note that you can use multiple wrap-access-rules wrappers together to
   create sets of rules each redirecting to a different URI, eg:

   (-> handler 
       (wrap-access-rules rule1 rule2)
       (wrap-access-rules {:redirect \"/unauthorized1\"} rule3 rule4)
       (wrap-access-rules {:redirect \"/unauthorized2\"} rule5)

   the first set of rules that fails will cause a redirect to its redirect target"
  [handler & rules]
  (fn [req]
    (handler (update-in req [:access-rules] 
                        #(if % (conj % rules) [rules])))))

(defn app-handler
  "creates the handler for the application and wraps it in base middleware:
  - api
  - wrap-file-info
  - wrap-multipart-params
  - wrap-request-map
  - wrap-noir-validation
  - wrap-noir-cookies
  - wrap-noir-flash
  - wrap-noir-session

  :store - optional session store, defaults to memory store
  :multipart - an optional map of multipart-params middleware options"
  [app-routes & {:keys [store multipart]}]
  (-> (apply routes app-routes)    
    (api)
    (wrap-file-info)
    (with-opts wrap-multipart-params multipart)
    (wrap-request-map)    
    (wrap-noir-validation)
    (wrap-noir-cookies)
    (wrap-noir-flash)
    (wrap-noir-session 
      {:store (or store (memory-store mem))})))

(defn war-handler
  "wraps the app-handler in middleware needed for WAR deployment:
  - wrap-resource
  - wrap-base-url"
  [app-handler]
  (-> app-handler    
    (wrap-resource "public")    
    (wrap-base-url)))

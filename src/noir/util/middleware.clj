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

(defn wrap-rewrites
  "Rewrites should be [regex replacement] pairs. The first regex that matches the request's URI will
  have the corresponding (global) replacement performed before calling the wrapped handler."
  [handler & rewrites]
  (if-not (even? (count rewrites))
    (throw (ex-info "must have an even number of rewrites: " {:rewrites rewrites})))
  (let [rules (partition 2 rewrites)]
    (fn [req]
      (handler (update-in req [:uri]
                          (fn [uri]
                            (or (first (keep (fn [[pattern replacement]]
                                               (when (re-find pattern uri)
                                                 (s/replace uri pattern replacement)))
                                             rules))
                                uri)))))))

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

(defn- wrap-middleware [routes [wrapper & more]]
  (if wrapper (recur (wrapper routes) more) routes))

(defn- set-access-rules [handler [rule & access-rules]]  
  (if rule 
    (recur (apply (partial wrap-access-rules handler) rule) access-rules)
    handler))

(defn app-handler
  "creates the handler for the application and wraps it in base middleware:
  - wrap-request-map
  - api
  - wrap-multipart-params
  - wrap-noir-validation
  - wrap-noir-cookies
  - wrap-noir-flash
  - wrap-noir-session

  :store - optional session store, defaults to memory store
  :multipart - an optional map of multipart-params middleware options
  :middleware - a vector of any custom middleware wrappers you wish to supply
  :access-rules - a vector of access rules you wish to supply,
                  each rule should be a vector of parameters accepted by wrap-access-rules, eg:

                  :access-rules [[{:redirect \"/unauthorized1\"} rule1]
                                 [{:redirect \"/unauthorized2\"} rule2 rule3]       
                                 [rules4 rule5]]"   
  [app-routes & {:keys [store multipart middleware access-rules]}]  
  (-> (apply routes app-routes)
      (wrap-middleware middleware)
      (wrap-request-map)
      (api)
      (with-opts wrap-multipart-params multipart)
      (set-access-rules access-rules)      
      (wrap-noir-validation)
      (wrap-noir-cookies)
      (wrap-noir-flash)
      (wrap-noir-session 
        {:store (or store (memory-store mem))})))

(defn war-handler
  "wraps the app-handler in middleware needed for WAR deployment:
  - wrap-resource
  - wrap-file-info
  - wrap-base-url"
  [app-handler]
  (-> app-handler
      (wrap-resource "public")
      (wrap-file-info)
      (wrap-base-url)))

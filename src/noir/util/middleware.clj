(ns noir.util.middleware
  (:use [noir.request :only [*request*]] 
        [noir.response :only [redirect]]        
        [compojure.core :only [routes]]
        [compojure.handler :only [site]]
        [hiccup.middleware :only [wrap-base-url]]
        [noir.validation :only [wrap-noir-validation]]
        [noir.cookies :only [wrap-noir-cookies]]
        [noir.session :only [mem wrap-noir-session]]
        [ring.middleware.session.memory :only [memory-store]]
        [ring.middleware.resource :only [wrap-resource]]
        [ring.middleware.file-info :only [wrap-file-info]]
        [ring.middleware.multipart-params :only [wrap-multipart-params]])
  (:require [clojure.string :as s]))

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
         (session/get :user)))"
  [handler & rules]
  (fn [req]
    (handler (assoc req :access-rules rules))))

(defn app-handler
  "creates the handler for the application and wraps it in base middleware:
  - site 
  - wrap-request-map
  - wrap-noir-validation
  - wrap-multipart-params
  - wrap-noir-session
  a session store can be passed in as an optional parameter, defaults to memory store"
  [app-routes & [store]]
  (-> (apply routes app-routes)
    (site)
    (wrap-request-map)
    (wrap-noir-validation)
    (wrap-multipart-params)
    (wrap-noir-cookies)
    (wrap-noir-session 
      {:store (or store (memory-store mem))})))

(defn war-handler
  "wraps the app-handler in middleware needed for WAR deployment:
  - wrap-resource
  - wrap-base-url
  - wrap-file-info"
  [app-handler]
  (-> app-handler    
    (wrap-resource "public")
    (wrap-file-info)
    (wrap-base-url)))

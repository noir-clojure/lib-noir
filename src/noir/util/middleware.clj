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

(defn wrap-if [handler pred wrapper & args]
  (if pred
    (apply wrapper handler args)
    handler))

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
   the request and returns a boolean indicating whether it passed or not, eg:

   (defn private-pages [req]
    (and (some #{(:uri req)} [\"/private-page1\" \"/private-page2\"])
         (session/get :user)))

   by default if none of the rules return true the client will be redirected
   to /. It's possible to pass a custom redirect target by specifying the
   :redirect key pointing to a URI.

   The value of the :redirect key can either be a string or a function that
   takes the request as its argument.

   The rules can be supplied either as a function or a map indicating the
   redirect target and the rules that redirect to that target, eg:

   (wrap-access-rules handler [some-rule
                               another-rule
                               {:redirect \"/unauthorized\"
                                :rules [rule3 rule4]}
                               {:redirect (fn [req] (println \"redirecting\")
                                            \"/unauthorized\")
                                :rule rule5}])

   the first set of rules that fails will cause a redirect to its redirect target.

   To restrict access rules to only active for specific URI patterns use the :uri key:

   (wrap-access-rules handler [{:redirect \"/unauthorized\"
                                :uri \"/users/*\"
                                :rules [rule1 rule2]}])

   (wrap-access-rules handler [{:redirect \"/unauthorized\"
                                :uris [\"/users/*\" \"/private\"]
                                :rules [rule1 rule2]}])
   
   Above, rule1 and rule2 will only be activated for URIs that start with /users/.
   
   It's also possible to specify :on-fail function to handle the failure as an
   alternative to a redirect:

   (wrap-access-rules handler [{:on-fail (fn [req] \"access to denied!\")
                                :uri \"/users/*\"
                                :rules [rule1 rule2]}])

   By default any of the rules have to match for the rule group to succeed.
   It's possible to use :any and :every keys to change the resolution behavior:
   
   (wrap-access-rules handler [{:rules {:every [rule1 rule2]}}])
   (wrap-access-rules handler [{:rules {:any [rule1 rule2]}}])
   (wrap-access-rules handler [{:rules {:every [rule1 rule2]
                                        :any   [rule3 rule4]}}])
   "
  [handler rules]
  (if (empty? rules)
    handler
    (let [{mapped-rules true
           unmapped-rules false} (group-by map? rules)]
      (fn [req]
        (handler
          (assoc req :access-rules
            (if (not-empty unmapped-rules)
                  (conj mapped-rules {:redirect "/" :rules unmapped-rules})
                  mapped-rules)))))))

(defn- wrap-middleware [routes [wrapper & more]]
  (if wrapper (recur (wrapper routes) more) routes))

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
                  each rule should a function or a rule map as specified in wrap-access-rules, eg:

                  :access-rules [rule1
                                 rule2
                                 {:redirect \"/unauthorized1\"
                                  :rules [rule3 rule4]}]"
  [app-routes & {:keys [store multipart middleware access-rules]}]
  (-> (apply routes app-routes)
      (wrap-request-map)
      (api)
      (with-opts wrap-multipart-params multipart)
      (wrap-middleware middleware)
      (wrap-access-rules access-rules)
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

(ns noir.util.middleware
  (:use [noir.request :only [*request*]]
        [noir.response :only [redirect]]
        [compojure.core :only [routes]]
        [compojure.handler :only [api]]
        [hiccup.middleware :only [wrap-base-url]]
        [noir.validation :only [wrap-noir-validation]]
        [noir.cookies :only [wrap-noir-cookies]]
        [noir.session :only [clear! mem wrap-noir-session wrap-noir-flash]]
        [ring.middleware.multipart-params :only [wrap-multipart-params]]
        [ring.middleware.session.memory :only [memory-store]]
        [ring.middleware.format :refer [wrap-restful-format]])
  (:require [clojure.string :as s])
  (:import java.util.Date))

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
  "wraps the handler with the supplied access rules.

   Use the noir.util.route/restricted macro to wrap any routes
   that you wish the access rules to apply to, eg:

   (GET \"/user/profile\" [] (restricted \"this route is private\"))

   Each rule accepts the request and returns a boolean indicating whether it
   passed or not, eg:

   (defn user-access [req]
    (session/get :user))

   (wrap-access-rules handler [user-access])

   Each rule can either be a function or a map. When a rule is a function, then
   it redirects to \"/\" and will be checked for each restricted route.

   When specifying rules as a map you can provide further directives using the
   following keys:

   :uri - the URI pattern to which the rules apply (optional, defaults to any URI)
   :uris - alternative to :uri, allows specifying a collection of URIs (optional)
   :redirect - the redirect target for the rules (optional defaults to \"/\")
   :on-fail - alternative to redirect, allows specifying a handler function for
              handling the failure, the function must accept the request as a
              parameter (optional)
   :rule - a single rule (either :rule or :rules is required)
   :rules - alternative to rule, allows specifying a list of rules

   The :rules can be specified in any of the following ways:

   :rules [rule1 rule2]
   :rules {:any [rule1 rule2]}
   :rules {:every [rule1 rule2] :any [rule3 rule4]}

   By default every rule has to pass, the :any key specifies that it's sufficient for
   any of the rules to pass.

   (defn admin-access [req]
    (session/get :admin))

   (wrap-access-rules handler [{:redirect \"/access-denied\"
                                :rule user-access}])

   (wrap-access-rules handler [{:uri \"/user/*\" :rule user-access}])

   (wrap-access-rules handler [{:uri \"/admin/*\" :rule admin-access}
                               {:uri \"/user/*\"
                                :rules {:any [user-access admin-access]}])

   (wrap-access-rules handler [{:on-fail (fn [req] \"access restricted\")
                                :rule user-access}])
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

  :session-options - optional map specifying Ring session parameters, eg: {:cookie-attrs {:max-age 1000}}
  :store           - deprecated: use sesion-options instead!
  :multipart       - an optional map of multipart-params middleware options
  :middleware      - a vector of any custom middleware wrappers you wish to supply
  :formats         - optional vector containing formats that should be serialized and
                     deserialized, eg:

                     :formats [:json-kw :edn]

                  available formats:
                  :json - JSON with string keys in :params and :body-params
                  :json-kw - JSON with keywodized keys in :params and :body-params
                  :yaml - YAML format
                  :yaml-kw - YAML format with keywodized keys in :params and :body-params
                  :edn - Clojure format
                  :yaml-in-html - yaml in a html page (useful for browser debugging)

  :access-rules - a vector of access rules you wish to supply,
                  each rule should a function or a rule map as specified in wrap-access-rules, eg:

                  :access-rules [rule1
                                 rule2
                                 {:redirect \"/unauthorized1\"
                                  :rules [rule3 rule4]}]"
  [app-routes & {:keys [session-options store multipart middleware access-rules formats]}]
  (letfn [(wrap-middleware-format [handler]
            (if formats (wrap-restful-format handler :formats formats) handler))]
    (-> (apply routes app-routes)
        (wrap-middleware middleware)
        (wrap-request-map)
        (api)
        (wrap-base-url)
        (wrap-middleware-format)
        (with-opts wrap-multipart-params multipart)
        (wrap-access-rules access-rules)
        (wrap-noir-validation)
        (wrap-noir-cookies)
        (wrap-noir-flash)
        (wrap-noir-session
         (update-in session-options [:store] #(or % (memory-store mem)))))))

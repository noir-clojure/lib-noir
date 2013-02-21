(ns noir.session
  "Stateful session handling functions. Uses a memory-store by
  default, but can use a custom store by supplying a :session-store
  option to server/start."
  (:refer-clojure :exclude [get get-in remove swap!])
  (:use ring.middleware.session
        ring.middleware.session.memory
        ring.middleware.flash))

;; ## Session

(declare ^:dynamic *noir-session*)
(defonce mem (atom {}))

(defn put!
  "Associates the key with the given value in the session"
  [k v]
  (clojure.core/swap! *noir-session* assoc k v))

(defn get
  "Get the key's value from the session, returns nil if it doesn't exist."
  ([k] (get k nil))
  ([k default]
    (clojure.core/get @*noir-session* k default)))

(defn get-in
  "Gets the value at the path specified by the vector ks from the session,
  returns nil if it doesn't exist."
  ([ks] (get-in ks nil))
  ([ks default]
    (clojure.core/get-in @*noir-session* ks default)))

(defn swap!
  "Replace the current session's value with the result of executing f with
  the current value and args."
  [f & args]
  (apply clojure.core/swap! *noir-session* f args))

(defn clear!
  "Remove all data from the session and start over cleanly."
  []
  (reset! *noir-session* {}))

(defn remove!
  "Remove a key from the session"
  [k]
  (clojure.core/swap! *noir-session* dissoc k))

(defn assoc-in!
  "Associates a value in the session, where ks is a
   sequence of keys and v is the new value and returns
   a new nested structure. If any levels do not exist,
   hash-maps will be created."
  [ks v]
  (clojure.core/swap! *noir-session* #(assoc-in % ks v)))

(defn get!
  "Destructive get from the session. This returns the current value of the key
  and then removes it from the session."
  ([k] (get! k nil))
  ([k default]
   (let [cur (get k default)]
     (remove! k)
     cur)))

(defn get-in!
  "Destructive get from the session. This returns the current value of the path
  specified by the vector ks and then removes it from the session."
  ([ks] (get-in! ks nil))
  ([ks default]
    (let [cur (clojure.core/get-in @*noir-session* ks default)]
      (assoc-in! ks nil)
      cur)))

(defn update-in!
  "'Updates' a value in the session, where ks is a
   sequence of keys and f is a function that will
   take the old value along with any supplied args and return
   the new value. If any levels do not exist, hash-maps
   will be created."
  [ks f & args]
  (clojure.core/swap!
    *noir-session*
    #(apply (partial update-in % ks f) args)))

(defn ^:private noir-session [handler]
   "Store noir session keys in a :noir map, because other middleware that
   expects pure functions may delete keys, and simply merging won't work.
   Ring takes (not (contains? response :session) to mean: don't update session.
   Ring takes (nil? (:session resonse) to mean: delete the session.
   Because noir-session mutates :session, it needs to duplicate ring/wrap-session
   functionality to handle these cases."
  (fn [request]
    (binding [*noir-session* (atom (get-in request [:session :noir] {}))]
      (remove! :_flash)
      (when-let [resp (handler request)]
        (if (=  (get-in request [:session :noir] {})  @*noir-session*)
          resp
          (if (contains? resp :session)
            (if (nil? (:session resp))
              resp
              (assoc-in resp [:session :noir] @*noir-session*))
            (assoc resp :session (assoc (:session request) :noir @*noir-session*))))))))

(defn wrap-noir-session
  "A stateful layer around wrap-session. Options are passed to wrap-session."
  ([handler]
    (wrap-noir-session handler {}))
  ([handler opts]
    (-> handler
      (noir-session)
      (wrap-session opts))))

(defn wrap-noir-session*
  "A stateful layer around wrap-session. Expects that wrap-session has already
   been used."
  [handler]
  (noir-session handler))

;; ## Flash

(declare ^:dynamic *noir-flash*)

(defn flash-put!
  "Store a value that will persist for this request and the next."
  [k v]
  (clojure.core/swap! *noir-flash* assoc-in [:outgoing k] v))

(defn flash-get
  "Retrieve the flash stored value."
  ([k]
     (flash-get k nil))
  ([k not-found]
   (let [in (get-in @*noir-flash* [:incoming k])
         out (get-in @*noir-flash* [:outgoing k])]
     (or out in not-found))))

(defn ^:private noir-flash [handler]
  (fn [request]
    (binding [*noir-flash* (atom {:incoming (:flash request)})]
      (let [resp (handler request)
            outgoing-flash (:outgoing @*noir-flash*)]
        (if (and resp outgoing-flash)
          (assoc resp :flash outgoing-flash)
          resp)))))

(defn wrap-noir-flash
  "A stateful layer over wrap-flash."
  [handler]
  (-> handler
      (noir-flash)
      (wrap-flash)))

(defn wrap-noir-flash*
  "A stateful layer over wrap-flash. Expects that wrap-flash has already
   been used."
  [handler]
  (noir-flash handler))
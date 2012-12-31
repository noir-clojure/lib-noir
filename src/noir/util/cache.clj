(ns noir.util.cache)

(defonce cached (atom {}))
(defonce cache-options (atom {}))

(defn invalidate-cache!
  "removes the id and the content associated with it from cache"
  [id]
  (swap! cached dissoc id))

(defn set-cache-timeout!
  "set the expiriy time for cached items in seconds,
   if the item has been in cache longer than this time
   it will be swapped with a new version"
  [seconds]
  (swap! cache-options assoc :timeout (* 1000 seconds)))

(defn set-cache-size!
  "set the maximum size for the cache,
   when the cache grows larger than the
   size specified oldest items will be 
   removed to make room for new items"
  [items]
  (swap! cache-options assoc :size items))

(defmacro cache
  "checks if there is a cached copy of the content available,
   if so the cached version is returned, otherwise the content
   is evaluated"
  [id content]
  `(let [timeout#    (:timeout @noir.util.cache/cache-options)
         max-size#   (:size @noir.util.cache/cache-options)
         cached#     (get @noir.util.cache/cached ~id)
         cur-time#   (.getTime (new java.util.Date))]     
     (if (or (not cached#)
             (and timeout# (> (- cur-time# (:time cached#)) timeout#)))       
       (swap! cached assoc ~id {:content ~content 
                                :time (.getTime (new java.util.Date))}))     
     (if (and max-size# (> (count @noir.util.cache/cached) max-size#))  
       (swap! cached (fn [cached-items#]
                       (->> cached-items#
                         (sort-by :time)
                         (take max-size#)
                         (into {})))))
     (:content (get @noir.util.cache/cached ~id)))) 

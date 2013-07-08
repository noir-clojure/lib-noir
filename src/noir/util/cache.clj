(ns noir.util.cache)

(defonce cached (atom {}))

(defn invalidate!
  "removes the id and the content associated with it from cache"
  [id]
  (swap! cached update-in [:items] dissoc id))

(defn set-timeout!
  "set the expiriy time for cached items in seconds,
   if the item has been in cache longer than this time
   it will be swapped with a new version"
  [seconds]
  (swap! cached assoc-in [:options :timeout] (* 1000 seconds)))

(defn set-size!
  "set the maximum size for the cache,
   when the cache grows larger than the
   size specified least used items will be 
   removed to make room for new items"
  [items]
  (swap! cached assoc-in [:options :size] items))

(defn clear!
  "remove all items which are currently cached"
  []
  (swap! cached assoc-in [:items] {}))

(defmacro cache!
  "checks if there is a cached copy of the content available,
   if so the cached version is returned, otherwise the content
   is evaluated"
  [id content]
  `(let [timeout#    (get-in @noir.util.cache/cached [:options :timeout])
         max-size#   (get-in @noir.util.cache/cached [:options :size])
         cur-item#   (get-in @noir.util.cache/cached [:items ~id])
         cur-time#   (.getTime (new java.util.Date))]
     (if (and max-size# (> (count (:items @noir.util.cache/cached)) max-size#))
       (swap! noir.util.cache/cached update-in [:items]
              (fn [items#]
                (->> items#
                     (sort-by #(:ticks (second %)))
                     (take-last max-size#)
                     (into {})))))
     (-> noir.util.cache/cached
       (swap! update-in [:items ~id]
              (fn [item#]
                (if (or (not item#) (and timeout# (> (- cur-time# (:time item#)) timeout#)))
                  {:time    cur-time#
                   :ticks   (inc (get cur-item# :ticks 0))
                   :content (or (try ~content (catch Exception ex#))
                                (:content cur-item#))}
                  (update-in item# [:ticks] inc))))
       (get-in [:items ~id :content]))))

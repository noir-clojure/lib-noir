(ns noir.util.cache)

(def cached (atom {}))

(defn invalidate-cache!
  "removes the id and the content associated with it from cache"
  [id]
  (swap! cached dissoc id))

(defmacro cache
  "checks if there is a cached copy of the content available,
   if so the cached version is returned, otherwise the content
   is evaluated"
  [id content]
  `(do
     (if-not (get @noir.util.cache/cached ~id)
       (swap! cached assoc ~id {:content ~content}))
     (:content (get @noir.util.cache/cached ~id)))) 
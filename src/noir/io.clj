(ns noir.io
  (:require [clojure.java.io :as io])
  (:import [java.io File FileInputStream FileOutputStream]))

(defn resource-path
  "returns the path to the public folder of the application"
  []
  (if-let [path (io/resource (str "public" File/separator))]
    (.getPath path)))

(defn- file-path [path & [filename]]
  (java.net.URLDecoder/decode
    (str path File/separator filename)
    "utf-8"))

(defn create-path [path create-path?]
  (let [working-dir  (File. (file-path path))]
    (when (and create-path? (not (.exists working-dir)))
      (.mkdirs working-dir))))

(defn upload-file
  "uploads a file to the target folder
   when :create-path? flag is set to true then the target path will be created"
  [path {:keys [tempfile size filename size]} & {:keys [create-path?]}]
  (create-path path create-path?)
  (try
    (with-open [in (new FileInputStream tempfile)
                out (new FileOutputStream (file-path path filename))]
      (let [source (.getChannel in)
            dest   (.getChannel out)]
        (.transferFrom dest source 0 (.size source))))))

(defn get-resource
  "returns a URL for a resource relative to the public folder of the application
   expects path to be a / separated string relative to the public folder, eg:
   (get-resource \"/css/screen.css\" )"
  [relative-path]
  (when relative-path
    (->> relative-path
         (str "public")
         (io/resource))))

(defn slurp-resource
  "Opens a reader on f and reads all its contents, returning a string.
   Path is specified the same way as for get-resource"
  [path]
  (if-let [resource (get-resource path)]
    (-> resource io/input-stream slurp)))

(ns noir.util.crypt
  "Simple functions for hashing strings and comparing them. Typically used for storing passwords."
  (:refer-clojure :exclude [compare])
  (:import [org.mindrot.jbcrypt BCrypt]
           java.security.MessageDigest))

(defn
  ^{:private true}
  hasher
  "Hashing digest action handler. Common types -> SHA1,SHA-256,MD5"
  [instance-type data salt]
  (let [_ (if-not salt
            (.toString data)
            (let [[s d] (map 
                         (memfn toString)
                         [salt data])]
              (apply str [s d s])))
        sha1-obj (doto (MessageDigest/getInstance instance-type)
                   .reset
                   (.update
                    (.getBytes _)))]
    (apply str
           (map (partial format "%02x")
                (.digest sha1-obj)))))

(defn md5
  [data & salt]
  (hasher "MD5" data salt))

(defn sha1 
  [data & salt]
  (hasher "SHA1" data salt))

(defn sha2
  [data & salt]
  (hasher "SHA-256" data salt))

(defn gen-salt
  ([size]
   (BCrypt/gensalt size))
  ([]
   (BCrypt/gensalt)))

(defn encrypt
  "Encrypt the given string with a generated or supplied salt. Uses BCrypt for strong hashing."
  ;; generate a salt
  ([salt raw] (BCrypt/hashpw raw salt))
  ([raw] (encrypt (gen-salt) raw)))

(defn compare
  "Compare a raw string with an already encrypted string"
  [raw encrypted]
  (BCrypt/checkpw raw encrypted))

(defn sha1-sign-hex [sign-key v]
  "Using a signing key, compute the sha1 hmac of v and convert to hex."
  (let [mac (javax.crypto.Mac/getInstance "HmacSHA1")
        secret (javax.crypto.spec.SecretKeySpec. (.getBytes sign-key), "HmacSHA1")]
    (.init mac secret)
    (apply str (map (partial format "%02x") (.doFinal mac (.getBytes v))))))

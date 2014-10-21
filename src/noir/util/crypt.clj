(ns noir.util.crypt
  "Simple functions for hashing strings and comparing them. Typically used for storing passwords."
  (:refer-clojure :exclude [compare])
  (:import [org.mindrot.jbcrypt BCrypt]
           java.security.MessageDigest
           java.security.SecureRandom
           javax.crypto.SecretKeyFactory
           [javax.crypto.spec PBEKeySpec SecretKeySpec]))

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
        hash-obj (doto (MessageDigest/getInstance instance-type)
                   .reset
                   (.update
                    (.getBytes _)))]
    (apply str
           (map (partial format "%02x")
                (.digest hash-obj)))))

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

(defn gen-pbkdf2
    ; Get a hash for the given string and optional salt
    ([x salt]
     (let [k (PBEKeySpec. (.toCharArray x) (.getBytes salt) 1000 192)
           f (SecretKeyFactory/getInstance "PBKDF2WithHmacSHA1")]
       (->> (.generateSecret f k) (.getEncoded) (java.math.BigInteger.) (format "%x")))))

(defn encrypt
  "Encrypt the given string with a generated or supplied salt. Uses BCrypt for strong hashing."
  ;; generate a salt
  ([salt raw] (BCrypt/hashpw raw salt))
  ([raw] (encrypt (gen-salt) raw)))

(defn compare
  "Compare a raw string with an already encrypted string"
  [raw encrypted]
  (boolean
   (if (and raw encrypted)
    (BCrypt/checkpw raw encrypted))))

(defn sha1-sign-hex
  "Using a signing key, compute the sha1 hmac of v and convert to hex."
  [sign-key v]
  (let [mac (javax.crypto.Mac/getInstance "HmacSHA1")
        secret (SecretKeySpec. (.getBytes sign-key), "HmacSHA1")]
    (.init mac secret)
    (apply str (map (partial format "%02x") (.doFinal mac (.getBytes v))))))

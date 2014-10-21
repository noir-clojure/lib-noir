(ns noir.util.crypt
  "Simple functions for hashing strings and comparing them. Typically used for storing passwords."
  (:refer-clojure :exclude [compare])
  (:require [clojurewerkz.scrypt.core :as sc])
  (:import org.mindrot.jbcrypt.BCrypt
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

(defn gen-pbkdf2
    ; Get a hash for the given string and optional salt
    ([x salt]
     (let [k (PBEKeySpec. (.toCharArray x) (.getBytes salt) 1000 192)
           f (SecretKeyFactory/getInstance "PBKDF2WithHmacSHA1")]
       (->> (.generateSecret f k) (.getEncoded) (java.math.BigInteger.) (format "%x")))))

(defn gen-salt
  ([size]
   (BCrypt/gensalt size))
  ([]
   (BCrypt/gensalt)))

(defn encrypt
  "Encrypts a string value using scrypt.
   Arguments are:

   raw (string): a string to encrypt
   :n (integer): CPU cost parameter (default is 16384)
   :r (integer): RAM cost parameter (default is 8)
   :p (integer): parallelism parameter (default is 1)

   The output of SCryptUtil.scrypt is a string in the modified MCF format:

   $s0$params$salt$key

   s0     - version 0 of the format with 128-bit salt and 256-bit derived key
   params - 32-bit hex integer containing log2(N) (16 bits), r (8 bits), and p (8 bits)
   salt   - base64-encoded salt
   key    - base64-encoded derived key"
    [raw & {:keys [n r p]
             :or {n 16384 r 8 p 1}}]
  (sc/encrypt raw n r p))

(defn compare
  "Compare a raw string with an already encrypted string"
  [raw encrypted]
  (boolean
   (if (and raw encrypted)
    (sc/verify raw encrypted))))

(defn sha1-sign-hex
  "Using a signing key, compute the sha1 hmac of v and convert to hex."
  [sign-key v]
  (let [mac (javax.crypto.Mac/getInstance "HmacSHA1")
        secret (SecretKeySpec. (.getBytes sign-key), "HmacSHA1")]
    (.init mac secret)
    (apply str (map (partial format "%02x") (.doFinal mac (.getBytes v))))))

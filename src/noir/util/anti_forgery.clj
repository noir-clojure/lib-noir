(ns noir.util.anti-forgery
  "Utility functions for inserting anti-forgery tokens into HTML forms."
  (:require [ring.util.anti-forgery :as anti-forgery]))

(defn anti-forgery-field
  "Create a hidden field with the session anti-forgery token as its value.
  This ensures that the form it's inside won't be stopped by the anti-forgery
  middleware."
  []
  (anti-forgery/anti-forgery-field))

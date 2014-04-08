(ns jecode.mg
  (:require [clj-http.client :as client]
            [cheshire.core :as json]
            [shoreleave.middleware.rpc :refer [defremote]]))

;;; * Variables

(def mg-prv-auth
  (str (System/getenv "MAILGUN_USER") ":" (System/getenv "MAILGUN_PVT_KEY")))

;; Used for mg-valid-email? only
(def mg-pub-auth
  (str (System/getenv "MAILGUN_USER") ":" (System/getenv "MAILGUN_PUB_KEY")))

(def jecode-list "lettre@jecode.org")

;;; * Subscription validation

(defn- mg-valid-email?
  "Return true is `email` is a valid email."
  [email]
  (:is_valid
   (json/parse-string
    (:body (client/get
            "https://api.mailgun.net/v2/address/validate"
            {:basic-auth mg-pub-auth
             :query-params {"address" email}}))
    true)))

(defn- mg-email-on-list?
  "Return true if email is already subscribed to list."
  [email list]
  (:subscribed
   (:member
    (json/parse-string
     (:body (client/get
             (format "https://api.mailgun.net/v2/lists/%s/members/%s" list email)
             {:basic-auth mg-prv-auth}))
     true))))

(defremote mg-subscribe-valid-email
  "Subscribe a new valid email address."
  [email]
  (cond (not (mg-valid-email? email)) "Adresse mail invalide"
        (mg-email-on-list? email jecode-list) "Adresse déjà inscrite"
        :else (do (add-email-list jecode-list email)
                  "Inscription réussie !")))

;;; * Add and delete email

(defn- add-email-list
  "Add email to list."
  [list email]
  (client/post
   (format "https://api.mailgun.net/v2/lists/%s/members" list)
   {:basic-auth mg-prv-auth
    :query-params {:address email}}))

(defn- delete-email-list
  "Delete email from list."
  [list email]
  (client/delete
   (format "https://api.mailgun.net/v2/lists/%s/members/%s" list email)
   {:basic-auth mg-prv-auth}))

;; Local Variables:
;; eval: (orgstruct-mode 1)
;; orgstruct-heading-prefix-regexp: ";;; "
;; End:

(ns jecode.db
  (:require
   [digest :as digest]
   [noir.session :as session]
   [jecode.model :refer :all]
   [jecode.views.templates :refer :all]
   [taoensso.carmine :as car]
   [clojurewerkz.scrypt.core :as sc]
   [postal.core :as postal]
   [cemerick.friend :as friend]
   (cemerick.friend [workflows :as workflows]
                    [credentials :as creds])
   [shoreleave.middleware.rpc :refer [defremote wrap-rpc]]))

(defn send-activation-email [email activation-link]
  (postal/send-message
   ^{:host "localhost"
     :port 25}
   {:from "jecode.org <contact@jecode.org>"
    :to email
    :subject "Merci d'avoir rejoint jecode.org !"
    :body (str "Cliquez sur le lien ci-dessous pour activer votre compte :\n"
               activation-link)}))

(defn activate-user [authid]
  (let [guid (wcar* (car/get (str "auth:" authid)))]
    (wcar* (car/hset (str "uid:" guid) "active" 1))))

(defn create-new-user
  "Create a new user."
  [{:keys [email password]}]
  (wcar* (car/incr "global:uid"))
  (let [guid (wcar* (car/get "global:uid"))
        authid (digest/md5 (str (System/currentTimeMillis) email))
        pic (str "http://www.gravatar.com/avatar/" (digest/md5 email))]
    (wcar* (car/hmset
            (str "uid:" guid)
            "u" email "p" (sc/encrypt password 16384 8 1) "pic" pic "d" (java.util.Date.)
            "active" 0)
           (car/set (str "uid:" guid ":auth") authid)
           (car/set (str "auth:" authid) guid)
           (car/set (str "user:" email ":uid") guid)
           (car/rpush "users" guid))
    (send-activation-email email (str "http://jecode.org/activer/" authid))))

(defn create-new-initiative
  "Create a new initiative."
  [{:keys [pname purl desc lat lon]}]
  (wcar* (car/incr "global:pid"))
  (let [pid (wcar* (car/get "global:pid"))
        uname (session/get :username)
        uid (get-username-uid uname)]
    (wcar* (car/hmset
            (str "pid:" pid)
            "name" pname "url" purl "desc" desc
            "lat" lat "lon" lon "updated" (java.util.Date.)))
    (wcar* (car/rpush "timeline" pid))
    (wcar* (car/set (str "pid:" pid ":auid") uid))
    (wcar* (car/sadd (str "uid:" uid ":apid") pid))))


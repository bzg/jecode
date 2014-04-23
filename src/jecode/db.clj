(ns jecode.db
  (:require
   [clojure.string :as s]
   [digest :as digest]
   [noir.session :as session]
   [ring.util.codec :as codec]
   [simple-time.core :as time]
   [cheshire.core :as json]
   [jecode.model :refer :all]
   [jecode.views.templates :refer :all]
   [taoensso.carmine :as car]
   [clojurewerkz.scrypt.core :as sc]
   [postal.core :as postal]
   [cemerick.friend :as friend]
   (cemerick.friend [workflows :as workflows]
                    [credentials :as creds])))

(def osm-search-format "http://nominatim.openstreetmap.org/search?q=%s&format=json")

(def user-date-re #"(\d+)-(\d+)-(\d+) (\d+):(\d+)")

(defn- get-lat-lon-from-location
  [location]
  "Return a map with latitude and longitude from location."
  (let [res (first (json/parse-string
                    (slurp (format osm-search-format
                                   (codec/url-encode location)))
                    true))]
    {:lat (:lat res) :lon (:lon res)}))

(defn- user-date-to-internal-time
  [user-date]
  "Convert a date string \"YYYY-MM-DD HH:MM\" to internal time."
  (time/format
   (apply time/datetime
          (map #(Integer. %)
           (concat (rest (re-find user-date-re user-date)) '(0 0))))))

(defn- user-date-to-readable-time
  "Convert a date string like \"YYYY-MM-DD HH:MM\" to a readable time
  representation."
  [user-date]
  (apply format
         (concat '("Le %d/%d/%d à %dh%d")
                 (map #(Integer. %)
                      (rest (re-find user-date-re user-date))))))

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
            "u" email "p" (sc/encrypt password 16384 8 1)
            "pic" pic "d" (time/format (time/now))
            "active" 0)
           (car/set (str "uid:" guid ":auth") authid)
           (car/set (str "auth:" authid) guid)
           (car/set (str "user:" email ":uid") guid)
           (car/rpush "users" guid))
    (send-activation-email email (str "http://jecode.org/activer/" authid))))

(defn create-new-initiative
  "Create a new initiative."
  [{:keys [pname purl logourl contact twitter plocation pdesc ptags]}]
  (wcar* (car/incr "global:pid"))
  (let [pid (wcar* (car/get "global:pid"))
        uname (session/get :username)
        uid (get-username-uid uname)
        lat (:lat (get-lat-lon-from-location plocation))
        lon (:lon (get-lat-lon-from-location plocation))]
    (wcar* (car/hmset
            (str "pid:" pid)
            "name" pname
            "url" purl
            "desc" pdesc
            "location" plocation
            "logourl" logourl
            "contact" contact
            "twitter" twitter
            "lat" lat "lon" lon
            "updated" (time/format (time/now)))
           (doseq [t (map s/trim (s/split ptags #","))]
             (car/sadd (str "pid:" pid ":tags") t))
           (car/rpush "timeline" pid)
           (car/set (str "pid:" pid ":auid") uid)
           (car/sadd (str "uid:" uid ":apid") pid))))

(defn create-new-event
  "Create a new event."
  [{:keys [eorga ename econtact eurl edate_start edate_end elocation edesc etags]}]
  (wcar* (car/incr "global:eid"))
  (let [eid (wcar* (car/get "global:eid"))
        uname (session/get :username)
        uid (get-username-uid uname)
        lat (:lat (get-lat-lon-from-location elocation))
        lon (:lon (get-lat-lon-from-location elocation))]
    (wcar* (car/hmset
            (str "eid:" eid)
            "orga" eorga
            "contact" econtact
            "name" ename
            "url" eurl
            "desc" edesc
            "hdate_start" (user-date-to-readable-time edate_start)
            "hdate_end" (user-date-to-readable-time edate_end)
            "date_start" (user-date-to-internal-time edate_start)
            "date_end" (user-date-to-internal-time edate_end)
            "location" elocation
            "lat" lat "lon" lon
            "updated" (time/format (time/now)))
           (doseq [t (map s/trim (s/split etags #","))]
             (car/sadd (str "eid:" eid ":tags") t))
           (car/rpush "timeline_events" eid)
           (car/set (str "eid:" eid ":auid") uid)
           (car/sadd (str "uid:" uid ":aeid") eid))))

;; Local Variables:
;; eval: (orgstruct-mode 1)
;; orgstruct-heading-prefix-regexp: ";;; "
;; End:

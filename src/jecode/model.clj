(ns jecode.model
  (:require
   [taoensso.carmine :as car]
   [noir.session :as session]
   [clojure.string :as s]
   [cheshire.core :refer :all :as json]
   [clj-rss.core :as rss]
   [shoreleave.middleware.rpc :refer [defremote]]))

(def server1-conn
  {:pool {} :spec {:uri "redis://localhost:6379/"}})

(defmacro wcar* [& body]
  `(car/wcar server1-conn ~@body))

;;; * Core model functions

(defn get-username-uid
  "Given a username, return the user's uid."
  [username]
  (wcar* (car/get (str "user:" username ":uid"))))

(defn get-uid-field
  "Given a uid and a field (as a string), return the field's value."
  [uid field]
  (wcar* (car/hget (str "uid:" uid) field)))

(defn get-pid-field
  "Given a pid and a field (as a string), return the field's value."
  [pid field]
  (wcar* (car/hget (str "pid:" pid) field)))

(defn get-eid-field
  "Given a eid and a field (as a string), return the field's value."
  [eid field]
  (wcar* (car/hget (str "eid:" eid) field)))

(defn get-pid-all
  "Given a pid and a field (as a string), return the field's value."
  [pid]
  (wcar* (car/hgetall (str "pid:" pid))))

(defn get-uid-all
  "Given a uid, return all field:value pairs."
  [uid]
  (wcar* (car/hgetall (str "uid:" uid))))

(defn get-eid-all
  "Given a eid and a field (as a string), return the field's value."
  [eid]
  (wcar* (car/hgetall (str "eid:" eid))))

(defn username-admin-of-id?
  "True is username is the admin of project pid."
  [username id type]
  (= (wcar* (car/get (str (if (= type "initiatives") "pid:" "eid:") id ":auid")))
     (get-username-uid username)))

;;; Remotes

(defmacro vec-to-kv-hmap [vec]
  `(into {}
         (for [v# (apply hash-map ~vec)]
           [(keyword (key v#)) (val v#)])))

(defn get-db-items
  "Return the list of database items.
When `type` is initiatives, return initiatives.
Otherwise return events.
Each item is represented as a hash-map."
  [type & tags]
  (let [id (if (= type "initiatives") "pid:" "eid:")
        idk (if (= type "initiatives") :pid :eid)
        tl (if (= type "initiatives")
             "timeline" "timeline_events")
        items (filter
               #(not= (:hide %) "hide")
               (map #(assoc (vec-to-kv-hmap (wcar* (car/hgetall (str id %))))
                       idk %
                       :isadmin (or (session/get :admin)
                                    (username-admin-of-id?
                                     (session/get :username) % type)))
                    (wcar* (car/lrange tl 0 -1))))]
    (sort-by
     :name
     (filter #(re-find (re-pattern (s/join "|" tags)) (:tags %))
             items))))

(defn get-initiatives
  "Return the list of initiatives.
Filter the list by tags, if any.
Each initiative is represented as a hash-map."
  [& tags]
  (apply get-db-items "initiatives" tags))

(defn get-events
  "Return the list of events.
Filter the list by tags, if any.
Each event is represented as a hash-map."
  [& tags]
  (apply get-db-items "evenements" tags))

(defremote get-for-map [type & tags]
  (filter #(or (seq (:lat %)) (seq (:lon %)))
          (if (= type "initiatives")
            (apply get-initiatives tags)
            (apply get-events tags))))

;;; * RSS

(defrecord event-rss-item [title link description])

(defn event-to-rss-item
  "Given `event`, maybe export it to a rss item."
  [event]
  (let [name (:name event)
        url (:url event)
        loc (:location event)
        contact (:contact event)]
    (->event-rss-item
     (str "Événement: " name)
     url
     (format
      (s/join
       '("<p>%s organise l'événement \"%s\" !</p>"
         "<p>Début : %s<p>"
         "<p>  Fin : %s<p>"
         "<p> Lieu : %s</p>"
         "<p>Contact : %s</p>%s<p><a href=\"%s\">%s</a></p>"))
      (:orga event)
      name
      (:hdate_start event)
      (:hdate_end event)
      (if (seq loc) loc "non précisé")
      (if (seq contact) contact "non précisé")
      (:desc event) url url))))

(defn events-rss []
  (apply rss/channel-xml
         {:title "jecode.org"
          :link "http://jecode.org"
          :description "jecode.org: apprenons à programmer ensemble !"}
         (reverse
          (remove empty? (map event-to-rss-item (get-events))))))

;;; * JSON

(defn items-json [type & tag]
  (json/generate-string
   {:source (str "jecode.org/" type "/json")
    :retrieved (java.util.Date.)
    (condp = type
      "evenements" :events
      "initiatives" :initiatives)
    (condp = type
      "evenements" (apply get-for-map "evenements" tag)
      "initiatives" (apply get-for-map "initiatives" tag))}
   {:date-format "yyyy-MM-dd HH:MM" :pretty true}))

;; Local Variables:
;; eval: (orgstruct-mode 1)
;; orgstruct-heading-prefix-regexp: ";;; "
;; End:

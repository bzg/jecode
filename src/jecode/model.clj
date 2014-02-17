(ns jecode.model
  (:require
   [taoensso.carmine :as car]
   [noir.session :as session]
   [shoreleave.middleware.rpc :refer [defremote wrap-rpc]]))

(def server1-conn
  {:pool {} :spec {:uri "redis://localhost:6379/"}})

(defmacro wcar* [& body]
  `(car/wcar server1-conn ~@body))

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

(defn get-uid-all
  "Given a uid, return all field:value pairs."
  [uid]
  (wcar* (car/hgetall (str "uid:" uid))))

(defmacro vec-to-kv-hmap [vec]
  `(into {}
         (for [v# (apply hash-map ~vec)]
           [(keyword (key v#)) (val v#)])))

(defn username-admin-of-pid?
  "True is username is the admin of project pid."
  [username pid]
  (= (wcar* (car/get (str "pid:" pid ":auid")))
     (get-username-uid username)))

;; (defn username-member-of-pid?
;;   "True is username is a member of project pid."
;;   [username pid]
;;   (if (nil? username)
;;     nil
;;     (wcar* (car/sismember
;;             (str "pid:" pid ":muid")
;;             (get-username-uid username)))))

(defremote get-initiatives
  "Return the list of initiatives.
Each initiative is represented as a hash-map."
  []
  (map #(assoc (vec-to-kv-hmap (wcar* (car/hgetall (str "pid:" %))))
          :pid %
          ;; :ismember (username-member-of-pid?
          ;;            (session/get :username) %)
          :isadmin (username-admin-of-pid?
                    (session/get :username) %))
       (wcar* (car/lrange "timeline" 0 -1))))

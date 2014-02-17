(ns jecode.handler
  (:require [noir.util.middleware :as middleware]
            [jecode.util :refer :all]
            [noir.session :as session]
            [clojurewerkz.scrypt.core :as sc]
            [jecode.db :refer :all]
            [jecode.model :refer :all]
            [jecode.views.layout :as layout]
            [jecode.views.templates :refer :all]
            [compojure.core :as compojure :refer (GET POST defroutes)]
            (compojure [route :as route])
            [cemerick.friend :as friend]
            (cemerick.friend [workflows :as workflows]
                             [credentials :as creds])
            [cemerick.friend.credentials :refer (hash-bcrypt)]
            [shoreleave.middleware.rpc :refer [wrap-rpc]]))

(defn render-page [tpl page]
  (layout/render
   tpl {:content (md->html (str "/md/" page))}))

(defn- load-user
  "Load a user from her username."
  [username]
  (let [uid (get-username-uid username)
        password (get-uid-field uid "p")]
    (session/put! :username username)
    {:identity username :password password :roles #{::users}}))

(defn- scrypt-credential-fn
  "Variant of `bcrypt-credential-fn` using scrypt."
  [load-credentials-fn {:keys [username password]}]
  (when-let [creds (load-credentials-fn username)]
    (let [password-key (or (-> creds meta ::password-key) :password)]
      (when (sc/verify password (get creds password-key))
        (dissoc creds password-key)))))

(defn wrap-friend [handler]
  "Wrap friend authentication around handler."
  (friend/authenticate
   handler
   {:allow-anon? true
    :workflows [(workflows/interactive-form
                 :allow-anon? true
                 :login-uri "/login"
                 :default-landing-uri "/carte"
                 :credential-fn (partial scrypt-credential-fn load-user))]}))

(defroutes app-routes 
  (GET "/" [] (render-page "home.html" "accueil"))
  (GET "/apropos" [] (render-page "about.html" "apropos"))
  (GET "/carte" [] (map-page))
  (GET "/proposer" [] (new-initiative))
  (POST "/proposer" {params :params}
        (do (create-new-initiative params)
            (main-tpl {:container "Initiative ajoutée, merci !"})))
  (GET "/login" req (login req))
  (GET "/logout" req (logout req))
  (GET "/inscription" [] (register))
  (GET "/test" req (pr-str req))
  (POST "/inscription" {params :params}
        (do (create-new-user params)
            (main-tpl {:container "Merci!"})))
  (GET "/activer/:authid" [authid]
       (do (activate-user authid)
           (main-tpl {:container "Utilisateur actif !"})))
  (GET "/codeurs" [] (render-page "person.html" "codeurs"))
  (GET "/codeurs/:person" [person] (render-page "person.html" person))
  (route/resources "/")
  (route/not-found (render-page "404.html" "404")))

(def app (middleware/app-handler [(wrap-friend (wrap-rpc app-routes))]))

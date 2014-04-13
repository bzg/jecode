(ns jecode.handler
  (:require [noir.util.middleware :as middleware]
            [hiccup.page :as h]
            [hiccup.element :as e]
            [jecode.util :refer :all]
            [noir.session :as session]
            [net.cgrand.enlive-html :as html]
            [clojurewerkz.scrypt.core :as sc]
            [jecode.db :refer :all]
            [jecode.model :refer :all]
            [jecode.views.templates :refer :all]
            [ring.util.response :as resp]
            [ring.middleware.reload :refer :all]
            [compojure.core :as compojure :refer (GET POST defroutes)]
            [org.httpkit.server :refer :all]
            (compojure [route :as route])
            [cemerick.friend :as friend]
            (cemerick.friend [workflows :as workflows]
                             [credentials :as creds])
            [cemerick.friend.credentials :refer (hash-bcrypt)]
            [shoreleave.middleware.rpc :refer [wrap-rpc]]))

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

(defn- wrap-friend [handler]
  "Wrap friend authentication around handler."
  (friend/authenticate
   handler
   {:allow-anon? true
    :workflows [(workflows/interactive-form
                 :allow-anon? true
                 :login-uri "/login"
                 :default-landing-uri "/initiatives"
                 :credential-fn (partial scrypt-credential-fn load-user))]}))

(defn- four-oh-four []
  (h/html5
   [:head
    [:title "jecode.org -- page non trouvée"]
    (h/include-css "/css/generic.css")]
   [:body
    (e/image {:class "logo"} "/pic/jecode_petit.png")
    [:p "Page non trouvée :/"]
    [:p "Retour à la "
     (e/link-to "http://jecode.org" "page d'accueil")]]))

(defroutes app-routes
  ;; Generic
  (GET "/" [] (main-tpl {:a "accueil" :jumbo "/md/description" :md "/md/accueil"}))
  (GET "/apropos" [] (main-tpl {:a "apropos" :md "/md/apropos"}))
  (GET "/codeurs" [] (main-tpl {:a "codeurs" :md "/md/liste_codeurs"}))
  (GET "/codeurs/:person" [person]
       (main-tpl {:a "codeurs" :md (str "/md/codeurs/" person)}))
  
  ;; Initiatives
  (GET "/initiatives" [] (main-tpl {:a "initiatives" :md "/md/initiatives"}))
  (GET "/initiatives/map" []
       (main-tpl {:a "initiatives" :map "showinits" :md "/md/initiatives_map"}))
  (GET "/initiatives/nouvelle" []
       (friend/authorize
        #{::users}
        (main-tpl {:a "initiatives" :container (new-init-snp) :map "newinit"})))
  (POST "/initiatives/nouvelle" {params :params}
        (do (create-new-initiative params)
            (main-tpl {:a "initiatives"
                       :container "Votre initiative a été ajoutée, merci !"})))
  
  ;; Événements
  (GET "/evenements" []
       (main-tpl {:a "evenements" :md "/md/evenements"}))
  (GET "/evenements/rss" [] (events-rss))
  (GET "/evenements/map" []
       (main-tpl {:a "evenements" :map "showevents" :md "/md/evenements_map"}))
  (GET "/evenements/nouveau" []
       (friend/authorize
        #{::users}
        (main-tpl {:a "evenements" :container (new-event-snp) :map "newevent"})))
  (POST "/evenements/nouveau" {params :params}
        (do (create-new-event params)
            (main-tpl {:a "evenements"
                       :container "Votre événement a été ajouté, merci !"})))

  ;; Login
  (GET "/login" [] (main-tpl {:a "accueil" :container (login-snp)}))
  (GET "/logout" req (friend/logout* (resp/redirect (str (:context req) "/"))))
  (GET "/inscription" [] (main-tpl {:a "accueil" :container (register-snp)}))
  (POST "/inscription" {params :params}
        (do (create-new-user params)
            (main-tpl {:a "accueil" :container "Merci!"})))
  (GET "/activer/:authid" [authid]
       (do (activate-user authid)
           (main-tpl {:a "accueil" :container "Utilisateur actif !"})))
  ;; Others
  (route/resources "/")
  (route/not-found (four-oh-four)))

(def app (wrap-reload
          (middleware/app-handler
           [(wrap-friend (wrap-rpc app-routes))])))

(defn -main [& args]
  (run-server #'app {:port 8080}))

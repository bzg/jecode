(ns jecode.handler
  (:require [noir.util.middleware :as middleware]
            [hiccup.page :as h]
            [hiccup.element :as e]
            [jecode.util :refer :all]
            [jecode.github :refer :all]
            [noir.session :as session]
            [net.cgrand.enlive-html :as html]
            [clojure.string :as s]
            [clojurewerkz.scrypt.core :as sc]
            [jecode.db :refer :all]
            [jecode.model :refer :all]
            [jecode.search :refer :all]
            [jecode.views.templates :refer :all]
            [ring.util.response :as resp]
            [ring.util.codec :as codec]
            [ring.middleware.reload :refer :all]
            [compojure.core :as compojure :refer (GET POST defroutes)]
            [org.httpkit.server :refer :all]
            (compojure [route :as route])
            [friend-oauth2.workflow :as oauth2]
            [friend-oauth2.util :as oauth2-util]
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

(defn credential-fn-gh
  [token]
  (let [at (:access-token token)
        basic-infos (github-user-basic-info at)
        username (:username basic-infos)]
    (session/put! :username username)
    {:identity username
     :access-token at
     :roles #{::users}}))

(def ^{:doc "Get the GitHub app configuration from environment variables."
       :private true}
  gh-client-config
  {:client-id (System/getenv "github_client_id")
   :client-secret (System/getenv "github_client_secret")
   :callback {:domain (System/getenv "github_client_domain")
              :path "/github.callback"}})

(def ^{:doc "Default roles for Friend authenticated users."
       :private true}
  friend-config-auth
  {:roles #{:kickhub.core/users}})

(def ^{:doc "Set up the GitHub authentication URI for Friend."
       :private true}
  friend-gh-uri-config
  {:authentication-uri
   {:url "https://github.com/login/oauth/authorize"
    :query {:client_id (:client-id gh-client-config)
            :response_type "code"
            :redirect_uri
            (oauth2-util/format-config-uri gh-client-config)
            :scope "user:email"}}
   :access-token-uri
   {:url "https://github.com/login/oauth/access_token"
    :query {:client_id (:client-id gh-client-config)
            :client_secret (:client-secret gh-client-config)
            :grant_type "authorization_code"
            :redirect_uri
            (oauth2-util/format-config-uri gh-client-config)
            :code ""}}})

(defn- access-token-parsefn
  "Parse the response to get an access-token."
  [response]
  (-> response
      :body
      codec/form-decode
      clojure.walk/keywordize-keys
      :access_token))

(defn- wrap-friend [handler]
  "Wrap friend authentication around handler."
  (friend/authenticate
   handler
   {:allow-anon? true
    :workflows [(workflows/interactive-form
                 :allow-anon? true
                 :login-uri "/login"
                 :default-landing-uri "/initiatives"
                 :credential-fn (partial scrypt-credential-fn load-user))
                (oauth2/workflow
                 {:client-config gh-client-config
                  :uri-config friend-gh-uri-config
                  :login-uri "/github"
                  :default-landing-uri "/initiatives"
                  :credential-fn credential-fn-gh
                  :access-token-parsefn access-token-parsefn
                  :config-auth friend-config-auth})
                ]}))

(defn- four-oh-four []
  (h/html5
   [:head
    [:title "jecode.org -- page non trouvée"]
    (h/include-css "/css/generic.css")]
   [:body
    (e/image {:class "logo"} "/pic/jecode_petit.png")
    [:p "Page non trouvée :-/"]
    [:p "Retour à la "
     (e/link-to "http://jecode.org" "page d'accueil")]]))

(defroutes app-routes
  ;; Generic
  (GET "/" [] (main-tpl {:a "accueil" :jumbo "/md/description" :md "/md/accueil"}))

  ;; Testing ElasticSearch
  (GET "/esr/reset" [] (friend/authorize #{::users} (reset-indexes)))
  (GET "/esr/create" [] (friend/authorize #{::users} (create-indexes)))
  (GET "/esr/add-initiatives" [] (friend/authorize #{::users} (feed-initiatives)))
  (GET "/esr/add-events" [] (friend/authorize #{::users} (feed-events)))

  (GET "/apropos" [] (main-tpl {:a "apropos" :md "/md/apropos"
                                :title "jecode.org - Qui sommes-nous et où allons-nous ?"}))
  (GET "/codeurs" [] (main-tpl {:a "codeurs" :md "/md/liste_codeurs"
                                :title "jecode.org - Témoignages de codeurs"}))
  (GET "/codeurs/:person" [person]
       (main-tpl {:a "codeurs"
                  :md (str "/md/codeurs/" person)
                  :title (str "jecode.org - "
                              (s/join " " (map s/capitalize (s/split person #"\.")))
                              " nous dit pourquoi il faut apprendre à coder !")}))

  ;; Initiatives
  (GET "/initiatives" []
       (main-tpl {:a "initiatives"
                  :md "/md/initiatives"
                  :list-initiatives true
                  :title "jecode.org - La liste des initiatives"}))
  (GET "/initiatives/json" [] (items-json "initiatives"))
  (GET "/initiatives/search/:q" [q]
       (main-tpl {:a "initiatives"
                  :title "jecode.org - Recherche d'initiatives"
                  :list-results {:initiatives-query q}}))
  (GET "/initiatives/map" []
       (main-tpl {:a "initiatives"
                  :showmap "showinits"
                  :title "jecode.org - La carte des initiatives"
                  :md "/md/initiatives_map"}))
  (GET "/initiatives/nouvelle" []
       (friend/authorize
        #{::users}
        (main-tpl {:a "initiatives" :container (new-init-snp)})))
  (POST "/initiatives/:pid/update" {params :params}
        (do (update-initiative params)
            (main-tpl {:a "initiatives"
                       :container "Votre initiative a été mise à jour, merci !"})))
  (GET "/initiatives/:pid/edit" [pid]
       (friend/authorize
        #{::users}
        (main-tpl {:a "initiatives"
                   :container (edit-init-snp
                               [(vec-to-kv-hmap (get-pid-all pid)) pid])})))
  (POST "/initiatives/nouvelle" {params :params}
        (do (create-new-initiative params)
            (main-tpl {:a "initiatives"
                       :container "Votre initiative a été ajoutée, merci !"})))
  
  ;; Events
  (GET "/evenements" []
       (main-tpl {:a "evenements" :md "/md/evenements" :list-events true
                  :title "jecode.org - La liste des événements"}))
  (GET "/evenements/search/:q" [q]
       (main-tpl {:a "evenements"
                  :title "jecode.org - Recherche d'événements"
                  :list-results {:events-query q}}))
  (GET "/evenements/rss" [] (events-rss))
  (GET "/evenements/json" [] (items-json "evenements"))
  (GET "/evenements/map" []
       (main-tpl {:a "evenements" :showmap "showevents"
                  :md "/md/evenements_map"
                  :title "jecode.org - La carte des événements"}))
  (POST "/evenements/:eid/update" {params :params}
        (do (update-event params)
            (main-tpl {:a "evenements"
                       :container "Votre événement a été mis à jour, merci !"})))
  (GET "/evenements/:eid/edit" [eid]
       (friend/authorize
        #{::users}
        (main-tpl {:a "evenements"
                   :container (edit-event-snp
                               [(vec-to-kv-hmap
                                 (get-eid-all eid)) eid])})))
  (GET "/evenements/nouveau" []
       (friend/authorize
        #{::users}
        (main-tpl {:a "evenements" :container (new-event-snp)})))
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

;; Local Variables:
;; eval: (orgstruct-mode 1)
;; orgstruct-heading-prefix-regexp: ";;; "
;; End:

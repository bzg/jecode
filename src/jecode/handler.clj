(ns jecode.handler
  (:require [noir.util.middleware :as middleware]
            [net.cgrand.enlive-html :as html]
            [jecode.util :refer :all]
            [jecode.github :refer :all]
            [noir.session :as session]
            [clojure.string :as s]
            [clojurewerkz.scrypt.core :as sc]
            [jecode.db :refer :all]
            [jecode.model :refer :all]
            [jecode.search :refer :all]
            [jecode.views.templates :refer :all]
            [ring.util.response :as resp]
            [ring.util.codec :as codec]
            [ring.middleware.reload :refer :all]
            [ring.middleware.defaults :refer [site-defaults]]
            [compojure.core :as compojure :refer (GET POST defroutes)]
            [org.httpkit.server :refer :all]
            (compojure [route :as route])
            [friend-oauth2.workflow :as oauth2]
            [friend-oauth2.util :as oauth2-util]
            [cemerick.friend :as friend]
            (cemerick.friend [workflows :as workflows]
                             [credentials :as creds])
            [shoreleave.middleware.rpc :refer [wrap-rpc]])
  (:gen-class))

(derive ::admins ::users)

(defn- load-user
  "Load a user from her username."
  [username]
  (let [admin (System/getenv "jecode_admin")
        uid (or (get-username-uid username) "")
        password (or (get-uid-field uid "p") "")]
    {:identity username :password password
     :roles (if (= username admin) #{::admins} #{::users})}))

(defn- scrypt-credential-fn
  "Variant of `bcrypt-credential-fn` using scrypt."
  [load-credentials-fn {:keys [username password]}]
  (when-let [creds (load-credentials-fn username)]
    (let [password-key (or (-> creds meta ::password-key) :password)]
      (when (try
              (sc/verify password (get creds password-key))
              (catch Exception e (prn "Error" e)))
        (session/put! :username username)
        (if (= username (System/getenv "jecode_admin"))
          (session/put! :admin "yes"))
        (dissoc creds password-key)))))

(defn credential-fn-gh
  [token]
  (let [admin (System/getenv "jecode_admin")
        at (:access-token token)
        basic-infos (github-user-basic-info at)
        username (:email basic-infos)]
    (session/put! :username username)
    (if (= username admin) (session/put! :admin "yes"))
    {:identity username
     :access-token at
     :roles
     (if (= username admin) #{::admins} #{::users})}))

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
  {:roles #{:jecode.core/users}})

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
  (apply str (html/emit* (four-oh-four-snp))))

(defroutes app-routes

  ;; Generic
  (GET "/" [] (main-tpl
               {:a "accueil" :jumbo "/md/description" :md "/md/accueil"
                :title "jecode.org - l'initiation à la programmation : qui, où, pourquoi, comment ?"}))

  ;; Testing ElasticSearch
  (GET "/esr/reset" [] (friend/authorize #{::admins} (reset-indexes)))
  (GET "/esr/create" [] (friend/authorize #{::admins} (create-indexes)))
  (GET "/esr/add-initiatives" [] (friend/authorize #{::admins} (feed-initiatives)))
  (GET "/esr/add-events" [] (friend/authorize #{::admins} (feed-events)))

  ;; Static pages
  (GET "/apropos" []
       (main-tpl {:a "apropos" :md "/md/apropos"
                  :title "jecode.org - Qui sommes-nous et où allons-nous ?"}))

  ;; (GET "/codeweek" []
  ;;      (main-tpl {:a "CodeWeek" :md "/md/codeweek"
  ;;                 :title "jecode.org - inscription à la soirée de la CodeWeek (23/10/2015)"}))

  ;; (GET "/codeweek_merci" []
  ;;      (main-tpl {:a "CodeWeek" :md "/md/codeweek_merci"
  ;;                 :title "jecode.org - Merci pour votre inscription !"}))
  
  (GET "/codeurs" []
       (main-tpl {:a "codeurs" :md "/md/liste_codeurs"
                  :title "jecode.org - Témoignages de codeurs"}))
  (GET "/codeurs/:person" [person]
       (main-tpl {:a "codeurs"
                  :md (str "/md/codeurs/" person)
                  :title (str "jecode.org - "
                              (s/join " " (map s/capitalize (s/split person #"\.")))
                              " nous dit pourquoi il faut apprendre à coder !")}))

  ;; Carte
  (GET "/carte" []
       (main-tpl {:a "carte"
                  :title "jecode.org - La carte des initiatives et des événements"
                  :maplinks "yes"
                  :md (str "/md/carte")
                  }))
       
  ;; Initiatives
  (GET "/initiatives" []
       (main-tpl {:a "initiatives"
                  :md "/md/initiatives"
                  :list-initiatives true
                  :title "jecode.org - La liste des initiatives"}))
  (GET "/initiatives/json" {params :params} (items-json "initiatives" (:tag params)))
  (GET "/initiatives/search/:q" [q]
       (main-tpl {:a "initiatives"
                  :title "jecode.org - Recherche d'initiatives"
                  :list-results {:initiatives-query q}}))
  
  (GET "/initiatives/nouvelle" []
       (friend/authorize
        #{::users}
        (main-tpl {:a "initiatives"
                   :formjs "validate_event_init"
                   :container (new-init-snp)})))
  (POST "/initiatives/:pid/update" {params :params}
        (do (update-initiative params)
            (main-tpl {:a "initiatives"
                       :container "Votre initiative a été mise à jour, merci !"})))
  (GET "/initiatives/:pid/edit" [pid]
       (friend/authorize
        #{::users}
        (main-tpl {:a "initiatives"
                   :formjs "validate_event_init"
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
  (GET "/evenements/json" {params :params} (items-json "evenements" (:tag params)))
  (POST "/evenements/:eid/update" {params :params}
        (do (update-event params)
            (main-tpl {:a "evenements"
                       :container "Votre événement a été mis à jour, merci !"})))
  (GET "/evenements/:eid/edit" [eid]
       (friend/authorize
        #{::users}
        (main-tpl {:a "evenements"
                   :formjs "validate_event_init"
                   :container (edit-event-snp
                               [(vec-to-kv-hmap
                                 (get-eid-all eid)) eid])})))
  (GET "/evenements/nouveau" []
       (friend/authorize
        #{::users}
        (main-tpl {:a "evenements"
                   :formjs "validate_event_init"
                   :container (new-event-snp)})))
  (POST "/evenements/nouveau" {params :params}
        (do (create-new-event params)
            (main-tpl {:a "evenements"
                       :container "Votre événement a été ajouté, merci !"})))

  ;; Login
  (GET "/login" []
       (main-tpl {:a "accueil" :container (login-snp)
                  :formjs "validate_email"
                  :title "jecode.org - connexion"}))

  ;; Rappel de mot de passe
  (GET "/rappel" []
       (main-tpl {:a "accueil" :container (rappel-snp)
                  :title "jecode.org - rappel de mot de passe"}))

  (POST "/rappel" {params :params}
        (main-tpl {:a "accueil"
                   :container (send-new-password (:email params))
                   :title "jecode.org - rappel de mot de passe"}))

  (GET "/logout" req (friend/logout* (resp/redirect (str (:context req) "/"))))
  (GET "/inscription" []
       (main-tpl {:a "accueil" :container (register-snp)
                  :formjs "validate_email"
                  :title "jecode.org - inscription"}))
  (POST "/inscription" {params :params}
        (do (create-new-user params)
            (main-tpl {:a "accueil"
                       :container "Merci !  Vous pouvez désormais vous connecter."
                       :title "jecode.org - vous êtes inscrit !"})))
  (GET "/activer/:authid" [authid]
       (do (activate-user authid)
           (main-tpl {:a "accueil" :container "Utilisateur actif !"
                      :title "jecode.org - utilisateur activé !"})))

  ;; Other routes
  (route/resources "/")
  (route/not-found (four-oh-four)))

;; Timeout sessions after 30 minutes
(def session-defaults
  {:timeout (* 60 30)
   :timeout-response (resp/redirect "/")})

(defn- mk-defaults
       "set to true to enable XSS protection"
       [xss-protection?]
       (-> site-defaults
           (update-in [:session] merge session-defaults)
           (assoc-in [:security :anti-forgery] xss-protection?)))

(def app
  (wrap-reload
   (middleware/app-handler
    [(wrap-friend (wrap-rpc app-routes))]
    :ring-defaults (mk-defaults false)
    ;; add access rules here
    :access-rules [])))
   
(defn -main [& args] (run-server #'app {:port 8080}))

;; Local Variables:
;; eval: (orgstruct-mode 1)
;; orgstruct-heading-prefix-regexp: ";;; "
;; End:

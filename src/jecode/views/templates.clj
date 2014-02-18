(ns jecode.views.templates
  (:require [net.cgrand.enlive-html :as html]
            [noir.session :as session]
            [jecode.util :refer :all]))

;;; * Utility functions

(defmacro maybe-content
  "Maybe content."
  ([expr] `(if-let [x# ~expr] (html/content x#) identity))
  ([expr & exprs] `(maybe-content (or ~expr ~@exprs))))

;;; * Generic snippets

(html/defsnippet ^{:doc "Snippet for the login form."}
  login-snp "jecode/views/html/forms.html" [:#login] [])

(html/defsnippet ^{:doc "Snippet for the register form."}
  register-snp "jecode/views/html/forms.html" [:#register] [])

(html/defsnippet ^{:doc "Snippet for the register form."}
  new-init-snp "jecode/views/html/forms.html" [:#submit-initiative] [])

;;; * Template

(html/deftemplate ^{:doc "Main index template"}
  main-tpl "jecode/views/html/base.html"
  [{:keys [container md jumbo map a]}]
  [:#container2] (if md (html/html-content
                         (md->html jumbo) (md->html md))
                     (maybe-content container))
  [:#accueil] (html/set-attr :class (if (= a "accueil") "active"))
  [:#codeurs] (html/set-attr :class (if (= a "codeurs") "active"))
  [:#carte] (html/set-attr :class (if (= a "carte") "active"))
  [:#apropos] (html/set-attr :class (if (= a "apropos") "active"))
  [:#map] (cond (= map "show") (html/set-attr :style "width:70%")
                (= map "new") (html/set-attr :style "max-height:300px;")
                :else (html/set-attr :style "display:none"))
  [:#log :a#login] (if (session/get :username)
                  (html/do-> (html/set-attr :href "/logout")
                             (maybe-content "Déconnexion"))
                  (html/do-> (html/set-attr :href "/login")
                             (maybe-content "Connexion")))
  [:#log :a#signin] (if (session/get :username)
                      (html/set-attr :style "display:none")
                      (maybe-content "Inscription"))
  [:#mapjs] (html/html-content
             (cond (= map "show") "<script src=\"/js/index.js\" type=\"text/javascript\"></script>"
                   (= map "new") "<script src=\"/js/newinit.js\" type=\"text/javascript\"></script>"
                   :else "")))

;;; * Local variables

;; Local Variables:
;; eval: (orgstruct-mode 1)
;; orgstruct-heading-prefix-regexp: ";;; "
;; End:

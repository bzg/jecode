(ns jecode.views.templates
  (:require [net.cgrand.enlive-html :as html]
            [noir.session :as session]
            [jecode.util :refer :all]
            [jecode.model :refer :all]))

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

(html/defsnippet ^{:doc "Snippet for the register form."}
  new-event-snp "jecode/views/html/forms.html" [:#submit-event] [])

;;; * Template

(html/defsnippet my-event "jecode/views/html/base.html"
  [:#list :ul] [arg]
  [:li :a] (html/do->
            (html/set-attr :href (:url arg))
            (html/set-attr :title (:desc arg))
            (html/content (:name arg))))

(html/defsnippet my-initiative "jecode/views/html/base.html"
  [:#list :ul] [arg]
  [:li :a] (html/do->
            (html/set-attr :href (:url arg))
            (html/set-attr :title (:desc arg))
            (html/content (:name arg))))

(html/deftemplate ^{:doc "Main index template"}
  main-tpl "jecode/views/html/base.html"
  [{:keys [container md jumbo a showmap title list-events list-initiatives]}]
  [:head :title] (html/content title)
  [:#container2] (if md (html/html-content
                         (md->html jumbo) (md->html md))
                     (maybe-content container))
  [:#accueil] (html/set-attr :class (if (= a "accueil") "active"))
  [:#codeurs] (html/set-attr :class (if (= a "codeurs") "active"))
  [:#initiatives] (html/set-attr :class (if (= a "initiatives") "active"))
  [:#evenements] (html/set-attr :class (if (= a "evenements") "active"))
  [:#apropos] (html/set-attr :class (if (= a "apropos") "active"))
  [:#map] (cond (or (= showmap "showinits") (= showmap "showevents"))
                (html/set-attr :style "width:70%")
                (or (= showmap "newinit") (= showmap "newevent"))
                (html/set-attr :style "max-height:500px;")
                :else (html/set-attr :style "display:none"))
  [:#log :a#login] (if (session/get :username)
                  (html/do-> (html/set-attr :href "/logout")
                             (maybe-content "Déconnexion"))
                  (html/do-> (html/set-attr :href "/login")
                             (maybe-content "Connexion")))
  [:#log :a#signin] (if (session/get :username)
                      (html/set-attr :style "display:none")
                      (maybe-content "Inscription"))
  [:#list] (cond list-events
                 (html/content (map #(my-event %) (get-events-for-map)))
                 list-initiatives
                 (html/content (map #(my-initiative %) (get-initiatives-for-map))))
  [:#mapjs]
  (html/html-content
   (cond (= showmap "showinits") "<script src=\"/js/showinits.js\" type=\"text/javascript\"></script>"
         (= showmap "newinit") "<script src=\"/js/newinit.js\" type=\"text/javascript\"></script>"
         (= showmap "showevents") "<script src=\"/js/showevents.js\" type=\"text/javascript\"></script>"
         (= showmap "newevent") "<script src=\"/js/newevent.js\" type=\"text/javascript\"></script>"
         :else "")))

;;; * Local variables

;; Local Variables:
;; eval: (orgstruct-mode 1)
;; orgstruct-heading-prefix-regexp: ";;; "
;; End:

(ns jecode.views.templates
  (:require [net.cgrand.enlive-html :as html]
            [noir.session :as session]
            [jecode.util :refer :all]
            [jecode.search :refer :all]
            [jecode.model :refer :all]))

;;; * Utility functions

(defmacro maybe-content
  "Maybe content."
  ([expr] `(if-let [x# ~expr] (html/content x#) identity))
  ([expr & exprs] `(maybe-content (or ~expr ~@exprs))))

;;; * Generic snippets

(html/defsnippet ^{:doc "Snippet for the login form."}
  login-snp "jecode/views/html/forms.html" [:#login_form] [])

(html/defsnippet ^{:doc "Snippet for the register form."}
  register-snp "jecode/views/html/forms.html" [:#register_form] [])

(html/defsnippet ^{:doc "Snippet for the initiative form."}
  new-init-snp "jecode/views/html/forms.html" [:#submit-initiative] [])

(html/defsnippet ^{:doc "Snippet for the register form."}
  edit-init-snp "jecode/views/html/forms.html" [:#submit-initiative]
  [[{:keys [name url logourl contact location twitter location desc tags]} pid]]
  [:#submit-initiative] (html/set-attr :action (format "/initiatives/%s/update" pid))
  [:#event_title] (html/content "Mise à jour de l'initiative")
  [:#pname] (html/set-attr :value name)
  [:#purl] (html/set-attr :value url)
  [:#logourl] (html/set-attr :value logourl)
  [:#contact] (html/set-attr :value contact)
  [:#twitter] (html/set-attr :value twitter)
  [:#plocation] (html/set-attr :value location)
  [:#ptags] (html/set-attr :value tags)
  [:#pdesc] (html/content desc)
  [:#submit-init-btn] (html/content "Mettre à jour"))

(html/defsnippet ^{:doc "Snippet for the event form."}
  edit-event-snp "jecode/views/html/forms.html" [:#submit-event]
  [[{:keys [name url orga contact desc hdate_start hdate_end location tags]} eid]]
  [:#submit-event] (html/set-attr :action (format "/evenements/%s/update" eid))
  [:#event_title] (html/content "Mise à jour de l'événement")
  [:#ename] (html/set-attr :value name)
  [:#eurl] (html/set-attr :value url)
  [:#eorga] (html/set-attr :value orga)
  [:#econtact] (html/set-attr :value contact)
  [:#edate_start] (html/set-attr :value hdate_start)
  [:#edate_end] (html/set-attr :value hdate_end)
  [:#elocation] (html/set-attr :value location)
  [:#etags] (html/set-attr :value tags)
  [:#edesc] (html/content desc)
  [:#submit-event-btn] (html/content "Mettre à jour"))

(html/defsnippet ^{:doc "Snippet for the register form."}
  new-event-snp "jecode/views/html/forms.html" [:#submit-event] [])

;;; * Template

(html/defsnippet my-result "jecode/views/html/base.html"
  [:#list :ul] [arg]
  [:li :a.item] (html/do->
                 (html/set-attr :href (:url arg))
                 (html/set-attr :title (:desc arg))
                 (html/content (:name arg))))

(html/defsnippet my-event "jecode/views/html/base.html"
  [:#list :ul] [arg]
  [:li :a.item] (html/do->
                 (html/set-attr :href (:url arg))
                 (html/set-attr :title (:desc arg))
                 (html/content (:name arg)))
  [:li :a.edit] (html/do->
                 (html/content (if (:isadmin arg) " (éditer)"))
                 (html/set-attr :href (str "/evenements/" (:eid arg) "/edit"))))

(html/defsnippet my-initiative "jecode/views/html/base.html"
  [:#list :ul] [arg]
  [:li :a.item] (html/do->
                 (html/set-attr :href (:url arg))
                 (html/set-attr :title (:desc arg))
                 (html/content (:name arg)))
  [:li :a.edit] (html/do->
                 (html/content (if (:isadmin arg) " (éditer)"))
                 (html/set-attr :href (str "/initiatives/" (:pid arg) "/edit"))))

(html/deftemplate ^{:doc "Main index template"}
  main-tpl "jecode/views/html/base.html"
  [{:keys [container md jumbo a showmap title formjs
           list-events list-initiatives list-results]}]
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
                :else (html/set-attr :style "display:none"))
  [:#log :a#github] (if (empty? (session/get :username))
                      (html/do-> (html/set-attr :href "/github")
                                 (maybe-content "Connexion Github"))
                      (html/set-attr :style "display:none;"))
  [:#log :a#connexion] (if (session/get :username)
                     (html/do-> (html/set-attr :href "/logout")
                                (maybe-content "Déconnexion"))
                     (html/do-> (html/set-attr :href "/login")
                                (maybe-content "Connexion email")))
  [:#log :a#signin] (if (session/get :username)
                      (html/set-attr :style "display:none;")
                      (maybe-content "Inscription"))
  [:#list] (cond
            (:initiatives-query list-results)
            (html/content
             (map #(my-result %)
                  (map :_source (get-in (query-initiatives
                                         (:initiatives-query list-results)) [:hits :hits]))))
            (:events-query list-results)
            (html/content
             (map #(my-result %)
                  (map :_source (get-in (query-events
                                         (:events-query list-results)) [:hits :hits]))))
            
            list-events
            (html/content (map #(my-event %) (get-events)))
            list-initiatives
            (html/content (map #(my-initiative %) (get-initiatives))))
  [:#mapjs]
  (html/html-content
   (cond (= showmap "showinits")
         "<script src=\"/js/showinits.js\" type=\"text/javascript\"></script>"
         (= showmap "showevents")
         "<script src=\"/js/showevents.js\" type=\"text/javascript\"></script>"
         :else ""))
  [:#formjs]
  (html/html-content
   (cond
    (= formjs "validate_email")
    "<script src=\"/js/validate_email.js\" type=\"text/javascript\"></script>"
    (= formjs "validate_event_init")
    "<script src=\"/js/validate_event_init.js\" type=\"text/javascript\"></script>"
    :else "")))

;;; * Local variables

;; Local Variables:
;; eval: (orgstruct-mode 1)
;; orgstruct-heading-prefix-regexp: ";;; "
;; End:

(ns jecode.views.templates
  (:require [net.cgrand.enlive-html :as html]))

;;; * Utility functions

(defmacro maybe-content
  "Maybe content."
  ([expr] `(if-let [x# ~expr] (html/content x#) identity))
  ([expr & exprs] `(maybe-content (or ~expr ~@exprs))))

;;; * Template

(html/deftemplate ^{:doc "Main index template"}
  main-tpl "jecode/views/enlive-html/base.html"
  [{:keys [container]}]
  [:#container2] (maybe-content container))

(html/deftemplate ^{:doc "Main index template"}
  map-tpl "jecode/views/enlive-html/map.html"
  [{:keys [container]}]
  [:#container2] (maybe-content container))

(html/deftemplate ^{:doc "Main index template"}
  init-tpl "jecode/views/enlive-html/init.html"
  [{:keys [container]}]
  [:#container2] (maybe-content container))

;;; * Generic snippets

(html/defsnippet ^{:doc "Snippet for the login form."}
  login-snp "jecode/views/enlive-html/forms.html" [:#login] [])

(html/defsnippet ^{:doc "Snippet for the register form."}
  register-snp "jecode/views/enlive-html/forms.html" [:#register] [])

(html/defsnippet ^{:doc "Snippet for the register form."}
  new-init-snp "jecode/views/enlive-html/forms.html" [:#submit-initiative] [])

;;; * Views

(defn login-page []
  (main-tpl {:container (login-snp)}))

(defn register-page []
  (main-tpl {:container (register-snp)}))

(defn newinit-page []
  (init-tpl {:container (new-init-snp)}))

(defn map-page []
  (map-tpl {:container "Map"}))

;;; * Local variables

;; Local Variables:
;; eval: (orgstruct-mode 1)
;; orgstruct-heading-prefix-regexp: ";;; "
;; End:

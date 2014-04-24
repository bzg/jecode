(ns jecode.validate_event
  (:use [domina :only [by-id value]]))

(def msg-address "Précisez une adresse physique pour que l'événement apparaisse sur la carte.")
(def msg-url "L'URL de l'événement semble incorrecte.")

(defn validate-form []
  (let [location (by-id "elocation")
        url (by-id "eurl")]
    (cond (<= (count (value location)) 10)
          (do (js/alert msg-address) false)
          (not (re-find #"^http://" (value url)))
          (do (js/alert msg-url) false)
          :else true)))

(defn init []
  (if (and js/document (.-getElementById js/document))
    (let [form (by-id "submit-event")]
      (set! (.-onsubmit form) validate-form))))

(set! (.-onload js/window) init)

(ns jecode.validate_event_init
  (:use [domina :only [by-id value]]))

(def msg-event-address "Précisez une adresse physique pour que l'événement apparaisse sur la carte.")
(def msg-event-url "L'URL de l'événement semble incorrecte.")
(def msg-init-address "Précisez une adresse physique pour que l'initiative apparaisse sur la carte.")
(def msg-init-url "L'URL de l'initiative semble incorrecte.")

(defn- validate-form-event []
  (let [location (by-id "elocation")
        url (by-id "eurl")]
    (cond (<= (count (value location)) 10)
          (do (js/alert msg-event-address) false)
          (not (re-find #"^https?://" (value url)))
          (do (js/alert msg-event-url) false)
          :else true)))

(defn- validate-form-init []
  (let [location (by-id "plocation")
        url (by-id "purl")]
    (cond (<= (count (value location)) 10)
          (do (js/alert msg-init-address) false)
          (not (re-find #"^https?://" (value url)))
          (do (js/alert msg-init-url) false)
          :else true)))

(defn- init []
  (if (and js/document (.-getElementById js/document))
    (let [loc (.-location js/window)
          eventp (= "evenements"
                    (second (re-find #"https://[^/]+/([^/]+)" loc)))]
    (let [form (by-id (if eventp "submit-event" "submit-initiative"))]
      (set! (.-onsubmit form)
            (if eventp validate-form-event validate-form-init))))))

(set! (.-onload js/window) init)

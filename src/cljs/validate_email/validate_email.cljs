(ns jecode.validate_email
  (:use [domina :only [by-id value]]))

(def msg "Merci de précisez une adresse mail et un mot de passe.")

(defn- validate-form-login []
  (let [username (by-id "username")
        password (by-id "password")]
    (if (and (> (count (value username)) 0)
             (> (count (value password)) 0))
      true
      (do (js/alert msg) false))))

(defn- validate-form-register []
  (let [email (by-id "email")
        password (by-id "password")]
    (if (and (> (count (value email)) 0)
             (> (count (value password)) 0))
      true
      (do (js/alert msg) false))))

(defn- init []
  (if (and js/document (.-getElementById js/document))
    (let [loc (.-location js/window)
          loginp (= "login" (re-find #"[^/]+$" loc))
          form (by-id (if loginp "login_form" "register_form"))]
      (set! (.-onsubmit form)
            (if loginp
              validate-form-login
              validate-form-register)))))

(set! (.-onload js/window) init)

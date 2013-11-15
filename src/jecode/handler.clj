(ns jecode.handler
  (:require [noir.util.middleware :as middleware]
            [jecode.util :refer :all]
            [jecode.views.layout :as layout]
            [compojure.core :as compojure :refer (GET POST defroutes)]
            (compojure [route :as route])))

(defn render-page [tpl page]
  (layout/render
   tpl {:content (md->html (str "/md/" page))}))

(defroutes app-routes 
  (GET "/" [] (render-page "home.html" "accueil"))
  (GET "/apropos" [] (render-page "about.html" "apropos"))
  (GET "/codeurs" [] (render-page "person.html" "codeurs"))
  (GET "/codeurs/:person" [person] (render-page "person.html" person))
  (route/resources "/")
  (route/not-found (render-page "404.html" "404")))

(def app (middleware/app-handler [app-routes]))

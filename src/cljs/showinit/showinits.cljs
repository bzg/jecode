(ns jecode.showinits
  (:require [mrhyde.extend-js]
            [blade :refer [L]]
            [shoreleave.remotes.http-rpc :refer [remote-callback]])
  (:require-macros [shoreleave.remotes.macros :as macros]))

(blade/bootstrap)

(def mymap (-> L .-mapbox (.map "map" "bzg.i8bb9pdk")
               (.setView [45 3.215] 6)))

(defn- add-initiatives []
  (let [markers (L/MarkerClusterGroup.)]
    (macros/rpc
     (get-for-map "initiatives") [res]
     (.addLayers
      markers
      (map #(let [{:keys [name url logourl desc location
                          contact twitter lat lon pid isadmin]} %
                          join (when isadmin "(Inscrit par vous.)")
                          icon ((get-in L [:mapbox :marker :icon])
                                {:marker-symbol ""
                                 :marker-color "0044FF"})
                          marker (-> L (.marker (L/LatLng. lat lon) {:icon icon}))]
              (.bindPopup marker (str "<a target=\"new\" href=\"" url "\">"
                                      name "</a><br/>"
                                      ;; "<img src=\"" logourl "\">"
                                      location "<br/>"
                                      contact "</br>"
                                      twitter "</br>"
                                      desc "<br/>"
                                      join))
              marker)
           res)))
    (.addLayer mymap markers)))

(set! (.-onload js/window) add-initiatives)

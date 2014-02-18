(ns jecode.map
  (:require [mrhyde.extend-js]
            [blade :refer [L]]
            [shoreleave.remotes.http-rpc :refer [remote-callback]])
  (:require-macros [shoreleave.remotes.macros :as macros]))

(blade/bootstrap)

(def mymap (-> L .-mapbox (.map "map" "examples.map-9ijuk24y")
               (.setView [45 3.215] 6)))

(defn- add-initiatives []
  (let [markers (L/MarkerClusterGroup.)]
    (macros/rpc
     (get-initiatives) [res]
     (.addLayers
      markers
      (map #(let [{:keys [name url lat lon desc pid isadmin]} %
                  join (when isadmin "(Inscrit par vous.)")
                  icon ((get-in L [:mapbox :marker :icon])
                        {:marker-symbol ""
                         :marker-color "0044FF"})
                  marker (-> L (.marker (L/LatLng. lat lon) {:icon icon}))]
              (.bindPopup marker (str "<a target=\"new\" href=\"" url "\">"
                                      name "</a><br/>"
                                      desc "<br/>"
                                      join))
              marker)
           res)))
    (.addLayer mymap markers)))

(set! (.-onload js/window) add-initiatives)

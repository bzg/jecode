(ns jecode.showinits
  (:require [mrhyde.extend-js]
            [clojure.browser.event :as event]
            [blade :refer [L]]
            [shoreleave.remotes.http-rpc :refer [remote-callback]]
            [domina :as d])
  (:require-macros [shoreleave.remotes.macros :as macros]))

(blade/bootstrap)

(def mymap (-> L .-mapbox (.map "map" "bzg.i8bb9pdk")
               (.setView [45 3.215] 5)))

(def markers-initiatives (L/MarkerClusterGroup.))
(def markers-events (L/MarkerClusterGroup.))

(defn- add-initiatives []
  (macros/rpc
   (get-for-map "initiatives") [res]
   (.addLayers
    markers-initiatives
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
  (.addLayer mymap markers-initiatives))

(defn- add-events []
  (macros/rpc
   (get-for-map "evenements") [res]
   (.addLayers
    markers-events
    (map #(let [{:keys [name url lat lon desc hdate_start hdate_end
                        orga location contact isadmin]} %
                        join (when isadmin "(Inscrit par vous.)")
                        icon ((get-in L [:mapbox :marker :icon])
                              {:marker-symbol ""
                               :marker-color "04B431"})
                        marker (-> L (.marker (L/LatLng. lat lon) {:icon icon}))]
            (.bindPopup marker (str "<a target=\"new\" href=\"" url "\">"
                                    name "</a><br/>"
                                    orga "<br/>"
                                    contact "<br/>"
                                    desc "<br/>"
                                    location "<br/>"
                                    hdate_start "<br/>"
                                    hdate_end "<br/>"
                                    join))
            marker)
         res)))
  (.addLayer mymap markers-events))

(defn- clear-initiatives []
  (do (.removeLayer mymap markers-initiatives)
      (def markers-initiatives (L/MarkerClusterGroup.))))

(defn- clear-events []
  (do (.removeLayer mymap markers-events)
      (def markers-events (L/MarkerClusterGroup.))))

(if (d/by-id "display_events")
  (event/listen
   (d/by-id "display_events") "click"
   (fn [] (if (not (.hasLayer mymap markers-events))
            (add-events)
            (clear-events)))))

(if (d/by-id "display_initiatives")
  (event/listen
   (d/by-id "display_initiatives") "click"
   (fn [] (if (not (.hasLayer mymap markers-initiatives))
            (add-initiatives)
            (clear-initiatives)))))

;; (set! (.-onload js/window) add-initiatives)

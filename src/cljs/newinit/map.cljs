(ns jecode.map
  (:require [mrhyde.extend-js]
            [blade :refer [L]]
            [domina :as dom]))

(blade/bootstrap)

(def mymap (-> L .-mapbox (.map "map" "examples.map-9ijuk24y")
               (.setView [45 3.215] 6)))

(defn- update-form [e]
  (let [flat (dom/by-id "lat")
        flon (dom/by-id "lon")
        ll (.-latlng e)
        p (-> L .popup)]
    (-> p (.setLatLng ll)
        (.setContent "Ici !")
        (.openOn mymap))
    (set! (.-value flat) (.-lat ll))
    (set! (.-value flon) (.-lng ll))))

(.on mymap "click" update-form)

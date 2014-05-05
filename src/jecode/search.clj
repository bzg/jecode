(ns jecode.search
  (:require
   [jecode.model :refer :all]
   [clojurewerkz.elastisch.rest       :as esr]
   [clojurewerkz.elastisch.rest.index :as esi]
   [clojurewerkz.elastisch.rest.document :as esd]
   [clojurewerkz.elastisch.query         :as q]
   [clojurewerkz.elastisch.rest.response :as esrsp]))

(defn create-indexes []
  (do
    (esr/connect! "http://127.0.0.1:9200")
    (let [inits
          {"initiative"
           {:properties {:name      {:type "string" :store "yes"}
                         :desc      {:type "string" :store "yes"}
                         :location  {:type "string" :store "yes"}
                         :url       {:type "string"}
                         :contact   {:type "string" :store "yes"}
                         :twitter   {:type "string" :store "yes"}
                         }}}
          events
          {"event"
           {:properties {:name      {:type "string" :store "yes"}
                         :desc      {:type "string" :store "yes"}
                         :location  {:type "string" :store "yes"}
                         :url       {:type "string"}
                         :contact   {:type "string" :store "yes"}
                         :orga      {:type "string" :store "yes"}
                         }}}]
      ;; Create the indexes
      (esi/create "initiatives" :mappings inits)
      (esi/create "events" :mappings events))))

(defn reset-indexes []
  (do (esr/connect! "http://127.0.0.1:9200")
      (esi/delete "initiatives")
      (esi/delete "events")))

(defn feed-initiatives []
  (do (esr/connect! "http://127.0.0.1:9200")
      (for [i (doall (get-initiatives-for-map))]
        (esd/create "initiatives" "initiative" i))))

(defn feed-events []
  (do (esr/connect! "http://127.0.0.1:9200")
      (for [i (doall (get-events-for-map))]
        (esd/create "events" "event" i))))

(defn query-initiatives [req]
  (esr/connect! "http://127.0.0.1:9200")
  (esd/search "initiatives" "initiative" :query (q/query-string :query req)))

(defn query-events [req]
  (esr/connect! "http://127.0.0.1:9200")
  (esd/search "events" "event" :query (q/query-string :query req)))

;; Local Variables:
;; eval: (orgstruct-mode 1)
;; orgstruct-heading-prefix-regexp: ";;; "
;; End:

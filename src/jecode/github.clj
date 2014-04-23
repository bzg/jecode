(ns jecode.github
  (:require [cheshire.core :as json]
            [noir.session :as session]
            [clj-http.client :as http]))

(defn- github-api-response
  "Get Github API response."
  [access-token api]
  (let [url (format "https://api.github.com%s?access_token=%s" api access-token)]
    (json/parse-string (:body (http/get url {:accept :json})) true)))

(defn github-user-basic-info
  "Get user GitHub basic info."
  [access-token]
  (let [infos (github-api-response access-token "/user")]
    (assoc {}
      :username (:login infos)
      :email (:email infos)
      :picurl (str "http://www.gravatar.com/avatar/" (:gravatar_id infos)))))

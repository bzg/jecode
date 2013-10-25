(defproject
  jecode "0.0.1"
  :url "http://github.com/bzg/jecode"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :immutant {:context-path "/"}
  :dependencies
  [[org.clojure/clojure "1.5.1"]
   [lib-noir "0.7.1"]
   [selmer "0.4.8"]
   [compojure "1.1.5"]
   [ring-server "0.3.0"]
   [markdown-clj "0.9.33"]]
  :url "http://jecode.org"
  :description "jecode.org"
  :min-lein-version "2.0.0")

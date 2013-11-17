(defproject
  jecode "0.0.1"
  :url "http://github.com/bzg/jecode"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :immutant {:context-path "/" :nrepl-port 4005}
  :dependencies
  [[org.clojure/clojure "1.5.1"]
   [lib-noir "0.7.5"]
   [selmer "0.5.2"]
   [compojure "1.1.6"]
   [ring-server "0.3.1"]
   [markdown-clj "0.9.35"]]
  :url "http://jecode.org"
  :description "jecode.org"
  :min-lein-version "2.0.0")

(defproject
  jecode "0.0.2"
  :url "http://github.com/bzg/jecode"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :immutant {:context-path "/" :nrepl-port 4005}
  :dependencies
  [[org.clojure/clojure "1.5.1"]
   [lib-noir "0.8.0"]
   [selmer "0.5.9"]
   [domina "1.0.2"]
   [compojure "1.1.6"]
   [clojurewerkz/scrypt "1.1.0"]
   [ring-server "0.3.1"]
   ;; [org.clojure/clojurescript "0.0-2156"]
   [org.clojure/clojurescript "0.0-1859"]
   [net.drib/blade "0.1.0"]
   [com.cemerick/friend "0.2.0" :exclusions [org.clojure/core.cache]]
   [com.taoensso/carmine "2.4.6"]
   [shoreleave/shoreleave-remote-ring "0.3.0"]
   [shoreleave/shoreleave-remote "0.3.0"]
   [com.draines/postal "1.11.1"]
   [digest "1.4.3"]
   [enlive "1.1.5"]
   [markdown-clj "0.9.41"]]
  :description "jecode.org"
  :min-lein-version "2.0.0"
  :plugins
  [[lein-cljsbuild "0.3.2"]]
  :cljsbuild {:builds
              [{:source-paths ["src/cljs/index"]
                :compiler
                {:output-to "resources/public/js/index.js"
                 :optimizations :whitespace
                 :pretty-print false}}
               {:source-paths ["src/cljs/newinit"]
                :compiler
                {:output-to "resources/public/js/newinit.js"
                 :optimizations :whitespace
                 :pretty-print false}}]})

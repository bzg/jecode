(defproject
  jecode "0.0.1"
  :url "http://github.com/bzg/jecode"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :immutant {:context-path "/"}
  :dependencies
  [[org.clojure/clojure "1.5.1"]
   [org.clojure/clojurescript "0.0-1934"]
   [net.drib/blade "0.1.0"]
   [lib-noir "0.7.1"]
   [selmer "0.4.8"]
   [compojure "1.1.5"]
   [ring-server "0.3.0"]
   [shoreleave/shoreleave-remote-ring "0.3.0"]
   [shoreleave/shoreleave-remote "0.3.0"]
   [com.draines/postal "1.11.0"]
   [digest "1.4.3"]
   [enlive "1.1.4"]
   [markdown-clj "0.9.33"]
   ;; [domina "1.0.2"]
   ;; [com.cemerick/friend "0.2.0"]
   ;; [com.taoensso/carmine "2.3.1"]
   ;; [cheshire "5.2.0"]
   ;; [com.taoensso/tower "2.0.0-beta1"]
   ]
  :url "http://jecode.org"
  ;; :plugins
  ;; [[lein-cljsbuild "0.3.2"]
  ;;  [lein-ring "0.8.5"]]
  ;; :cljsbuild {:builds
  ;;             [{:source-paths ["src/cljs/index"]
  ;;               :compiler
  ;;               {:output-to "resources/public/js/index.js"
  ;;                :optimizations :whitespace
  ;;                :pretty-print false}}
  ;;              {:source-paths ["src/cljs/newinit"]
  ;;               :compiler
  ;;               {:output-to "resources/public/js/newinit.js"
  ;;                :optimizations :whitespace
  ;;                :pretty-print false}}]}
  :description "jecode.org"
  :min-lein-version "2.0.0")

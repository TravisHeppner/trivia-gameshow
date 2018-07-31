(defproject gameshow "0.1.0-SNAPSHOT"
  :description "This is a trivia gameshow."
  :url "https://github.com/TravisHeppner/gameshow"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :min-lein-version "2.5.3"
  
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.8.34"]
                 [org.clojure/core.async "0.2.374"]
                 [org.clojure/core.memoize "0.5.8"]
                 [reagent "0.5.1"]
                 [com.taoensso/sente "1.8.1"]
                 [com.taoensso/timbre "4.10.0"]
                 [com.taoensso/encore "2.96.0"]

                 [environ "1.0.2"]
                 [http-kit "2.1.19"]
                 [compojure "1.5.0"]
                 [ring "1.4.0"]
                 [ring/ring-defaults "0.2.0"]
                 [ring-cors "0.1.7"]]

  :plugins [[lein-figwheel "0.5.0-6"]
            [lein-cljsbuild "1.1.7"]
            [lein-environ "1.0.2"]]

  :source-paths ["src"]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"]

  :main gameshow.server.main

  :uberjar-name "gameshow-standalone.jar"

  :profiles
  {:dev {:env {:dev? "true"}
         :cljsbuild {:builds
                     [{:id "dev"
                       :source-paths ["src" "dev"]
                       :figwheel {}
                       :compiler {:main gameshow.main
                                  :asset-path "js/compiled/out"
                                  :output-to "resources/public/js/compiled/gameshow.js"
                                  :output-dir "resources/public/js/compiled/out"
                                  :source-map-timestamp true}}]}}
   :uberjar {:hooks [leiningen.cljsbuild]
             :aot :all
             :cljsbuild {:builds
                         [{:id "min"
                           :source-paths ["src" "prod"]
                           :compiler {:main gameshow.main
                                      :output-to "resources/public/js/compiled/gameshow.js"
                                      :optimizations :advanced
                                      :pretty-print false}}]}}}



  :figwheel {:css-dirs ["resources/public/css"] })

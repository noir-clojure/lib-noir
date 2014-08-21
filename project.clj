(defproject lib-noir "0.8.5"
  :description "Libraries from Noir for your enjoyment."
  :url "https://github.com/noir-clojure/lib-noir"
  :license {:name "Eclipse Public License - v 1.0"
            :url "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [cheshire "5.3.1"]
                 [ring "1.3.0"]
                 [compojure "1.1.8"]
                 [clout "1.2.0"]
                 [hiccup "1.0.5"]
                 [ring-middleware-format "0.4.0"]
                 [ring/ring-session-timeout "0.1.0"]
                 [org.mindrot/jbcrypt "0.3m"]]
  :plugins [[codox "0.8.10"]
            [lein-ancient "0.5.5"]]
  :codox {:output-dir "doc"})

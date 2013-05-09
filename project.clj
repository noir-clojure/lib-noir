(defproject lib-noir "0.5.3"
  :description "Libraries from Noir for your enjoyment."
  :url "http://webnoir.org"
  :license {:name "Eclipse Public License - v 1.0"
            :url "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [cheshire "5.1.1"]
                 [ring "1.1.8"]
                 [compojure "1.1.5"]
                 [clout "1.1.0"]
                 [hiccup "1.0.3"]
                 [org.mindrot/jbcrypt "0.3m"]]
  :plugins [[codox "0.6.4"]]
  :codox {:output-dir "doc"})

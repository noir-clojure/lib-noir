(defproject lib-noir "0.3.7"
  :description "Libraries from Noir for your enjoyment."
  :url "http://webnoir.org"
  :license {:name "Eclipse Public License - v 1.0"
            :url "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo}
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [cheshire "5.0.1"]
                 [ring "1.1.1"]
                 [compojure "1.1.3"]
                 [hiccup "1.0.2"]
                 [org.mindrot/jbcrypt "0.3m"]]
  :plugins [[codox "0.6.4"]]
  :codox {:output-dir "doc/0.3.6"})

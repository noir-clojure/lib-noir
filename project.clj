(defproject lib-noir "0.3.0"
  :description "Libraries from Noir for your enjoyment."
  :url "http://webnoir.org"
  :license {:name "Eclipse Public License - v 1.0"
            :url "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo}
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [cheshire "4.0.0"]
                 [ring "1.1.1"]
                 [org.mindrot/jbcrypt "0.3m"]]
  :codox {:output-dir "doc/0.1.1"})
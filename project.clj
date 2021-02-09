(defproject milgra.com.server "0.6.0"
  :description "milgra.com homepgae"
  :url "http://example.com/FIXME"
  :min-lein-version "2.0.0"
  :repositories {"my.datomic.com" {:url "https://my.datomic.com/repo"
                                   :creds :gpg}}
  :source-paths ["src/clj"]
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/tools.nrepl "0.2.13"]
                 [compojure "1.6.1"]
                 [com.datomic/datomic-pro "0.9.6024"]
                 [org.clojure/data.json "0.2.6"]
                 [ring-cors "0.1.13"]
                 [crypto-password "0.2.1"]
                 [ring/ring-defaults "0.3.2"]]
  :plugins [[lein-ring "0.12.5"]]
  :ring {:handler milgracom.handler/app
         :nrepl {:start? true
                 :port 3001}}
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring/ring-mock "0.3.2"]]}
   :uberjar {:dependencies [[javax.servlet/servlet-api "2.5"]
                            [ring/ring-mock "0.3.2"]]
             :ring {:port 80}}})

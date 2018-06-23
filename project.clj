(defproject markov-text-gen "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.8.0"]]
  :main ^:skip-aot markov-text-gen.core
  :profiles {:uberjar {:aot :all}})

(ns main
  (:require [clojure.core.async :refer [<!!]]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            scraper))

(def conf
  (edn/read-string (slurp "conf.edn")))

(defn -main
  "Main"
  []
  (<!! (scraper/start (conf :url))))

(-main)

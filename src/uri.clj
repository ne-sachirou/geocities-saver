(ns uri
  (:require [clojure.java.io :as io]
            [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [lambdaisland.uri :as uri]
            [ring.util.codec :refer [url-encode]]))

(defn uri
  ""
  [s]
  (uri/uri s))

(defn join
  ""
  [base-uri uri]
  (uri/join base-uri uri))

(defn descendant?
  ""
  [base-uri uri]
  (let [base-uri (uri/uri base-uri)
        uri (join base-uri uri)
        base-uri-path (-> base-uri :path (or "/"))
        uri-path (-> uri :path (or "/"))]
    (and (= (uri :host) (base-uri :host))
         (str/starts-with? uri-path base-uri-path))))

(defn saveavle-file-name
  ""
  [base-uri uri]
  (when (descendant? base-uri uri)
    (let [base-uri (uri/uri base-uri)
          uri (join base-uri uri)]
      (str/replace-first (uri :path) (base-uri :path) ""))))

(defn encode
  ""
  [s]
  (-> s str url-encode))

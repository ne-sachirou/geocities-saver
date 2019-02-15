(ns uri-test
  (:use clojure.test)
  (:require [clojure.string :as str]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :refer [for-all]]
            [uri]))

(defn gen-string-alphanumeric-and
  [chars]
  (->> [[10 gen/char-alphanumeric]
        [1 (gen/elements chars)]]
       gen/frequency
       gen/vector
       gen/not-empty
       (gen/fmap #(str/join %1))))

(def gen-uri
  (gen/let [schema (gen/elements ["http" "https"])
            domain (gen-string-alphanumeric-and ["."])
            path (gen-string-alphanumeric-and ["." "/"])]
    (str schema "://" domain "/" path)))

(deftest uri-test
  (testing
   (is (= "http://example.com/" (-> "http://example.com/" uri/uri str)))))

(defspec uri-spec
  100
  (for-all [uri gen-uri]
           (= uri (-> uri uri/uri str))))

(deftest join-test
  (testing
   (are [expected base-uri uri] (= (uri/uri expected) (uri/join base-uri uri))
     "http://example.com/" "http://example.com/" ""
     "http://example.com/" "http://example.com/" "/"
     "http://example.com/a" "http://example.com/" "/a"
     "http://example.com/a" "http://example.com/" "a"
     "http://example.com/a" "http://example.com" "/a"
     "http://example.com/a/" "http://example.com/" "/a/"
     "http://example.com/?q=1" "http://example.com/" "?q=1"
     "http://no_example.com/" "http://example.com/" "http://no_example.com/")))

(deftest descendant?-test
  (testing
   (are [base-uri uri] (uri/descendant? base-uri uri)
     "http://example.com/" "http://example.com/"
     "http://example.com" "http://example.com/"
     "http://example.com/" "http://example.com"
     "http://example.com" "http://example.com"
     "http://example.com/" "http://example.com/a"
     "http://example.com" "http://example.com/a"
     "http://example.com/" "http://example.com/?q=1"
     "http://example.com" "http://example.com/?q=1"
     "http://example.com/a" "http://example.com/a"
     "http://example.com/a/" "http://example.com/a/")
    (are [base-uri uri] (not (uri/descendant? base-uri uri))
      "http://example.com/" "http://no_example.com/"
      "http://example.com/a" "http://example.com/")))

(deftest saveavle-file-name-test
  (testing
   (is true)))

(deftest encode-test
  (testing
   (are [expected original] (= expected (uri/encode original))
     "" ""
     "example" "example"
     "%2F" "/"
     "%3F" "?")))

(run-tests)

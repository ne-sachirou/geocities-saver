(ns scraper
  (:require [clj-http.client :as client]
            [clojure.core.async :refer [chan go go-loop >! <!] :as async]
            [clojure.core.match :refer [match]]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [hickory.core :as hickory]
            [hickory.select :as s]
            [uri]))

(def- wait-ms 1000)

(defn- gather-hrefs
  ""
  [doc]
  (let [href-tags [:a :area :audio :embed :iframe :img :link :track :script :source :video]
        href-uris (flatten
                   (map #(let [{:keys [attrs]} %1 {:keys [href src]} attrs] [href src])
                        (s/select (apply s/or (map s/tag href-tags)) doc)))
        object-uris (map #(get-in %1 [:attrs :data]) (s/select (s/tag :object) doc))
        source-uris (map #(get-in %1 [:attrs :srcset]) (s/select (s/tag :source) doc))]
    (filter some? (concat href-uris object-uris source-uris))))

(defn- save-page
  ""
  [uri dest-file scraper]
  (println (str "Saving " uri "\n\tto " dest-file))
  (let [{:keys [body headers status]} (client/get (str uri))]
    (if (and (<= 200 status) (<= status 299))
      (do ; (io/make-parents dest-file)
          ; (with-open [out (io/writer dest-file)]
          ;   (.write out body))
        (println (str "Saved " uri))
        (when (str/starts-with? (headers "Content-Type") "text/html")
          (let [doc (-> body hickory/parse hickory/as-hickory)]
            (doseq [uri (gather-hrefs doc)]
              (Thread/sleep wait-ms)
              (>!! scraper [:save-page {:uri uri}])))))
      (println "Fail to save " uri))))

(defn- handle-message
  ""
  [msg scraper state]
  (match
   msg
    [:save-page {:uri uri}]
    (let [uri (uri/join (state :base-uri) uri)
          dest-file (uri/saveavle-file-name base-uri uri)]
      (if (and
           (some? dest-file)
           (not (contains? (state :saved-paths) dest-file)))
        (do (let [abs-dest-file (.getAbsolutePath (io/file (state :dest-dir) dest-file))]
              (go (save-page uri abs-dest-file scraper)))
            (update-in state [:saved-paths] #(conj %1 dest-file)))
        state))))

(defn start
  ""
  [base-uri]
  (let [scraper (chan)]
    (go (>! scraper [:save-page {:uri base-uri}]))
    (go-loop [state {:base-uri (uri/uri base-uri)
                     :dest-dir (.getAbsolutePath (io/file (url/encode base-uri)))
                     :saved-paths []}]
      (when-let [msg (<! scraper)]
        (recur (handle-message msg scraper state))))))

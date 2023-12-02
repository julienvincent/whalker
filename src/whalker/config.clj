(ns whalker.config
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [malli.core :as m]
   [malli.error :as me]
   [malli.transform :as mt]))

(def ?BinTranscriber
  [:map
   [:transcriber [:= :bin]]
   [:bin-path :string]])

(def ?JniTranscriber
  [:map
   [:transcriber [:= :jni]]
   [:lib-path {:optional true} [:maybe :string]]])

(def ?Config
  [:and
   [:map
    [:model-path [:maybe :string]]
    [:chord [:or [:set :int] [:set :string]]]]

   [:or ?BinTranscriber ?JniTranscriber]])

(defn load-config [path]
  (try
    (->> (slurp (io/file path))
         edn/read-string)
    (catch Exception _
      {})))

(defn parse-config [config]
  (try
    (m/coerce ?Config config mt/json-transformer)
    (catch Exception e
      (let [{:keys [type data]} (ex-data e)]
        (when-not (= type :malli.core/invalid-input)
          (throw e))

        (println "Config invalid")
        (println (me/humanize (:explain data)))
        (throw (ex-info "Config invalid" {:reason (me/humanize (:explain data))}))))))

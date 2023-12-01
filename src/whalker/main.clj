(ns whalker.main
  (:require
   [babashka.process :as proc]
   [clojure.string :as str]
   [promesa.core :as p]
   [promesa.exec.csp :as c]
   [whalker.audio :as audio]
   [whalker.notification :as notification]
   [whalker.keylistener :as keylistener])
  (:import
   [java.awt Toolkit]
   [java.awt.datatransfer StringSelection])
  (:gen-class))

(defn handle-event [chords event]
  (case (:action event)
    :down (conj chords (:code event))
    :up (disj chords (:code event))))

(defn set-clipboard [data]
  (let [clipboard (-> (Toolkit/getDefaultToolkit)
                      .getSystemClipboard)
        selection (StringSelection. data)]
    (.setContents clipboard selection nil)))

(defn- handle-sample [{audio-data :data duration-ms :duration-ms}]
  (p/vthread
   (try
     (let [file (audio/write-audio-to-file audio-data)
           file-path (.getPath file)

           res (proc/sh ["/Users/julienvincent/code/whisper.cpp/main"
                         "-nt"
                         "-d" duration-ms
                         "-m" "/Users/julienvincent/code/whisper.cpp/models/ggml-medium.en.bin"
                         "-f" file-path])]

       (if (= 0 (:exit res))
         (do (-> res
                 :out
                 (str/replace #"\[BLANK_AUDIO\]" "")
                 str/trim
                 set-clipboard)
             (notification/notify "Audio Transcribed"
                                  "Audio capture has been transcribed and put into your clipboard"))
         (println "Failed to process audio data" (:err res)))

       (io/delete-file file))

     (catch Exception e
       (println "Failed to process audio data" e)))))

(defn -main [& _]
  (let [key-stream (keylistener/create-keylistener)]

    (p/vthread
     (loop [chords #{} capture nil]
       (when-let [event (c/take! key-stream)]

         (let [chords (handle-event chords event)
               is-match? (= #{3675 0} chords)]

           (cond
             (and (not capture)
                  is-match?)
             (recur chords (audio/start-capture-audio))

             (and capture
                  (not is-match?))
             (do (handle-sample (audio/stop-audio-capture capture))
                 (recur chords nil))

             :else (recur chords capture))))))))

(comment
  (-main)
  nil)

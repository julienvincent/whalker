(ns whalker.main
  (:require
   [clojure.tools.cli :as cli]
   [promesa.core :as p]
   [promesa.exec.csp :as c]
   [whalker.audio :as audio]
   [whalker.config :as util.config]
   [whalker.keylistener :as keylistener]
   [whalker.notification :as notification]
   [whalker.whisper.api :as whisper.api]
   [whalker.whisper.shell :as whisper.shell]
   [whalker.whisper.whisper-jni :as whisper.jni])
  (:import
   [java.awt Toolkit]
   [java.awt.datatransfer StringSelection])
  (:gen-class))

(set! *warn-on-reflection* true)

(defn handle-event [chords event]
  (case (:action event)
    :down (conj chords (:code event))
    :up (disj chords (:code event))))

(defn set-clipboard [data]
  (let [clipboard (-> (Toolkit/getDefaultToolkit)
                      .getSystemClipboard)
        selection (StringSelection. data)]
    (.setContents clipboard selection nil)))

(defn- handle-sample [transcriber recording]
  (println "Transcribing audio...")

  (p/vthread
   (try
     (let [time-start (System/nanoTime)
           text (whisper.api/transcribe transcriber recording)
           dx (- (System/nanoTime) time-start)
           seconds (->> (/ dx 1000000000.0)
                        (format "%.2f")
                        Double/parseDouble)]

       (set-clipboard text)

       (notification/notify "Audio Transcribed"
                            "Audio capture has been transcribed and put into your clipboard")

       (println (str "Transcription completed successfully after " seconds "s"))
       (println "  " text))

     (catch Exception e
       (println "Failed to process audio data" e)))))

(defn start! [opts]
  (let [key-stream (keylistener/create-keylistener)

        transcriber
        (cond
          (= :bin (:transcriber opts))
          (whisper.shell/create-shell-transcriber
           {:bin-path (:bin-path opts)
            :model-path (:model-path opts)})

          (= :jni (:transcriber opts))
          (whisper.jni/create-jni-transcriber
           {:lib-opts (cond-> nil
                        (:lib-path opts)
                        (assoc :lib-path (:lib-path opts)))
            :model-path (:model-path opts)}))]

    (p/vthread
     (loop [chords #{} capture nil]
       (when-let [event (c/take! key-stream)]
         (let [chords (handle-event chords event)
               is-match? (= #{3675 0} chords)]

           (cond
             (and (not capture)
                  is-match?)
             (do (println "Key pressed. Starting audio capture...")
                 (recur chords (audio/start-capture-audio)))

             (and capture
                  (not is-match?))
             (do (println "Key released.")
                 (handle-sample transcriber (audio/stop-audio-capture capture))
                 (recur chords nil))

             :else (recur chords capture))))))))

(defn start-keylogger! []
  (let [key-stream (keylistener/create-keylistener)]
    (p/vthread
     (loop []
       (when-let [event (c/take! key-stream)]
         (println (select-keys event [:action :code :key]))
         (recur))))))

(def cli-config
  [["-c" "--config"
    :id :config
    :default "config.edn"]

   [nil "--key-logger" "Start the script in a debug mode that prints key-pressed. 
                        Useful for inspecting keycodes for chord configuration"
    :id :key-logger]

   ["-t" "--transcriber" "The transcriber implementation to use"
    :id :transcriber
    :default :jni]

   ["-m" "--model-path" "Filesystem path to the whisper model"
    :id :model-path]

   [nil "--bin-path" "Filesystem path to the whisper.cpp built executable. Required when --transcriber=bin"
    :id :bin-path]

   [nil "--lib-path" "Filesystem path to the libwhisper.so native lib. Only used when --transcriber=jni"
    :id :lib-path]])

(defn -main [& args]
  (let [{opts :options} (cli/parse-opts args cli-config)]
    (when (:key-logger opts)
      @(start-keylogger!))

    (let [config-path (:config opts)

          config (-> (when config-path (util.config/load-config config-path))
                     (merge opts)
                     util.config/parse-config)]

      (doseq [[key value] config]
        (println (str (name key) "=" value)))

      (start! config))))

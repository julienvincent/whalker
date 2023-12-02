(ns whalker.main
  (:require
   [clojure.string :as str]
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

(defn handle-event [state event]
  (case (:action event)
    :down (-> state
              (update :key-chord conj (:key event))
              (update :code-chord conj (:code event)))
    :up (-> state
            (update :key-chord disj (:key event))
            (update :code-chord disj (:code event)))))

(defn chords-match? [chord state]
  (or (= chord (:key-chord state))
      (= chord (:code-chord state))))

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
     (loop [state {:key-chord #{} :code-chord #{}}
            capture nil]
       (when-let [event (c/take! key-stream)]
         (let [next-state (handle-event state event)
               is-match? (chords-match? (:chord opts) next-state)]

           (cond
             (and (not capture)
                  is-match?)
             (do (println "Key pressed. Starting audio capture...")
                 (recur next-state (audio/start-capture-audio)))

             (and capture
                  (not is-match?))
             (do (println "Key released.")
                 (handle-sample transcriber (audio/stop-audio-capture capture))
                 (recur next-state nil))

             :else (recur next-state capture))))))))

(defn start-keylogger! []
  (let [key-stream (keylistener/create-keylistener)]
    (p/vthread
     (loop []
       (when-let [event (c/take! key-stream)]
         (println (select-keys event [:action :code :key]))
         (recur))))))

(def cli-config
  [["-c" "--config CONFIG"
    :id :config
    :default "config.edn"]

   [nil "--keylogger" "Start the script in a debug mode that prints key-pressed. 
                      Useful for inspecting keycodes for chord configuration"
    :id :keylogger]

   ["-k" "--chord CHORD" "The chord to press to activate the recorder"
    :id :chord
    :parse-fn (fn [val]
                (let [keys (str/split val #"\+")
                      as-int (try
                               (->> keys
                                    (map #(Integer/parseInt %))
                                    set)
                               (catch Exception _ nil))]
                  (or as-int (set keys))))]

   ["-t" "--transcriber TRANSCRIBER" "The transcriber implementation to use"
    :id :transcriber
    :default :jni]

   ["-m" "--model-path PATH" "Filesystem path to the whisper model"
    :id :model-path]

   [nil "--bin-path PATH" "Filesystem path to the whisper.cpp built executable. Required when --transcriber=bin"
    :id :bin-path]

   [nil "--lib-path PATH" "Filesystem path to the libwhisper.so native lib. Only used when --transcriber=jni"
    :id :lib-path]])

(defn -main [& args]
  (let [{opts :options} (cli/parse-opts args cli-config)]
    (when (:keylogger opts)
      @(start-keylogger!))

    (let [config-path (:config opts)

          config (-> (when config-path (util.config/load-config config-path))
                     (merge opts)
                     util.config/parse-config)]

      (doseq [[key value] config]
        (println (str (name key) "=" value)))

      @(start! config))))

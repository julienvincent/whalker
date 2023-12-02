(ns whalker.whisper.shell
  (:require
   [babashka.process :as proc]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [whalker.audio :as audio]
   [whalker.whisper.api :as whisper.api]))

(defn- -transcribe [{:keys [bin-path model-path]} {audio-data :data duration-ms :duration-ms}]
  (let [file (audio/write-audio-to-file audio-data)
        file-path (.getPath file)

        res (proc/sh [bin-path
                      "-nt"
                      "-d" duration-ms
                      "-m" model-path
                      "-f" file-path])]

    (io/delete-file file)

    (if (= 0 (:exit res))
      (-> res
          :out
          (str/replace #"\[BLANK_AUDIO\]" "")
          str/trim)
      (throw (ex-info "whisper shell execution failed" {:error (:err res)
                                                        :status (:exit res)})))))

(defn create-shell-transcriber [config]
  (reify whisper.api/Transcriber
    (transcribe [_ params] (-transcribe config params))))

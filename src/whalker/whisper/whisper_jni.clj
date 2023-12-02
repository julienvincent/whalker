(ns whalker.whisper.whisper-jni
  "This is a whisper implementation around the https://github.com/GiviMAD/whisper-jni library"
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [whalker.audio :as audio]
   [whalker.whisper.api :as whisper.api])
  (:import
   [io.github.givimad.whisperjni WhisperFullParams WhisperJNI WhisperJNI$LoadOptions]))

(def ^:private loaded? (atom false))

(defn- load-native [{:keys [enable-logging?]}]
  (WhisperJNI/loadLibrary)
  (WhisperJNI/setLibraryLogger nil))

(defn- load-custom [{:keys [whisper-lib-path enable-logging?]}]
  (let [load-options (WhisperJNI$LoadOptions.)]
    #_(when-not enable-logging?
      (set! (.logger load-options) (fn [_])))
    (set! (.whisperLib load-options) (.toPath (io/file whisper-lib-path)))
    (WhisperJNI/loadLibrary load-options)))

(defn- create-whisper-context [{:keys [model-path lib-opts enable-logging?]}]
  (when-not @loaded?
    (if lib-opts
      (load-custom (assoc lib-opts :enable-logging? enable-logging?))
      (load-native {:enable-logging? enable-logging?}))
    (reset! loaded? true))

  (let [whisper (WhisperJNI.)
        context (.init whisper (.toPath (io/file model-path)))]
    {:whisper whisper
     :context context}))

(defn- -transcribe [{:keys [audio-data context duration-ms]}]
  (let [samples (audio/audio-data->samples audio-data)
        params (WhisperFullParams.)
        {:keys [whisper context]} context]
    (set! (.durationMs params) duration-ms)
    (.full whisper context params samples (count samples))
    (-> (.fullGetSegmentText whisper context 0)
        str/trim)))

(defn create-jni-transcriber [config]
  (let [context (create-whisper-context config)]
    (reify whisper.api/Transcriber
      (transcribe [_ params]
        (-transcribe (merge {:context context} params))))))

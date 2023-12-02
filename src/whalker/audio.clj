(ns whalker.audio
  (:require
   [clojure.java.io :as io]
   [promesa.core :as p])
  (:import
   [java.io ByteArrayInputStream ByteArrayOutputStream]
   [java.nio ByteBuffer ByteOrder]
   [javax.sound.sampled
    AudioFileFormat$Type
    AudioFormat
    AudioInputStream
    AudioSystem]))

(set! *warn-on-reflection* true)

(defn- gen-hash [n]
  (->> (repeatedly n #(rand-int 256))
       (map #(format "%02x" %))
       (apply str)))

(defn- create-tmp-file ^java.io.File []
  (io/file (System/getProperty "java.io.tmpdir") (str "whalker-" (gen-hash 5) ".wav")))

(defn start-capture-audio []
  (let [format (AudioFormat. 16000.0 16 1 true true)
        line (AudioSystem/getTargetDataLine format)]

    (.open line format)
    (.start line)

    (let [output-stream (ByteArrayOutputStream.)
          buffer (byte-array 1024)
          running? (atom true)
          writer (p/vthread
                  (loop []
                    (when @running?
                      (let [read (.read line buffer 0 1024)]
                        (try (.write output-stream buffer 0 read)
                             (catch Exception _)))
                      (recur)))

                  (.close output-stream))]

      {:get-data (fn []
                   (.toByteArray output-stream))
       :started-at (System/nanoTime)
       :stop (fn []
               (.stop line)
               (.close line)

               (reset! running? false)
               @writer)})))

(defn stop-audio-capture [capture]
  (let [dx (- (System/nanoTime) (:started-at capture))
        ms (->> (/ dx 1000000.0)
                (format "%.2f")
                Double/parseDouble)]
    ((:stop capture))
    {:audio-data ((:get-data capture))
     :duration-ms ms}))

(comment

  (def capture (start-capture-audio))
  (def audio (stop-audio-capture capture))

  nil)

(defn write-audio-to-file ^java.io.File [data]
  (let [file (create-tmp-file)
        stream (ByteArrayInputStream. data)
        format (AudioFormat. 16000.0 16 1 true true)]

    (AudioSystem/write
     (AudioInputStream. stream format (count data))
     AudioFileFormat$Type/WAVE
     file)

    file))

;; The below audio->sample implementations were based off of the test code here:
;; https://github.com/GiviMAD/whisper-jni/blob/298eb26d13a7dda460767c631ea7144f8ded049f/src/test/java/io/github/givimad/whisperjni/WhisperJNITest.java#L213

(defn buffer->samples [^ByteBuffer buffer]
  (let [short-buffer (.asShortBuffer buffer)
        samples (float-array (/ (.capacity buffer) 2))]
    (loop [i 0]
      (if (.hasRemaining short-buffer)
        (let [val (Float/min (/ (.get short-buffer) Short/MAX_VALUE) 1.0)]
          (aset samples i (Float/max -1.0 val))
          (recur (inc i)))

        samples))))

(defn audio-data->samples [audio-data]
  (let [stream (ByteArrayInputStream. audio-data)
        format (AudioFormat. 16000.0 16 1 true true)

        audio-stream (AudioInputStream. stream format (count audio-data))
        capture-buffer (ByteBuffer/allocate (.available audio-stream))]

    (.order capture-buffer ByteOrder/BIG_ENDIAN)
    (.read stream (.array capture-buffer))

    (buffer->samples capture-buffer)))

(defn audio-file->samples [file-path]
  (let [stream (AudioSystem/getAudioInputStream (io/file file-path))
        capture-buffer (ByteBuffer/allocate (.available stream))]

    (.order capture-buffer ByteOrder/LITTLE_ENDIAN)
    (.read stream (.array capture-buffer))

    (buffer->samples capture-buffer)))

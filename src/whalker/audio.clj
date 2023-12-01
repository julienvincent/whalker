(ns whalker.audio
  (:require
   [clojure.java.io :as io]
   [promesa.core :as p])
  (:import
   [java.io ByteArrayInputStream ByteArrayOutputStream]
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
                      (recur))))]

      {:get-data (fn []
                   (.toByteArray output-stream))
       :started-at (System/nanoTime)
       :stop (fn []
               (.close output-stream)

               (reset! running? false)
               @writer

               (.stop line)
               (.close line))})))

(defn stop-audio-capture [capture]
  (let [dx (- (System/nanoTime) (:started-at capture))
        ms (->> (/ dx 1000000.0)
                (format "%.2f")
                Double/parseDouble)]
    ((:stop capture))
    {:data ((:get-data capture))
     :duration-ms ms}))

(defn write-audio-to-file ^java.io.File [data]
  (let [file (create-tmp-file)
        stream (ByteArrayInputStream. data)
        format (AudioFormat. 16000.0 16 1 true true)]

    (AudioSystem/write
     (AudioInputStream. stream format (count data))
     AudioFileFormat$Type/WAVE
     file)

    file))
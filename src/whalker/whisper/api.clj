(ns whalker.whisper.api)

(defprotocol Transcriber
  (transcribe [_ opts]))

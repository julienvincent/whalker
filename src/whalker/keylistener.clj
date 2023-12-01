(ns whalker.keylistener
  (:require
   [promesa.exec.csp :as c])
  (:import
   [com.github.kwhat.jnativehook GlobalScreen NativeHookException]))

(set! *warn-on-reflection* true)

(def ^:private registered (atom false))

(defn create-keylistener []
  (when-not @registered
    (try
      (GlobalScreen/registerNativeHook)
      (catch NativeHookException ex
        (println "There was a problem registering the native hook.")
        (println (.getMessage ex))
        (System/exit 1)))
    (reset! registered true))

  (let [event-stream (c/chan)
        handler (whalker.keylistener.Handler. {:event-handler #(c/put event-stream %)})]
    (GlobalScreen/addNativeKeyListener handler)
    event-stream))

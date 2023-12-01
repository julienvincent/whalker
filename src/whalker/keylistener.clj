(ns whalker.keylistener
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

  (let [handler (whalker.keylistener.Handler.)]
    (GlobalScreen/addNativeKeyListener handler)
    (:stream (.state handler))))

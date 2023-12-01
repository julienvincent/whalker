(ns whalker.keylistener.handler
  (:require
   [promesa.exec.csp :as c])
  (:import
   [com.github.kwhat.jnativehook.keyboard NativeKeyEvent])
  (:gen-class
   :name whalker.keylistener.Handler
   :implements [com.github.kwhat.jnativehook.keyboard.NativeKeyListener]
   :state state
   :init init))

(set! *warn-on-reflection* true)

(defn -init []
  [[] {:stream (c/chan)}])

(defn -nativeKeyPressed [^whalker.keylistener.Handler this ^NativeKeyEvent e]
  (let [{stream :stream} (.state this)]
    (c/put stream {:action :down
                   :code (.getKeyCode e)
                   :key (NativeKeyEvent/getKeyText (.getKeyCode e))
                   :native-event e})))

(defn -nativeKeyReleased [^whalker.keylistener.Handler this ^NativeKeyEvent e]
  (let [{stream :stream} (.state this)]
    (c/put stream {:action :up
                   :code (.getKeyCode e)
                   :key (NativeKeyEvent/getKeyText (.getKeyCode e))
                   :native-event e})))

(defn -nativeKeyTyped [_ _])

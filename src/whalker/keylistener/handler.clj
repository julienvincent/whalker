(ns whalker.keylistener.handler
  (:import
   [com.github.kwhat.jnativehook.keyboard NativeKeyEvent])
  (:gen-class
   :name whalker.keylistener.Handler
   :implements [com.github.kwhat.jnativehook.keyboard.NativeKeyListener]
   :state state
   :init init
   :constructors {[clojure.lang.IPersistentMap] []}))

(set! *warn-on-reflection* true)

(defn -init [{:keys [event-handler]}]
  [[] {:event-handler event-handler}])

(defn -nativeKeyPressed [^whalker.keylistener.Handler this ^NativeKeyEvent e]
  (let [{handler :event-handler} (.state this)]
    (handler {:action :down
              :code (.getKeyCode e)
              :key (NativeKeyEvent/getKeyText (.getKeyCode e))
              :native-event e})))

(defn -nativeKeyReleased [^whalker.keylistener.Handler this ^NativeKeyEvent e]
  (let [{handler :event-handler} (.state this)]
    (handler {:action :up
              :code (.getKeyCode e)
              :key (NativeKeyEvent/getKeyText (.getKeyCode e))
              :native-event e})))

(defn -nativeKeyTyped [_ _])

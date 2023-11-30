(ns user)

(defn compile-classes [& _]
  (compile 'whalker.keylistener.handler))

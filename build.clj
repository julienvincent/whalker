(ns build
  (:require
   [clojure.tools.build.api :as b]))

(def basis (b/create-basis {:project "deps.edn"
                            :aliases [:native]}))
(def class-dir "target/classes")
(def uber-file "target/whalker.jar")

(defn clean [_]
  (b/delete {:path "target"}))

(defn uber [_]
  (clean nil)
  (b/copy-dir {:src-dirs ["src" "resources" "classes"]
               :target-dir class-dir})
  (b/compile-clj {:basis basis
                  :src-dirs ["src" "resources" "classes"]
                  :class-dir class-dir
                  :ns-compile ['whalker.main]
                  :java-opts ["-Dclojure.compiler.direct-linking=true"
                              "-Dclojure.spec.skip-macros=true"]})

  (b/uber {:class-dir class-dir
           :uber-file uber-file
           :basis basis
           :main 'whalker.main}))

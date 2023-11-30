prepare:
    mkdir -p classes
    clojure -X:precompile

run:
  clojure -M -m whalker.main

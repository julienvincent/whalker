prepare:
    mkdir -p classes
    clojure -X:precompile

clean:
  clojure -T:build clean

build: clean prepare
  clojure -T:build uber

native-image:
  $GRAALVM_HOME/bin/native-image \
    -jar target/whalker.jar \
    --no-fallback \
    --features=clj_easy.graal_build_time.InitClojureClasses \
    --report-unsupported-elements-at-runtime \
    -march=native \
    -o target/whalker \
    -H:+UnlockExperimentalVMOptions \
    -H:ConfigurationFileDirectories=./graal/ \
    -H:+ReportExceptionStackTraces

build-native: build native-image

build-and-run: build
  java --enable-preview -jar target/cli.jar

run *ARGS:
  clojure -M -m whalker.main {{ARGS}}

run-with-reflection:
  $GRAALVM_HOME/bin/java -agentlib:native-image-agent=config-output-dir=./target/graal/ -jar target/whalker.jar

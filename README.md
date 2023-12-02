# Whalker

This is a small wrapper around [whisper.cpp](https://github.com/ggerganov/whisper.cpp) that enabled an easy to use way of transcribing mic audio on the fly. This is done by registering a global keybinding which, when pressed, starts recording mic audio. When the keybinding is released the audio is transcribed using whisper and the transcribed audio is placed into the system clipboard.

## Usage

This tool requires that you download whisper ml models separately. This was tested on the models provided by `whisper.cpp` - head to [the whisper.cpp model docs](https://github.com/ggerganov/whisper.cpp/tree/master/models) for instructions on how to download them.

Once you have an appropriate whisper ml model on your machine you can proceed to downloading the whalker jar from the [releases page](https://github.com/julienvincent/whalker/releases) and then run:

```bash
java -jar whalker.jar --model-path=/path/to/downloaded/model.bin --chord Ctrl+Shift+1
```

Native binaries will eventually be made available one I can figure out how to get JNI working with GraalVM.

## Configuration

You can configure the tool with cli args or with a `config.edn` file. By default the tool will look for a `config.edn` file in the current working directory or this can be changed by specifying `--config=/path/to/config.edn`.

The config file looks as follows:

```clj
{:model-path "/path/to/ggml-large-v3.bin"

 ; Or using key text - #{"Ctrl" "Shift" "1"}
 :chord #{55 10}}
```

Any key in the config file can be used as a cli flag. For example `--chord` and `--model-path` can be given as cli args.

### Chords

When specifying key chords it is recommended to use the numeric key codes like `59+56+18` instead of the textual codes like `Ctrl+Shift+1` because there is some bug either in the keylogger lib I am using or in my usage of it that causes the textual codes to be unstable.

To find the raw numeric keycodes that you can use you can run the jar in keylogger mode by adding the flag `--keylogger`.

```bash
java -jar whalker.jar --keylogger
```

This will print out to console every keypress. You can then press the keys you are wanting to use and see what their raw keycodes are.

---

## Using the GPU

This tool uses by default a pre-built version of libwhisper that comes bundled with the [io.github.givimad/whisper-jni](https://github.com/GiviMAD/whisper-jni) module. This pre-built version of libwhisper does not come with GPU support included which means the model executes on the CPU and the transcription will be painfully slow.

To speed things up it is recommended to build the [whisper.cpp](https://github.com/ggerganov/whisper.cpp) binaries yourself and configure whalker to use the locally built versions. This might sound daunting but the `whisper.cpp` homepage has pretty good docs on how to do all this and it's really straight forward.

When it comes to configuring whalker there are essentially two strategies:

### 1. Use the bin transcriber

This will shell out to the whisper.cpp executable binary for transcription. It's simpler to configure but a bit slower to execute.

Make sure you have built the executable in your local clone of `whisper.cpp`:

```bash
make main
```

Then use the following config:

```clj
{...
 :transcriber :bin
 :bin-path "/path/to/whisper.cpp/main"
 ...}
```

### 1. Use the jni transcriber

This uses the same JNI native interface as the default whalker configuration with the exception that it will load `libwhisper.so` from your local build of whisper.cpp

First of all make sure you have compiled libwhisper in whisper.cpp:

```bash
# Run this in your local clone of `whisper.cpp`
make libwhisper.so
```

Then use the following config:

```clj
{...
 :transcriber :jni
 :lib-path "/path/to/whisper.cpp/libwhisper.so"
 ...}
```

> [!NOTE]
>
> On MacOS (and maybe other platforms?) the CPU support is provided by a separately built `ggml-metal.metal` binary. This binary needs to be in the CWD of wherever you execute `whalker.jar` from or else this will not load.
>
> You can copy the binary as-is to wherever you are running `whalker.jar` from.
>
> This is a limitation in how `whisper-jni` loads `libwhisper.so` and will hopefully be solved upstream at some point.

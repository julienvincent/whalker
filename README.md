# Whalker

This is a small wrapper around [whisper.cpp](https://github.com/ggerganov/whisper.cpp) that enabled an easy to use way of transcribing mic audio on the fly. This is done by registering a global keybinding which, when pressed, starts recording mic audio. When the keybinding is released the audio is transcribed using whisper and the transcribed audio is placed into the system clipboard.

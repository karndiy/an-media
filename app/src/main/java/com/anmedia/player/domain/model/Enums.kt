package com.anmedia.player.domain.model

enum class SourceType {
    LOCAL_AUDIO,
    LOCAL_VIDEO,
    YOUTUBE
}

enum class StreamMode {
    AUDIO_ONLY,
    VIDEO
}

enum class RepeatMode {
    OFF,
    ONE,
    ALL
}

enum class EqPreset(val label: String) {
    FLAT("Flat"),
    ROCK("Rock"),
    POP("Pop"),
    JAZZ("Jazz"),
    BASS_BOOST("Bass Boost"),
    CUSTOM("Custom")
}

enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark"),
    CLASSIC("Winamp Classic")
}

package com.anmedia.player.player

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Lightweight, in-memory rolling log for diagnosing playback issues (stuttering, which stream
 * quality got picked, network/buffering events) without needing adb/logcat access - the user can
 * copy it straight out of the app's debug log screen. Off by default so there's zero overhead
 * when not actively diagnosing something; enabling it clears any stale log from a prior session
 * so a fresh repro isn't mixed in with old entries.
 */
object PlaybackDiagnostics {
    private const val MAX_LINES = 2000
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    private val _enabled = MutableStateFlow(false)
    val enabled: StateFlow<Boolean> = _enabled

    private val lines = ArrayDeque<String>()
    private val _log = MutableStateFlow("")
    val log: StateFlow<String> = _log

    @Synchronized
    fun setEnabled(value: Boolean) {
        if (_enabled.value == value) return
        _enabled.value = value
        if (value) {
            lines.clear()
            _log.value = ""
            log("Diagnostic logging enabled")
        } else {
            log("Diagnostic logging disabled")
        }
    }

    @Synchronized
    fun log(message: String) {
        if (!_enabled.value) return
        val line = "${timeFormat.format(System.currentTimeMillis())}  $message"
        if (lines.size >= MAX_LINES) lines.removeFirst()
        lines.addLast(line)
        _log.value = lines.joinToString("\n")
    }

    @Synchronized
    fun clear() {
        lines.clear()
        _log.value = ""
        if (_enabled.value) log("Log cleared")
    }
}

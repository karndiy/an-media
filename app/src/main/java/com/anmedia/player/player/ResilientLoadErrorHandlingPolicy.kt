@file:OptIn(UnstableApi::class)

package com.anmedia.player.player

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy

/**
 * Retries network-level load failures far more persistently than Media3's default, which gives
 * up after just [DefaultLoadErrorHandlingPolicy.DEFAULT_MIN_LOADABLE_RETRY_COUNT] (3) attempts
 * spread over roughly 3 seconds total - easily outlasted by a completely ordinary mobile-network
 * blip (a few seconds of weak signal, switching cell towers, a Wi-Fi/mobile handoff).
 *
 * Observed directly in a real playback log: a brief connectivity drop (DNS temporarily
 * unresolvable - "no address associated with hostname", i.e. the device had no network path at
 * that instant, not just a slow one) escalated straight to a fatal player error after 3 quick
 * retries and stopped playback entirely, when simply retrying a bit longer would very likely
 * have recovered once the connection came back - which is exactly what happened moments later
 * in that same session once the user re-triggered playback manually.
 */
class ResilientLoadErrorHandlingPolicy : DefaultLoadErrorHandlingPolicy() {

    override fun getRetryDelayMsFor(loadErrorInfo: LoadErrorHandlingPolicy.LoadErrorInfo): Long {
        // Exponential backoff capped at 10s, so a longer outage doesn't get hammered with
        // requests but still checks back often enough to resume quickly once it's over.
        val exponent = (loadErrorInfo.errorCount - 1).coerceIn(0, 4)
        return (1000L shl exponent).coerceAtMost(10_000L)
    }

    override fun getMinimumLoadableRetryCount(dataType: Int): Int = RETRY_COUNT

    companion object {
        // Combined with the backoff above, this tolerates on the order of several minutes of
        // total outage before finally giving up, instead of ~3 seconds.
        private const val RETRY_COUNT = 50
    }
}

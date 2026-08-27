package de.binauralbeats.app.ui

import de.binauralbeats.app.data.Phase
import de.binauralbeats.app.service.AudioPlaybackService

/**
 * What a recreated ViewModel should show when it (re)binds to a service that
 * is already playing - e.g. Activity/process recreated while a foreground
 * session kept running in the background. Pulled out as a pure function so
 * this decision is unit-testable without an Android runtime or a real
 * AudioPlaybackService.
 */
data class RestoredPlaybackState(
    val phases: List<Phase>?,
    val carrier: Float?,
    val volume: Float?,
    val noiseVolume: Float?,
    val transitionMs: Int?,
    val isPaused: Boolean,
    val currentPhaseIndex: Int
)

/** Returns null when nothing needs restoring (service isn't playing). */
fun computeRestoredPlaybackState(
    generatorIsPlaying: Boolean,
    generatorIsPaused: Boolean,
    generatorCurrentPhaseIndex: Int,
    lastPlaybackParams: AudioPlaybackService.PlaybackParams?
): RestoredPlaybackState? {
    if (!generatorIsPlaying) return null
    return RestoredPlaybackState(
        phases = lastPlaybackParams?.phases,
        carrier = lastPlaybackParams?.carrier,
        volume = lastPlaybackParams?.volume,
        noiseVolume = lastPlaybackParams?.noiseVolume,
        transitionMs = lastPlaybackParams?.transitionMs,
        isPaused = generatorIsPaused,
        currentPhaseIndex = generatorCurrentPhaseIndex
    )
}

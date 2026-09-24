package de.binauralbeats.app.data

import de.binauralbeats.app.alarm.WakeRamps
import kotlinx.serialization.Serializable

/**
 * One alarm of the wake-alarm add-on. Persisted as JSON in DataStore, so every
 * field needs a default - an alarm saved by an older version must still load
 * after a field is added. See docs/KLANGWECKER_KONZEPT.md.
 *
 * [days] holds ISO day numbers (1 = Monday ... 7 = Sunday) rather than
 * java.time.DayOfWeek, which kotlinx.serialization cannot serialize out of the
 * box. An empty set means a one-off alarm that switches itself off after
 * ringing.
 */
@Serializable
data class WakeAlarm(
    val id: String,
    val hour: Int,
    val minute: Int,
    val days: Set<Int> = emptySet(),
    val rampKey: String = WakeRamps.DEFAULT_KEY,
    val rampMinutes: Int = 20,
    val volume: Float = 0.7f,
    val ambient: AmbientSound? = null,
    val ambientVolume: Float = 0.4f,
    /** Off by default: the point is to wake gently (decided 23.09.). */
    val vibrate: Boolean = false,
    val enabled: Boolean = true
) {
    val isOneOff: Boolean get() = days.isEmpty()
}

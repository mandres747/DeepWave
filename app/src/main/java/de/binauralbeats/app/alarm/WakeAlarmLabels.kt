package de.binauralbeats.app.alarm

import java.time.Duration

/**
 * The decisions behind the short texts in the alarm list, kept free of
 * Android resources so they can be tested. The sheet turns them into
 * localized strings.
 */
object WakeAlarmLabels {

    sealed interface Days {
        data object Once : Days
        data object Daily : Days
        data object Weekdays : Days
        data object Weekend : Days
        /** ISO day numbers in week order, for "Mo, Mi, Fr". */
        data class Some(val days: List<Int>) : Days
    }

    fun days(days: Set<Int>): Days {
        val valid = days.filter { it in 1..7 }.toSortedSet()
        return when (valid) {
            emptySet<Int>() -> Days.Once
            (1..7).toSet() -> Days.Daily
            (1..5).toSet() -> Days.Weekdays
            setOf(6, 7) -> Days.Weekend
            else -> Days.Some(valid.toList())
        }
    }

    /**
     * "Rings in 7 h 40 min" after saving, like every alarm clock does - the
     * cheapest way to catch a wrong AM/PM or a wrong weekday. Rounded up to
     * the next minute, so a ring in 30 s never reads "in 0 min".
     */
    fun until(duration: Duration): Pair<Long, Long> {
        val minutes = (duration.toMillis() + 59_999L) / 60_000L
        return (minutes / 60) to (minutes % 60)
    }
}

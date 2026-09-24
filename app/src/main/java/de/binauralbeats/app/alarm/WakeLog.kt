package de.binauralbeats.app.alarm

import android.content.Context
import android.content.pm.ApplicationInfo
import java.io.File
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Timestamped trail of what the alarm did, for debug builds only: an
 * overnight test has to be read the next morning, and on a Samsung the
 * logcat buffer rolls over within hours. Release builds write nothing.
 *
 *   adb shell run-as de.binauralbeats.app.debug cat files/wake_log.txt
 */
object WakeLog {

    private const val FILE = "wake_log.txt"
    private const val MAX_BYTES = 256 * 1024L
    private val stamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")

    fun event(context: Context, message: String) {
        val app = context.applicationContext
        if (app.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE == 0) return
        runCatching {
            val file = File(app.filesDir, FILE)
            if (file.length() > MAX_BYTES) file.delete()
            file.appendText("${ZonedDateTime.now().format(stamp)}  $message\n")
        }
    }
}

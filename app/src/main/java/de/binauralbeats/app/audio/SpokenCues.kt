package de.binauralbeats.app.audio

import android.content.Context
import android.media.AudioAttributes
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * Spoken announcements for a running rhythm program - "Tempo 120" at a step
 * boundary, a word when the program ends.
 *
 * Uses Android's own TextToSpeech rather than shipping audio, which keeps this
 * free of any proprietary dependency and out of the F-Droid question entirely.
 * Whether a voice is installed is the device's business: if none is, nothing
 * is spoken and nothing breaks. That is why every call here is safe to make
 * before, during or after initialisation.
 */
class SpokenCues(private val context: Context) {

    private var tts: TextToSpeech? = null

    @Volatile
    private var usable = false

    /**
     * Starts the engine. Initialisation is asynchronous, so the first cue in a
     * session may be missed - acceptable for an announcement, and far better
     * than blocking playback until a voice is ready.
     */
    fun start() {
        if (tts != null) return
        tts = TextToSpeech(context.applicationContext) { status ->
            usable = status == TextToSpeech.SUCCESS && applyLanguage()
        }.apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
        }
    }

    /**
     * Follows the app's locale, which the user can override in the settings,
     * rather than the device's - otherwise a German user who switched the app
     * to English would be told "Tempo" in the wrong voice.
     */
    private fun applyLanguage(): Boolean {
        val engine = tts ?: return false
        val locale = Locale.getDefault()
        val result = engine.setLanguage(locale)
        return result != TextToSpeech.LANG_MISSING_DATA &&
            result != TextToSpeech.LANG_NOT_SUPPORTED
    }

    /** Says one line, replacing anything still being spoken. */
    fun say(text: String) {
        if (!usable) return
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, text.hashCode().toString())
    }

    fun shutdown() {
        usable = false
        tts?.let {
            try {
                it.stop()
                it.shutdown()
            } catch (_: Exception) {}
        }
        tts = null
    }
}

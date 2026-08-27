package de.binauralbeats.app.data

import androidx.annotation.StringRes
import de.binauralbeats.app.R

enum class AmbientSound(@StringRes val labelRes: Int, val emoji: String) {
    RAIN(R.string.sound_rain, "🌧️"),
    OCEAN(R.string.sound_ocean, "🌊"),
    WIND(R.string.sound_wind, "🍃"),
    FIRE(R.string.sound_fire, "🔥"),
    STREAM(R.string.sound_stream, "🏞️"),
    WHITE(R.string.sound_white, "📻"),
    PINK(R.string.sound_pink, "🌸"),
    BROWN(R.string.sound_brown, "🟤")
}

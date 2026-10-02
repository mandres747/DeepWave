package de.binauralbeats.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import de.binauralbeats.app.ui.theme.LocalBinauralColors
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun WaveformVisualizer(
    beatFrequency: Float,
    isPlaying: Boolean,
    isPaused: Boolean = false,
    modifier: Modifier = Modifier
) {
    val colors = LocalBinauralColors.current
    val accentColor = colors.accentPrimary
    val secondaryColor = colors.accentSecondary

    // Advanced frame by frame rather than by an infinite transition, so a pause
    // holds the wave where it is, matching the sound that has stopped, and a
    // resume carries on from there.
    var phase by remember { mutableFloatStateOf(0f) }
    val periodMillis by rememberUpdatedState(
        if (beatFrequency > 0) (1000f / beatFrequency).coerceIn(200f, 5000f) else 2000f
    )
    LaunchedEffect(isPlaying, isPaused) {
        if (!isPlaying || isPaused) return@LaunchedEffect
        var last = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            val step = (now - last) / 1_000_000f / periodMillis * (2 * PI).toFloat()
            phase = (phase + step) % (2 * PI).toFloat()
            last = now
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
    ) {
        if (!isPlaying) return@Canvas

        val w = size.width
        val h = size.height
        val mid = h / 2

        val leftPath = Path()
        val rightPath = Path()
        val steps = 200

        for (i in 0..steps) {
            val x = w * i / steps
            val t = i.toFloat() / steps

            val leftY = mid + (h * 0.35f) * sin(2 * PI * 3 * t + phase).toFloat()
            val rightY = mid + (h * 0.25f) * sin(2 * PI * 3.5 * t + phase * 1.1f).toFloat()

            if (i == 0) {
                leftPath.moveTo(x, leftY)
                rightPath.moveTo(x, rightY)
            } else {
                leftPath.lineTo(x, leftY)
                rightPath.lineTo(x, rightY)
            }
        }

        // The beat itself: the two tones drift in and out of phase, so their sum
        // swells and fades. Drawn as a faint dashed envelope around both waves.
        val upper = Path()
        val lower = Path()
        for (i in 0..steps) {
            val x = w * i / steps
            val t = i.toFloat() / steps
            val e = (h * 0.42f) * kotlin.math.abs(kotlin.math.cos(PI * t + phase / 2).toFloat())
            if (i == 0) { upper.moveTo(x, mid - e); lower.moveTo(x, mid + e) }
            else { upper.lineTo(x, mid - e); lower.lineTo(x, mid + e) }
        }
        val dash = Stroke(
            width = 1.dp.toPx(),
            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 4.dp.toPx()))
        )
        drawPath(upper, accentColor.copy(alpha = 0.35f), style = dash)
        drawPath(lower, accentColor.copy(alpha = 0.35f), style = dash)

        drawPath(
            path = leftPath,
            brush = Brush.horizontalGradient(
                colors = listOf(
                    accentColor.copy(alpha = 0.2f),
                    accentColor.copy(alpha = 0.8f),
                    accentColor.copy(alpha = 0.2f)
                )
            ),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )

        drawPath(
            path = rightPath,
            brush = Brush.horizontalGradient(
                colors = listOf(
                    secondaryColor.copy(alpha = 0.15f),
                    secondaryColor.copy(alpha = 0.6f),
                    secondaryColor.copy(alpha = 0.15f)
                )
            ),
            style = Stroke(width = 1.5f.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

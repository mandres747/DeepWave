package de.binauralbeats.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import de.binauralbeats.app.ui.theme.AccentPrimary
import de.binauralbeats.app.ui.theme.AccentSecondary
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun WaveformVisualizer(
    beatFrequency: Float,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "waveform")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (beatFrequency > 0) (1000f / beatFrequency).toInt().coerceIn(200, 5000) else 2000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

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

        drawPath(
            path = leftPath,
            brush = Brush.horizontalGradient(
                colors = listOf(
                    AccentPrimary.copy(alpha = 0.2f),
                    AccentPrimary.copy(alpha = 0.8f),
                    AccentPrimary.copy(alpha = 0.2f)
                )
            ),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )

        drawPath(
            path = rightPath,
            brush = Brush.horizontalGradient(
                colors = listOf(
                    AccentSecondary.copy(alpha = 0.15f),
                    AccentSecondary.copy(alpha = 0.6f),
                    AccentSecondary.copy(alpha = 0.15f)
                )
            ),
            style = Stroke(width = 1.5f.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

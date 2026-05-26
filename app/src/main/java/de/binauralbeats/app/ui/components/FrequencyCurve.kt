package de.binauralbeats.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.binauralbeats.app.data.Phase
import de.binauralbeats.app.ui.theme.AccentPrimary
import de.binauralbeats.app.ui.theme.OnSurfaceMuted
import kotlin.math.max

private val BandColors = mapOf(
    "Delta" to Color(0xFF9C27B0),
    "Theta" to Color(0xFF2196F3),
    "Alpha" to Color(0xFFA8E6CF),
    "Beta" to Color(0xFFFFEB3B),
    "Gamma" to Color(0xFFFF5722)
)

private fun bandName(freq: Float): String = when {
    freq < 4f -> "Delta"
    freq < 8f -> "Theta"
    freq < 13f -> "Alpha"
    freq < 30f -> "Beta"
    else -> "Gamma"
}

private fun bandColor(freq: Float): Color = BandColors[bandName(freq)] ?: AccentPrimary

@Composable
fun FrequencyCurve(
    phases: List<Phase>,
    totalProgress: Float,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val totalMinutes = remember(phases) { phases.sumOf { it.durationMinutes } }
    val maxFreq = remember(phases) { max(phases.maxOfOrNull { it.frequency } ?: 40f, 10f) }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(12.dp))
    ) {
        val w = size.width
        val h = size.height
        val padding = 32f
        val chartW = w - padding * 2
        val chartH = h - padding

        // Background
        drawRect(Color.White.copy(alpha = 0.03f))

        // Band zone backgrounds
        drawBandZones(padding, chartW, chartH, maxFreq)

        // Frequency line
        val path = Path()
        var xOffset = padding
        phases.forEachIndexed { idx, phase ->
            val phaseWidth = (phase.durationMinutes.toFloat() / totalMinutes) * chartW
            val y = chartH - (phase.frequency / maxFreq) * chartH + padding / 2

            if (idx == 0) path.moveTo(xOffset, y)
            else path.lineTo(xOffset, y)
            path.lineTo(xOffset + phaseWidth, y)

            // Phase frequency label
            if (phaseWidth > 60f) {
                drawText(
                    textMeasurer = textMeasurer,
                    text = "${phase.frequency}",
                    topLeft = Offset(xOffset + 4f, y - 16f),
                    style = TextStyle(
                        color = bandColor(phase.frequency),
                        fontSize = 9.sp
                    )
                )
            }

            // Phase separator
            if (idx > 0) {
                drawLine(
                    Color.White.copy(0.1f),
                    Offset(xOffset, padding / 2),
                    Offset(xOffset, chartH + padding / 2),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
                )
            }

            xOffset += phaseWidth
        }

        // Draw the frequency path
        drawPath(
            path,
            color = AccentPrimary,
            style = Stroke(width = 2.5f)
        )

        // Fill under curve with gradient
        val fillPath = Path().apply {
            addPath(path)
            lineTo(xOffset, chartH + padding / 2)
            lineTo(padding, chartH + padding / 2)
            close()
        }
        drawPath(
            fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(AccentPrimary.copy(0.15f), Color.Transparent),
                startY = 0f,
                endY = chartH + padding / 2
            )
        )

        // Playback position line
        if (isPlaying || totalProgress > 0f) {
            val posX = padding + totalProgress * chartW
            drawLine(
                Color(0xFFFF6B6B),
                Offset(posX, padding / 2),
                Offset(posX, chartH + padding / 2),
                strokeWidth = 2f
            )
            drawCircle(
                Color(0xFFFF6B6B),
                radius = 4f,
                center = Offset(posX, padding / 2)
            )
        }

        // Bottom time labels
        drawText(
            textMeasurer = textMeasurer,
            text = "0",
            topLeft = Offset(padding, chartH + padding / 2 + 2f),
            style = TextStyle(color = OnSurfaceMuted.copy(0.5f), fontSize = 8.sp)
        )
        drawText(
            textMeasurer = textMeasurer,
            text = "${totalMinutes}m",
            topLeft = Offset(w - padding - 20f, chartH + padding / 2 + 2f),
            style = TextStyle(color = OnSurfaceMuted.copy(0.5f), fontSize = 8.sp)
        )
    }
}

private fun DrawScope.drawBandZones(padding: Float, chartW: Float, chartH: Float, maxFreq: Float) {
    val bands = listOf(
        Triple(0f, 4f, "Delta"),
        Triple(4f, 8f, "Theta"),
        Triple(8f, 13f, "Alpha"),
        Triple(13f, 30f, "Beta"),
        Triple(30f, maxFreq, "Gamma")
    )

    for ((low, high, name) in bands) {
        if (low >= maxFreq) break
        val clampedHigh = high.coerceAtMost(maxFreq)

        val yTop = chartH - (clampedHigh / maxFreq) * chartH + padding / 2
        val yBottom = chartH - (low / maxFreq) * chartH + padding / 2
        val color = BandColors[name] ?: AccentPrimary

        drawRect(
            color = color.copy(alpha = 0.04f),
            topLeft = Offset(padding, yTop),
            size = Size(chartW, yBottom - yTop)
        )
    }
}

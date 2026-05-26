package de.binauralbeats.app.ui.components

import androidx.annotation.StringRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.binauralbeats.app.R
import de.binauralbeats.app.ui.theme.AccentPrimary
import de.binauralbeats.app.ui.theme.OnSurfaceMuted
import kotlinx.coroutines.delay

enum class BreathPhase(@StringRes val labelRes: Int) {
    INHALE(R.string.inhale),
    HOLD(R.string.hold),
    EXHALE(R.string.exhale)
}

enum class BreathingPattern(@StringRes val labelRes: Int, val inhale: Int, val hold1: Int, val exhale: Int, val hold2: Int) {
    BOX(R.string.breath_pattern_box, 4, 4, 4, 4),
    RELAXING(R.string.breath_pattern_relaxing, 4, 7, 8, 0),
    CALM(R.string.breath_pattern_calm, 4, 2, 6, 0),
    ENERGIZE(R.string.breath_pattern_energize, 2, 0, 2, 0);

    val totalSeconds: Int get() = inhale + hold1 + exhale + hold2
}

@Composable
fun BreathingGuide(
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    var selectedPattern by remember { mutableStateOf(BreathingPattern.RELAXING) }
    var isRunning by remember { mutableStateOf(false) }
    var currentPhase by remember { mutableStateOf(BreathPhase.INHALE) }
    var progress by remember { mutableFloatStateOf(0f) }
    var cycleCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(isRunning, selectedPattern) {
        if (!isRunning) return@LaunchedEffect
        cycleCount = 0
        while (isRunning) {
            val p = selectedPattern
            val steps = buildList {
                if (p.inhale > 0) add(BreathPhase.INHALE to p.inhale)
                if (p.hold1 > 0) add(BreathPhase.HOLD to p.hold1)
                if (p.exhale > 0) add(BreathPhase.EXHALE to p.exhale)
                if (p.hold2 > 0) add(BreathPhase.HOLD to p.hold2)
            }

            for ((phase, duration) in steps) {
                currentPhase = phase
                val totalMs = duration * 1000L
                val stepMs = 50L
                var elapsed = 0L
                while (elapsed < totalMs && isRunning) {
                    progress = elapsed.toFloat() / totalMs
                    delay(stepMs)
                    elapsed += stepMs
                }
            }
            if (isRunning) cycleCount++
        }
    }

    LaunchedEffect(isActive) {
        if (!isActive) isRunning = false
    }

    Surface(
        color = Color.White.copy(alpha = 0.04f),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.breathing_header),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AccentPrimary,
                    letterSpacing = 2.sp
                )
                if (cycleCount > 0) {
                    Text(
                        stringResource(R.string.breathing_cycles, cycleCount),
                        fontSize = 11.sp,
                        color = OnSurfaceMuted
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                BreathingPattern.entries.forEach { pattern ->
                    Surface(
                        onClick = {
                            selectedPattern = pattern
                            isRunning = false
                        },
                        color = if (pattern == selectedPattern)
                            AccentPrimary.copy(alpha = 0.15f) else Color.Transparent,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            stringResource(pattern.labelRes),
                            fontSize = 10.sp,
                            color = if (pattern == selectedPattern) AccentPrimary
                            else OnSurfaceMuted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            BreathingCircle(
                isRunning = isRunning,
                currentPhase = currentPhase,
                progress = progress,
                pattern = selectedPattern
            )

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = { isRunning = !isRunning },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRunning) Color(0x33FF6B6B) else AccentPrimary.copy(alpha = 0.15f)
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text(
                    stringResource(if (isRunning) R.string.stop_label else R.string.start_label),
                    fontSize = 12.sp,
                    color = if (isRunning) Color(0xFFFF8A8A) else AccentPrimary
                )
            }
        }
    }
}

@Composable
private fun BreathingCircle(
    isRunning: Boolean,
    currentPhase: BreathPhase,
    progress: Float,
    pattern: BreathingPattern
) {
    val infiniteTransition = rememberInfiniteTransition(label = "breathPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val circleScale = if (isRunning) {
        when (currentPhase) {
            BreathPhase.INHALE -> 0.5f + 0.5f * progress
            BreathPhase.EXHALE -> 1f - 0.5f * progress
            BreathPhase.HOLD -> 1f
        }
    } else 0.6f

    Box(
        modifier = Modifier.size(120.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(120.dp)) {
            val center = Offset(size.width / 2, size.height / 2)
            val maxRadius = size.minDimension / 2 - 4f

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        AccentPrimary.copy(alpha = if (isRunning) 0.2f * circleScale else 0.05f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = maxRadius
                ),
                radius = maxRadius * circleScale,
                center = center
            )

            drawCircle(
                color = AccentPrimary.copy(alpha = if (isRunning) pulseAlpha else 0.3f),
                radius = maxRadius * circleScale,
                center = center,
                style = Stroke(width = 3f)
            )

            if (isRunning) {
                drawCircle(
                    color = AccentPrimary.copy(alpha = 0.1f),
                    radius = maxRadius * circleScale * 1.15f,
                    center = center,
                    style = Stroke(width = 1.5f)
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                if (isRunning) stringResource(currentPhase.labelRes) else stringResource(R.string.start),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = AccentPrimary
            )
            if (isRunning) {
                Text(
                    stringResource(pattern.labelRes),
                    fontSize = 10.sp,
                    color = OnSurfaceMuted
                )
            }
        }
    }
}

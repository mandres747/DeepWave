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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.binauralbeats.app.R
import de.binauralbeats.app.ui.theme.LocalBinauralColors

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

/**
 * The phases that actually run, in order. Zero-length ones are dropped - 4-7-8
 * has no second hold, ENERGIZE has none at all.
 *
 * This must stay in step with RhythmPattern.breathing, which drops the same
 * phases for the audible pulses. If the two ever disagree, the click and the
 * circle describe different breaths.
 */
internal fun BreathingPattern.phases(): List<Pair<BreathPhase, Int>> = buildList {
    if (inhale > 0) add(BreathPhase.INHALE to inhale)
    if (hold1 > 0) add(BreathPhase.HOLD to hold1)
    if (exhale > 0) add(BreathPhase.EXHALE to exhale)
    if (hold2 > 0) add(BreathPhase.HOLD to hold2)
}

/** Where a breath stands: which phase, how far into it, which cycle. */
data class BreathMoment(
    val phase: BreathPhase,
    val progress: Float,
    val cycle: Int
)

/**
 * Which phase is running after [elapsedMillis] and how far it has come.
 * Counterpart to stepAt for rhythm programs.
 *
 * Pure on purpose: the animation used to count its own milliseconds by adding
 * up delay() steps, which always run a little long and therefore drifted away
 * from the audible pulse. Deriving the phase from an elapsed time handed in
 * from outside means whoever owns the clock decides, and the awkward cases -
 * zero-length phases, the exact boundary, the wrap into the next cycle - are
 * settled by tests instead of by an animation loop.
 */
fun breathMomentAt(pattern: BreathingPattern, elapsedMillis: Long): BreathMoment {
    val phases = pattern.phases()
    val cycleMillis = pattern.totalSeconds * 1000L
    if (phases.isEmpty() || cycleMillis <= 0L) return BreathMoment(BreathPhase.INHALE, 0f, 0)

    val elapsed = elapsedMillis.coerceAtLeast(0L)
    val cycle = (elapsed / cycleMillis).toInt()
    var offset = elapsed % cycleMillis

    for ((phase, seconds) in phases) {
        val durationMillis = seconds * 1000L
        if (offset < durationMillis) {
            return BreathMoment(phase, offset.toFloat() / durationMillis, cycle)
        }
        offset -= durationMillis
    }

    // Unreachable while the phases add up to the cycle length. Returning the
    // end of the last phase beats throwing sixty times a second.
    return BreathMoment(phases.last().first, 1f, cycle)
}

/**
 * The visual half of the breathing guide. Owns no clock and no pattern of its
 * own - both are handed in, so the circle and the audible rhythm track can be
 * driven from a single source. Without that, the two ran on separate state and
 * separate timers and drifted apart within a cycle.
 */
@Composable
fun BreathingGuide(
    isActive: Boolean,
    pattern: BreathingPattern,
    isRunning: Boolean,
    elapsedMillis: () -> Long,
    onPatternChange: (BreathingPattern) -> Unit,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalBinauralColors.current
    val currentElapsed by rememberUpdatedState(elapsedMillis)
    val currentToggle by rememberUpdatedState(onToggle)
    var moment by remember { mutableStateOf(BreathMoment(BreathPhase.INHALE, 0f, 0)) }

    // One update per displayed frame, each asking the shared clock where the
    // breath stands. Nothing is counted here, so there is nothing to drift.
    LaunchedEffect(isRunning, pattern) {
        if (!isRunning) {
            moment = BreathMoment(BreathPhase.INHALE, 0f, 0)
            return@LaunchedEffect
        }
        while (true) {
            withFrameNanos { }
            moment = breathMomentAt(pattern, currentElapsed())
        }
    }

    // A session ending - the sleep timer running out, say - takes the guide
    // with it, as it did before.
    LaunchedEffect(isActive) {
        if (!isActive && isRunning) currentToggle()
    }

    val view = LocalView.current
    DisposableEffect(isRunning) {
        view.keepScreenOn = isRunning
        onDispose { view.keepScreenOn = false }
    }

    Surface(
        color = colors.overlay.copy(alpha = 0.04f),
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
                    color = colors.accentPrimary,
                    letterSpacing = 2.sp
                )
                if (moment.cycle > 0) {
                    Text(
                        pluralStringResource(R.plurals.breathing_cycles, moment.cycle, moment.cycle),
                        fontSize = 11.sp,
                        color = colors.onSurfaceMuted
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                BreathingPattern.entries.forEach { entry ->
                    Surface(
                        onClick = { onPatternChange(entry) },
                        color = if (entry == pattern)
                            colors.accentPrimary.copy(alpha = 0.15f) else Color.Transparent,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            stringResource(entry.labelRes),
                            fontSize = 10.sp,
                            color = if (entry == pattern) colors.accentPrimary
                            else colors.onSurfaceMuted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            BreathingCircle(
                isRunning = isRunning,
                currentPhase = moment.phase,
                progress = moment.progress,
                pattern = pattern
            )

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = { onToggle() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRunning) Color(0x33FF6B6B) else colors.accentPrimary.copy(alpha = 0.15f)
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text(
                    stringResource(if (isRunning) R.string.stop_label else R.string.start_label),
                    fontSize = 12.sp,
                    color = if (isRunning) Color(0xFFFF8A8A) else colors.accentPrimary
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
    val colors = LocalBinauralColors.current
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

    val accentColor = colors.accentPrimary

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
                        accentColor.copy(alpha = if (isRunning) 0.2f * circleScale else 0.05f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = maxRadius
                ),
                radius = maxRadius * circleScale,
                center = center
            )

            drawCircle(
                color = accentColor.copy(alpha = if (isRunning) pulseAlpha else 0.3f),
                radius = maxRadius * circleScale,
                center = center,
                style = Stroke(width = 3f)
            )

            if (isRunning) {
                drawCircle(
                    color = accentColor.copy(alpha = 0.1f),
                    radius = maxRadius * circleScale * 1.15f,
                    center = center,
                    style = Stroke(width = 1.5f)
                )
            }
        }

        // The circle reports, it does not offer. It used to read "Start" while
        // idle, right above a button reading "Starten" - two labels for one
        // action, and only one of them was actually tappable.
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (isRunning) {
                Text(
                    stringResource(currentPhase.labelRes),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.accentPrimary
                )
            }
            Text(
                stringResource(pattern.labelRes),
                fontSize = 10.sp,
                color = colors.onSurfaceMuted
            )
        }
    }
}

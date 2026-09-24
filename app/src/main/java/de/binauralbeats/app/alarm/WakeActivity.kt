package de.binauralbeats.app.alarm

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.binauralbeats.app.R
import de.binauralbeats.app.ui.components.BreathPhase
import de.binauralbeats.app.ui.components.BreathingPattern
import de.binauralbeats.app.ui.components.breathMomentAt
import de.binauralbeats.app.ui.theme.BinauralBeatsTheme
import de.binauralbeats.app.ui.theme.LocalBinauralColors
import de.binauralbeats.app.ui.theme.ThemeMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Date
import kotlin.math.roundToInt

/**
 * The full-screen alarm over the lock screen: time, a slowly breathing circle,
 * a large snooze button and a swipe to stop (decided 23.09.: a swipe is hard
 * to trigger by accident while half asleep, a tap is not).
 *
 * Owns no alarm state. Snooze and stop go to WakeAlarmService as intents, and
 * the screen closes itself when WakeAlarmService.ringing turns false - which
 * also covers the notification actions and the auto-stop.
 */
class WakeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showOverLockScreen()
        enableEdgeToEdge()

        // Back must not silently leave a ringing alarm behind the lock screen.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = Unit
        })

        val wakeAt = intent.getLongExtra(AlarmScheduler.EXTRA_WAKE_AT, System.currentTimeMillis())
        setContent {
            // Always dark: this screen is the first light of the morning.
            BinauralBeatsTheme(themeMode = ThemeMode.DARK) {
                val ringing by WakeAlarmService.ringing.collectAsState()
                LaunchedEffect(ringing) {
                    // Opened from a stale notification after the alarm ended.
                    if (!ringing) finishAndRemoveTask()
                }
                WakeScreen(
                    wakeAt = wakeAt,
                    onSnooze = { send(WakeAlarmService.ACTION_SNOOZE) },
                    onStop = { send(WakeAlarmService.ACTION_DISMISS) }
                )
            }
        }
    }

    private fun send(action: String) {
        startService(Intent(this, WakeAlarmService::class.java).setAction(action))
        finishAndRemoveTask()
    }

    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}

@Composable
private fun WakeScreen(wakeAt: Long, onSnooze: () -> Unit, onStop: () -> Unit) {
    val colors = LocalBinauralColors.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val timeText = remember(wakeAt) {
        android.text.format.DateFormat.getTimeFormat(context).format(Date(wakeAt))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(colors.primaryDark, colors.primaryMid)))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(32.dp))
                Text(stringResource(R.string.wake_notif_title), fontSize = 20.sp, color = colors.onSurfaceMuted)
                Text(timeText, fontSize = 72.sp, fontWeight = FontWeight.Light, color = colors.onSurface)
            }

            BreathingCircle(Modifier.size(220.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Button(
                    onClick = onSnooze,
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    shape = RoundedCornerShape(32.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accentPrimary)
                ) {
                    Text(
                        stringResource(R.string.wake_action_snooze, WakeSchedule.SNOOZE_MINUTES),
                        fontSize = 18.sp,
                        color = colors.onAccent
                    )
                }
                Spacer(Modifier.height(24.dp))
                SwipeToStop(onStop)
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

/**
 * The circle from the breathing guide, driven by the same pure
 * breathMomentAt, on the calm 4-2-6 pattern: slow enough to look like
 * something still asleep, and a cue to take a first deep breath.
 */
@Composable
private fun BreathingCircle(modifier: Modifier) {
    val colors = LocalBinauralColors.current
    var elapsed by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        val start = withFrameMillis { it }
        while (true) {
            elapsed = withFrameMillis { it } - start
        }
    }
    val moment = breathMomentAt(BreathingPattern.CALM, elapsed)
    val fill = when (moment.phase) {
        BreathPhase.INHALE -> moment.progress
        BreathPhase.HOLD -> 1f
        BreathPhase.EXHALE -> 1f - moment.progress
    }
    Canvas(modifier) {
        val maxR = size.minDimension / 2
        val r = maxR * (0.45f + 0.55f * fill)
        drawCircle(colors.accentPrimary.copy(alpha = 0.10f), radius = maxR)
        drawCircle(colors.accentPrimary.copy(alpha = 0.35f), radius = r)
        drawCircle(colors.accentSecondary.copy(alpha = 0.55f), radius = r * 0.55f)
    }
}

/**
 * A thumb that has to be dragged across most of the track. Let go early and
 * it springs back. TalkBack users get a plain "stop" action instead, since a
 * drag gesture is awkward with a screen reader.
 */
@Composable
private fun SwipeToStop(onStop: () -> Unit) {
    val colors = LocalBinauralColors.current
    val scope = rememberCoroutineScope()
    val label = stringResource(R.string.wake_swipe_to_stop)
    val stopLabel = stringResource(R.string.wake_action_dismiss)
    val thumb = 64.dp

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(thumb)
            .clip(RoundedCornerShape(32.dp))
            .background(colors.overlay.copy(alpha = 0.10f))
            .semantics {
                customActions = listOf(CustomAccessibilityAction(stopLabel) { onStop(); true })
            }
    ) {
        val density = LocalDensity.current
        val maxPx = with(density) { (maxWidth - thumb).toPx() }.coerceAtLeast(1f)
        val offset = remember { Animatable(0f) }
        val progress = offset.value / maxPx

        Text(
            label,
            modifier = Modifier.align(Alignment.Center).alpha(1f - progress),
            color = colors.onSurfaceMuted,
            fontSize = 16.sp
        )
        Box(
            modifier = Modifier
                .offset { IntOffset(offset.value.roundToInt(), 0) }
                .size(thumb)
                .clip(CircleShape)
                .background(colors.onSurface)
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        scope.launch { offset.snapTo((offset.value + delta).coerceIn(0f, maxPx)) }
                    },
                    onDragStopped = {
                        if (offset.value >= maxPx * STOP_THRESHOLD) {
                            offset.animateTo(maxPx)
                            delay(80)
                            onStop()
                        } else {
                            offset.animateTo(0f)
                        }
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.AlarmOff, contentDescription = null, tint = colors.primaryDark)
        }
    }
}

private const val STOP_THRESHOLD = 0.85f

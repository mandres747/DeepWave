package de.binauralbeats.app.ui.screens

import android.app.Activity
import android.text.format.DateFormat
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import de.binauralbeats.app.R
import de.binauralbeats.app.alarm.WakeAlarmLabels
import de.binauralbeats.app.alarm.WakeRamps
import de.binauralbeats.app.billing.PurchaseResult
import de.binauralbeats.app.data.AmbientSound
import de.binauralbeats.app.data.WakeAlarm
import de.binauralbeats.app.ui.WakeAlarmViewModel
import de.binauralbeats.app.ui.theme.LocalBinauralColors
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale
import de.binauralbeats.app.ui.theme.TitleFont

/**
 * The wake-alarm add-on's sheet: a list of alarms with an editor dialog, or -
 * before purchase - what it does, a listen button and the unlock. Layout
 * follows RhythmSheet so the two add-ons look like one family.
 */
@Composable
fun WakeAlarmSheet(
    vm: WakeAlarmViewModel,
    onClose: () -> Unit,
    /** Opens the bundle offer; null while the bundle is not on offer. */
    onShowBundle: (() -> Unit)? = null
) {
    val colors = LocalBinauralColors.current
    val context = LocalContext.current
    val owned by vm.owned.collectAsState()
    val price by vm.price.collectAsState()
    val alarms by vm.alarms.collectAsState()

    val ringsIn = rememberRingsInToast()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colors.surfaceDark.copy(alpha = 0.97f),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.wake_header),
                    fontSize = 20.sp,
                    color = colors.accentPrimary,
                    fontFamily = TitleFont
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (owned) {
                        IconButton(onClick = { vm.editing = vm.newAlarm() }) {
                            Icon(Icons.Default.Add, stringResource(R.string.wake_add), tint = colors.accentPrimary)
                        }
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, stringResource(R.string.close), tint = colors.onSurface)
                    }
                }
            }
            HorizontalDivider(color = colors.overlay.copy(0.06f))
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.wake_hint),
                fontSize = 12.sp,
                color = colors.onSurfaceMuted,
                lineHeight = 18.sp
            )
            Spacer(Modifier.height(20.dp))

            if (!owned) {
                WakeLockedCard(
                    price = price,
                    onListen = { vm.playPreview(vm.newAlarm()) },
                    onPurchase = {
                        val activity = context as? Activity
                        if (activity == null) {
                            Toast.makeText(context, R.string.rhythm_purchase_unavailable, Toast.LENGTH_SHORT).show()
                        } else {
                            vm.purchase(activity) { result ->
                                Toast.makeText(context, purchaseMessage(result), Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onRestore = { vm.restore() },
                    onShowBundle = onShowBundle
                )
                Spacer(Modifier.height(24.dp))
                return@Column
            }

            if (!vm.canScheduleExact && alarms.any { it.enabled }) {
                PermissionCard(
                    text = stringResource(R.string.wake_exact_missing),
                    onFix = { context.startActivity(vm.exactAlarmSettingsIntent()) }
                )
                Spacer(Modifier.height(12.dp))
            }
            if (!vm.canFullScreen) {
                PermissionCard(
                    text = stringResource(R.string.wake_fullscreen_missing),
                    onFix = { context.startActivity(vm.fullScreenSettingsIntent()) }
                )
                Spacer(Modifier.height(12.dp))
            }

            if (alarms.isEmpty()) {
                Text(
                    stringResource(R.string.wake_empty),
                    fontSize = 13.sp,
                    color = colors.onSurfaceMuted,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            }
            alarms.sortedWith(compareBy({ it.hour }, { it.minute })).forEach { alarm ->
                AlarmRow(
                    alarm = alarm,
                    waiting = alarm.enabled && !vm.canScheduleExact,
                    onClick = { vm.editing = alarm },
                    onToggle = { on -> vm.setEnabled(alarm, on) { ringsIn(it) } }
                )
                Spacer(Modifier.height(8.dp))
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    vm.editing?.let { alarm ->
        AlarmEditor(
            initial = alarm,
            isNew = alarms.none { it.id == alarm.id },
            onListen = { vm.playPreview(it) },
            onStopListen = { vm.stopPreview() },
            onSave = { vm.save(it.copy(enabled = true)) { d -> ringsIn(d) } },
            onDelete = { vm.delete(alarm) },
            onDismiss = { vm.stopPreview(); vm.editing = null }
        )
    }

    if (vm.showExactAlarmExplanation) {
        AlertDialog(
            onDismissRequest = { vm.showExactAlarmExplanation = false },
            title = { Text(stringResource(R.string.wake_exact_title)) },
            text = { Text(stringResource(R.string.wake_exact_body)) },
            confirmButton = {
                TextButton(onClick = {
                    vm.showExactAlarmExplanation = false
                    context.startActivity(vm.exactAlarmSettingsIntent())
                }) { Text(stringResource(R.string.wake_exact_open)) }
            },
            dismissButton = {
                TextButton(onClick = { vm.showExactAlarmExplanation = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun rememberRingsInToast(): (Duration?) -> Unit {
    val context = LocalContext.current
    return remember(context) {
        { duration ->
            if (duration != null) {
                val (h, m) = WakeAlarmLabels.until(duration)
                val text = if (h == 0L) context.getString(R.string.wake_rings_in_minutes, m.toInt())
                else context.getString(R.string.wake_rings_in, h.toInt(), m.toInt())
                Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
            }
        }
    }
}

private fun purchaseMessage(result: PurchaseResult): Int = when (result) {
    PurchaseResult.OWNED -> R.string.wake_purchase_thanks
    PurchaseResult.CANCELLED -> R.string.rhythm_purchase_cancelled
    PurchaseResult.UNAVAILABLE -> R.string.rhythm_purchase_unavailable
    PurchaseResult.ERROR -> R.string.rhythm_purchase_error
}

@Composable
private fun WakeLockedCard(
    price: String?,
    onListen: () -> Unit,
    onPurchase: () -> Unit,
    onRestore: () -> Unit,
    onShowBundle: (() -> Unit)?
) {
    val colors = LocalBinauralColors.current
    Surface(
        color = colors.accentPrimary.copy(alpha = 0.08f),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                stringResource(R.string.wake_locked_title),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurface
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.wake_locked_body),
                fontSize = 13.sp,
                color = colors.onSurfaceMuted,
                lineHeight = 19.sp
            )
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onListen, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.PlayArrow, null, tint = colors.accentPrimary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.wake_listen), color = colors.accentPrimary)
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onPurchase,
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accentPrimary,
                    contentColor = colors.onAccent
                )
            ) {
                Text(
                    if (price != null) stringResource(R.string.wake_unlock_price, price)
                    else stringResource(R.string.wake_unlock),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(4.dp))
            TextButton(onClick = onRestore, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.wake_restore), fontSize = 12.sp, color = colors.onSurfaceMuted)
            }
            onShowBundle?.let { BundleHint(it) }
        }
    }
}

@Composable
private fun PermissionCard(text: String, onFix: () -> Unit) {
    val colors = LocalBinauralColors.current
    Surface(
        color = colors.warning.copy(alpha = 0.12f),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.WarningAmber, null, tint = colors.warning, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(text, fontSize = 12.sp, color = colors.onSurface, lineHeight = 17.sp, modifier = Modifier.weight(1f))
            TextButton(onClick = onFix) { Text(stringResource(R.string.wake_fix), color = colors.warning) }
        }
    }
}

@Composable
private fun AlarmRow(
    alarm: WakeAlarm,
    waiting: Boolean,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit
) {
    val colors = LocalBinauralColors.current
    val dim = if (alarm.enabled) 1f else 0.5f
    Surface(
        color = colors.overlay.copy(alpha = 0.05f),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    formatTime(alarm.hour, alarm.minute),
                    fontSize = 32.sp,
                    fontFamily = TitleFont,
                    color = colors.onSurface.copy(alpha = dim)
                )
                Text(
                    listOf(daysText(alarm.days), rampName(alarm.rampKey), "${alarm.rampMinutes}′")
                        .joinToString(" · "),
                    fontSize = 12.sp,
                    color = colors.onSurfaceMuted.copy(alpha = dim)
                )
                if (waiting) {
                    Text(
                        stringResource(R.string.wake_waiting_permission),
                        fontSize = 11.sp,
                        color = colors.warning
                    )
                }
            }
            Switch(
                checked = alarm.enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(checkedTrackColor = colors.accentPrimary)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlarmEditor(
    initial: WakeAlarm,
    isNew: Boolean,
    onListen: (WakeAlarm) -> Unit,
    onStopListen: () -> Unit,
    onSave: (WakeAlarm) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalBinauralColors.current
    val context = LocalContext.current
    var draft by remember(initial.id) { mutableStateOf(initial) }
    var listening by remember { mutableStateOf(false) }
    val timeState = rememberTimePickerState(
        initialHour = initial.hour,
        initialMinute = initial.minute,
        is24Hour = DateFormat.is24HourFormat(context)
    )
    DisposableEffect(Unit) { onDispose { onStopListen() } }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            color = colors.surfaceDark,
            shape = RoundedCornerShape(20.dp),
            // Capped height with the buttons in a fixed footer: the editor is
            // taller than a phone screen, and without usePlatformDefaultWidth
            // the dialog window grew under the navigation bar with Save
            // unreachable behind it (Galaxy A54, 24.09.). Window insets did not
            // reach the dialog, so the cap does not rely on them.
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.86f)
        ) {
            Column {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp, top = 16.dp, end = 16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        stringResource(R.string.wake_edit_title),
                        fontSize = 20.sp,
                        color = colors.accentPrimary,
                        fontFamily = TitleFont
                    )
                    Spacer(Modifier.height(12.dp))
                    TimePicker(
                        state = timeState,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        colors = TimePickerDefaults.colors(
                            clockDialColor = colors.overlay.copy(alpha = 0.06f),
                            selectorColor = colors.accentPrimary,
                            timeSelectorSelectedContainerColor = colors.accentPrimary.copy(alpha = 0.2f),
                            timeSelectorSelectedContentColor = colors.accentPrimary
                        )
                    )

                    EditorLabel(stringResource(R.string.wake_days))
                    DayChips(draft.days) { draft = draft.copy(days = it) }
                    if (draft.days.isEmpty()) {
                        Text(
                            stringResource(R.string.wake_days_none_hint),
                            fontSize = 11.sp,
                            color = colors.onSurfaceMuted,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    EditorLabel(stringResource(R.string.wake_ramp))
                    ChoiceRow(
                        options = WakeRamps.all.keys.map { it to rampName(it) },
                        selected = draft.rampKey
                    ) { draft = draft.copy(rampKey = it) }
                    Spacer(Modifier.height(8.dp))
                    ChoiceRow(
                        options = WakeRamps.DURATIONS.map { it to stringResource(R.string.wake_ramp_minutes, it) },
                        selected = draft.rampMinutes
                    ) { draft = draft.copy(rampMinutes = it) }

                    EditorLabel(stringResource(R.string.wake_volume))
                    Slider(
                        value = draft.volume,
                        onValueChange = { draft = draft.copy(volume = it) },
                        valueRange = 0.1f..1f,
                        colors = SliderDefaults.colors(thumbColor = colors.accentPrimary, activeTrackColor = colors.accentPrimary)
                    )

                    EditorLabel(stringResource(R.string.wake_ambient))
                    // Opens scrolled to the chosen sound; the default (stream) sits
                    // past the right edge otherwise and the row looks unselected.
                    val ambientOptions = listOf<AmbientSound?>(null) + AmbientSound.entries
                    LazyRow(
                        state = rememberLazyListState(
                            initialFirstVisibleItemIndex = ambientOptions.indexOf(initial.ambient).coerceAtLeast(0)
                        ),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(ambientOptions) { sound ->
                            val label = if (sound == null) stringResource(R.string.wake_ambient_none)
                            else "${sound.emoji} ${stringResource(sound.labelRes)}"
                            Chip(label, draft.ambient == sound) { draft = draft.copy(ambient = sound) }
                        }
                    }
                    if (draft.ambient != null) {
                        Slider(
                            value = draft.ambientVolume,
                            onValueChange = { draft = draft.copy(ambientVolume = it) },
                            valueRange = 0.05f..1f,
                            colors = SliderDefaults.colors(thumbColor = colors.accentPrimary, activeTrackColor = colors.accentPrimary)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(R.string.wake_vibrate),
                            fontSize = 13.sp,
                            color = colors.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = draft.vibrate,
                            onCheckedChange = { draft = draft.copy(vibrate = it) },
                            colors = SwitchDefaults.colors(checkedTrackColor = colors.accentPrimary)
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = {
                            if (listening) onStopListen() else onListen(draft)
                            listening = !listening
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            if (listening) Icons.Default.Stop else Icons.Default.PlayArrow,
                            null, tint = colors.accentPrimary, modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            stringResource(if (listening) R.string.wake_listen_stop else R.string.wake_listen),
                            color = colors.accentPrimary
                        )
                    }
                }

                HorizontalDivider(color = colors.overlay.copy(0.06f))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    if (!isNew) {
                        TextButton(onClick = onDelete) {
                            Text(stringResource(R.string.delete), color = colors.onSurfaceMuted)
                        }
                        Spacer(Modifier.weight(1f))
                    }
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel), color = colors.onSurfaceMuted)
                    }
                    Button(
                        onClick = { onSave(draft.copy(hour = timeState.hour, minute = timeState.minute)) },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accentPrimary, contentColor = colors.onAccent)
                    ) { Text(stringResource(R.string.save)) }
                }
            }
        }
    }
}

@Composable
private fun EditorLabel(text: String) {
    val colors = LocalBinauralColors.current
    Text(
        text,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = colors.onSurfaceMuted,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
    )
}

/** Mon..Sun in the user's locale; the week starts on Monday as ISO does. */
@Composable
private fun DayChips(selected: Set<Int>, onChange: (Set<Int>) -> Unit) {
    val locale = currentLocale()
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        (1..7).forEach { day ->
            val on = day in selected
            Chip(
                shortDay(day, locale),
                on,
                modifier = Modifier.weight(1f)
            ) { onChange(if (on) selected - day else selected + day) }
        }
    }
}

@Composable
private fun <T> ChoiceRow(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (value, label) ->
            Chip(label, value == selected, modifier = Modifier.weight(1f)) { onSelect(value) }
        }
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = LocalBinauralColors.current
    Surface(
        onClick = onClick,
        color = if (selected) colors.accentPrimary.copy(alpha = 0.15f) else colors.overlay.copy(alpha = 0.06f),
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                label,
                fontSize = 12.sp,
                maxLines = 1,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) colors.accentPrimary else colors.onSurfaceMuted
            )
        }
    }
}

@Composable
private fun currentLocale(): Locale = LocalConfiguration.current.locales[0]

private fun shortDay(day: Int, locale: Locale): String =
    DayOfWeek.of(day).getDisplayName(TextStyle.SHORT, locale).removeSuffix(".")

@Composable
private fun formatTime(hour: Int, minute: Int): String {
    val context = LocalContext.current
    val pattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a"
    return LocalTime.of(hour, minute).format(java.time.format.DateTimeFormatter.ofPattern(pattern, currentLocale()))
}

@Composable
private fun daysText(days: Set<Int>): String {
    val locale = currentLocale()
    return when (val d = WakeAlarmLabels.days(days)) {
        WakeAlarmLabels.Days.Once -> stringResource(R.string.wake_once)
        WakeAlarmLabels.Days.Daily -> stringResource(R.string.wake_daily)
        WakeAlarmLabels.Days.Weekdays -> stringResource(R.string.wake_weekdays)
        WakeAlarmLabels.Days.Weekend -> stringResource(R.string.wake_weekend)
        is WakeAlarmLabels.Days.Some -> d.days.joinToString(", ") { shortDay(it, locale) }
    }
}

@Composable
private fun rampName(key: String): String = stringResource(
    when (key) {
        "gentle" -> R.string.wake_ramp_gentle
        "energetic" -> R.string.wake_ramp_energetic
        else -> R.string.wake_ramp_fresh
    }
)

/**
 * The line under the sleep timer (decided 2026-09-24): shows an alarm that
 * already rings by tomorrow morning, or switches a one-off one at the last
 * used time; non-buyers get a quiet pointer to the add-on instead.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepTimerWakeRow(vm: WakeAlarmViewModel, onOpenWakeSheet: () -> Unit) {
    val colors = LocalBinauralColors.current
    val context = LocalContext.current
    val owned by vm.owned.collectAsState()
    val alarms by vm.alarms.collectAsState()
    val lastMinute by vm.lastWakeMinuteOfDay.collectAsState()
    val ringsIn = rememberRingsInToast()

    if (!owned) {
        Text(
            stringResource(R.string.sleep_wake_teaser),
            fontSize = 12.sp,
            color = colors.accentPrimary,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenWakeSheet)
                .padding(vertical = 8.dp)
        )
        return
    }

    val covering = vm.coveringAlarm(alarms)
    if (covering != null) {
        val (_, at) = covering
        Text(
            stringResource(R.string.sleep_wake_existing, formatTime(at.hour, at.minute)),
            fontSize = 12.sp,
            color = colors.onSurfaceMuted,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenWakeSheet)
                .padding(vertical = 8.dp)
        )
        return
    }

    val own = vm.sleepTimerAlarm(alarms)
    val hour = own?.hour ?: (lastMinute / 60)
    val minute = own?.minute ?: (lastMinute % 60)
    var picking by remember { mutableStateOf(false) }

    Surface(
        color = colors.accentPrimary.copy(alpha = 0.08f),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.sleep_wake_row),
                fontSize = 13.sp,
                color = colors.onSurface,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = { picking = true }) {
                Text(formatTime(hour, minute), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = colors.accentPrimary)
            }
            Switch(
                checked = own != null,
                onCheckedChange = { on -> vm.setSleepTimerWake(on, hour, minute) { ringsIn(it) } },
                colors = SwitchDefaults.colors(checkedTrackColor = colors.accentPrimary)
            )
        }
    }

    if (picking) {
        val state = rememberTimePickerState(hour, minute, DateFormat.is24HourFormat(context))
        AlertDialog(
            onDismissRequest = { picking = false },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    picking = false
                    // Picking a time means "wake me then": switch it on.
                    vm.setSleepTimerWake(true, state.hour, state.minute) { ringsIn(it) }
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { picking = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

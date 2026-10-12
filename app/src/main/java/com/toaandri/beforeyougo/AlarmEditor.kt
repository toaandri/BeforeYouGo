package com.toaandri.beforeyougo

import android.app.Activity
import android.app.TimePickerDialog
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AlarmEditor(alarm: DepartureAlarm, objects: List<SavedItem>, onDismiss: () -> Unit, onSave: (DepartureAlarm) -> Unit) {
    val context = LocalContext.current
    var name by rememberSaveable(alarm.id) { mutableStateOf(alarm.name) }
    var hour by rememberSaveable(alarm.id) { mutableIntStateOf(alarm.hour) }
    var minute by rememberSaveable(alarm.id) { mutableIntStateOf(alarm.minute) }
    var days by rememberSaveable(alarm.id) { mutableStateOf(alarm.days.toList()) }
    var deliveryMode by rememberSaveable(alarm.id) { mutableStateOf(alarm.deliveryMode) }
    var sound by rememberSaveable(alarm.id) { mutableStateOf(alarm.soundUri) }
    var soundName by rememberSaveable(alarm.id) { mutableStateOf(alarm.displaySoundName) }
    var vibrate by rememberSaveable(alarm.id) { mutableStateOf(alarm.vibrate) }
    var snooze by rememberSaveable(alarm.id) { mutableIntStateOf(alarm.snoozeMinutes) }
    var allItems by rememberSaveable(alarm.id) { mutableStateOf(alarm.allItems) }
    var chosenItems by rememberSaveable(alarm.id) { mutableStateOf(alarm.itemIds.toList()) }
    var soundError by remember { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            @Suppress("DEPRECATION")
            val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            sound = uri?.toString() ?: "silent"
            soundName = if (uri == null) uiText(R.string.ui_silent) else runCatching {
                RingtoneManager.getRingtone(context, uri)?.getTitle(context)
            }.getOrNull() ?: uiText(R.string.ui_custom_ringtone)
        }
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onDismiss) { Icon(Icons.Outlined.Close, uiText(R.string.ui_close)) }
                    Text(uiText(R.string.ui_a_departure_page), style = MaterialTheme.typography.titleLarge, fontFamily = FontFamily.Serif)
                }
                LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(uiText(R.string.ui_the_appointment), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, letterSpacing = 2.sp)
                            Text(uiText(R.string.ui_where_are_we_going), style = MaterialTheme.typography.headlineLarge, fontFamily = FontFamily.Serif)
                            Text(uiText(R.string.ui_a_small_ritual_before_you_go_at_your_own_pace), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    item { Panel {
                        OutlinedTextField(name, { name = it.take(60) }, Modifier.fillMaxWidth(), label = { Text(uiText(R.string.ui_name_this_departure)) },
                            placeholder = { Text(uiText(R.string.ui_work_a_weekend_away_a_workout)) }, singleLine = true, shape = RoundedCornerShape(18.dp))
                        Text(uiText(R.string.ui_when_should_we_give_you_a_nudge), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Row(Modifier.fillMaxWidth().clickable(role = Role.Button, onClickLabel = uiText(R.string.ui_change_departure_time)) {
                            TimePickerDialog(context, { _, h, m -> hour = h; minute = m }, hour, minute, true).show()
                        }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("%02d:%02d".format(hour, minute), fontSize = 56.sp, fontFamily = FontFamily.Serif, modifier = Modifier.weight(1f))
                            Icon(Icons.Outlined.Schedule, null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Text(uiText(R.string.ui_choose_your_moment_a_little_before_leaving_if_you_prefer), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } }
                    item { Panel {
                        Text(uiText(R.string.ui_the_days_that_fit), style = MaterialTheme.typography.titleLarge, fontFamily = FontFamily.Serif)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(uiText(R.string.ui_mon), uiText(R.string.ui_tue), uiText(R.string.ui_wed), uiText(R.string.ui_thu), uiText(R.string.ui_fri), uiText(R.string.ui_sat), uiText(R.string.ui_sun)).forEachIndexed { index, label ->
                                val day = index + 1
                                FilterChip(day in days, { days = if (day in days) days - day else days + day }, label = { Text(label) })
                            }
                        }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton({ days = (1..5).toList() }) { Text(uiText(R.string.ui_weekdays)) }
                            TextButton({ days = listOf(6, 7) }) { Text(uiText(R.string.ui_weekend)) }
                            TextButton({ days = (1..7).toList() }) { Text(uiText(R.string.ui_every_day)) }
                        }
                        if (days.isEmpty()) Text(uiText(R.string.ui_choose_at_least_one_day_for_this_appointment), color = MaterialTheme.colorScheme.error)
                    } }
                    item { Panel {
                        Text(uiText(R.string.ui_your_little_nudge), style = MaterialTheme.typography.titleLarge, fontFamily = FontFamily.Serif)
                        Text(uiText(R.string.ui_quiet_or_with_a_sound_you_choose), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        listOf(DeliveryMode.NOTIFICATION, DeliveryMode.RING).forEach { mode ->
                            val selected = deliveryMode == mode
                            Surface(
                                modifier = Modifier.fillMaxWidth().selectable(selected, role = Role.RadioButton) { deliveryMode = mode },
                                shape = RoundedCornerShape(20.dp),
                                color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(selected, onClick = null)
                                    Column(Modifier.padding(start = 10.dp).weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(if (mode == DeliveryMode.NOTIFICATION) uiText(R.string.ui_a_quiet_note) else uiText(R.string.ui_a_sound), fontWeight = FontWeight.SemiBold)
                                        Text(if (mode == DeliveryMode.NOTIFICATION) uiText(R.string.ui_a_notification_without_sound_or_vibration) else uiText(R.string.ui_a_sound_with_stop_and_snooze_controls), style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                        Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(18.dp)) {
                            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(uiText(R.string.ui_a_preview_of_the_tone), style = MaterialTheme.typography.labelSmall, letterSpacing = 1.sp)
                                Text(uiText(R.string.ui_shall_we_check_the_bag), fontWeight = FontWeight.SemiBold)
                                Text(uiText(R.string.ui_a_quick_check_then_off_you_go, name.ifBlank { uiText(R.string.ui_your_departure) }), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Column(Modifier.animateContentSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (deliveryMode == DeliveryMode.RING) {
                                HorizontalDivider()
                                Text(uiText(R.string.ui_sound_your_way), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                OutlinedButton({
                                    val existing = when (sound) {
                                        "silent" -> null
                                        "default" -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                                        else -> Uri.parse(sound)
                                    }
                                    runCatching {
                                        picker.launch(Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                                            .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                                            .putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, uiText(R.string.ui_departure_sound))
                                            .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                                            .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                            .putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
                                            .putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, existing))
                                    }.onFailure { soundError = uiText(R.string.ui_the_ringtone_picker_is_unavailable_on_this_phone) }
                                }, Modifier.fillMaxWidth()) {
                                    Icon(Icons.Outlined.MusicNote, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text(soundName)
                                }
                                soundError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(uiText(R.string.ui_with_vibration), Modifier.weight(1f))
                                    Switch(vibrate, { vibrate = it })
                                }
                                if (sound == "silent" && !vibrate) Text(uiText(R.string.ui_with_these_settings_only_a_notification_will_appear), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Text(uiText(R.string.ui_a_few_more_minutes), fontWeight = FontWeight.SemiBold)
                        Text(uiText(R.string.ui_how_long_to_wait_when_you_want_to_come_back), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(5, 10, 15).forEach { duration ->
                                FilterChip(snooze == duration, { snooze = duration }, label = { Text(uiText(R.string.ui_min, duration)) })
                            }
                        }
                    } }
                    item { Panel {
                        Text(uiText(R.string.ui_to_bring_along), style = MaterialTheme.typography.titleLarge, fontFamily = FontFamily.Serif)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(uiText(R.string.ui_my_whole_daily_collection), Modifier.weight(1f))
                            Switch(allItems, { allItems = it })
                        }
                        Text(if (allItems) uiText(R.string.ui_new_objects_also_join_this_departure_starting_on_their_chosen) else uiText(R.string.ui_compose_this_departure_bag_dated_objects_appear_from_their_scheduled), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (!allItems && objects.isEmpty()) Text(uiText(R.string.ui_your_collection_is_empty_add_objects_in_the_my_objects))
                        if (!allItems && objects.isNotEmpty()) {
                            val count = objects.count { it.id in chosenItems }
                            Text(if (count > 1) uiText(R.string.ui_objects_selected, count) else uiText(R.string.ui_object_selected, count), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        }
                    } }
                    if (!allItems) items(objects, key = { it.id }) { item ->
                        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
                            Row(Modifier.fillMaxWidth().toggleable(item.id in chosenItems, role = Role.Checkbox) { checked ->
                                chosenItems = if (checked) chosenItems + item.id else chosenItems - item.id
                            }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                ObjectBadge(iconFor(item))
                                Spacer(Modifier.width(12.dp))
                                Text(item.title, Modifier.weight(1f))
                                Checkbox(item.id in chosenItems, null)
                            }
                        }
                    }
                }
                Button({
                    onSave(alarm.copy(name = name.trim(), hour = hour, minute = minute, days = days.toSet(), deliveryMode = deliveryMode,
                        soundUri = sound, soundName = soundName, vibrate = vibrate, snoozeMinutes = snooze, allItems = allItems,
                        itemIds = chosenItems.toSet(), skipUntil = 0))
                }, enabled = name.isNotBlank() && days.isNotEmpty(), modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp).heightIn(min = 52.dp)) {
                    Text(uiText(R.string.ui_save_this_departure))
                }
            }
        }
    }
}

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
    var soundName by rememberSaveable(alarm.id) { mutableStateOf(alarm.soundName) }
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
            soundName = if (uri == null) "Silencieux" else runCatching {
                RingtoneManager.getRingtone(context, uri)?.getTitle(context)
            }.getOrNull() ?: "Sonnerie personnalisée"
        }
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onDismiss) { Icon(Icons.Outlined.Close, "Fermer") }
                    Text("Une page de départ", style = MaterialTheme.typography.titleLarge, fontFamily = FontFamily.Serif)
                }
                LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("LE RENDEZ-VOUS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, letterSpacing = 2.sp)
                            Text("On part où ?", style = MaterialTheme.typography.headlineLarge, fontFamily = FontFamily.Serif)
                            Text("Un petit rituel avant de filer. À ton rythme.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    item { Panel {
                        OutlinedTextField(name, { name = it.take(60) }, Modifier.fillMaxWidth(), label = { Text("Ce départ s’appelle…") },
                            placeholder = { Text("Bureau, escapade, séance de sport…") }, singleLine = true, shape = RoundedCornerShape(18.dp))
                        Text("À quelle heure te faire signe ?", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Row(Modifier.fillMaxWidth().clickable(role = Role.Button, onClickLabel = "Changer l’heure du départ") {
                            TimePickerDialog(context, { _, h, m -> hour = h; minute = m }, hour, minute, true).show()
                        }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("%02d:%02d".format(hour, minute), fontSize = 56.sp, fontFamily = FontFamily.Serif, modifier = Modifier.weight(1f))
                            Icon(Icons.Outlined.Schedule, null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Text("Choisis ton moment, un peu avant de partir si tu préfères.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } }
                    item { Panel {
                        Text("Les jours qui vont avec", style = MaterialTheme.typography.titleLarge, fontFamily = FontFamily.Serif)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim").forEachIndexed { index, label ->
                                val day = index + 1
                                FilterChip(day in days, { days = if (day in days) days - day else days + day }, label = { Text(label) })
                            }
                        }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton({ days = (1..5).toList() }) { Text("Semaine") }
                            TextButton({ days = listOf(6, 7) }) { Text("Week-end") }
                            TextButton({ days = (1..7).toList() }) { Text("Tous les jours") }
                        }
                        if (days.isEmpty()) Text("Choisis au moins un jour pour ce rendez-vous.", color = MaterialTheme.colorScheme.error)
                    } }
                    item { Panel {
                        Text("Ton petit signe", style = MaterialTheme.typography.titleLarge, fontFamily = FontFamily.Serif)
                        Text("Discret ou sonore, c’est toi qui choisis.", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                        Text(if (mode == DeliveryMode.NOTIFICATION) "Un mot discret" else "Une sonnerie", fontWeight = FontWeight.SemiBold)
                                        Text(if (mode == DeliveryMode.NOTIFICATION) "Une notification, sans son ni vibration." else "Un signal sonore, avec arrêt et report à la demande.", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                        Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(18.dp)) {
                            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("UN APERÇU DU TON", style = MaterialTheme.typography.labelSmall, letterSpacing = 1.sp)
                                Text("On fait le tour ?", fontWeight = FontWeight.SemiBold)
                                Text("${name.ifBlank { "Ton départ" }} · Un petit tour et tu files.", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Column(Modifier.animateContentSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (deliveryMode == DeliveryMode.RING) {
                                HorizontalDivider()
                                Text("Le son à ta façon", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                OutlinedButton({
                                    val existing = when (sound) {
                                        "silent" -> null
                                        "default" -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                                        else -> Uri.parse(sound)
                                    }
                                    runCatching {
                                        picker.launch(Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                                            .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                                            .putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Son de ce départ")
                                            .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                                            .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                            .putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
                                            .putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, existing))
                                    }.onFailure { soundError = "Le sélecteur de sonnerie n’est pas disponible sur ce téléphone." }
                                }, Modifier.fillMaxWidth()) {
                                    Icon(Icons.Outlined.MusicNote, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text(soundName)
                                }
                                soundError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Avec une vibration", Modifier.weight(1f))
                                    Switch(vibrate, { vibrate = it })
                                }
                                if (sound == "silent" && !vibrate) Text("Avec ces réglages, seule la notification s’affichera.", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Text("Encore quelques minutes ?", fontWeight = FontWeight.SemiBold)
                        Text("La durée du report quand tu préfères y revenir plus tard.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(5, 10, 15).forEach { duration ->
                                FilterChip(snooze == duration, { snooze = duration }, label = { Text("$duration min") })
                            }
                        }
                    } }
                    item { Panel {
                        Text("À emporter", style = MaterialTheme.typography.titleLarge, fontFamily = FontFamily.Serif)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Toute ma collection du jour", Modifier.weight(1f))
                            Switch(allItems, { allItems = it })
                        }
                        Text(if (allItems) "Les nouveaux objets rejoignent aussi ce départ, à partir de la date choisie." else "Compose le sac de ce départ. Les objets datés apparaîtront à partir du jour prévu.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (!allItems && objects.isEmpty()) Text("Ta collection est encore vide. Tu peux la composer dans l’onglet Mes objets.")
                        if (!allItems && objects.isNotEmpty()) {
                            val count = objects.count { it.id in chosenItems }
                            Text(if (count > 1) "$count objets choisis" else "$count objet choisi", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
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
                    Text("Enregistrer ce départ")
                }
            }
        }
    }
}

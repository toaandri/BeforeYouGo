package com.toaandri.beforeyougo

import android.app.Activity
import android.app.TimePickerDialog
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
            soundName = if (uri == null) "Silencieux" else runCatching { RingtoneManager.getRingtone(context, uri)?.getTitle(context) }.getOrNull() ?: "Sonnerie personnalisée"
        }
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onDismiss) { Icon(Icons.Outlined.Close, "Fermer") }
                    Text("Votre alarme", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    item { Panel {
                        Text("L’heure de votre départ", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Text("%02d:%02d".format(hour, minute), fontSize = 56.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth().clickable { TimePickerDialog(context, { _, h, m -> hour = h; minute = m }, hour, minute, true).show() })
                        Text("Touchez l’heure pour la modifier", style = MaterialTheme.typography.bodySmall)
                        OutlinedTextField(name, { name = it.take(60) }, Modifier.fillMaxWidth(), label = { Text("Nom · Travail, Sport, École…") }, singleLine = true)
                    } }
                    item { Panel {
                        Text("Les jours qui vous ressemblent", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim").forEachIndexed { index, label ->
                                val day = index + 1
                                FilterChip(day in days, { days = if (day in days) days - day else days + day }, label = { Text(label) })
                            }
                        }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton({ days = (1..5).toList() }) { Text("Semaine") }
                            TextButton({ days = listOf(6, 7) }) { Text("Week-end") }
                            TextButton({ days = (1..7).toList() }) { Text("Tous les jours") }
                        }
                        if (days.isEmpty()) Text("Sélectionnez au moins un jour.", color = MaterialTheme.colorScheme.error)
                    } }
                    item { Panel {
                        Text("Comment vous prévenir ?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        DeliveryMode.entries.forEach { mode ->
                            Row(Modifier.fillMaxWidth().clickable { deliveryMode = mode }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(deliveryMode == mode, { deliveryMode = mode })
                                Column(Modifier.padding(start = 8.dp)) { Text(mode.label, fontWeight = FontWeight.SemiBold); Text(mode.description, style = MaterialTheme.typography.bodySmall) }
                            }
                        }
                        if (deliveryMode == DeliveryMode.RING) {
                            HorizontalDivider()
                            Text("Son et vibration", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        OutlinedButton({
                            val existing = when (sound) { "silent" -> null; "default" -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM); else -> Uri.parse(sound) }
                            runCatching { picker.launch(Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                                .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                                .putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Sonnerie du départ")
                                .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                                .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                .putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
                                .putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, existing)) }
                                .onFailure { soundError = "Le sélecteur de sonnerie n’est pas disponible sur ce téléphone." }
                        }, Modifier.fillMaxWidth()) { Icon(Icons.Outlined.MusicNote, null); Spacer(Modifier.width(8.dp)); Text(soundName) }
                        soundError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        Row(verticalAlignment = Alignment.CenterVertically) { Text("Vibration", Modifier.weight(1f)); Switch(vibrate, { vibrate = it }) }
                        if (sound == "silent" && !vibrate) Text("Seule la notification s’affichera.", style = MaterialTheme.typography.bodySmall)
                        } else {
                            Text("Le téléphone affiche une notification discrète. Le son et la vibration restent désactivés pour cette routine.", style = MaterialTheme.typography.bodySmall)
                        }
                        Text("Reporter la sonnerie de…", fontWeight = FontWeight.SemiBold)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(5, 10, 15).forEach { duration -> FilterChip(snooze == duration, { snooze = duration }, label = { Text("$duration min") }) }
                        }
                    } }
                    item { Panel {
                        Text("La checklist de ce départ", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically) { Text("Tous mes objets du jour", Modifier.weight(1f)); Switch(allItems, { allItems = it }) }
                        Text(if (allItems) "Inclut les nouveaux objets automatiquement, selon leur date." else "Choisissez les objets utiles à cette routine. Les objets ponctuels respectent leur date.", style = MaterialTheme.typography.bodySmall)
                        if (!allItems && objects.isEmpty()) Text("Ajoutez d’abord vos objets dans l’onglet Objets.")
                    } }
                    if (!allItems) items(objects, key = { it.id }) { item ->
                        Row(Modifier.fillMaxWidth().toggleable(item.id in chosenItems, role = Role.Checkbox) { checked ->
                            chosenItems = if (checked) chosenItems + item.id else chosenItems - item.id
                        }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            ObjectBadge(iconFor(item)); Spacer(Modifier.width(12.dp)); Text(item.title, Modifier.weight(1f)); Checkbox(item.id in chosenItems, null)
                        }
                    }
                }
                Button({ onSave(alarm.copy(name = name.trim(), hour = hour, minute = minute, days = days.toSet(), deliveryMode = deliveryMode, soundUri = sound,
                    soundName = soundName, vibrate = vibrate, snoozeMinutes = snooze, allItems = allItems,
                    itemIds = chosenItems.toSet(), skipUntil = 0)) }, enabled = name.isNotBlank() && days.isNotEmpty(), modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                    Text("Enregistrer l’alarme")
                }
            }
        }
    }
}

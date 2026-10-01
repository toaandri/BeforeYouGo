package com.toaandri.beforeyougo

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.toaandri.beforeyougo.ui.theme.BeforeYouGoTheme
import com.toaandri.beforeyougo.ui.theme.Ink
import com.toaandri.beforeyougo.ui.theme.Amber
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : ComponentActivity() {
    private var openedAlarm by mutableStateOf<Long?>(null)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        readAlarmIntent(intent)
        setContent {
            val store = remember { AppStore(this) }
            var theme by remember { mutableStateOf(store.themePreference()) }
            BeforeYouGoTheme(theme) {
                DepartureApp(store, theme, { theme = it; store.saveThemePreference(it) }, openedAlarm)
            }
        }
    }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); readAlarmIntent(intent) }
    private fun readAlarmIntent(intent: Intent) {
        openedAlarm = if (intent.hasExtra(ReminderScheduler.EXTRA_ID)) intent.getLongExtra(ReminderScheduler.EXTRA_ID, -1) else null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DepartureApp(store: AppStore, theme: ThemePreference, onTheme: (ThemePreference) -> Unit, openedAlarm: Long?) {
    val context = LocalContext.current
    val owner = context as ComponentActivity
    var alarms by remember { mutableStateOf(store.alarms()) }
    var objects by remember { store.checksForToday(); mutableStateOf(store.items()) }
    var checks by remember { mutableStateOf(store.checksForToday()) }
    var now by remember { mutableStateOf(ZonedDateTime.now()) }
    var page by rememberSaveable { mutableIntStateOf(0) }
    var selectedAlarm by rememberSaveable { mutableStateOf<Long?>(null) }
    var editor by remember { mutableStateOf<DepartureAlarm?>(null) }
    var objectEditor by remember { mutableStateOf<SavedItem?>(null) }
    var deleting by remember { mutableStateOf<DepartureAlarm?>(null) }
    var exact by remember { mutableStateOf(ReminderScheduler.canSchedule(context)) }
    var notifications by remember { mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled()) }
    var notice by remember { mutableStateOf<String?>(null) }
    val notificationRequest = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notifications = granted && NotificationManagerCompat.from(context).areNotificationsEnabled()
        if (!notifications) notice = "Notifications refusées : les alarmes restent enregistrées mais ne sonneront pas."
    }
    fun refresh() {
        now = ZonedDateTime.now(); alarms = store.alarms(); checks = store.checksForToday(); objects = store.items()
        exact = ReminderScheduler.canSchedule(context)
        notifications = NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
    fun saveAlarm(alarm: DepartureAlarm) {
        ReminderScheduler.cancel(context, alarm.id)
        AlarmRingingService.command(context, alarm.id, AlarmRingingService.STOP)
        alarms = if (alarms.any { it.id == alarm.id }) alarms.map { if (it.id == alarm.id) alarm else it } else alarms + alarm
        store.saveAlarms(alarms)
        val scheduled = ReminderScheduler.schedule(context, alarm)
        if (alarm.enabled && !scheduled) notice = "Alarme enregistrée. Autorisez les alarmes exactes pour qu’elle puisse sonner."
        if (alarm.enabled && !notifications && Build.VERSION.SDK_INT >= 33) notificationRequest.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
    fun saveObjects(values: List<SavedItem>) { objects = values; store.saveItems(values) }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) { refresh(); ReminderScheduler.restore(context) }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(Unit) {
        ReminderScheduler.restore(context)
        while (true) {
            delay(15_000)
            val updated = ZonedDateTime.now()
            if (updated.toLocalDate() != now.toLocalDate()) { checks = store.checksForToday(); objects = store.items() }
            now = updated
        }
    }
    LaunchedEffect(openedAlarm, RingingState.alarmId) {
        (RingingState.alarmId ?: openedAlarm)?.let { selectedAlarm = it; page = 0 }
    }
    val selected = alarms.find { it.id == selectedAlarm }
    val todayItems = if (selected != null) itemsForAlarm(objects, selected, now.toLocalDate()) else itemsForDay(objects, now.toLocalDate())
    Scaffold(
        topBar = { TopAppBar(title = { Text("BeforeYouGo", fontWeight = FontWeight.ExtraBold) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)) },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                listOf("Aujourd’hui" to Icons.Outlined.Alarm, "Objets" to Icons.Outlined.Inventory2, "Réglages" to Icons.Outlined.Settings).forEachIndexed { index, (label, icon) ->
                    NavigationBarItem(page == index, { page = index }, icon = { Icon(icon, null) }, label = { Text(label) })
                }
            }
        },
        floatingActionButton = {
            if (page == 1) FloatingActionButton({ objectEditor = SavedItem(newId(objects.map { it.id }), "", true, null) },
                containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary) {
                Icon(Icons.Outlined.Add, "Ajouter un objet personnel")
            }
        }
    ) { padding ->
        when (page) {
            0 -> LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                item {
                    val next = alarms.mapNotNull { alarm -> nextOccurrence(alarm, now)?.let { alarm to it } }.minByOrNull { it.second.toInstant() }
                    Hero(next, exact && notifications)
                }
                item {
                    RingingState.alarmId?.let { id -> alarms.find { it.id == id }?.let { ringing ->
                        Panel {
                            Text(if (RingingState.preview) "Essai en cours · 10 secondes" else "C’est l’heure · ${ringing.name}", style = MaterialTheme.typography.titleLarge)
                            Button({ AlarmRingingService.command(context, id, AlarmRingingService.STOP) }, Modifier.fillMaxWidth()) { Text("Arrêter la sonnerie") }
                            if (!RingingState.preview) OutlinedButton({ AlarmRingingService.command(context, id, AlarmRingingService.SNOOZE); refresh() }, Modifier.fillMaxWidth()) { Text("Reporter ${ringing.snoozeMinutes} minutes") }
                        }
                    } }
                }
                if (!exact || !notifications) item {
                    Panel {
                        Text("Terminer la configuration", fontWeight = FontWeight.Bold)
                        Text("Pour sonner à l’heure, BeforeYouGo a besoin des alarmes exactes et des notifications.", style = MaterialTheme.typography.bodyMedium)
                        TextButton({ page = 2 }) { Text("Vérifier les autorisations") }
                    }
                }
                item {
                    Text("Vos départs", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Une routine pour chaque jour de votre semaine.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                items(alarms.sortedWith(compareBy({ it.hour }, { it.minute })), key = { "alarm-${it.id}" }) { alarm ->
                    AlarmCard(alarm, now, store.snoozedUntil(alarm.id), exact && notifications,
                        { editor = alarm }, { saveAlarm(alarm.copy(enabled = it)) },
                        { nextOccurrence(alarm, now)?.let { saveAlarm(alarm.copy(skipUntil = it.toInstant().toEpochMilli())) } },
                        { saveAlarm(alarm.copy(skipUntil = 0)) }, { deleting = alarm },
                        { selectedAlarm = alarm.id }, { ReminderScheduler.cancel(context, alarm.id); ReminderScheduler.schedule(context, alarm); refresh() })
                }
                item { OutlinedButton({ editor = DepartureAlarm(newId(alarms.map { it.id })) }, Modifier.fillMaxWidth()) { Text("Créer une alarme") } }
                item {
                    Text("Votre checklist", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(if (selected == null) "Tous les objets prévus aujourd’hui" else "Routine : ${selected.name}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (selected != null) TextButton({ selectedAlarm = null }) { Text("Afficher tous mes objets") }
                    if (todayItems.isNotEmpty()) {
                        Text("${todayItems.count { checks[it.id] == true }} / ${todayItems.size} prêts", modifier = Modifier.padding(vertical = 8.dp))
                        LinearProgressIndicator(progress = { completionFor(todayItems, checks) }, modifier = Modifier.fillMaxWidth())
                    }
                }
                if (todayItems.isEmpty()) item { Panel { Text("Rien à préparer pour cette liste."); TextButton({ page = 1 }) { Text("Voir mes objets") } } }
                items(todayItems, key = { "item-${it.id}" }) { item ->
                    ChecklistRow(item, checks[item.id] == true) { checked ->
                        checks = checks.toMutableMap().apply { put(item.id, checked) }; store.saveChecksForToday(checks)
                    }
                }
            }
            1 -> ObjectsScreen(objects.filterNot { it.archived }, Modifier.padding(padding),
                { objectEditor = it }, { removed ->
                    saveObjects(objects.filterNot { it.id == removed.id })
                    alarms = alarms.map { it.copy(itemIds = it.itemIds - removed.id) }; store.saveAlarms(alarms)
                }, { idea ->
                    if (objects.none { !it.archived && normalized(it.title) == normalized(idea.name) })
                        saveObjects(objects + SavedItem(newId(objects.map { it.id }), idea.name, true, null, iconKey = idea.key))
                })
            2 -> SettingsScreen(theme, onTheme, exact, notifications, Modifier.padding(padding), {
                if (Build.VERSION.SDK_INT >= 31) openSettings(context, Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")))
            }, {
                openSettings(context, Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
            })
        }
    }
    editor?.let { alarm -> AlarmEditor(alarm, objects.filterNot { it.archived }, { editor = null }) { saveAlarm(it); editor = null } }
    objectEditor?.let { item -> ObjectEditor(item, { objectEditor = null }) { updated ->
        saveObjects(if (objects.any { it.id == updated.id }) objects.map { if (it.id == updated.id) updated else it } else objects + updated)
        objectEditor = null
    } }
    deleting?.let { alarm -> AlertDialog(onDismissRequest = { deleting = null }, title = { Text("Supprimer « ${alarm.name} » ?") }, text = { Text("Cette sonnerie sera annulée. Vos objets seront conservés.") },
        confirmButton = { TextButton({
            ReminderScheduler.cancel(context, alarm.id); AlarmRingingService.command(context, alarm.id, AlarmRingingService.STOP)
            alarms = alarms.filterNot { it.id == alarm.id }; store.saveAlarms(alarms); deleting = null
        }) { Text("Supprimer") } }, dismissButton = { TextButton({ deleting = null }) { Text("Annuler") } }) }
    notice?.let { message -> AlertDialog(onDismissRequest = { notice = null }, title = { Text("Alarme enregistrée") }, text = { Text(message) },
        confirmButton = { TextButton({ notice = null; page = 2 }) { Text("Ouvrir les réglages") } }, dismissButton = { TextButton({ notice = null }) { Text("Plus tard") } }) }
}

fun newId(existing: List<Long>): Long = maxOf(System.currentTimeMillis(), (existing.maxOrNull() ?: 0) + 1)
fun openSettings(context: android.content.Context, intent: Intent) {
    runCatching { context.startActivity(intent) }.onFailure {
        runCatching { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))) }
    }
}

@Composable fun Panel(content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable fun ObjectBadge(icon: ImageVector) {
    Box(Modifier.size(46.dp).clip(RoundedCornerShape(15.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = .10f)), contentAlignment = Alignment.Center) {
        Icon(icon, null, Modifier.size(25.dp), tint = MaterialTheme.colorScheme.primary)
    }
}

@Composable private fun Hero(next: Pair<DepartureAlarm, ZonedDateTime>?, ready: Boolean) {
    Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = Ink)) {
        Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.Alarm, null, tint = Amber); Spacer(Modifier.width(10.dp)); Text("LE PROCHAIN DÉPART", color = Amber, style = MaterialTheme.typography.labelLarge) }
            Text(next?.first?.timeLabel ?: "À votre rythme.", color = Color.White, fontSize = if (next != null) 52.sp else 30.sp, fontWeight = FontWeight.Bold)
            Text(next?.let { "${it.first.name} · ${it.second.format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH))}" } ?: "Composez votre semaine, partez l’esprit léger.", color = Color.White.copy(alpha = .8f))
            if (next != null && !ready) Text("En attente d’autorisation Android", color = Amber, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable private fun AlarmCard(alarm: DepartureAlarm, now: ZonedDateTime, snoozedUntil: Long, ready: Boolean,
    onEdit: () -> Unit, onToggle: (Boolean) -> Unit, onSkip: () -> Unit, onUnskip: () -> Unit,
    onDelete: () -> Unit, onChecklist: () -> Unit, onCancelSnooze: () -> Unit) {
    val context = LocalContext.current
    Panel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).clickable(onClick = onEdit)) {
                Text(alarm.timeLabel, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                Text(alarm.name, style = MaterialTheme.typography.titleMedium)
            }
            Switch(alarm.enabled, onToggle, modifier = Modifier.minimumInteractiveComponentSize())
        }
        Text(daysLabel(alarm.days), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        Text(if (!alarm.enabled) "En pause" else if (!ready) "Autorisations requises" else nextOccurrence(alarm, now)?.let {
            "Prochaine : ${it.format(DateTimeFormatter.ofPattern("EEE d MMM · HH:mm", Locale.FRENCH))}"
        }.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(if (alarm.deliveryMode == DeliveryMode.NOTIFICATION) "Notification discrète" else "${if (alarm.soundUri == "silent") "Sans son" else alarm.soundName} · ${if (alarm.vibrate) "Vibration" else "Sans vibration"}", style = MaterialTheme.typography.bodySmall)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onEdit) { Text("Personnaliser") }
            TextButton(onChecklist) { Text("Checklist") }
            IconButton(onDelete) { Icon(Icons.Outlined.DeleteOutline, "Supprimer ${alarm.name}") }
        }
        if (alarm.enabled) {
            if (snoozedUntil > now.toInstant().toEpochMilli()) TextButton(onCancelSnooze) { Text("Annuler le report en cours") }
            else if (alarm.skipUntil > now.toInstant().toEpochMilli()) TextButton(onUnskip) { Text("Rétablir la sonnerie ignorée") }
            else TextButton(onSkip) { Text("Passer la prochaine sonnerie") }
        }
        if (ready && alarm.deliveryMode == DeliveryMode.RING) TextButton({ context.startForegroundService(Intent(context, AlarmRingingService::class.java)
            .putExtra(ReminderScheduler.EXTRA_ID, alarm.id).putExtra(AlarmRingingService.PREVIEW, true)) }) { Text("Écouter un essai · 10 s") }
    }
}

@Composable private fun ChecklistRow(item: SavedItem, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = if (checked) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface)) {
        Row(Modifier.fillMaxWidth().toggleable(checked, role = Role.Checkbox, onValueChange = onChecked).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            ObjectBadge(iconFor(item)); Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.title, fontWeight = FontWeight.SemiBold)
                Text(if (item.essential) "Tous les jours" else "Prévu le ${item.dueDate}", style = MaterialTheme.typography.bodySmall)
            }
            Checkbox(checked, null)
        }
    }
}

@Composable private fun SettingsScreen(theme: ThemePreference, onTheme: (ThemePreference) -> Unit, exact: Boolean, notifications: Boolean,
    modifier: Modifier, onExact: () -> Unit, onNotifications: () -> Unit) {
    val context = LocalContext.current
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text("À votre façon", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
        item { Panel {
            Text("Apparence", style = MaterialTheme.typography.titleLarge)
            ThemePreference.entries.forEach { option ->
                Row(Modifier.fillMaxWidth().clickable { onTheme(option) }, verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(theme == option, { onTheme(option) }); Text(option.label)
                }
            }
        } }
        item { Panel {
            Text("Des sonneries à l’heure", style = MaterialTheme.typography.titleLarge)
            Text("Alarmes exactes : ${if (exact) "autorisées" else "à autoriser"}")
            if (!exact) OutlinedButton(onExact, Modifier.fillMaxWidth()) { Text("Autoriser les alarmes exactes") }
            Text("Notifications : ${if (notifications) "autorisées" else "à autoriser"}")
            OutlinedButton(onNotifications, Modifier.fillMaxWidth()) { Text("Réglages des notifications") }
            Text("Chaque routine peut être une sonnerie ou une notification discrète. Les sonneries utilisent le volume Alarme, s’arrêtent après 2 minutes, et leur essai dure 10 secondes.", style = MaterialTheme.typography.bodySmall)
            TextButton({ openSettings(context, Intent(Settings.ACTION_SOUND_SETTINGS)) }) { Text("Volume et sons du téléphone") }
        } }
        item { Panel {
            Text("Votre semaine, vos routines", style = MaterialTheme.typography.titleLarge)
            Text("Créez plusieurs alarmes dans Aujourd’hui : Travail en semaine, Sport le mardi et le jeudi, ou Sortie le week-end. Chaque alarme possède ses jours, sa sonnerie et sa checklist.")
        } }
        item { Panel {
            Text("Vos données restent ici", style = MaterialTheme.typography.titleLarge)
            Text("Sans compte, publicité ni suivi. Vos listes et sonneries sont stockées sur ce téléphone. Une désinstallation les efface.", style = MaterialTheme.typography.bodyMedium)
        } }
    }
}

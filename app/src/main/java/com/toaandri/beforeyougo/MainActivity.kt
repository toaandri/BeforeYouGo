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
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.toaandri.beforeyougo.ui.theme.BeforeYouGoTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : ComponentActivity() {
    private var openedAlarm by mutableStateOf<Long?>(null)
    private var openRequest by mutableIntStateOf(0)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        readAlarmIntent(intent)
        setContent {
            val store = remember { AppStore(this) }
            var theme by remember { mutableStateOf(store.themePreference()) }
            BeforeYouGoTheme(theme) {
                DepartureApp(store, theme, { theme = it; store.saveThemePreference(it) }, openedAlarm, openRequest)
            }
        }
    }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); readAlarmIntent(intent) }
    private fun readAlarmIntent(intent: Intent) {
        openedAlarm = if (intent.hasExtra(ReminderScheduler.EXTRA_ID)) intent.getLongExtra(ReminderScheduler.EXTRA_ID, -1) else null
        openRequest++
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DepartureApp(store: AppStore, theme: ThemePreference, onTheme: (ThemePreference) -> Unit, openedAlarm: Long?, openRequest: Int) {
    val context = LocalContext.current
    val owner = context as ComponentActivity
    var alarms by remember { mutableStateOf(store.alarms()) }
    var objects by remember { store.checksForToday(); mutableStateOf(store.items()) }
    var checks by remember { mutableStateOf(store.checksForToday()) }
    var now by remember { mutableStateOf(ZonedDateTime.now()) }
    val todayScroll = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    var dragPointer by remember { mutableStateOf<Offset?>(null) }
    var viewport by remember { mutableStateOf(Rect.Zero) }
    var page by rememberSaveable { mutableIntStateOf(0) }
    var selectedAlarm by rememberSaveable { mutableStateOf<Long?>(null) }
    var editor by remember { mutableStateOf<DepartureAlarm?>(null) }
    var objectEditor by remember { mutableStateOf<SavedItem?>(null) }
    var deleting by remember { mutableStateOf<DepartureAlarm?>(null) }
    var exact by remember { mutableStateOf(ReminderScheduler.canSchedule(context)) }
    var notifications by remember { mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled()) }
    var notice by remember { mutableStateOf<String?>(null) }
    var departing by rememberSaveable { mutableStateOf(false) }
    val notificationRequest = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notifications = granted && NotificationManagerCompat.from(context).areNotificationsEnabled()
        if (!notifications) notice = "Ton départ est enregistré. Les invitations sont désactivées ; ton sac reste accessible à tout moment."
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
        if (alarm.enabled && !scheduled) notice = "Ton départ est enregistré. Pour recevoir le petit signe à l’heure choisie, autorise les alarmes exactes dans Android. Tu peux aussi utiliser ton sac sans notification."
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
    LaunchedEffect(RingingState.alarmId) {
        RingingState.alarmId?.let { selectedAlarm = it; page = 0; departing = false }
    }
    LaunchedEffect(openRequest) {
        openedAlarm?.let { selectedAlarm = it; page = 0; departing = false }
    }
    LaunchedEffect(selectedAlarm, openRequest) { todayScroll.scrollToItem(0) }
    LaunchedEffect(dragPointer != null) {
        // Keep a long collection usable: holding a dragged object near the upper edge reveals the bag.
        val edge = with(density) { 80.dp.toPx() }
        val step = with(density) { 12.dp.toPx() }
        while (dragPointer != null) {
            if (dragPointer!!.y < viewport.top + edge) todayScroll.scrollBy(-step)
            delay(16)
        }
    }
    val selected = alarms.find { it.id == selectedAlarm }
    val todayItems = if (selected != null) itemsForAlarm(objects, selected, now.toLocalDate()) else itemsForDay(objects, now.toLocalDate())
    Scaffold(
        topBar = { TopAppBar(title = { Text("before you go.", style = MaterialTheme.typography.titleLarge) },
            actions = { Icon(Icons.Outlined.WbSunny, null, Modifier.padding(end = 22.dp), tint = MaterialTheme.colorScheme.primary) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)) },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                listOf("Mon départ" to Icons.Outlined.ShoppingBag, "Mes objets" to Icons.Outlined.Category, "À ma façon" to Icons.Outlined.Tune).forEachIndexed { index, (label, icon) ->
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
            0 -> LazyColumn(Modifier.fillMaxSize().padding(padding).onGloballyPositioned { viewport = it.boundsInRoot() }, state = todayScroll, contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                item {
                    Text(now.format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH)),
                        style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(10.dp))
                    Text("On fait le tour ?", style = MaterialTheme.typography.displaySmall)
                    Spacer(Modifier.height(8.dp))
                    Text("Un petit rituel avant la suite.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (selected != null) {
                        Spacer(Modifier.height(12.dp))
                        Text(selected.name, style = MaterialTheme.typography.titleMedium)
                        TextButton({ selectedAlarm = null }) { Text("Revenir au sac du jour") }
                    }
                }
                item {
                    RingingState.alarmId?.let { id -> alarms.find { it.id == id }?.let { ringing ->
                        Panel {
                            Text(if (RingingState.preview) "On essaie le son · 10 secondes" else "Ton départ · ${ringing.name}", style = MaterialTheme.typography.titleLarge)
                            Button({ AlarmRingingService.command(context, id, AlarmRingingService.STOP) }, Modifier.fillMaxWidth()) { Text("Arrêter la sonnerie") }
                            if (!RingingState.preview) OutlinedButton({ AlarmRingingService.command(context, id, AlarmRingingService.SNOOZE); refresh() }, Modifier.fillMaxWidth()) { Text("Reporter ${ringing.snoozeMinutes} minutes") }
                        }
                    } }
                }
                item {
                    DepartureRitual(todayItems, checks, { id, checked ->
                        val currentChecks = store.checksForToday()
                        objects = store.items(); now = ZonedDateTime.now()
                        checks = if (itemsForDay(objects, now.toLocalDate()).any { it.id == id }) currentChecks + (id to checked) else currentChecks
                        store.saveChecksForToday(checks)
                    }, { page = 1 }, {
                        RingingState.alarmId?.let { AlarmRingingService.command(context, it, AlarmRingingService.STOP) }
                        departing = true
                    }, { dragPointer = it })
                }
                item { HorizontalDivider(Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant) }
                if (alarms.any { it.enabled } && (!exact || !notifications)) item {
                    Panel {
                        Text("Envie d’un petit signe ?", style = MaterialTheme.typography.titleLarge)
                        Text("Tes départs sont enregistrés. Pour recevoir leurs invitations, il reste les autorisations Android à activer.", style = MaterialTheme.typography.bodyMedium)
                        TextButton({ page = 2 }) { Text("Choisir mes autorisations") }
                    }
                }
                item {
                    Text("Les rendez-vous du sac", style = MaterialTheme.typography.headlineMedium)
                    Text("Un petit signe, quand ça t’arrange.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                items(alarms.sortedWith(compareBy({ it.hour }, { it.minute })), key = { "alarm-${it.id}" }) { alarm ->
                    AlarmCard(alarm, now, store.snoozedUntil(alarm.id), exact && notifications,
                        { editor = alarm }, { saveAlarm(alarm.copy(enabled = it)) },
                        { nextOccurrence(alarm, now)?.let { saveAlarm(alarm.copy(skipUntil = it.toInstant().toEpochMilli())) } },
                        { saveAlarm(alarm.copy(skipUntil = 0)) }, { deleting = alarm },
                        { selectedAlarm = alarm.id; scope.launch { todayScroll.animateScrollToItem(0) } }, { ReminderScheduler.cancel(context, alarm.id); ReminderScheduler.schedule(context, alarm); refresh() })
                }
                item { OutlinedButton({ editor = DepartureAlarm(newId(alarms.map { it.id })) }, Modifier.fillMaxWidth()) { Icon(Icons.Outlined.Add, null); Spacer(Modifier.width(8.dp)); Text("Imaginer un départ") } }
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
    if (departing) AlertDialog(onDismissRequest = { departing = false },
        icon = { Icon(Icons.Outlined.WbSunny, null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("La suite est à toi.") },
        text = { Text(if (todayItems.all { checks[it.id] == true }) "Ton sac est prêt. Bonne route !" else "Bonne route ! Tu retrouveras ton sac ici quand tu veux.") },
        confirmButton = { TextButton({ departing = false }) { Text("À tout à l’heure") } })
    deleting?.let { alarm -> AlertDialog(onDismissRequest = { deleting = null }, title = { Text("Supprimer « ${alarm.name} » ?") }, text = { Text("Ce rendez-vous sera annulé. Tes objets restent dans ta collection.") },
        confirmButton = { TextButton({
            ReminderScheduler.cancel(context, alarm.id); AlarmRingingService.command(context, alarm.id, AlarmRingingService.STOP)
            alarms = alarms.filterNot { it.id == alarm.id }; store.saveAlarms(alarms); deleting = null
        }) { Text("Supprimer") } }, dismissButton = { TextButton({ deleting = null }) { Text("Annuler") } }) }
    notice?.let { message -> AlertDialog(onDismissRequest = { notice = null }, title = { Text("Ton départ est enregistré") }, text = { Text(message) },
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

@Composable private fun AlarmCard(alarm: DepartureAlarm, now: ZonedDateTime, snoozedUntil: Long, ready: Boolean,
    onEdit: () -> Unit, onToggle: (Boolean) -> Unit, onSkip: () -> Unit, onUnskip: () -> Unit,
    onDelete: () -> Unit, onChecklist: () -> Unit, onCancelSnooze: () -> Unit) {
    val context = LocalContext.current
    var expanded by rememberSaveable(alarm.id) { mutableStateOf(false) }
    Panel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).clickable(onClick = onEdit)) {
                Text(alarm.timeLabel, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                Text(alarm.name, style = MaterialTheme.typography.titleMedium)
            }
            Switch(alarm.enabled, onToggle, modifier = Modifier.minimumInteractiveComponentSize()
                .semantics { contentDescription = "Invitation pour ${alarm.name}" })
        }
        Text(daysLabel(alarm.days), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        Text(if (!alarm.enabled) "Invitation en pause" else if (!ready) "Invitation en attente d’autorisation" else nextOccurrence(alarm, now)?.let {
            "Prochain signe : ${it.format(DateTimeFormatter.ofPattern("EEE d MMM · HH:mm", Locale.FRENCH))}"
        }.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onChecklist) { Text("Ouvrir ce sac") }
            IconButton({ expanded = !expanded }) { Icon(if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.MoreHoriz, if (expanded) "Masquer les options de ${alarm.name}" else "Options de ${alarm.name}") }
        }
        if (expanded) {
            Text(if (alarm.deliveryMode == DeliveryMode.NOTIFICATION) "Petit signe · notification silencieuse" else "${if (alarm.soundUri == "silent") "Sans son" else alarm.soundName} · ${if (alarm.vibrate) "Vibration" else "Sans vibration"}", style = MaterialTheme.typography.bodySmall)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onEdit) { Text("Personnaliser") }
                IconButton(onDelete) { Icon(Icons.Outlined.DeleteOutline, "Supprimer ${alarm.name}") }
            }
            if (alarm.enabled) {
                if (snoozedUntil > now.toInstant().toEpochMilli()) TextButton(onCancelSnooze) { Text("Annuler le report en cours") }
                else if (alarm.skipUntil > now.toInstant().toEpochMilli()) TextButton(onUnskip) { Text("Rétablir le prochain rendez-vous") }
                else TextButton(onSkip) { Text("Passer le prochain rendez-vous") }
            }
            if (ready && alarm.deliveryMode == DeliveryMode.RING) TextButton({ context.startForegroundService(Intent(context, AlarmRingingService::class.java)
                .putExtra(ReminderScheduler.EXTRA_ID, alarm.id).putExtra(AlarmRingingService.PREVIEW, true)) }) { Text("Écouter un essai · 10 s") }
        }
    }
}

@Composable private fun SettingsScreen(theme: ThemePreference, onTheme: (ThemePreference) -> Unit, exact: Boolean, notifications: Boolean,
    modifier: Modifier, onExact: () -> Unit, onNotifications: () -> Unit) {
    val context = LocalContext.current
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text("À ta façon.", style = MaterialTheme.typography.displaySmall) }
        item { Panel {
            Text("Apparence", style = MaterialTheme.typography.titleLarge)
            ThemePreference.entries.forEach { option ->
                Row(Modifier.fillMaxWidth().clickable { onTheme(option) }, verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(theme == option, { onTheme(option) }); Text(option.label)
                }
            }
        } }
        item { Panel {
            Text("Un signe, si tu veux", style = MaterialTheme.typography.titleLarge)
            Text("Ton sac reste disponible même sans notification.", style = MaterialTheme.typography.bodyMedium)
            Text("Alarmes exactes : ${if (exact) "autorisées" else "à autoriser"}")
            if (!exact) OutlinedButton(onExact, Modifier.fillMaxWidth()) { Text("Autoriser les alarmes exactes") }
            Text("Notifications : ${if (notifications) "autorisées" else "à autoriser"}")
            OutlinedButton(onNotifications, Modifier.fillMaxWidth()) { Text("Réglages des notifications") }
            Text("Les nouveaux départs proposent un petit signe silencieux. Tu peux choisir une sonnerie pour chacun : volume Alarme, arrêt automatique après 2 minutes et essai de 10 secondes.", style = MaterialTheme.typography.bodySmall)
            TextButton({ openSettings(context, Intent(Settings.ACTION_SOUND_SETTINGS)) }) { Text("Volume et sons du téléphone") }
        } }
        item { Panel {
            Text("Ton rythme, tes départs", style = MaterialTheme.typography.titleLarge)
            Text("Dans Mon départ, imagine un rendez-vous pour le bureau, la piscine ou les virées du week-end. À chacun ses jours et ses objets. Tu peux passer un rendez-vous, mettre un départ en pause ou reporter une invitation.")
        } }
        item { Panel {
            Text("Ton carnet reste ici", style = MaterialTheme.typography.titleLarge)
            Text("Sans compte, publicité ni suivi. Tes objets et tes départs restent sur ce téléphone. Une désinstallation les efface.", style = MaterialTheme.typography.bodyMedium)
        } }
    }
}

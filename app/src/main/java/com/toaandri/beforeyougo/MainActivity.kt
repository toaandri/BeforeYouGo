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
import androidx.compose.foundation.selection.selectable
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
    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(AppText.localizedContext(newBase))
    }
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
        if (!notifications) notice = uiText(R.string.ui_your_departure_is_saved_invitations_are_disabled_your_bag_is)
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
        if (alarm.enabled && !scheduled) notice = uiText(R.string.ui_your_departure_is_saved_allow_exact_alarms_in_android_to)
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
                listOf(uiText(R.string.ui_my_departure) to Icons.Outlined.ShoppingBag, uiText(R.string.ui_my_objects) to Icons.Outlined.Category, uiText(R.string.ui_my_preferences) to Icons.Outlined.Tune).forEachIndexed { index, (label, icon) ->
                    NavigationBarItem(page == index, { page = index }, icon = { Icon(icon, null) }, label = { Text(label) })
                }
            }
        },
        floatingActionButton = {
            if (page == 1) FloatingActionButton({ objectEditor = SavedItem(newId(objects.map { it.id }), "", true, null) },
                containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary) {
                Icon(Icons.Outlined.Add, uiText(R.string.ui_add_a_personal_object))
            }
        }
    ) { padding ->
        when (page) {
            0 -> LazyColumn(Modifier.fillMaxSize().padding(padding).onGloballyPositioned { viewport = it.boundsInRoot() }, state = todayScroll, contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                item {
                    Text(now.format(DateTimeFormatter.ofPattern("EEEE d MMMM", AppText.locale)),
                        style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(10.dp))
                    Text(uiText(R.string.ui_shall_we_check_the_bag), style = MaterialTheme.typography.displaySmall)
                    Spacer(Modifier.height(8.dp))
                    Text(uiText(R.string.ui_a_small_ritual_before_what_comes_next), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (selected != null) {
                        Spacer(Modifier.height(12.dp))
                        Text(selected.name, style = MaterialTheme.typography.titleMedium)
                        TextButton({ selectedAlarm = null }) { Text(uiText(R.string.ui_back_to_today_s_bag)) }
                    }
                }
                item {
                    RingingState.alarmId?.let { id -> alarms.find { it.id == id }?.let { ringing ->
                        Panel {
                            Text(if (RingingState.preview) uiText(R.string.ui_sound_preview_10_seconds) else uiText(R.string.ui_your_departure_57fd9c, ringing.name), style = MaterialTheme.typography.titleLarge)
                            Button({ AlarmRingingService.command(context, id, AlarmRingingService.STOP) }, Modifier.fillMaxWidth()) { Text(uiText(R.string.ui_stop_the_sound)) }
                            if (!RingingState.preview) OutlinedButton({ AlarmRingingService.command(context, id, AlarmRingingService.SNOOZE); refresh() }, Modifier.fillMaxWidth()) { Text(uiText(R.string.ui_snooze_for_minutes, ringing.snoozeMinutes)) }
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
                        Text(uiText(R.string.ui_fancy_a_little_nudge), style = MaterialTheme.typography.titleLarge)
                        Text(uiText(R.string.ui_your_departures_are_saved_enable_the_android_permissions_to_receive), style = MaterialTheme.typography.bodyMedium)
                        TextButton({ page = 2 }) { Text(uiText(R.string.ui_choose_my_permissions)) }
                    }
                }
                item {
                    Text(uiText(R.string.ui_your_bag_appointments), style = MaterialTheme.typography.headlineMedium)
                    Text(uiText(R.string.ui_a_little_nudge_when_it_suits_you), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                items(alarms.sortedWith(compareBy({ it.hour }, { it.minute })), key = { "alarm-${it.id}" }) { alarm ->
                    AlarmCard(alarm, now, store.snoozedUntil(alarm.id), exact && notifications,
                        { editor = alarm }, { saveAlarm(alarm.copy(enabled = it)) },
                        { nextOccurrence(alarm, now)?.let { saveAlarm(alarm.copy(skipUntil = it.toInstant().toEpochMilli())) } },
                        { saveAlarm(alarm.copy(skipUntil = 0)) }, { deleting = alarm },
                        { selectedAlarm = alarm.id; scope.launch { todayScroll.animateScrollToItem(0) } }, { ReminderScheduler.cancel(context, alarm.id); ReminderScheduler.schedule(context, alarm); refresh() })
                }
                item { OutlinedButton({ editor = DepartureAlarm(newId(alarms.map { it.id })) }, Modifier.fillMaxWidth()) { Icon(Icons.Outlined.Add, null); Spacer(Modifier.width(8.dp)); Text(uiText(R.string.ui_plan_a_departure)) } }
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
        title = { Text(uiText(R.string.ui_the_next_part_is_yours)) },
        text = { Text(if (todayItems.all { checks[it.id] == true }) uiText(R.string.ui_your_bag_is_ready_have_a_good_journey) else uiText(R.string.ui_have_a_good_journey_your_bag_will_be_here_whenever)) },
        confirmButton = { TextButton({ departing = false }) { Text(uiText(R.string.ui_see_you_soon)) } })
    deleting?.let { alarm -> AlertDialog(onDismissRequest = { deleting = null }, title = { Text(uiText(R.string.ui_delete, alarm.name)) }, text = { Text(uiText(R.string.ui_this_appointment_will_be_cancelled_your_objects_stay_in_your)) },
        confirmButton = { TextButton({
            ReminderScheduler.cancel(context, alarm.id); AlarmRingingService.command(context, alarm.id, AlarmRingingService.STOP)
            alarms = alarms.filterNot { it.id == alarm.id }; store.saveAlarms(alarms); deleting = null
        }) { Text(uiText(R.string.ui_delete_16ce0b)) } }, dismissButton = { TextButton({ deleting = null }) { Text(uiText(R.string.ui_cancel)) } }) }
    notice?.let { message -> AlertDialog(onDismissRequest = { notice = null }, title = { Text(uiText(R.string.ui_your_departure_is_saved)) }, text = { Text(message) },
        confirmButton = { TextButton({ notice = null; page = 2 }) { Text(uiText(R.string.ui_open_settings)) } }, dismissButton = { TextButton({ notice = null }) { Text(uiText(R.string.ui_later)) } }) }
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
                .semantics { contentDescription = uiText(R.string.ui_invitation_for, alarm.name) })
        }
        Text(daysLabel(alarm.days), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        Text(if (!alarm.enabled) uiText(R.string.ui_invitation_paused) else if (!ready) uiText(R.string.ui_invitation_needs_permission) else nextOccurrence(alarm, now)?.let {
            uiText(R.string.ui_next_nudge, it.format(DateTimeFormatter.ofPattern("EEE d MMM · HH:mm", AppText.locale)))
        }.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onChecklist) { Text(uiText(R.string.ui_open_this_bag)) }
            IconButton({ expanded = !expanded }) { Icon(if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.MoreHoriz, if (expanded) uiText(R.string.ui_hide_options_for, alarm.name) else uiText(R.string.ui_options_for, alarm.name)) }
        }
        if (expanded) {
            Text(if (alarm.deliveryMode == DeliveryMode.NOTIFICATION) uiText(R.string.ui_quiet_nudge_silent_notification) else uiText(R.string.ui, if (alarm.soundUri == "silent") uiText(R.string.ui_no_sound) else alarm.displaySoundName, if (alarm.vibrate) uiText(R.string.ui_vibration) else uiText(R.string.ui_no_vibration)), style = MaterialTheme.typography.bodySmall)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onEdit) { Text(uiText(R.string.ui_customize)) }
                IconButton(onDelete) { Icon(Icons.Outlined.DeleteOutline, uiText(R.string.ui_delete_ecb441, alarm.name)) }
            }
            if (alarm.enabled) {
                if (snoozedUntil > now.toInstant().toEpochMilli()) TextButton(onCancelSnooze) { Text(uiText(R.string.ui_cancel_the_current_snooze)) }
                else if (alarm.skipUntil > now.toInstant().toEpochMilli()) TextButton(onUnskip) { Text(uiText(R.string.ui_restore_the_next_appointment)) }
                else TextButton(onSkip) { Text(uiText(R.string.ui_skip_the_next_appointment)) }
            }
            if (ready && alarm.deliveryMode == DeliveryMode.RING) TextButton({ context.startForegroundService(Intent(context, AlarmRingingService::class.java)
                .putExtra(ReminderScheduler.EXTRA_ID, alarm.id).putExtra(AlarmRingingService.PREVIEW, true)) }) { Text(uiText(R.string.ui_try_the_sound_10_s)) }
        }
    }
}

@Composable private fun SettingsScreen(theme: ThemePreference, onTheme: (ThemePreference) -> Unit, exact: Boolean, notifications: Boolean,
    modifier: Modifier, onExact: () -> Unit, onNotifications: () -> Unit) {
    val context = LocalContext.current
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text(uiText(R.string.ui_your_way), style = MaterialTheme.typography.displaySmall) }
        item { Panel {
            Text(uiText(R.string.language_title), style = MaterialTheme.typography.titleLarge)
            Text(uiText(R.string.language_help), style = MaterialTheme.typography.bodyMedium)
            listOf("en" to "English", "fr" to "Français").forEach { (tag, label) ->
                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(
                    selected = AppText.locale.language == tag,
                    onClick = { AppText.selectLanguage(context, tag) },
                    role = androidx.compose.ui.semantics.Role.RadioButton
                ), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(AppText.locale.language == tag, null)
                    Text(label)
                }
            }
        } }

        item { Panel {
            Text(uiText(R.string.ui_appearance), style = MaterialTheme.typography.titleLarge)
            ThemePreference.entries.forEach { option ->
                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(selected = theme == option, onClick = { onTheme(option) }, role = androidx.compose.ui.semantics.Role.RadioButton), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(theme == option, null); Text(option.label)
                }
            }
        } }
        item { Panel {
            Text(uiText(R.string.ui_a_nudge_if_you_want), style = MaterialTheme.typography.titleLarge)
            Text(uiText(R.string.ui_your_bag_is_available_even_without_notifications), style = MaterialTheme.typography.bodyMedium)
            Text(uiText(R.string.ui_exact_alarms, if (exact) uiText(R.string.ui_allowed) else uiText(R.string.ui_permission_needed)))
            if (!exact) OutlinedButton(onExact, Modifier.fillMaxWidth()) { Text(uiText(R.string.ui_allow_exact_alarms)) }
            Text(uiText(R.string.ui_notifications, if (notifications) uiText(R.string.ui_allowed) else uiText(R.string.ui_permission_needed)))
            OutlinedButton(onNotifications, Modifier.fillMaxWidth()) { Text(uiText(R.string.ui_notification_settings)) }
            Text(uiText(R.string.ui_new_departures_use_a_quiet_nudge_by_default_you_can), style = MaterialTheme.typography.bodySmall)
            TextButton({ openSettings(context, Intent(Settings.ACTION_SOUND_SETTINGS)) }) { Text(uiText(R.string.ui_phone_volume_and_sounds)) }
        } }
        item { Panel {
            Text(uiText(R.string.ui_your_rhythm_your_departures), style = MaterialTheme.typography.titleLarge)
            Text(uiText(R.string.ui_in_my_departure_plan_an_appointment_for_work_swimming_or))
        } }
        item { Panel {
            Text(uiText(R.string.ui_your_notebook_stays_here), style = MaterialTheme.typography.titleLarge)
            Text(uiText(R.string.ui_no_account_ads_or_tracking_your_objects_and_departures_stay), style = MaterialTheme.typography.bodyMedium)
        } }
    }
}

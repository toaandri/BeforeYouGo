package com.toaandri.beforeyougo

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.toaandri.beforeyougo.ui.theme.BeforeYouGoTheme
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    private var afterPermission: (() -> Unit)? = null
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { afterPermission?.invoke() }
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); enableEdgeToEdge(); setContent {
        BeforeYouGoTheme { BeforeYouGoApp { action ->
            if (Build.VERSION.SDK_INT < 33 || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) action()
            else { afterPermission = action; notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS) }
        } }
    } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun BeforeYouGoApp(requestNotifications: ((() -> Unit) -> Unit)) {
    val context = LocalContext.current; val store = remember { AppStore(context) }
    val saved = remember { mutableStateListOf<SavedItem>().also { it.addAll(store.items()) } }
    var checks by remember { mutableStateOf(store.checksForToday()) }; var tab by remember { mutableIntStateOf(0) }
    var adding by remember { mutableStateOf(false) }; var editing by remember { mutableStateOf<SavedItem?>(null) }
    fun persist() = store.saveItems(saved)
    Scaffold(topBar = { TopAppBar(title = { Text("BeforeYouGo") }) }) { padding -> Column(Modifier.fillMaxSize().padding(padding)) {
        TabRow(selectedTabIndex = tab) { listOf("Aujourd’hui", "Mes objets", "Réglages").forEachIndexed { index, label -> Tab(tab == index, { tab = index }, text = { Text(label) }) } }
        when (tab) {
            0 -> TodayScreen(itemsForDay(saved, LocalDate.now()), checks, { item, checked ->
                checks = checks.toMutableMap().apply { put(item.id, checked) }; store.saveChecksForToday(checks)
                if (checked && !item.essential) { saved.remove(item); persist() }
            }, { adding = true })
            1 -> ItemsScreen(saved, { adding = true }, { editing = it }, { saved.remove(it); persist() })
            else -> SettingsScreen(store, requestNotifications)
        }
    } }
    if (adding) ItemDialog(today = LocalDate.now(), onDismiss = { adding = false }) { title, essential, date -> saved.add(SavedItem(System.currentTimeMillis(), title, essential, if (essential) null else date)); persist(); adding = false }
    editing?.let { item -> ItemDialog(item, LocalDate.now(), { editing = null }) { title, essential, date -> val index = saved.indexOfFirst { it.id == item.id }; saved[index] = item.copy(title = title, essential = essential, dueDate = if (essential) null else date); persist(); editing = null } }
}

@Composable private fun TodayScreen(items: List<SavedItem>, checks: Map<Long, Boolean>, onChecked: (SavedItem, Boolean) -> Unit, onAdd: () -> Unit) = Column(Modifier.padding(16.dp)) {
    Text("À prendre avant de partir", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text(LocalDate.now().toString()); Spacer(Modifier.height(12.dp))
    if (items.isEmpty()) { Text("Votre liste est vide. Ajoutez vos essentiels ou un rappel ponctuel."); Spacer(Modifier.height(12.dp)); Button(onClick = onAdd) { Text("Ajouter un objet") } }
    else LazyColumn { items(items, key = { it.id }) { item -> Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checks[item.id] == true, { onChecked(item, it) }); Column(Modifier.weight(1f)) { Text(item.title, style = MaterialTheme.typography.titleMedium); Text(if (item.essential) "Essentiel quotidien" else "À ne pas oublier (${item.dueDate})", style = MaterialTheme.typography.bodySmall) }
    }; HorizontalDivider() } }
}

@Composable private fun ItemsScreen(items: List<SavedItem>, onAdd: () -> Unit, onEdit: (SavedItem) -> Unit, onDelete: (SavedItem) -> Unit) = Column(Modifier.padding(16.dp)) {
    Button(onClick = onAdd) { Text("Ajouter") }; Spacer(Modifier.height(12.dp)); if (items.isEmpty()) Text("Aucun objet enregistré.")
    LazyColumn { items(items, key = { it.id }) { item -> Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(item.title, fontWeight = FontWeight.Bold); Text(if (item.essential) "Essentiel" else "Ponctuel : ${item.dueDate}") }; TextButton({ onEdit(item) }) { Text("Modifier") }; TextButton({ onDelete(item) }) { Text("Supprimer") }
    }; HorizontalDivider() } }
}

@Composable private fun SettingsScreen(store: AppStore, requestNotifications: ((() -> Unit) -> Unit)) {
    var hour by remember { mutableStateOf(store.reminderHour()) }; var status by remember { mutableStateOf(if (store.reminderEnabled()) "Rappel activé" else "Aucun rappel activé") }
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Text("Rappel du matin", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text("Heure au format 24 h, par exemple 08:00.")
        OutlinedTextField(hour, { hour = it }, label = { Text("Heure") }, singleLine = true)
        Button(onClick = { if (Regex("^([01]\\d|2[0-3]):[0-5]\\d$").matches(hour)) requestNotifications { store.saveReminder(hour, true); ReminderScheduler.schedule(store.context, hour); status = "Rappel quotidien activé à $hour" } else status = "Entrez une heure valide." }) { Text("Activer le rappel") }
        OutlinedButton(onClick = { store.saveReminder(hour, false); ReminderScheduler.cancel(store.context); status = "Rappel désactivé" }) { Text("Désactiver") }; Text(status); Text("Les données restent uniquement sur cet appareil.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable private fun ItemDialog(existing: SavedItem? = null, today: LocalDate, onDismiss: () -> Unit, onSave: (String, Boolean, String) -> Unit) {
    var title by remember { mutableStateOf(existing?.title.orEmpty()) }; var essential by remember { mutableStateOf(existing?.essential ?: true) }; var date by remember { mutableStateOf(existing?.dueDate ?: today.toString()) }; var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (existing == null) "Ajouter un objet" else "Modifier l’objet") }, text = { Column {
        OutlinedTextField(title, { title = it }, label = { Text("Nom") }, singleLine = true); Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(essential, { essential = it }); Text("Essentiel quotidien") }; if (!essential) OutlinedTextField(date, { date = it }, label = { Text("Date (AAAA-MM-JJ)") }, singleLine = true); error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    } }, confirmButton = { TextButton(onClick = { error = when { title.trim().isEmpty() -> "Saisissez un nom."; !essential && runCatching { LocalDate.parse(date) }.isFailure -> "La date doit être au format AAAA-MM-JJ."; else -> null }; if (error == null) onSave(title.trim(), essential, date) }) { Text("Enregistrer") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } })
}

package com.toaandri.beforeyougo

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import java.time.LocalDate

@Composable
fun ObjectsScreen(objects: List<SavedItem>, modifier: Modifier, onEdit: (SavedItem) -> Unit, onDelete: (SavedItem) -> Unit, onAdd: (CatalogObject) -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("Tous") }
    var deleting by remember { mutableStateOf<SavedItem?>(null) }
    val categories = listOf("Tous") + objectCatalog.map { it.category }.distinct()
    val filtered = objectCatalog.filter { (category == "Tous" || it.category == category) && normalized(it.name).contains(normalized(query)) }
    Column(modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Vos indispensables", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Une place pour chaque chose.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterChip(tab == 0, { tab = 0 }, label = { Text("Mes objets · ${objects.size}") })
                FilterChip(tab == 1, { tab = 1 }, label = { Text("Du quotidien") })
            }
            OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), label = { Text("Rechercher un objet") }, leadingIcon = { Icon(Icons.Outlined.Search, null) }, singleLine = true, shape = RoundedCornerShape(18.dp))
        }
        if (tab == 1) Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            categories.forEach { name -> FilterChip(category == name, { category = name }, label = { Text(name) }) }
        }
        LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (tab == 0) {
                val found = objects.filter { normalized(it.title).contains(normalized(query)) }
                if (found.isEmpty()) item { Panel { Text(if (objects.isEmpty()) "Votre collection vous attend." else "Aucun objet ne correspond.", fontWeight = FontWeight.Bold); Text("Choisissez des idées dans Du quotidien, ou utilisez le + pour créer un objet personnel.") } }
                items(found, key = { it.id }) { item ->
                    Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            ObjectBadge(iconFor(item)); Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f).clickable { onEdit(item) }) {
                                Text(item.title, fontWeight = FontWeight.Bold)
                                Text(if (item.essential) "Tous les jours" else "Le ${item.dueDate}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton({ onEdit(item) }) { Icon(Icons.Outlined.Edit, "Modifier ${item.title}") }
                            IconButton({ deleting = item }) { Icon(Icons.Outlined.DeleteOutline, "Supprimer ${item.title}") }
                        }
                    }
                }
            } else {
                item { Text("${filtered.size} idées pour vos routines", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                items(filtered.chunked(2)) { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        pair.forEach { idea ->
                            val added = objects.any { normalized(it.title) == normalized(idea.name) }
                            Card(onClick = { onAdd(idea) }, enabled = !added, modifier = Modifier.weight(1f), shape = RoundedCornerShape(22.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface, disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant, disabledContentColor = MaterialTheme.colorScheme.onSurface)) {
                                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    ObjectBadge(idea.icon)
                                    Text(idea.name, fontWeight = FontWeight.Bold)
                                    Text(if (added) "✓ Dans mes objets" else "Ajouter à ma liste", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
    deleting?.let { item -> AlertDialog(onDismissRequest = { deleting = null }, title = { Text("Supprimer cet objet ?") }, text = { Text("« ${item.title} » sera aussi retiré des checklists associées aux alarmes.") },
        confirmButton = { TextButton({ onDelete(item); deleting = null }) { Text("Supprimer") } }, dismissButton = { TextButton({ deleting = null }) { Text("Annuler") } }) }
}

@Composable
fun ObjectEditor(item: SavedItem, onDismiss: () -> Unit, onSave: (SavedItem) -> Unit) {
    val context = LocalContext.current
    var title by rememberSaveable(item.id) { mutableStateOf(item.title) }
    var essential by rememberSaveable(item.id) { mutableStateOf(item.essential) }
    var date by rememberSaveable(item.id) { mutableStateOf(item.dueDate ?: LocalDate.now().toString()) }
    var icon by rememberSaveable(item.id) { mutableStateOf(item.iconKey) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxWidth().heightIn(max = 640.dp).padding(20.dp)) {
                Text(if (item.title.isEmpty()) "Nouvel objet" else "Modifier l’objet", style = MaterialTheme.typography.titleLarge)
                LazyColumn(Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(vertical = 16.dp)) {
                    item { OutlinedTextField(title, { title = it.take(80) }, Modifier.fillMaxWidth(), label = { Text("Nom") }, singleLine = true) }
                    item { Row(verticalAlignment = Alignment.CenterVertically) { Text("Tous les jours", Modifier.weight(1f)); Switch(essential, { essential = it }) } }
                    if (!essential) item { OutlinedButton({
                        val day = runCatching { LocalDate.parse(date) }.getOrDefault(LocalDate.now())
                        DatePickerDialog(context, { _, y, m, d -> date = LocalDate.of(y, m + 1, d).toString() }, day.year, day.monthValue - 1, day.dayOfMonth).show()
                    }, Modifier.fillMaxWidth()) { Text("Date : $date") } }
                    item { Text("Une icône à votre image", fontWeight = FontWeight.Bold) }
                    items(objectCatalog.chunked(4)) { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            row.forEach { idea ->
                                FilledIconToggleButton(icon == idea.key, { icon = idea.key }) { Icon(idea.icon, idea.name) }
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onDismiss) { Text("Annuler") }
                    Button({ onSave(item.copy(title = title.trim(), essential = essential, dueDate = if (essential) null else date, iconKey = icon)) }, enabled = title.isNotBlank()) { Text("Enregistrer") }
                }
            }
        }
    }
}

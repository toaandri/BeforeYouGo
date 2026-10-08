package com.toaandri.beforeyougo

import android.app.DatePickerDialog
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
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
            Text("LES PETITES AFFAIRES", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, letterSpacing = 2.sp)
            Text("Tout un petit monde.", style = MaterialTheme.typography.headlineLarge, fontFamily = FontFamily.Serif)
            Text("Tes essentiels, tes envies, ce qui vient avec toi.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(tab == 0, { tab = 0 }, label = { Text("Ma collection · ${objects.size}") })
                FilterChip(tab == 1, { tab = 1 }, label = { Text("Piocher des idées") })
            }
            OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), label = { Text("Rechercher un objet") },
                leadingIcon = { Icon(Icons.Outlined.Search, null) },
                trailingIcon = { if (query.isNotEmpty()) IconButton({ query = "" }) { Icon(Icons.Outlined.Close, "Effacer la recherche") } },
                singleLine = true, shape = RoundedCornerShape(18.dp))
        }
        if (tab == 1) Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            categories.forEach { name -> FilterChip(category == name, { category = name }, label = { Text(name) }) }
        }
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (tab == 0) {
                val found = objects.filter { normalized(it.title).contains(normalized(query)) }
                if (found.isEmpty()) item {
                    Panel {
                        Icon(Icons.Outlined.AutoStories, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                        Text(if (objects.isEmpty()) "La première page est à toi." else "Rien sous ce nom, pour l’instant.",
                            style = MaterialTheme.typography.titleLarge, fontFamily = FontFamily.Serif)
                        Text(if (objects.isEmpty()) "Pioche quelques idées ou crée ton propre objet avec le +." else "Essaie un autre mot, ou ajoute cet objet avec le +.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (objects.isEmpty()) TextButton({ tab = 1 }) { Text("Explorer les idées") }
                    }
                }
                items(found, key = { it.id }) { item ->
                    Card(shape = RoundedCornerShape(topStart = 8.dp, topEnd = 22.dp, bottomEnd = 22.dp, bottomStart = 22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            ObjectBadge(iconFor(item))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f).clickable(onClickLabel = "Modifier ${item.title}") { onEdit(item) }, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(item.title, fontWeight = FontWeight.SemiBold)
                                Text(if (item.essential) "Au quotidien" else "À partir du ${friendlyObjectDate(item.dueDate)}",
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton({ onEdit(item) }) { Icon(Icons.Outlined.Edit, "Modifier ${item.title}") }
                            IconButton({ deleting = item }) { Icon(Icons.Outlined.DeleteOutline, "Supprimer ${item.title}", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                    }
                }
            } else {
                item { Text("${filtered.size} idées à faire tiennes", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                if (filtered.isEmpty()) item { Panel {
                    Text("Pas encore d’idée sous ce nom.", style = MaterialTheme.typography.titleLarge, fontFamily = FontFamily.Serif)
                    Text("Change de catégorie ou crée ton propre objet avec le +.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } }
                items(filtered.chunked(2)) { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        pair.forEach { idea ->
                            val added = objects.any { normalized(it.title) == normalized(idea.name) }
                            val cardColor by animateColorAsState(if (added) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface, label = "collectionCard")
                            Card(onClick = { onAdd(idea) }, enabled = !added, modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 24.dp, bottomEnd = 24.dp, bottomStart = 24.dp),
                                colors = CardDefaults.cardColors(containerColor = cardColor, disabledContainerColor = cardColor, disabledContentColor = MaterialTheme.colorScheme.onSecondaryContainer)) {
                                Column(Modifier.fillMaxWidth().animateContentSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        ObjectBadge(idea.icon)
                                        Icon(if (added) Icons.Outlined.Check else Icons.Outlined.Add, null, Modifier.size(19.dp), tint = MaterialTheme.colorScheme.primary)
                                    }
                                    Text(idea.name, fontWeight = FontWeight.SemiBold)
                                    Text(if (added) "✓ Dans ma collection" else "Je l’emporte", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
    deleting?.let { item ->
        AlertDialog(onDismissRequest = { deleting = null }, title = { Text("Retirer cet objet ?", fontFamily = FontFamily.Serif) },
            text = { Text("« ${item.title} » quittera ta collection et les sacs de tes départs.") },
            confirmButton = { TextButton({ onDelete(item); deleting = null }) { Text("Retirer") } },
            dismissButton = { TextButton({ deleting = null }) { Text("Garder") } })
    }
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
                Text(if (item.title.isEmpty()) "Une chose à emporter" else "Un objet à ta façon", style = MaterialTheme.typography.headlineSmall, fontFamily = FontFamily.Serif)
                LazyColumn(Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(vertical = 16.dp)) {
                    item {
                        OutlinedTextField(title, { title = it.take(80) }, Modifier.fillMaxWidth(), label = { Text("Son petit nom") },
                            placeholder = { Text("Mon carnet, le livre de Léa…") }, singleLine = true, shape = RoundedCornerShape(18.dp))
                    }
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Un compagnon du quotidien", fontWeight = FontWeight.SemiBold)
                                Text(if (essential) "Il retrouve sa place chaque jour." else "Pour une occasion particulière.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(essential, { essential = it })
                        }
                    }
                    if (!essential) item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton({
                                val day = runCatching { LocalDate.parse(date) }.getOrDefault(LocalDate.now())
                                DatePickerDialog(context, { _, y, m, d -> date = LocalDate.of(y, m + 1, d).toString() }, day.year, day.monthValue - 1, day.dayOfMonth).show()
                            }, Modifier.fillMaxWidth()) {
                                Icon(Icons.Outlined.CalendarToday, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("À partir du ${friendlyObjectDate(date)}")
                            }
                            Text("Il apparaîtra à partir de cette date et restera disponible jusqu’au lendemain de ta confirmation.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    item { Text("Le détail qui lui ressemble", style = MaterialTheme.typography.titleMedium, fontFamily = FontFamily.Serif) }
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
                    Button({ onSave(item.copy(title = title.trim(), essential = essential, dueDate = if (essential) null else date, iconKey = icon)) }, enabled = title.isNotBlank()) {
                        Text(if (item.title.isEmpty()) "Ajouter" else "Enregistrer")
                    }
                }
            }
        }
    }
}

private fun friendlyObjectDate(value: String?): String = runCatching {
    LocalDate.parse(value).format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.FRENCH))
}.getOrElse { value.orEmpty() }

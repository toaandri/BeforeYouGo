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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
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
    var category by rememberSaveable { mutableStateOf<Int?>(null) }
    var deleting by remember { mutableStateOf<SavedItem?>(null) }
    val categories = listOf<Int?>(null) + objectCatalog.map { it.categoryResource }.distinct()
    val filtered = objectCatalog.filter { (category == null || it.categoryResource == category) && normalized(it.name).contains(normalized(query)) }
    Column(modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(uiText(R.string.ui_the_little_things), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, letterSpacing = 2.sp)
            Text(uiText(R.string.ui_a_little_world_of_your_own), style = MaterialTheme.typography.headlineLarge, fontFamily = FontFamily.Serif)
            Text(uiText(R.string.ui_your_essentials_your_wishes_what_comes_with_you), color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(tab == 0, { tab = 0 }, label = { Text(uiText(R.string.ui_my_collection, objects.size)) })
                FilterChip(tab == 1, { tab = 1 }, label = { Text(uiText(R.string.ui_browse_ideas)) })
            }
            OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), label = { Text(uiText(R.string.ui_search_objects)) },
                leadingIcon = { Icon(Icons.Outlined.Search, null) },
                trailingIcon = { if (query.isNotEmpty()) IconButton({ query = "" }) { Icon(Icons.Outlined.Close, uiText(R.string.ui_clear_search)) } },
                singleLine = true, shape = RoundedCornerShape(18.dp))
        }
        if (tab == 1) Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            categories.forEach { name -> FilterChip(category == name, { category = name }, label = { Text(uiText(name ?: R.string.ui_all)) }) }
        }
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (tab == 0) {
                val found = objects.filter { normalized(it.title).contains(normalized(query)) }
                if (found.isEmpty()) item {
                    Panel {
                        Icon(Icons.Outlined.AutoStories, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                        Text(if (objects.isEmpty()) uiText(R.string.ui_the_first_page_is_yours) else uiText(R.string.ui_nothing_under_that_name_for_now),
                            style = MaterialTheme.typography.titleLarge, fontFamily = FontFamily.Serif)
                        Text(if (objects.isEmpty()) uiText(R.string.ui_browse_a_few_ideas_or_create_your_own_object_with) else uiText(R.string.ui_try_another_word_or_add_this_object_with), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (objects.isEmpty()) TextButton({ tab = 1 }) { Text(uiText(R.string.ui_explore_the_ideas)) }
                    }
                }
                items(found, key = { it.id }) { item ->
                    Card(shape = RoundedCornerShape(topStart = 8.dp, topEnd = 22.dp, bottomEnd = 22.dp, bottomStart = 22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            ObjectBadge(iconFor(item))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f).clickable(onClickLabel = uiText(R.string.ui_edit, item.title)) { onEdit(item) }, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(item.title, fontWeight = FontWeight.SemiBold)
                                Text(if (item.essential) uiText(R.string.ui_every_day_e8bdcc) else uiText(R.string.ui_from, friendlyObjectDate(item.dueDate)),
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton({ onEdit(item) }) { Icon(Icons.Outlined.Edit, uiText(R.string.ui_edit, item.title)) }
                            IconButton({ deleting = item }) { Icon(Icons.Outlined.DeleteOutline, uiText(R.string.ui_delete_b7c5a9, item.title), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                    }
                }
            } else {
                item { Text(uiText(R.string.ui_ideas_to_make_your_own, filtered.size), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                if (filtered.isEmpty()) item { Panel {
                    Text(uiText(R.string.ui_no_idea_under_that_name_yet), style = MaterialTheme.typography.titleLarge, fontFamily = FontFamily.Serif)
                    Text(uiText(R.string.ui_choose_another_category_or_create_your_own_object_with), color = MaterialTheme.colorScheme.onSurfaceVariant)
                } }
                items(filtered.chunked(2)) { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        pair.forEach { idea ->
                            val added = objects.any { idea.matchesTitle(it.title) }
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
                                    Text(if (added) uiText(R.string.ui_in_my_collection) else uiText(R.string.ui_i_ll_bring_it), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
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
        AlertDialog(onDismissRequest = { deleting = null }, title = { Text(uiText(R.string.ui_remove_this_object), fontFamily = FontFamily.Serif) },
            text = { Text(uiText(R.string.ui_will_leave_your_collection_and_departure_bags, item.title)) },
            confirmButton = { TextButton({ onDelete(item); deleting = null }) { Text(uiText(R.string.ui_remove)) } },
            dismissButton = { TextButton({ deleting = null }) { Text(uiText(R.string.ui_keep)) } })
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
                Text(if (item.title.isEmpty()) uiText(R.string.ui_something_to_bring) else uiText(R.string.ui_an_object_your_way), style = MaterialTheme.typography.headlineSmall, fontFamily = FontFamily.Serif)
                LazyColumn(Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(vertical = 16.dp)) {
                    item {
                        OutlinedTextField(title, { title = it.take(80) }, Modifier.fillMaxWidth(), label = { Text(uiText(R.string.ui_its_name)) },
                            placeholder = { Text(uiText(R.string.ui_my_notebook_lea_s_book)) }, singleLine = true, shape = RoundedCornerShape(18.dp))
                    }
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(uiText(R.string.ui_an_everyday_companion), fontWeight = FontWeight.SemiBold)
                                Text(if (essential) uiText(R.string.ui_it_finds_its_place_every_day) else uiText(R.string.ui_for_a_particular_occasion), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(essential, { essential = it }, Modifier.semantics { contentDescription = uiText(R.string.ui_an_everyday_companion) })
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
                                Text(uiText(R.string.ui_from_ccd57b, friendlyObjectDate(date)))
                            }
                            Text(uiText(R.string.ui_it_appears_from_this_date_and_stays_available_until_the), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    item { Text(uiText(R.string.ui_the_detail_that_suits_it), style = MaterialTheme.typography.titleMedium, fontFamily = FontFamily.Serif) }
                    items(objectCatalog.chunked(4)) { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            row.forEach { idea ->
                                FilledIconToggleButton(icon == idea.key, { icon = idea.key }) { Icon(idea.icon, idea.name) }
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onDismiss) { Text(uiText(R.string.ui_cancel)) }
                    Button({ onSave(item.copy(title = title.trim(), essential = essential, dueDate = if (essential) null else date, iconKey = icon)) }, enabled = title.isNotBlank()) {
                        Text(if (item.title.isEmpty()) uiText(R.string.ui_add) else uiText(R.string.ui_save))
                    }
                }
            }
        }
    }
}

private fun friendlyObjectDate(value: String?): String = runCatching {
    LocalDate.parse(value).format(DateTimeFormatter.ofPattern("d MMM yyyy", AppText.locale))
}.getOrElse { value.orEmpty() }

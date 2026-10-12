package com.toaandri.beforeyougo

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt

/** Checking is always an explicit, reversible choice. Dragging is an optional shortcut to tapping. */
@Composable
fun DepartureRitual(
    items: List<SavedItem>, checks: Map<Long, Boolean>,
    onCheck: (Long, Boolean) -> Unit, onAdd: () -> Unit, onFinish: () -> Unit,
    onDragPosition: (Offset?) -> Unit = {}
) {
    var bagBounds by remember { mutableStateOf(Rect.Zero) }
    var rootPosition by remember { mutableStateOf(Offset.Zero) }
    var dragging by remember { mutableStateOf<SavedItem?>(null) }
    var dragPosition by remember { mutableStateOf(Offset.Zero) }
    val checkItem by rememberUpdatedState(onCheck)
    val reportDrag by rememberUpdatedState(onDragPosition)
    val packed = items.count { checks[it.id] == true }
    val overBag = dragging != null && bagBounds.contains(dragPosition)
    val bagColor by animateColorAsState(
        if (overBag || (items.isNotEmpty() && packed == items.size)) MaterialTheme.colorScheme.secondaryContainer
        else MaterialTheme.colorScheme.primaryContainer, label = "bag color"
    )
    val bagScale by animateFloatAsState(if (overBag) 1.03f else 1f, label = "bag invitation")
    Box(Modifier.fillMaxWidth().onGloballyPositioned { rootPosition = it.positionInRoot() }) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Surface(
                modifier = Modifier.fillMaxWidth().testTag("ritual-bag")
                    .onGloballyPositioned { bagBounds = it.boundsInRoot() }
                    .graphicsLayer { scaleX = bagScale; scaleY = bagScale },
                shape = RoundedCornerShape(12.dp, 32.dp, 32.dp, 32.dp), color = bagColor
            ) {
                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    ToteIllustration(Modifier.size(82.dp, 100.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(if (overBag) uiText(R.string.ui_over_here) else uiText(R.string.ui_my_bag_for_today), style = MaterialTheme.typography.titleLarge)
                        Text(
                            if (items.isEmpty()) uiText(R.string.ui_there_is_room_for_what_you_want)
                            else if (packed == items.size) uiText(R.string.ui_everything_is_here)
                            else uiText(R.string.ui_of_on_board, packed, items.size),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                        )
                        if (packed > 0) Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            items.filter { checks[it.id] == true }.take(4).forEach {
                                Icon(iconFor(it), null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.secondary)
                            }
                            if (packed > 4) Text("+${packed - 4}", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
            if (items.isEmpty()) {
                Text(uiText(R.string.ui_a_departure_that_feels_like_you), style = MaterialTheme.typography.titleLarge)
                Text(uiText(R.string.ui_a_book_a_water_bottle_your_keys_choose_what_comes), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onAdd, Modifier.fillMaxWidth()) { Text(uiText(R.string.ui_choose_my_first_objects)) }
            } else {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(uiText(R.string.ui_to_bring_along), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    TextButton(onAdd) { Icon(Icons.Outlined.Add, null, Modifier.size(18.dp)); Text(uiText(R.string.ui_an_object)) }
                }
                Text(uiText(R.string.ui_tap_an_object_or_hold_it_to_drag_it_into),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                items.chunked(2).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { item ->
                            key(item.id) {
                                var tilePosition by remember { mutableStateOf(Offset.Zero) }
                                val checked = checks[item.id] == true
                                val tileColor by animateColorAsState(
                                    if (checked) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
                                    label = "packed item"
                                )
                                Surface(
                                    color = tileColor, shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier.weight(1f).testTag("pack-${item.id}")
                                        .onGloballyPositioned { tilePosition = it.positionInRoot() }
                                        .pointerInput(item.id, checked) {
                                            if (!checked) detectDragGesturesAfterLongPress(
                                                onDragStart = { offset -> dragging = item; dragPosition = tilePosition + offset; reportDrag(dragPosition) },
                                                onDrag = { change, amount -> change.consume(); dragPosition += amount; reportDrag(dragPosition) },
                                                onDragCancel = { dragging = null; reportDrag(null) },
                                                onDragEnd = {
                                                    if (bagBounds.contains(dragPosition)) checkItem(item.id, true)
                                                    dragging = null; reportDrag(null)
                                                }
                                            )
                                        }
                                        .toggleable(checked, role = Role.Checkbox) { checkItem(item.id, it) }
                                ) {
                                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Icon(iconFor(item), null, Modifier.size(32.dp), tint = if (checked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary)
                                            Icon(if (checked) Icons.Outlined.CheckCircle else Icons.Outlined.AddCircleOutline, null,
                                                Modifier.size(21.dp), tint = if (checked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline)
                                        }
                                        Text(item.title, style = MaterialTheme.typography.titleMedium)
                                        Text(if (checked) uiText(R.string.ui_in_the_bag_remove) else if (item.essential) uiText(R.string.ui_with_me) else uiText(R.string.ui_for_today),
                                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
                Button(onFinish, Modifier.fillMaxWidth().heightIn(min = 54.dp)) {
                    Text(uiText(R.string.ui_off_i_go))
                    Spacer(Modifier.width(10.dp))
                    Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, Modifier.size(20.dp))
                }
                Text(uiText(R.string.ui_your_bag_your_choices_leave_whenever_you_want), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        dragging?.let { item ->
            Surface(
                modifier = Modifier.offset { IntOffset((dragPosition.x - rootPosition.x).roundToInt() - 28.dp.roundToPx(),
                    (dragPosition.y - rootPosition.y).roundToInt() - 28.dp.roundToPx()) }.size(56.dp).zIndex(2f),
                shape = CircleShape, color = MaterialTheme.colorScheme.primary, shadowElevation = 6.dp
            ) { Box(contentAlignment = Alignment.Center) { Icon(iconFor(item), null, Modifier.size(30.dp), tint = MaterialTheme.colorScheme.onPrimary) } }
        }
    }
}

@Composable
private fun ToteIllustration(modifier: Modifier) {
    val stroke = MaterialTheme.colorScheme.primary
    val fill = MaterialTheme.colorScheme.surface
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val line = 2.3.dp.toPx()
        drawRoundRect(fill, Offset(w * .06f, h * .3f), Size(w * .88f, h * .66f), CornerRadius(12.dp.toPx()))
        drawRoundRect(stroke, Offset(w * .06f, h * .3f), Size(w * .88f, h * .66f), CornerRadius(12.dp.toPx()), style = Stroke(line))
        val handle = Path().apply {
            moveTo(w * .29f, h * .43f)
            lineTo(w * .29f, h * .23f)
            cubicTo(w * .29f, h * -.01f, w * .71f, h * -.01f, w * .71f, h * .23f)
            lineTo(w * .71f, h * .43f)
        }
        drawPath(handle, stroke, style = Stroke(line, cap = StrokeCap.Round))
        drawLine(stroke.copy(alpha = .4f), Offset(w * .29f, h * .72f), Offset(w * .71f, h * .72f), line, StrokeCap.Round)
        drawLine(stroke.copy(alpha = .4f), Offset(w * .39f, h * .8f), Offset(w * .61f, h * .8f), line, StrokeCap.Round)
    }
}

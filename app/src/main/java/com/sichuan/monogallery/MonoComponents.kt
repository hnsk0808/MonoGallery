package com.sichuan.monogallery

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gigamole.composescrollbars.Scrollbars
import com.gigamole.composescrollbars.ScrollbarsState
import com.gigamole.composescrollbars.config.ScrollbarsConfig
import com.gigamole.composescrollbars.config.ScrollbarsOrientation
import com.gigamole.composescrollbars.config.layercontenttype.ScrollbarsLayerContentType
import com.gigamole.composescrollbars.rememberScrollbarsState
import com.gigamole.composescrollbars.scrolltype.ScrollbarsScrollType

// ---------- Screen scaffolding ----------

/**
 * Shared screen scaffold: the light app background with a top bar showing [title] and, when
 * [onBack] is not null, a back button. A null [onBack] means the screen has no parent (the root
 * directory), so it gets no back button either.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonoScaffold(
    title: @Composable () -> Unit,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        containerColor = ColorBackground,
        topBar = {
            TopAppBar(
                title = title,
                navigationIcon = {
                    onBack?.let { back ->
                        IconButton(onClick = back) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回",
                            )
                        }
                    }
                },
                actions = actions,
            )
        },
        bottomBar = bottomBar,
        content = content,
    )
}

/** [MonoScaffold] with a plain text title. */
@Composable
fun MonoScaffold(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    MonoScaffold(
        title = { Text(title) },
        onBack = onBack,
        modifier = modifier,
        actions = actions,
        bottomBar = bottomBar,
        content = content,
    )
}

/**
 * Full-screen form for entering a single name: a text field, an optional [hint] below it and a
 * confirm button that stays disabled while the name is blank. Confirms with the trimmed name.
 */
@Composable
fun NameFormScreen(
    title: String,
    label: String,
    placeholder: String,
    confirmText: String,
    onBack: () -> Unit,
    onConfirm: (String) -> Unit,
    initialValue: String = "",
    hint: String? = null,
    confirmEnabled: Boolean = true,
) {
    var name by remember { mutableStateOf(initialValue) }

    MonoScaffold(title = title, onBack = onBack) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
        ) {
            TextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(label) },
                placeholder = { Text(placeholder) },
                singleLine = true,
            )
            hint?.let {
                Spacer(Modifier.height(8.dp))
                Text(text = it, fontSize = 13.sp, color = ColorTextSecondary)
            }
            Spacer(Modifier.height(16.dp))
            Button(
                // trim: drop leading/trailing whitespace from the entered name
                onClick = { onConfirm(name.trim()) },
                enabled = confirmEnabled && name.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(confirmText)
            }
        }
    }
}

/** Number of columns [MonoGrid] lays its cards out in. */
private const val MonoGridColumns = 2
/**
 * Standard two-column card grid shared by the directory content and the folder picker, with a
 * ComposeScrollbars scrollbar overlaid on its trailing edge.
 *
 * The scrollbar state observes the grid's own [androidx.compose.foundation.lazy.grid.LazyGridState],
 * so the thumb tracks the real scroll position instead of a separate proxy. A press that lands on the
 * thumb is claimed by [monoGridThumbDrag], which scrolls the grid so the thumb follows the finger;
 * everywhere else the overlay stays inert, so tapping, long-pressing and dragging the cards keeps
 * working.
 */
@Composable
fun MonoGrid(
    modifier: Modifier = Modifier,
    content: LazyGridScope.() -> Unit,
) {
    val gridState = rememberLazyGridState()
    val scrollbarsState = rememberScrollbarsState(
        config = remember {
            ScrollbarsConfig(
                orientation = ScrollbarsOrientation.Vertical,
                // Deliberately the idle-only colour type: IdleActive would switch to its active
                // colour whenever ScrollbarsScrollType.isScrollInProgress is true, i.e. on every
                // ordinary scroll, not just while the thumb is being dragged.
                knobLayerContentType = ScrollbarsLayerContentType.Default.Colored.Idle(
                    idleColor = ColorTextSecondary.copy(alpha = 0.5F),
                ),
            )
        },
        // Dynamic knob: a row's height varies with the tallest card it holds.
        scrollType = ScrollbarsScrollType.Lazy.Grid.Dynamic(
            state = gridState,
            spanCount = MonoGridColumns,
        ),
    )
    Box(modifier = modifier.fillMaxSize().monoGridThumbDrag(gridState, scrollbarsState)) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(MonoGridColumns),
            state = gridState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
        // Drawn above (Z-order) the grid: the library requires the scrollbars to overlay the content.
        Scrollbars(
            state = scrollbarsState,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

// ---------- Scrollbar dragging ----------

/**
 * Width of the trailing strip that owns the scrollbar gestures. It matches the grid's
 * `contentPadding` of 16.dp, so the strip sits entirely in the gutter left of the cards and never
 * steals a card press.
 */
private val ScrollbarHitWidth = 16.dp

/** Extra vertical slack, in dp, around the thumb when deciding whether a press grabbed it. */
private val ScrollbarTouchSlop = 12.dp

/**
 * Makes the scrollbar thumb draggable.
 *
 * ComposeScrollbars 1.0.4 has no way to move the thumb itself — both fractions are read-only and the
 * library derives them from [gridState] — so the drag works the other way round: the grid is scrolled
 * so that the thumb ends up under the finger. The thumb covers `knobFraction` of the track, hence
 * moving it by `dy` px means scrolling the content by `dy / knobFraction` px.
 *
 * The gesture is claimed in [PointerEventPass.Initial] from the grid's *parent*, because Compose
 * delivers overlapping siblings to the topmost one first but does not stop there: the grid would
 * otherwise keep scrolling 1:1 as well, and the thumb would run ahead of the finger by a factor of
 * `1 + knobFraction`. Consuming the press on the parent's initial pass keeps the grid's own drag
 * detector out of the gesture, and presses outside the thumb are left untouched.
 */
private fun Modifier.monoGridThumbDrag(
    gridState: LazyGridState,
    scrollbarsState: ScrollbarsState,
): Modifier = pointerInput(gridState, scrollbarsState) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        val knobFraction = (scrollbarsState.endKnobFraction - scrollbarsState.startKnobFraction)
            .coerceIn(0F, 1F)
        val slop = ScrollbarTouchSlop.toPx()
        val thumbTop = scrollbarsState.startKnobFraction * size.height - slop
        val thumbBottom = scrollbarsState.endKnobFraction * size.height + slop
        val isOnThumb = down.position.y in thumbTop..thumbBottom
        if (knobFraction <= 0F || knobFraction >= 1F ||
            down.position.x < size.width - ScrollbarHitWidth.toPx() || !isOnThumb
        ) {
            return@awaitEachGesture
        }
        down.consume()
        scrollbarsState.highlight()
        var lastY = down.position.y
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            if (!change.pressed) break
            val dy = change.position.y - lastY
            lastY = change.position.y
            change.consume()
            if (dy != 0F) {
                gridState.dispatchRawDelta(dy / knobFraction)
            }
        }
    }
}

// ---------- Dialogs ----------

/**
 * Single-line name entry dialog shared by "new folder" and "rename": confirms with the trimmed
 * value and keeps the confirm button disabled while the value is blank.
 */
@Composable
fun NameInputDialog(
    title: String,
    label: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    initialValue: String = "",
    placeholder: String? = null,
    confirmText: String = "确定",
) {
    var value by remember(initialValue) { mutableStateOf(initialValue) }
    val placeholderContent: (@Composable () -> Unit)? = placeholder?.let { text -> { Text(text) } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            TextField(
                value = value,
                onValueChange = { value = it },
                label = { Text(label) },
                placeholder = placeholderContent,
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(
                // trim: drop leading/trailing whitespace from the entered name
                onClick = { onConfirm(value.trim()) },
                enabled = value.isNotBlank(),
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

/** Plain confirmation dialog: a title, a message and confirm/cancel buttons. */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmText: String = "确定",
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirmText) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

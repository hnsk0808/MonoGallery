package com.sichuan.monogallery

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * The 本地图片 page: every image outside the library (see [scanLocalImages]) gathered into a single
 * flat grid — the folders those images live in are not reproduced.
 *
 * The page carries the same chrome as a library folder: a "⋮" menu with refresh and the three sort
 * modes (remembered in [SortPreference], separately from the library's own order) and cards whose
 * name region opens the usual context menu. It has no "new" action, though: these files belong to
 * the device rather than to the library, so nothing is created here.
 *
 * Multi-selection behaves like a library folder as well — the same [SelectionBottomBar] with
 * "添加到 / 删除 / 压缩" — and the two actions that touch the library reuse its screens: "添加到" is
 * [AddToFolderPicker] (the picker behind the library's copy/move sheet) and "压缩" is
 * [LocalImagesCompressScreen], which shares the library's compress form.
 *
 * A fullscreen image, the properties of one image and those two overlays are handled in place instead
 * of as routes: those files have no library id, and an absolute path does not belong in a navigation
 * route.
 */
@Composable
fun LocalImagesScreen(
    state: LocalImagesState,
    library: MonoLibrary,
    sortPreference: SortPreference,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()

    // Hoisted so the grid keeps its scroll position while the viewer or a properties page covers it
    val gridState = rememberLazyGridState()

    val selection = remember { LocalImageSelectionState() }

    // The image shown fullscreen, the image whose properties are open, and the two library overlays
    // ("添加到" and "压缩"); all inert while the grid is up
    var opened by remember { mutableStateOf<File?>(null) }
    var propertiesOf by remember { mutableStateOf<File?>(null) }
    var addingTo by remember { mutableStateOf(false) }
    var compressing by remember { mutableStateOf(false) }

    val selectedFiles = state.images.filter { it.absolutePath in selection.paths }

    // One handler for every layer this page puts over the grid — fullscreen image, image properties,
    // the two library overlays, multi-selection — so a back press closes that layer and returns to the
    // grid. Intercepting them one by one is what used to let the properties page fall through to the
    // navigation back stack and drop the user on the 本地 tab instead of the page they came from.
    BackHandler(
        enabled = opened != null || propertiesOf != null || addingTo || compressing || selection.mode,
    ) {
        when {
            opened != null -> opened = null
            propertiesOf != null -> propertiesOf = null
            addingTo -> addingTo = false
            compressing -> compressing = false
            else -> selection.exit()
        }
    }

    val image = opened
    val properties = propertiesOf
    when {
        image != null -> LocalImageViewer(file = image, onClose = { opened = null })

        properties != null -> LocalImagePropertiesScreen(file = properties, onBack = { propertiesOf = null })

        addingTo -> AddToFolderPicker(
            library = library,
            onBack = { addingTo = false },
            onConfirm = { targetFolderId, move ->
                // "移动" deletes the originals, so the page is re-scanned either way: after a move the
                // grid has to lose the pictures that are now library entries
                val files = selectedFiles
                library.importExternalFiles(files, targetFolderId, move)
                selection.exit()
                addingTo = false
                scope.launch { state.scan() }
            },
        )

        compressing -> LocalImagesCompressScreen(
            library = library,
            files = selectedFiles,
            onBack = { compressing = false },
            onDone = {
                compressing = false
                selection.exit()
            },
        )

        else -> LocalImagesGrid(
            state = state,
            sortPreference = sortPreference,
            gridState = gridState,
            selection = selection,
            onBack = onBack,
            onOpenImage = { opened = it },
            onOpenProperties = { propertiesOf = it },
            onAddTo = { addingTo = true },
            onCompress = { compressing = true },
        )
    }
}

/** The grid of local images, with the refresh and sort actions in the top bar. */
@Composable
private fun LocalImagesGrid(
    state: LocalImagesState,
    sortPreference: SortPreference,
    gridState: LazyGridState,
    selection: LocalImageSelectionState,
    onBack: () -> Unit,
    onOpenImage: (File) -> Unit,
    onOpenProperties: (File) -> Unit,
    onAddTo: () -> Unit,
    onCompress: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    MonoScaffold(
        title = LocalImagesFolderName,
        // While selecting, the back button leaves selection mode instead of the page, as in a folder
        onBack = { if (selection.mode) selection.exit() else onBack() },
        actions = {
            MoreMenu(
                onRefresh = { scope.launch { state.scan() } },
                sortMode = sortPreference.sortMode,
                onSortModeChange = { sortPreference.updateSortMode(it) },
            )
        },
        bottomBar = {
            if (selection.mode) {
                val selected = state.images.filter { it.absolutePath in selection.paths }
                // The library's own multi-selection bar, so both lists offer the same actions
                SelectionBottomBar(
                    count = selected.size,
                    onAddTo = onAddTo,
                    onCompress = onCompress,
                    onDelete = {
                        // Deleting the user's own pictures is permanent, so it only happens behind the
                        // confirm dialog inside the bar, and the list is re-scanned afterwards
                        selection.exit()
                        scope.launch {
                            withContext(Dispatchers.IO) { selected.forEach { it.delete() } }
                            state.scan()
                        }
                    },
                    deleteTitle = "删除图片",
                    deleteMessage = "确定删除选中的 ${selected.size} 张图片吗？图片会从设备上永久删除，此操作不可恢复。",
                )
            }
        },
    ) { innerPadding ->
        val sortMode = sortPreference.sortMode
        val images = remember(state.images, sortMode) { state.sortedImages(sortMode) }
        when {
            !state.hasScanned -> Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            images.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "暂无本地图片", color = ColorTextSecondary)
            }

            else -> MonoGrid(modifier = Modifier.padding(innerPadding), state = gridState) {
                items(images, key = { it.absolutePath }) { image ->
                    LocalImageCard(
                        file = image,
                        selected = image.absolutePath in selection.paths,
                        isSelecting = selection.mode,
                        // In selection mode a tap toggles the image; otherwise it opens it fullscreen
                        onClick = {
                            if (selection.mode) selection.toggle(image.absolutePath) else onOpenImage(image)
                        },
                        onLongClick = { selection.enter(image.absolutePath) },
                        onRename = { newName ->
                            // Swap the renamed file into the list instead of re-walking the storage
                            renameLocalImage(image, newName)?.let { renamed -> state.replaceImage(image, renamed) }
                        },
                        onProperties = { onOpenProperties(image) },
                        onOpenWith = { openFileWith(context, image, image.extension) },
                        onShare = { shareFile(context, image, image.extension) },
                    )
                }
            }
        }
    }
}

/**
 * Fullscreen viewer for one local image: the shared overlay chrome and image viewer in place, since
 * a file outside the library has no library id to route to. The overlay's own back button routes
 * through the activity dispatcher, which [LocalImagesScreen]'s handler picks up.
 */
@Composable
private fun LocalImageViewer(
    file: File,
    onClose: () -> Unit,
) {
    var chromeVisible by remember { mutableStateOf(true) }

    FullscreenPreviewScreen(
        title = file.name,
        onBack = onClose,
        chromeVisible = chromeVisible,
        onChromeVisibleChange = { chromeVisible = it },
    ) {
        ImageViewer(
            file = file,
            modifier = Modifier.fillMaxSize(),
            onTap = { chromeVisible = !chromeVisible },
        )
    }
}

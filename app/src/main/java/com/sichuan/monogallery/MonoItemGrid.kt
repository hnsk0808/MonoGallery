package com.sichuan.monogallery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * Reusable two-column grid of folders and files under [parentFolderId] (a null id means the
 * root directory). Renders the empty state with empty text when there is nothing to show,
 * and wires every card's click / long-press / rename / properties / share actions.
 * Folders are listed before files; both are ordered by [sortMode]. The [modifier] is used to
 * align the grid with the scaffold insets (such as its inner padding).
 */
@Composable
fun MonoItemGrid(
    library: MonoLibrary,
    parentFolderId: Long?,
    selection: FileSelectionState,
    sortMode: SortMode,
    onOpenFolder: (Folder) -> Unit,
    onOpenFile: (MonoFile) -> Unit,
    onOpenFolderProperties: (Long) -> Unit,
    onOpenFileProperties: (Long) -> Unit,
    onShareFolder: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val folders = library.sortedFolders(parentFolderId, sortMode)
    val files = library.sortedFiles(parentFolderId, sortMode)

    if (folders.isEmpty() && files.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "暂无内容，点右上角 + 新建", color = ColorTextSecondary)
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // In multi-selection mode a tap toggles the item; otherwise it opens it.
            items(folders, key = { it.id }) { folder ->
                SelectableFolderCard(
                    folder = folder,
                    itemCount = library.itemCount(folder.id),
                    selected = folder.id in selection.ids,
                    isSelecting = selection.mode,
                    onClick = {
                        if (selection.mode) selection.toggle(folder.id) else onOpenFolder(folder)
                    },
                    onLongClick = { selection.enter(folder.id) },
                    onRename = { library.renameFolder(folder.id, it) },
                    onProperties = { onOpenFolderProperties(folder.id) },
                    onShare = { onShareFolder(folder.id) },
                )
            }
            items(files, key = { it.id }) { file ->
                SelectableFileCard(
                    file = file,
                    thumbnailFile = library.fileOnDisk(file.id),
                    selected = file.id in selection.ids,
                    isSelecting = selection.mode,
                    onClick = {
                        if (selection.mode) selection.toggle(file.id) else onOpenFile(file)
                    },
                    onLongClick = { selection.enter(file.id) },
                    onOpenWith = {
                        library.fileOnDisk(file.id)?.let { openFileWith(context, it, file.extension) }
                    },
                    onRename = { library.renameFile(file.id, it) },
                    onProperties = { onOpenFileProperties(file.id) },
                    onShare = { library.fileOnDisk(file.id)?.let { shareFile(context, it, file.extension) } },
                )
            }
        }
    }
}

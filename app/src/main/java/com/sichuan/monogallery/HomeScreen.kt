package com.sichuan.monogallery

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Home screen: lists the folders and files in the root directory, with multi-selection support (add to folder / delete / compress / copy to clipboard). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    library: MonoLibrary,
    onOpenFolder: (Folder) -> Unit,
    onOpenFile: (MonoFile) -> Unit,
    onNewFolder: () -> Unit,
    onNewFile: () -> Unit,
    onRefresh: () -> Unit,
    onAddToFolder: (Set<Long>) -> Unit,
    onCompress: (Set<Long>) -> Unit,
    onOpenFolderProperties: (Long) -> Unit,
    onOpenFileProperties: (Long) -> Unit,
) {
    val selection = remember { FileSelectionState() }
    val context = LocalContext.current
    var sharingFolderId by remember { mutableStateOf<Long?>(null) }

    // Back button exits multi-selection mode instead of leaving the screen
    BackHandler(enabled = selection.mode) { selection.exit() }

    sharingFolderId?.let { id ->
        FolderShareProgressDialog(library = library, folderId = id, onDone = { sharingFolderId = null })
    }

    Scaffold(
        containerColor = ColorBackground,
        topBar = { HomeTopBar(onNewFolder = onNewFolder, onNewFile = onNewFile, onRefresh = onRefresh) },
        bottomBar = {
            if (selection.mode) {
                SelectionBottomBar(
                    library = library,
                    selection = selection,
                    onAddTo = { onAddToFolder(it) },
                    onCompress = { onCompress(it) },
                    onDelete = { fileIds, folderIds ->
                        library.deleteItems(fileIds, folderIds)
                        selection.exit()
                    },
                )
            }
        },
    ) { innerPadding ->
        if (library.rootFolders().isEmpty() && library.rootFiles().isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "点击 + 新建文件夹或文件", color = ColorTextSecondary)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // In multi-selection mode a tap toggles the item; otherwise it opens it.
                items(library.rootFolders(), key = { it.id }) { folder ->
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
                        onShare = { sharingFolderId = folder.id },
                    )
                }
                items(library.rootFiles(), key = { it.id }) { file ->
                    SelectableFileCard(
                        file = file,
                        thumbnailFile = library.fileOnDisk(file.id),
                        selected = file.id in selection.ids,
                        isSelecting = selection.mode,
                        onClick = {
                            if (selection.mode) selection.toggle(file.id) else onOpenFile(file)
                        },
                        onLongClick = { selection.enter(file.id) },
                        onRename = { library.renameFile(file.id, it) },
                        onProperties = { onOpenFileProperties(file.id) },
                        onShare = { library.fileOnDisk(file.id)?.let { shareFile(context, it, file.extension) } },
                    )
                }
            }
        }
    }
}

/** Top app bar for the home screen: app title and the add and overflow menus. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopBar(
    onNewFolder: () -> Unit,
    onNewFile: () -> Unit,
    onRefresh: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ColorBackground)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Mono",
            modifier = Modifier.weight(1f),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = ColorTextPrimary,
        )

        AddMenu(onNewFolder = onNewFolder, onNewFile = onNewFile)

        MoreMenu(onRefresh = onRefresh)
    }
}

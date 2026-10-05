package com.sichuan.monogallery

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/** Folder content screen: shows subfolders and files, with the same multi-selection actions and "+" new menu as the home screen. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderContentScreen(
    library: MonoLibrary,
    folderId: Long,
    onBack: () -> Unit,
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
    val folder = library.folder(folderId)
    val selection = remember { FileSelectionState() }
    val context = LocalContext.current
    var sharingFolderId by remember { mutableStateOf<Long?>(null) }
    var sortMode by remember { mutableStateOf(SortMode.NAME) }
    val subfolders = library.sortedFolders(folderId, sortMode)
    val files = library.sortedFiles(folderId, sortMode)

    BackHandler(enabled = selection.mode) { selection.exit() }

    sharingFolderId?.let { id ->
        FolderShareProgressDialog(library = library, folderId = id, onDone = { sharingFolderId = null })
    }

    Scaffold(
        containerColor = ColorBackground,
        topBar = {
            TopAppBar(
                title = { Text(folder?.name ?: "文件夹") },
                navigationIcon = {
                    IconButton(onClick = { if (selection.mode) selection.exit() else onBack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                        )
                    }
                },
                actions = {
                    AddMenu(onNewFolder = onNewFolder, onNewFile = onNewFile)
                    MoreMenu(
                        onRefresh = onRefresh,
                        sortMode = sortMode,
                        onSortModeChange = { sortMode = it },
                    )
                },
            )
        },
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
        if (subfolders.isEmpty() && files.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "暂无内容，点右上角 + 新建", color = ColorTextSecondary)
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
                items(subfolders, key = { it.id }) { subfolder ->
                    SelectableFolderCard(
                        folder = subfolder,
                        itemCount = library.itemCount(subfolder.id),
                        selected = subfolder.id in selection.ids,
                        isSelecting = selection.mode,
                        onClick = {
                            if (selection.mode) selection.toggle(subfolder.id) else onOpenFolder(subfolder)
                        },
                        onLongClick = { selection.enter(subfolder.id) },
                        onRename = { library.renameFolder(subfolder.id, it) },
                        onProperties = { onOpenFolderProperties(subfolder.id) },
                        onShare = { sharingFolderId = subfolder.id },
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
                        onRename = { library.renameFile(file.id, it) },
                        onProperties = { onOpenFileProperties(file.id) },
                        onShare = { library.fileOnDisk(file.id)?.let { shareFile(context, it, file.extension) } },
                    )
                }
            }
        }
    }
}

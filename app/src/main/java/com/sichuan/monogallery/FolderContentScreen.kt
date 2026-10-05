package com.sichuan.monogallery

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Modifier

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
    var sharingFolderId by remember { mutableStateOf<Long?>(null) }
    var sortMode by remember { mutableStateOf(SortMode.NAME) }

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
        MonoItemGrid(
            library = library,
            parentFolderId = folderId,
            selection = selection,
            sortMode = sortMode,
            onOpenFolder = onOpenFolder,
            onOpenFile = onOpenFile,
            onOpenFolderProperties = onOpenFolderProperties,
            onOpenFileProperties = onOpenFileProperties,
            onShareFolder = { sharingFolderId = it },
            modifier = Modifier.padding(innerPadding),
        )
    }
}

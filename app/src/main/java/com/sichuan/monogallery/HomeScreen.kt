package com.sichuan.monogallery

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
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
    var sharingFolderId by remember { mutableStateOf<Long?>(null) }
    var sortMode by remember { mutableStateOf(SortMode.NAME) }

    // Back button exits multi-selection mode instead of leaving the screen
    BackHandler(enabled = selection.mode) { selection.exit() }

    sharingFolderId?.let { id ->
        FolderShareProgressDialog(library = library, folderId = id, onDone = { sharingFolderId = null })
    }

    Scaffold(
        containerColor = ColorBackground,
        topBar = {
            HomeTopBar(
                library = library,
                onNewFolder = onNewFolder,
                onNewFile = onNewFile,
                onRefresh = onRefresh,
                sortMode = sortMode,
                onSortModeChange = { sortMode = it },
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
            parentFolderId = null,
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

/** Top app bar for the home screen: app title and the add and overflow menus. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopBar(
    library: MonoLibrary,
    onNewFolder: () -> Unit,
    onNewFile: () -> Unit,
    onRefresh: () -> Unit,
    sortMode: SortMode,
    onSortModeChange: (SortMode) -> Unit,
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

        AddMenu(
            library = library,
            parentFolderId = null,
            onNewFolder = onNewFolder,
            onNewFile = onNewFile,
        )

        MoreMenu(
            onRefresh = onRefresh,
            sortMode = sortMode,
            onSortModeChange = onSortModeChange,
        )
    }
}

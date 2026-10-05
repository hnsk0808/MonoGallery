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

/** Title of the home screen, which is the root directory. */
private const val APP_TITLE = "Mono"

/**
 * Directory content screen: shows the subfolders and files of one directory, with the same
 * multi-selection actions and "+" new menu everywhere.
 *
 * The home screen is the special case of the root directory: a null [folderId] means the root
 * (the add menu and the grid then work on the root items), and a null [onBack] means there is no
 * parent directory to return to, which hides the back button and shows [APP_TITLE] as the title
 * instead of a folder name.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderContentScreen(
    library: MonoLibrary,
    folderId: Long?,
    onBack: (() -> Unit)?,
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
    val folder = folderId?.let { library.folder(it) }
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
            TopAppBar(
                title = { Text(folder?.name ?: if (folderId == null) APP_TITLE else "文件夹") },
                navigationIcon = {
                    // The root directory has no parent, so it has no back button either
                    onBack?.let { back ->
                        IconButton(onClick = { if (selection.mode) selection.exit() else back() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回",
                            )
                        }
                    }
                },
                actions = {
                    AddMenu(
                        library = library,
                        parentFolderId = folderId,
                        onNewFolder = onNewFolder,
                        onNewFile = onNewFile,
                    )
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

package com.sichuan.monogallery

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
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
 *
 * The sort order is read from and written to [sortPreference], so it is shared by every directory
 * screen and survives navigating away and restarting the app.
 */
@Composable
fun FolderContentScreen(
    library: MonoLibrary,
    sortPreference: SortPreference,
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
    // The sort mode lives in the shared, persisted SortPreference (not in local remember state),
    // so entering another folder keeps the order the user picked
    val sortMode = sortPreference.sortMode

    // Back button exits multi-selection mode instead of leaving the screen
    BackHandler(enabled = selection.mode) { selection.exit() }

    sharingFolderId?.let { id ->
        FolderShareProgressDialog(library = library, folderId = id, onDone = { sharingFolderId = null })
    }

    MonoScaffold(
        title = { Text(folder?.name ?: if (folderId == null) APP_TITLE else "文件夹") },
        // A null onBack (the root directory) hides the back button; while selecting, the back
        // button leaves selection mode instead of the screen.
        onBack = onBack?.let { back -> { if (selection.mode) selection.exit() else back() } },
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
                onSortModeChange = { sortPreference.updateSortMode(it) },
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
